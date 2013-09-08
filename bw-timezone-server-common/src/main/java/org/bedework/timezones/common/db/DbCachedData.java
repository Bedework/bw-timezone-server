/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.timezones.common.db;

import org.bedework.timezones.common.AbstractCachedData;
import org.bedework.timezones.common.CachedData;
import org.bedework.timezones.common.Differ.DiffListEntry;
import org.bedework.timezones.common.Stat;
import org.bedework.timezones.common.TzConfig;
import org.bedework.timezones.common.TzException;
import org.bedework.timezones.common.TzServerUtil;
import org.bedework.timezones.common.ZipCachedData;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.timezones.DateTimeUtil;
import org.bedework.util.timezones.Timezones;
import org.bedework.util.timezones.Timezones.TaggedTimeZone;
import org.bedework.util.timezones.TimezonesImpl;
import org.bedework.util.timezones.TzUnknownHostException;
import org.bedework.util.timezones.model.LocalNameType;
import org.bedework.util.timezones.model.TimezoneListType;
import org.bedework.util.timezones.model.TimezoneType;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** Cached timezone data in a database.
 *
 * @author douglm
 */
public class DbCachedData extends AbstractCachedData {
  private boolean running;

  /** Current hibernate session - exists only across one user interaction
   */
  protected HibSession sess;

  private static SessionFactory sessionFactory;

  /** */
  protected boolean open;

  private long reloads;
  private long primaryFetches;
  private long lastFetchCt;
  private String lastFetchStatus = "None";

  private class UpdateThread extends Thread {
    boolean showedTrace;

    /**
     * @param name - for the thread
     */
    public UpdateThread(final String name) {
      super(name);
    }

    @Override
    public void run() {
      while (running) {
        long refreshWait = 9999;

        try {
          open();
          refreshWait = cfg.getRefreshDelay();

          if (debug) {
            trace("Updater: About to update");
          }

          if (!updateFromPrimary()) {
            // Try again in at most 10 minutes (need an error retry param)
            refreshWait = Math.min(refreshWait, 600);
          }
        } catch (Throwable t) {
          if (!showedTrace) {
            error(t);
            showedTrace = true;
          } else {
            error(t.getMessage());
          }

          try {
            fail();
          } catch (Throwable t1) {
          }
        } finally {
          try {
            close();
          } catch (Throwable t1) {
          }
        }

        if (debug) {
          trace("Updater: About to wait for " +
              refreshWait +
                " seconds");

        }

        if (!running) {
          break;
        }

        // Hang around
        try {
          Object o = new Object();
          synchronized (o) {
            o.wait(refreshWait * 1000);
          }
        } catch (InterruptedException ie) {
          if (debug) {
            trace("Updater: Interrupted ");
          }
        } catch (Throwable t) {
          error(t.getMessage());
        }
      }
    }
  }

  private UpdateThread updater;

  /** Start from database cache. Fall back is probably to use the
   * zipped data.
   *
   * @param cfg
   * @throws TzException
   */
  public DbCachedData(final TzConfig cfg) throws TzException {
    super(cfg, "Db");

    loadData();

    running = true;

    if (!cfg.getPrimaryServer()) {
      updater = new UpdateThread("DbdataUpdater");
      updater.start();
    }
  }

  @Override
  public void stop() throws TzException {
    running = false;

    if (!cfg.getPrimaryServer()) {
      if (updater == null) {
        error("Already stopped");
        return;
      }

      updater.interrupt();
      updater = null;

      info("************************************************************");
      info(" * TZdb cache updater terminated ");
      info("************************************************************");
    }
  }

  @Override
  public List<Stat> getStats() throws TzException {
    List<Stat> stats = new ArrayList<Stat>();

    stats.addAll(super.getStats());

    stats.add(new Stat("Db reloads", String.valueOf(reloads)));
    stats.add(new Stat("Db primary fetches", String.valueOf(primaryFetches)));
    stats.add(new Stat("Db last fetch count",
                       String.valueOf(lastFetchCt)));
    stats.add(new Stat("Db last fetch status", lastFetchStatus));

    return stats;
  }

  /**
   * @throws TzException
   */
  @Override
  public void checkData() throws TzException {
    if (updater != null) {
      updater.interrupt();
    }
  }

