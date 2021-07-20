/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.timezones.common.db;

import org.bedework.timezones.common.AbstractCachedData;
import org.bedework.timezones.common.CachedData;
import org.bedework.timezones.common.Differ.DiffListEntry;
import org.bedework.timezones.common.Stat;
import org.bedework.timezones.common.TzConfig;
import org.bedework.timezones.common.TzException;
import org.bedework.timezones.common.TzServerUtil;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.misc.Util;
import org.bedework.util.timezones.DateTimeUtil;
import org.bedework.util.timezones.Timezones;
import org.bedework.util.timezones.Timezones.TaggedTimeZone;
import org.bedework.util.timezones.TimezonesImpl;
import org.bedework.util.timezones.TzNoPrimaryException;
import org.bedework.util.timezones.TzUnknownHostException;
import org.bedework.util.timezones.model.LocalNameType;
import org.bedework.util.timezones.model.TimezoneListType;
import org.bedework.util.timezones.model.TimezoneType;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * User: mike Date: 6/22/21 Time: 23:07
 */
public abstract class AbstractDb extends AbstractCachedData {
  private boolean running;

  private long reloads;
  private long primaryFetches;
  private long lastFetchCt;
  private String lastFetchStatus = "None";

  private UpdateThread updater;

  protected static final Object dbLock = new Object();

  /** */
  protected boolean open;

  protected class UpdateThread extends Thread {
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

        synchronized (this) {
          try {
            refreshWait = cfg.getRefreshDelay();

            if (debug()) {
              debug("Updater: About to update");
            }

            if (!updateFromPrimary()) {
              // Try again in at most 10 minutes (need an error retry param)
              refreshWait = Math.min(refreshWait, 600);
            }
          } catch (final Throwable t) {
            if (!showedTrace) {
              error(t);
              showedTrace = true;
            } else {
              error(t.getMessage());
            }

            try {
              fail();
            } catch (final Throwable ignored) {
            }
          }
        }

        if (debug()) {
          debug("Updater: About to wait for " +
                        refreshWait +
                        " seconds");

        }

        if (!running) {
          break;
        }