  @Override
  public void updateData(final String dtstamp,
                         final List<DiffListEntry> dles) throws TzException {
    if (Util.isEmpty(dles)) {
      return;
    }

    try {
      open();

      AliasMaps amaps = buildAliasMaps();

      for (DiffListEntry dle: dles) {
        updateFromDiffEntry(dtstamp, amaps, dle);
      }

      cfg.setDtstamp(dtstamp);

      TzServerUtil.saveConfig();
    } catch (TzException te) {
      fail();
      throw te;
    } catch (Throwable t) {
      fail();
      throw new TzException(t);
    } finally {
      close();
    }
  }

  @Override
  public List<String> findIds(final String val) throws TzException {
    try {
      open();

      List<String> ids = new ArrayList<String>();

      ids.addAll(findTzs(val));

      List<TzAlias> as = findTzAliases(val);
      for (TzAlias a: as) {
        ids.add(a.getToId());
      }

      return ids;
    } catch (TzException te) {
      fail();
      throw te;
    } catch (Throwable t) {
      fail();
      throw new TzException(t);
    } finally {
      close();
    }
  }

  /* ====================================================================
   *                   DbCachedData methods
   * ==================================================================== */

  /**
   * @param val
   * @throws TzException
   */
  public void addTzAlias(final TzAlias val) throws TzException {
    sess.save(val);
  }

  /**
   * @param val
   * @throws TzException
   */
  public void removeTzAlias(final TzAlias val) throws TzException {
    sess.delete(val);
  }

  private final String getTzAliasQuery =
      "from " + TzAlias.class.getName() + " alias where alias.fromId=:id";

  /**
   * @param val
   * @return alias entry
   * @throws TzException
   */
  public TzAlias getTzAlias(final String val) throws TzException {
    sess.createQuery(getTzAliasQuery);

    sess.setString("id", val);

    return (TzAlias)sess.getUnique();
  }

  private final String findTzAliasesQuery =
      "from " + TzAlias.class.getName() + " alias where alias.fromId like :id";

  /**
   * @param val
   * @return matching alias entries
   * @throws TzException
   */
  @SuppressWarnings("unchecked")
  public List<TzAlias> findTzAliases(final String val) throws TzException {
    sess.createQuery(findTzAliasesQuery);

    sess.setString("id", "%" + val + "%");

    return sess.getList();
  }

  private final String findTzsQuery =
      "select spec.name from " + TzDbSpec.class.getName() + " spec where spec.name like :name";

  /**
   * @param val
   * @return matching tz entry names
   * @throws TzException
   */
  @SuppressWarnings("unchecked")
  public List<String> findTzs(final String val) throws TzException {
    sess.createQuery(findTzsQuery);

    sess.setString("name", "%" + val + "%");

    return sess.getList();
  }

  /**
   * @param val
   * @throws TzException
   */
  public void addTzSpec(final TzDbSpec val) throws TzException {
    sess.save(val);
  }

  /**
   * @param val
   * @throws TzException
   */
  public void updateTzSpec(final TzDbSpec val) throws TzException {
    sess.save(val);
  }

  /* ====================================================================
   *                   Transaction methods
   * ==================================================================== */

  private void open() throws TzException {
    if (isOpen()) {
      return;
    }
    openSession();
    open = true;
  }

  private void close() {
    try {
      endTransaction();
    } catch (TzException wde) {
      try {
        rollbackTransaction();
      } catch (TzException wde1) {}

      getLogger().error("failed close", wde);
    } finally {
      try {
        closeSession();
      } catch (TzException wde1) {}
    }
  }

  private void fail() {
    try {
      rollbackTransaction();
    } catch (TzException wde) {
      getLogger().error("failed fail", wde);
    }
  }

  private boolean isOpen() {
    return open;
  }

  /* ====================================================================
   *                   Session methods
   * ==================================================================== */

  protected void checkOpen() throws TzException {
    if (!isOpen()) {
      throw new TzException("Session call when closed");
    }
  }

  protected synchronized void openSession() throws TzException {
    if (isOpen()) {
      throw new TzException("Already open");
    }

    open = true;

    if (sess != null) {
      warn("Session is not null. Will close");
      try {
        close();
      } finally {
      }
    }

    if (sess == null) {
      if (debug) {
        trace("New hibernate session for " + objTimestamp);
      }
      sess = new HibSessionImpl();
      sess.init(getSessionFactory(), getLogger());
      trace("Open session for " + objTimestamp);
    }

    beginTransaction();
  }

  protected synchronized void closeSession() throws TzException {
    if (!isOpen()) {
      if (debug) {
        trace("Close for " + objTimestamp + " closed session");
      }
      return;
    }

    if (debug) {
      trace("Close for " + objTimestamp);
    }

    try {
      if (sess != null) {
        if (sess.rolledback()) {
          sess = null;
          return;
        }

        if (sess.transactionStarted()) {
          sess.rollback();
        }
//        sess.disconnect();
        sess.close();
        sess = null;
      }
    } catch (Throwable t) {
      try {
        sess.close();
      } catch (Throwable t1) {}
      sess = null; // Discard on error
    } finally {
      open = false;
    }
  }

  protected void beginTransaction() throws TzException {
    checkOpen();

    if (debug) {
      trace("Begin transaction for " + objTimestamp);
    }
    sess.beginTransaction();
  }

  protected void endTransaction() throws TzException {
    checkOpen();

    if (debug) {
      trace("End transaction for " + objTimestamp);
    }

    if (!sess.rolledback()) {
      sess.commit();
    }
  }

  protected void rollbackTransaction() throws TzException {
    checkOpen();
    sess.rollback();
  }

  /* ====================================================================
   *                   private methods
   * ==================================================================== */

  /**
   * @throws TzException
   */
  private synchronized void loadData() throws TzException {
    reloads++;

    try {
      open();

      if (!cfg.getPrimaryServer()) {
        updateFromPrimary();
      } else {
        loadInitialData();
      }

      dtstamp = cfg.getDtstamp();

      TzServerUtil.lastDataFetch = System.currentTimeMillis();

      /* ===================== Rebuild the alias maps ======================= */

      aliasMaps = buildAliasMaps();

      /* ===================== All tzs into the table ======================= */

      processSpecs(dtstamp);

      expansions.clear();
    } catch (TzException te) {
      fail();
      throw te;
    } catch (Throwable t) {
      fail();
      throw new TzException(t);
    } finally {
      close();
    }
  }

  /** Call the primary server and get a list of data that's changed since we last
   * looked. Then fetch each changed timezone and update the db.
   *
   * <p>Db is already open.
   *
   * @return true if we successfully contacted the server
   * @throws TzException
   */
  private synchronized boolean updateFromPrimary() throws TzException {
    if (debug) {
      trace("Updating from primary");
    }

    try {
      if (cfg.getPrimaryServer()) {
        // We are a primary. No update needed
        if (debug) {
          trace("We are a primary: exit");
        }

        return true; // good enough
      }

      if (cfg.getPrimaryUrl() == null) {
        warn("No primary URL: exit");

        return true; // good enough
      }

      Timezones tzs = new TimezonesImpl();
      tzs.init(cfg.getPrimaryUrl());

      String changedSince = cfg.getDtstamp();

      TimezoneListType tzl;

      try {
        tzl = tzs.getList(changedSince);
      } catch (TzUnknownHostException tuhe) {
        error("Unknown host exception contacting " + cfg.getPrimaryUrl());
        return false;
      }

      String svrCs = tzl.getDtstamp();

      if ((changedSince == null) ||
                 !svrCs.equals(changedSince)) {
        cfg.setDtstamp(svrCs);

        TzServerUtil.saveConfig();
      }

      primaryFetches++;
      lastFetchCt = tzl.getTimezones().size();

      String isAre = "are";
      String theS = "s";

      if (lastFetchCt == 1) {
        isAre = "is";
        theS = "";
      }

      info("There " + isAre + " " + lastFetchCt +
      		 " timezone" + theS + " to fetch");

      /* Go through the returned timezones and try to update.
       * If we have the timezone and it has an etag do a conditional fetch.
       * If we don't have the timezone do an unconditional fetch.
       */

      AliasMaps amaps = buildAliasMaps();

      for (TimezoneType sum: tzl.getTimezones()) {
        String id = sum.getTzid();
        if (debug) {
          trace("Updating timezone " + id);
        }

        TzDbSpec dbspec = getSpec(id);

        String etag = null;
        if (dbspec != null) {
          etag = dbspec.getEtag();
        }

        TaggedTimeZone ttz = tzs.getTimeZone(id, etag);

        if ((ttz != null) && (ttz.vtz == null)) {
          // No change
          continue;
        }

        if (ttz == null) {
          warn("Received timezone id " + id + " but not available.");
          continue;
        }

        boolean add = dbspec == null;

        if (add) {
          // Create a new one
          dbspec = new TzDbSpec();
        }

        dbspec.setName(id);
        dbspec.setEtag(ttz.etag);
        dbspec.setDtstamp(DateTimeUtil.rfcDateTimeUTC(
                sum.getLastModified()));
        dbspec.setSource(cfg.getPrimaryUrl());
        dbspec.setActive(true);
        dbspec.setVtimezone(ttz.vtz);

        if (!Util.isEmpty(sum.getLocalNames())) {
          Set<LocalizedString> dns = new TreeSet<LocalizedString>();

          if (add) {
            dns = new TreeSet<LocalizedString>();
            dbspec.setDisplayNames(dns);
          } else {
            dns = dbspec.getDisplayNames();
            dns.clear(); // XXX not good - forces delete and recreate
          }

          for (LocalNameType ln: sum.getLocalNames()) {
            LocalizedString ls = new LocalizedString(ln.getLang(), ln.getValue());

            dns.add(ls);
          }
        }

        if (add) {
          addTzSpec(dbspec);
        } else {
          updateTzSpec(dbspec);
        }

        SortedSet<String> aliases = amaps.byTzid.get(id);

        if (!Util.isEmpty(sum.getAliases())) {
          for (String a: sum.getAliases()) {
            String alias = amaps.byAlias.get(a);

            if (alias == null) {
              TzAlias tza = new TzAlias();

              tza.setFromId(a);
              tza.setToId(id);

              addTzAlias(tza);

              continue;
            }

            if (aliases != null) {
              aliases.remove(a);
            }
          }
        }

        if (aliases != null) {
          /* remaining aliases should be deleted */
          for (String alias: aliases) {
            TzAlias tza = getTzAlias(alias);
            removeTzAlias(tza);
          }
        }
      }

      lastFetchStatus = "Success";
    } catch (TzException tze) {
      lastFetchStatus = "Failed";
      throw tze;
    } catch (Throwable t) {
      lastFetchStatus = "Failed";
      throw new TzException(t);
    }

    return true;
  }