        // Hang around
        try {
          final Object o = new Object();
          synchronized (o) {
            o.wait(refreshWait * 1000);
          }
        } catch (final InterruptedException ie) {
          if (debug()) {
            debug("Updater: Interrupted ");
          }
        } catch (final Throwable t) {
          error(t.getMessage());
        }
      }
    }
  }

  /**
   * @param cfg tz configuration
   * @param msgPrefix - for messages
   * @throws TzException on fatal error
   */
  public AbstractDb(final TzConfig cfg,
                    final String msgPrefix) throws TzException {
    super(cfg, msgPrefix);

  }

  /**
   *
   * @param clear remove all data from db first
   * @throws TzException on fatal error
   */
  protected void initData(final boolean clear) throws TzException {
    info("Load timezone data");
    loadData(clear);

    running = true;

    if (!cfg.getPrimaryServer()) {
      info("start timezone data update thread");
      updater = new UpdateThread("DbdataUpdater");
      updater.start();
    }
  }

  /**
   * @param val the spec
   * @throws TzException on fatal error
   */
  public abstract void addTzSpec(TzDbSpec val) throws TzException;

  /**
   * @param val the spec
   * @throws TzException on fatal error
   */
  public abstract void putTzSpec(TzDbSpec val) throws TzException;

  protected abstract TzDbSpec getSpec(String id) throws TzException;

  /**
   * @param val the alias
   * @throws TzException on fatal error
   */
  public abstract void addTzAlias(TzAlias val) throws TzException;

  /**
   * @param val the alias
   * @throws TzException on fatal error
   */
  public abstract void putTzAlias(TzAlias val) throws TzException;

  /**
   * @param val the alias
   * @throws TzException on fatal error
   */
  public abstract void removeTzAlias(TzAlias val) throws TzException;

  /**
   * @param val the alias
   * @return alias entry
   * @throws TzException on fatal error
   */
  public abstract TzAlias getTzAlias(String val) throws TzException;

  public abstract static class DbIterator<T>
          implements Iterator<T>, Closeable {
  }

  protected abstract DbIterator<TzAlias> getAliasIterator();

  protected abstract DbIterator<TzDbSpec> getTzSpecIterator();

  protected abstract void open() throws TzException;

  protected abstract void close();

  protected abstract void clearDb() throws TzException;

  @Override
  public void checkData() {
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
      final AliasMaps amaps = buildAliasMaps();

      for (final DiffListEntry dle: dles) {
        updateFromDiffEntry(dtstamp, amaps, dle);
      }

      cfg.setDtstamp(dtstamp);

      TzServerUtil.saveConfig();
    } catch (final TzException te) {
      fail();
      throw te;
    } catch (final Throwable t) {
      fail();
      throw new TzException(t);
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
  public String getSource() throws TzException {
    return cfg.getSource();
  }

  @Override
  public List<Stat> getStats() throws TzException {
    final List<Stat> stats = new ArrayList<>(super.getStats());

    stats.add(new Stat("Db reloads", String.valueOf(reloads)));
    stats.add(new Stat("Db primary fetches", String.valueOf(primaryFetches)));
    stats.add(new Stat("Db last fetch count",
                       String.valueOf(lastFetchCt)));
    stats.add(new Stat("Db last fetch status", lastFetchStatus));

    return stats;
  }

  @Override
  public List<String> findIds(final String val) throws TzException {
    try {
      open();

      final List<String> ids = new ArrayList<>(findTzs(val));

      final List<TzAlias> as = findTzAliases(val);
      for (final TzAlias a: as) {
        ids.add(a.getTargetId());
      }

      return ids;
    } catch (final TzException te) {
      fail();
      throw te;
    } catch (final Throwable t) {
      fail();
      throw new TzException(t);
    } finally {
      close();
    }
  }

  /**
   * @param val to match
   * @return matching tz entry names
   * @throws TzException on fatal error
   */
  public List<String> findTzs(final String val) throws TzException {
    try {
      final List<String> ids = new ArrayList<>();

      try (final DbIterator<TzDbSpec> it = getTzSpecIterator()) {
        while (it.hasNext()) {
          final var spec = it.next();
          final String tzid = spec.getName();

          if (!tzid.contains(val)) {
            continue;
          }

          ids.add(tzid);
        }
      }

      return ids;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  /**
   * @param val the alias
   * @return matching alias entries
   * @throws TzException on fatal error
   */
  public List<TzAlias> findTzAliases(final String val) throws TzException {
    try {
      final List<TzAlias> aliases = new ArrayList<>();

      try (final DbIterator<TzAlias> it = getAliasIterator()) {
        while (it.hasNext()) {
          final TzAlias alias = it.next();

          final String id = alias.getAliasId();

          if (!id.contains(val)) {
            continue;
          }

          aliases.add(alias);
        }
      }

      return aliases;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  protected void fail() {
  }

  /* ====================================================================
   *                   Session methods
   * ==================================================================== */

  protected boolean isOpen() {
    return open;
  }

  protected void checkOpen() throws TzException {
    if (!isOpen()) {
      throw new TzException("Session call when closed");
    }
  }

  protected static class TzEntry {
    String id;
    TimezoneType sum;
    TzDbSpec dbspec;
    TaggedTimeZone ttz;
  }

  /** Call the primary server and get a list of data that's changed since we last
   * looked. Then fetch each changed timezone and update the db.
   *
   * <p>We try not to keep the db locked for long periods</p>
   *
   * @return true if we successfully contacted the server
   * @throws TzException on fatal error
   */
  private synchronized boolean updateFromPrimary() throws TzException {
    if (debug()) {
      debug("Updating from primary");
    }

    try {
      if (cfg.getPrimaryServer()) {
        // We are a primary. No update needed
        if (debug()) {
          debug("We are a primary: exit");
        }

        return true; // good enough
      }

      if (cfg.getPrimaryUrl() == null) {
        warn("No primary URL: exit");

        return true; // good enough
      }

      /* Get the list of changed tzs from the primary */

      final Timezones tzs = new TimezonesImpl();
      tzs.init(cfg.getPrimaryUrl());

      final String changedSince = cfg.getDtstamp();

      final long startTime = System.currentTimeMillis();
      long fetchTime = 0;

      final TimezoneListType tzl;

      try {
        tzl = tzs.getList(changedSince);
      } catch (final TzNoPrimaryException tznpe) {
        error("Unable to contact primary: " + tznpe.getExtra());
        return false;
      } catch (final TzUnknownHostException tuhe) {
        error("Unknown host exception contacting " + cfg.getPrimaryUrl());
        return false;
      } catch (final Throwable t) {
        error("Exception contacting " + cfg.getPrimaryUrl());
        error(t);
        return false;
      }

      final String svrCs = tzl.getSynctoken();

      if (!svrCs.equals(changedSince)) {
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

      final List<TzEntry> tzEntries = new ArrayList<>();

      /* First go through the returned list and get our own spec.
         Need the db for that.
       */
      try {
        open();

        for (final TimezoneType sum : tzl.getTimezones()) {
          final TzEntry entry = new TzEntry();

          entry.id = sum.getTzid();
          entry.sum = sum;
          if (debug()) {
            debug("Get db spec for timezone " + entry.id);
          }

          entry.dbspec = getSpec(entry.id);

          tzEntries.add(entry);
        }
      } finally {
        close();
      }

      /* Now fetch the timezones from the primary - no db needed
       */

      for (final TzEntry entry : tzEntries) {
        if (debug()) {
          debug("Fetching timezone " + entry.id);
        }

        String etag = null;
        if (entry.dbspec != null) {
          etag = entry.dbspec.getEtag();
        }

        final long startFetch = System.currentTimeMillis();
        final TaggedTimeZone ttz = tzs.getTimeZone(entry.id, etag);

        fetchTime += System.currentTimeMillis() - startFetch;

        if ((ttz != null) && (ttz.vtz == null)) {
          // No change
          continue;
        }

        if (ttz == null) {
          warn("Received timezone id " + entry.id +
                       " but not available.");
          continue;
        }

        entry.ttz = ttz;
      }

      /* Go through the entries and try to update.
       * If ttz is null no update needed.
       * If dbspec is null it's an add.
       */

      /* Something is wrong here -
         If we are updating from a primary we don't need to use
         alias maps - the tz def should have an aliased to
         property. The alias maps should only be used when we are the
         primary
       */

      // final AliasMaps amaps = buildAliasMaps();

      try {
        open();

        for (final TzEntry entry : tzEntries) {
          if (debug()) {
            debug("Processing timezone " + entry.id);
          }

          if (entry.ttz == null) {
            if (debug()) {
              debug("No change.");
            }
            continue;
          }

          final boolean add = entry.dbspec == null;

          if (add) {
            // Create a new one
            entry.dbspec = new TzDbSpec();
          }

          entry.dbspec.setName(entry.id);
          entry.dbspec.setEtag(entry.ttz.etag);
          entry.dbspec.setDtstamp(DateTimeUtil.rfcDateTimeUTC(
                  entry.sum.getLastModified()));
          entry.dbspec.setSource(cfg.getPrimaryUrl());
          entry.dbspec.setActive(true);
          entry.dbspec.setVtimezone(entry.ttz.vtz);

          if (!Util.isEmpty(entry.sum.getLocalNames())) {
            final Set<LocalizedString> dns;

            if (add) {
              dns = new TreeSet<>();
              entry.dbspec.setDisplayNames(dns);
            } else {
              dns = entry.dbspec.getDisplayNames();
              dns.clear(); // XXX not good - forces delete and recreate
            }

            for (final LocalNameType ln : entry.sum.getLocalNames()) {
              final LocalizedString ls =
                      new LocalizedString(ln.getLang(),
                                          ln.getValue());

              dns.add(ls);
            }
          }

          if (add) {
            addTzSpec(entry.dbspec);
          } else {
            putTzSpec(entry.dbspec);
          }

          /* don't do this for the moment
          /* Get all aliases for this id * /
          final SortedSet<String> aliases = amaps.byTzid.get(entry.id);

          if (!Util.isEmpty(entry.sum.getAliases())) {
            for (final String a : entry.sum.getAliases()) {
              final String targetId = amaps.byAlias.get(a);

              if (tza == null) {
                tza = new TzAlias(a);
              }

              tza.addTargetId(entry.id);

              putTzAlias(new TzAlias());

              /* We've seen this alias. Remove from the list * /
              if (aliases != null) {
                aliases.remove(a);
              }
            }
          }

          if (aliases != null) {
            /* remaining aliases should be deleted * /
            for (final String alias: aliases) {
              final TzAlias tza = getTzAlias(alias);
              removeTzAlias(tza);
            }
          } */
        }
      } finally {
        close();
      }

      info("Total time: " +
                   TzServerUtil.printableTime(
                           System.currentTimeMillis() - startTime));
      info("Fetch time: " + TzServerUtil.printableTime(fetchTime));
      lastFetchStatus = "Success";
    } catch (final TzException tze) {
      lastFetchStatus = "Failed";
      throw tze;
    } catch (final Throwable t) {
      lastFetchStatus = "Failed";
      throw new TzException(t);
    }

    return true;
  }

  private AliasMaps buildAliasMaps() throws TzException {
    try {
      open();

      final AliasMaps maps = new AliasMaps();

      maps.byTzid = new HashMap<>();
      maps.byAlias = new HashMap<>();
      maps.aliases = new Properties();

      final StringBuilder aliasStr = new StringBuilder();

      try (final DbIterator<TzAlias> it = getAliasIterator()) {
        while (it.hasNext()) {
          final TzAlias alias = it.next();

          final String aliasId = alias.getAliasId();

          final String targetId = alias.getTargetId();

          final SortedSet<String> as = maps.byTzid
                  .computeIfAbsent(targetId, k -> new TreeSet<>());

          as.add(aliasId);

          aliasStr.append(escape(aliasId));
          aliasStr.append('=');
          aliasStr.append(escape(targetId));
          aliasStr.append('\n');

          maps.aliases.setProperty(aliasId, targetId);

          maps.byAlias.put(aliasId, targetId);
        }
      }

      maps.aliasesStr = aliasStr.toString();

      return maps;
    } catch (final Throwable t) {
      throw new TzException(t);
    } finally {
      close();
    }
  }

  private void updateFromDiffEntry(final String dtstamp,
                                   final AliasMaps amaps,
                                   final DiffListEntry dle) throws TzException {
    try {
      open();

      final String id = dle.tzid;

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
          putTzSpec(dbspec);
        }
      }

      if (Util.isEmpty(dle.aliases)) {
        return;
      }

      /* I don't think we shoudl be doing this...
      final SortedSet<String> aliases = amaps.byTzid.get(id);

      for (final String a: dle.aliases) {
        TzAlias alias = getTzAlias(a);
        final boolean adding;

        if (alias == null) {
          alias = new TzAlias(a, id);
          adding = true;
        } else {
          adding = false;
        }

        alias.addTargetId(id);

        if (adding) {
          addTzAlias(alias);
        } else {
          putTzAlias(alias);
        }

        aliases.remove(a);
      }

      /* remaining aliases should be deleted * /
      for (final String alias: aliases) {
        final TzAlias tza = getTzAlias(alias);
        removeTzAlias(tza);
      }
      */
    } catch (final TzException tze) {
      throw tze;
    } catch (final Throwable t) {
      throw new TzException(t);
    } finally {
      close();
    }
  }

  /**
   * @throws TzException on fatal error
   */
  private void loadData(final boolean clear) throws TzException {
    synchronized (dbLock) {
      reloads++;

      try {
        try {
          open();

          if (clear) {
            clearDb();
          }
        } finally {
          close();
        }

        if (!cfg.getPrimaryServer()) {
          updateFromPrimary();
        } else if (clear) {
          loadInitialData();
        }

        dtstamp = cfg.getDtstamp();

        TzServerUtil.lastDataFetch = System.currentTimeMillis();

        /* ===================== Rebuild the alias maps ======================= */

        aliasMaps = buildAliasMaps();

        /* ===================== All tzs into the table ======================= */

        processSpecs(dtstamp);

        expansions.clear();
      } catch (final TzException te) {
        fail();
        throw te;
      } catch (final Throwable t) {
        fail();
        throw new TzException(t);
      } finally {
        close();
      }
    }
  }

  protected boolean loadInitialData() throws TzException {
    try {
      open();

      if (debug()) {
        debug("Loading initial data from " + cfg.getTzdataUrl());
      }

      final CachedData cachedData = TzServerUtil.getDataSource(cfg);

      cfg.setDtstamp(cachedData.getDtstamp());
      cfg.setSource(cachedData.getSource());

      TzServerUtil.saveConfig();

      final List<TimezoneType> tzs = cachedData.getTimezones((String)null);

      if (debug()) {
        debug("Initial load has " + tzs.size() + " timezones");
      }

      int ct = 0;

      for (final TimezoneType tz: tzs) {
        /*
        if (tz.getAliases() != null) {
          for (final String a: tz.getAliases()) {
            TzAlias alias = getTzAlias(a);

            if (alias == null) {
              alias = new TzAlias(a);
            }

            alias.addTargetId(tz.getTzid());

            putTzAlias(alias);
          }
        }*/

        final TzDbSpec spec = new TzDbSpec();

        spec.setName(tz.getTzid());

        spec.setVtimezone(TzServerUtil.getCalHdr() +
                                  cachedData.getCachedVtz(tz.getTzid()) +
                                  TzServerUtil.getCalTlr());
        if (spec.getVtimezone() == null) {
          error("No timezone spec for " + tz.getTzid());
        }

        spec.setDtstamp(cachedData.getDtstamp());

        // TODO - should this be another value?
        spec.setEtag(cachedData.getDtstamp());
        spec.setActive(true);

        addTzSpec(spec);

        ct++;
        if (debug() && ((ct%25) == 0)) {
          debug("Initial load has processed " + ct + " timezones");
        }
      }

      if (debug()) {
        debug("Initial load processed " + ct + " timezones");
      }

      return true;
    } catch (final TzException te) {
      error("Unable to add tz data to db", te);
      throw te;
    } finally {
      close();
    }
  }

  private void processSpecs(final String dtstamp) throws TzException {
    try {
      open();

      resetTzs();

      try (final DbIterator<TzDbSpec> it = getTzSpecIterator()) {
        while (it.hasNext()) {
          final TzDbSpec spec = it.next();

          String dt = spec.getDtstamp();
          if (!dt.endsWith("Z")) {
            // Pretend it's UTC
            dt += "Z";
          }

          processSpec(spec.getName(),
                      spec.getVtimezone(),
                      spec.getEtag(),
                      XcalUtil.getXmlFormatDateTime(dt));
        }
      }
    } catch (final TzException te) {
      throw te;
    } catch (final Throwable t) {
      throw new TzException(t);
    } finally {
      close();
    }
  }
}