  private void updateFromDiffEntry(final String dtstamp,
                                   final AliasMaps amaps,
                                   final DiffListEntry dle) throws TzException {
    try {
      String id = dle.tzid;

      if (!dle.aliasChangeOnly) {
        TzDbSpec dbspec = getSpec(id);

        if (dbspec != null) {
          if (dle.add) {
            throw new TzException("Inconsistent change list");
          }
        } else {
          if (!dle.add) {
            throw new TzException("Inconsistent change list");
          }
          dbspec = new TzDbSpec();
          dbspec.setName(id);
        }

        dbspec.setDtstamp(dtstamp);
        dbspec.setSource(cfg.getPrimaryUrl());
        dbspec.setActive(true);
        dbspec.setVtimezone(TzServerUtil.getCalHdr() +
                            dle.tzSpec +
                            TzServerUtil.getCalTlr());

        // XXX Localized names?

        if (dle.add) {
          addTzSpec(dbspec);
        } else {
          updateTzSpec(dbspec);
        }
      }

      if (Util.isEmpty(dle.aliases)) {
        return;
      }

      SortedSet<String> aliases = amaps.byTzid.get(id);

      for (String a: dle.aliases) {
        TzAlias alias = getTzAlias(a);

        if (alias == null) {
          alias = new TzAlias();

          alias.setFromId(a);
          alias.setToId(id);

          addTzAlias(alias);

          continue;
        }

        aliases.remove(a);
      }

      /* remaining aliases should be deleted */
      for (String alias: aliases) {
        TzAlias tza = getTzAlias(alias);
        removeTzAlias(tza);
      }
    } catch (TzException tze) {
      throw tze;
    } catch (Throwable t) {
      throw new TzException(t);
    }
  }

  private boolean loadInitialData() throws TzException {
    try {
      if (cfg.getTzdataUrl() == null) {
        error("No config data or no data url");
        return false;
      }

      if (debug) {
        trace("Loading initial data from " + cfg.getTzdataUrl());
      }

      CachedData z = new ZipCachedData(cfg);

      cfg.setDtstamp(z.getDtstamp());

      TzServerUtil.saveConfig();

      List<TimezoneType> tzs = z.getTimezones(null);

      if (debug) {
        trace("Initial load has " + tzs.size() + " timezones");
      }

      int ct = 0;

      for (TimezoneType tz: tzs) {
        if (tz.getAliases() != null) {
          for (String a: tz.getAliases()) {
            TzAlias alias = new TzAlias();

            alias.setFromId(a);
            alias.setToId(tz.getTzid());

            addTzAlias(alias);
          }
        }

        TzDbSpec spec = new TzDbSpec();

        spec.setName(tz.getTzid());

        spec.setVtimezone(TzServerUtil.getCalHdr() +
                          z.getCachedVtz(tz.getTzid()) +
                          TzServerUtil.getCalTlr());
        if (spec.getVtimezone() == null) {
          error("No timezone spec for " + tz.getTzid());
        }

        spec.setDtstamp(z.getDtstamp());
        spec.setActive(true);

        addTzSpec(spec);

        ct++;
        if (debug && ((ct%25) == 0)) {
          trace("Initial load has processed " + ct + " timezones");
        }
      }

      if (debug) {
        trace("Initial load processed " + ct + " timezones");
      }

      return true;
    } catch (TzException te) {
      getLogger().error("Unable to add tz data to db", te);
      throw te;
    }
  }

  private static final String getSpecQuery =
    "from " +
      TzDbSpec.class.getName() +
      " where name=:name";

  private TzDbSpec getSpec(final String id) throws TzException {
    sess.createQuery(getSpecQuery);

    sess.setParameter("name", id);

    return (TzDbSpec)sess.getUnique();
  }

  private static final String getAliasesQuery =
      "from " +
          TzAlias.class.getName();

  @SuppressWarnings("unchecked")
  private AliasMaps buildAliasMaps() throws TzException {
    try {
      AliasMaps maps = new AliasMaps();

      maps.byTzid = new HashMap<String, SortedSet<String>>();
      maps.byAlias = new HashMap<String, String>();
      maps.aliases = new Properties();

      sess.createQuery(getAliasesQuery);
      List<TzAlias> aliases = sess.getList();
      if (aliases == null) {
        return maps;
      }

      StringBuilder aliasStr = new StringBuilder();

      for (TzAlias alias: aliases) {
        String from = alias.getFromId();
        String id = alias.getToId();

        aliasStr.append(escape(from));
        aliasStr.append('=');
        aliasStr.append(escape(id));

        maps.aliases.setProperty(from, id);

        maps.byAlias.put(from, id);

        SortedSet<String> as = maps.byTzid.get(id);

        if (as == null) {
          as = new TreeSet<String>();
          maps.byTzid.put(id, as);
        }

        as.add(from);
      }

      maps.aliasesStr = aliasStr.toString();

      return maps;
    } catch (Throwable t) {
      throw new TzException(t);
    }
  }

  private static final String getSpecsQuery =
    "from " +
      TzDbSpec.class.getName();

  @SuppressWarnings("unchecked")
  private void processSpecs(final String dtstamp) throws TzException {
    try {
      resetTzs();

      sess.createQuery(getSpecsQuery);
      List<TzDbSpec> specs = sess.getList();

      for (TzDbSpec spec: specs) {
        String dt = spec.getDtstamp();
        if (!dt.endsWith("Z")) {
          // Pretend it's UTC
          dt += "Z";
        }

        processSpec(spec.getName(), spec.getVtimezone(),
                    XcalUtil.getXmlFormatDateTime(dt));
      }
    } catch (TzException te) {
      throw te;
    } catch (Throwable t) {
      throw new TzException(t);
    }
  }

  private SessionFactory getSessionFactory() throws TzException {
    if (sessionFactory != null) {
      return sessionFactory;
    }

    synchronized (this) {
      if (sessionFactory != null) {
        return sessionFactory;
      }

      /** Get a new hibernate session factory. This is configured from an
       * application resource hibernate.cfg.xml together with some run time values
       */
      try {
        Configuration conf = new Configuration();

        /*
        if (props != null) {
          String cachePrefix = props.getProperty("cachePrefix");
          if (cachePrefix != null) {
            conf.setProperty("hibernate.cache.use_second_level_cache",
                             props.getProperty("cachingOn"));
            conf.setProperty("hibernate.cache.region_prefix",
                             cachePrefix);
          }
        }
        */

        conf.configure();

        sessionFactory = conf.buildSessionFactory();

        return sessionFactory;
      } catch (Throwable t) {
        // Always bad.
        error(t);
        throw new TzException(t);
      }
    }
  }
}
