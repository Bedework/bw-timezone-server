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
package org.bedework.timezones.common.leveldb;

import org.bedework.timezones.common.AbstractCachedData;
import org.bedework.timezones.common.CachedData;
import org.bedework.timezones.common.Differ.DiffListEntry;
import org.bedework.timezones.common.Stat;
import org.bedework.timezones.common.TzConfig;
import org.bedework.timezones.common.TzException;
import org.bedework.timezones.common.TzServerUtil;
import org.bedework.timezones.common.db.LocalizedString;
import org.bedework.timezones.common.db.TzAlias;
import org.bedework.timezones.common.db.TzDbSpec;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** Cached timezone data in a leveldb database.
 *
 * @author douglm
 */
public class LdbCachedData extends AbstractCachedData {
  private boolean running;

  protected ObjectMapper mapper = new ObjectMapper(); // create once, reuse

  /** Current Database
   */
  protected DB db;

  /* Leveldb has no concept of table. It's just key-value pairs.
   * We prefix all the timezone spec names with timezoneSpecPrefix and all the
   * aliases with aliasPrefix. The remainder of the name is the 'table' key,
   * usually a tzid.
   */

  private final static String timezoneSpecPrefix = "TZ:";

  private final static String aliasPrefix = "AL:";

  /** */
  protected boolean open;

  private long reloads;
  private long primaryFetches;
  private long lastFetchCt;
  private String lastFetchStatus = "None";

  private String lastConfigLevelDbPath;

  /* Calculated from config level db path */
  private String levelDbPath;

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

        synchronized (LdbCachedData.this) {
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
   * @param cfg the configuration
   * @param clear remove all data from leveldb first
   * @throws TzException
   */
  public LdbCachedData(final TzConfig cfg,
                       final boolean clear) throws TzException {
    super(cfg, "Db");

    try {
      if (debug) {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      }

      final DateFormat df =
              new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'");

      mapper.setDateFormat(df);

      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    } catch (final Throwable t) {
      throw new TzException(t);
    }

    info("Load leveldb timezone data");
    loadData(clear);

    running = true;

    if (!cfg.getPrimaryServer()) {
      info("start timezone data update thread");
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
  public String getSource() throws TzException {
    return cfg.getSource();
  }

  @Override
  public List<Stat> getStats() throws TzException {
    List<Stat> stats = new ArrayList<>();

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
    } finally {
      close();
    }
  }

  @Override
  public List<String> findIds(final String val) throws TzException {
    try {
      open();

      final List<String> ids = new ArrayList<>();

      ids.addAll(findTzs(val));

      final List<TzAlias> as = findTzAliases(val);
      for (final TzAlias a: as) {
        ids.addAll(a.getTargetIds());
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

  /* ====================================================================
   *                   DbCachedData methods
   * ==================================================================== */

  /**
   * @param val the alias
   * @throws TzException
   */
  public void putTzAlias(final TzAlias val) throws TzException {
    db.put(Iq80DBFactory.bytes(aliasPrefix + val.getAliasId()),
           bytesJson(val));
  }

  /**
   * @param val the alias
   * @throws TzException
   */
  public void removeTzAlias(final TzAlias val) throws TzException {
    db.delete(Iq80DBFactory.bytes(aliasPrefix + val.getAliasId()));
  }

  /**
   * @param val the alias
   * @return alias entry
   * @throws TzException
   */
  public TzAlias getTzAlias(final String val) throws TzException {
    final byte[] aliasBytes = db.get(Iq80DBFactory.bytes(aliasPrefix + val));

    if (aliasBytes == null) {
      return null;
    }

    return getJson(aliasBytes, TzAlias.class);
  }

  /**
   * @param val the alias
   * @return matching alias entries
   * @throws TzException
   */
  public List<TzAlias> findTzAliases(final String val) throws TzException {
    try {
      final List<TzAlias> aliases = new ArrayList<>();

      try (DBIterator it = db.iterator()) {
        for (it.seekToFirst(); it.hasNext(); it.next()) {
          final String key = Iq80DBFactory.asString(it.peekNext().getKey());

          if (!key.startsWith(timezoneSpecPrefix)) {
            continue;
          }

          final String id = key.substring(aliasPrefix.length());

          if (!id.contains(val)) {
            continue;
          }

          final TzAlias alias = getJson(it.peekNext().getValue(),
                                        TzAlias.class);

          aliases.add(alias);
        }
      }

      return aliases;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  /**
   * @param val to match
   * @return matching tz entry names
   * @throws TzException
   */
  public List<String> findTzs(final String val) throws TzException {
    try {
      final List<String> ids = new ArrayList<>();

      try (DBIterator it = db.iterator()) {
        for (it.seekToFirst(); it.hasNext(); it.next()) {
          final String key = Iq80DBFactory.asString(it.peekNext().getKey());

          if (!key.startsWith(timezoneSpecPrefix)) {
            continue;
          }

          final String tzid = key.substring(timezoneSpecPrefix.length());

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
   * @param val the spec
   * @throws TzException
   */
  public void putTzSpec(final TzDbSpec val) throws TzException {
    db.put(Iq80DBFactory.bytes(timezoneSpecPrefix + val.getName()),
           bytesJson(val));
  }

  /* ====================================================================
   *                   Transaction methods
   * ==================================================================== */

  private synchronized void open() throws TzException {
    if (isOpen()) {
      return;
    }
    getDb();
    open = true;
  }

  private synchronized void close() {
    if (!open) {
      return;
    }
    closeDb();
    open = false;
  }

  private void fail() {
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

  /* ====================================================================
   *                   private methods
   * ==================================================================== */

  /**
   * @throws TzException
   */
  private synchronized void loadData(final boolean clear) throws TzException {
    reloads++;

    try {
      open();

      if (clear) {
        try (DBIterator iterator = getDb().iterator()) {
          for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
            getDb().delete(iterator.peekNext().getKey());
          }
        }
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

      final Timezones tzs = new TimezonesImpl();
      tzs.init(cfg.getPrimaryUrl());

      final String changedSince = cfg.getDtstamp();

      final long startTime = System.currentTimeMillis();
      long fetchTime = 0;


      final TimezoneListType tzl;

      try {
        tzl = tzs.getList(changedSince);
      } catch (final TzUnknownHostException tuhe) {
        error("Unknown host exception contacting " + cfg.getPrimaryUrl());
        return false;
      } catch (final Throwable t) {
        error("Exception contacting " + cfg.getPrimaryUrl());
        error(t);
        return false;
      }

      final String svrCs = tzl.getDtstamp();

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

      final AliasMaps amaps = buildAliasMaps();

      for (final TimezoneType sum: tzl.getTimezones()) {
        final String id = sum.getTzid();
        if (debug) {
          trace("Updating timezone " + id);
        }

        TzDbSpec dbspec = getSpec(id);

        String etag = null;
        if (dbspec != null) {
          etag = dbspec.getEtag();
        }

        final long startFetch = System.currentTimeMillis();
        final TaggedTimeZone ttz = tzs.getTimeZone(id, etag);

        fetchTime += System.currentTimeMillis() - startFetch;

        if ((ttz != null) && (ttz.vtz == null)) {
          // No change
          continue;
        }

        if (ttz == null) {
          warn("Received timezone id " + id + " but not available.");
          continue;
        }

        final boolean add = dbspec == null;

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
          final Set<LocalizedString> dns;

          if (add) {
            dns = new TreeSet<>();
            dbspec.setDisplayNames(dns);
          } else {
            dns = dbspec.getDisplayNames();
            dns.clear(); // XXX not good - forces delete and recreate
          }

          for (final LocalNameType ln: sum.getLocalNames()) {
            final LocalizedString ls =
                    new LocalizedString(ln.getLang(), ln.getValue());

            dns.add(ls);
          }
        }

        putTzSpec(dbspec);

        /* Get all aliases for this id */
        final SortedSet<String> aliases = amaps.byTzid.get(id);

        if (!Util.isEmpty(sum.getAliases())) {
          for (final String a: sum.getAliases()) {
            TzAlias tza = amaps.byAlias.get(a);

            if (tza == null) {
              tza = new TzAlias(a);
            }

            tza.addTargetId(id);

            putTzAlias(tza);

            /* We've seen this alias. Remove from the list */
            if (aliases != null) {
              aliases.remove(a);
            }
          }
        }

        if (aliases != null) {
          /* remaining aliases should be deleted */
          for (final String alias: aliases) {
            final TzAlias tza = getTzAlias(alias);
            removeTzAlias(tza);
          }
        }
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

  private void updateFromDiffEntry(final String dtstamp,
                                   final AliasMaps amaps,
                                   final DiffListEntry dle) throws TzException {
    try {
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

        putTzSpec(dbspec);
      }

      if (Util.isEmpty(dle.aliases)) {
        return;
      }

      final SortedSet<String> aliases = amaps.byTzid.get(id);

      for (final String a: dle.aliases) {
        TzAlias alias = getTzAlias(a);

        if (alias == null) {
          alias = new TzAlias(a);
        }

        alias.addTargetId(id);

        putTzAlias(alias);

        aliases.remove(a);
      }

      /* remaining aliases should be deleted */
      for (final String alias: aliases) {
        final TzAlias tza = getTzAlias(alias);
        removeTzAlias(tza);
      }
    } catch (final TzException tze) {
      throw tze;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  private boolean loadInitialData() throws TzException {
    try {
      if (debug) {
        trace("Loading initial data from " + cfg.getTzdataUrl());
      }

      final CachedData cachedData = TzServerUtil.getDataSource(cfg);

      cfg.setDtstamp(cachedData.getDtstamp());
      cfg.setSource(cachedData.getSource());

      TzServerUtil.saveConfig();

      final List<TimezoneType> tzs = cachedData.getTimezones((String)null);

      if (debug) {
        trace("Initial load has " + tzs.size() + " timezones");
      }

      int ct = 0;

      for (final TimezoneType tz: tzs) {
        if (tz.getAliases() != null) {
          for (final String a: tz.getAliases()) {
            TzAlias alias = getTzAlias(a);

            if (alias == null) {
              alias = new TzAlias(a);
            }

            alias.addTargetId(tz.getTzid());

            putTzAlias(alias);
          }
        }

        final TzDbSpec spec = new TzDbSpec();

        spec.setName(tz.getTzid());

        spec.setVtimezone(TzServerUtil.getCalHdr() +
                          cachedData.getCachedVtz(tz.getTzid()) +
                          TzServerUtil.getCalTlr());
        if (spec.getVtimezone() == null) {
          error("No timezone spec for " + tz.getTzid());
        }

        spec.setDtstamp(cachedData.getDtstamp());
        spec.setActive(true);

        putTzSpec(spec);

        ct++;
        if (debug && ((ct%25) == 0)) {
          trace("Initial load has processed " + ct + " timezones");
        }
      }

      if (debug) {
        trace("Initial load processed " + ct + " timezones");
      }

      return true;
    } catch (final TzException te) {
      getLogger().error("Unable to add tz data to db", te);
      throw te;
    }
  }

  private TzDbSpec getSpec(final String id) throws TzException {
    final byte[] specBytes = db.get(Iq80DBFactory.bytes(timezoneSpecPrefix + id));

    if (specBytes == null) {
      return null;
    }

    return getJson(specBytes, TzDbSpec.class);
  }

  private AliasMaps buildAliasMaps() throws TzException {
    try {
      final AliasMaps maps = new AliasMaps();

      maps.byTzid = new HashMap<>();
      maps.byAlias = new HashMap<>();
      maps.aliases = new Properties();

      final StringBuilder aliasStr = new StringBuilder();

      try (DBIterator it = db.iterator()) {
        for(it.seekToFirst(); it.hasNext(); it.next()) {
          final String key = Iq80DBFactory.asString(it.peekNext().getKey());

          if (!key.startsWith(aliasPrefix)) {
            continue;
          }

          final TzAlias alias = getJson(it.peekNext().getValue(),
                                  TzAlias.class);

          final String aliasId = alias.getAliasId();
          final StringBuilder ids = new StringBuilder();
          String delim = "";

          for (final String s: alias.getTargetIds()) {
            ids.append(delim);

            final String id = escape(s);
            ids.append(id);
            delim=",";

            SortedSet<String> as = maps.byTzid.get(id);

            if (as == null) {
              as = new TreeSet<>();
              maps.byTzid.put(id, as);
            }

            as.add(aliasId);
          }

          aliasStr.append(escape(aliasId));
          aliasStr.append('=');
          aliasStr.append(ids.toString());
          aliasStr.append('\n');

          maps.aliases.setProperty(aliasId, ids.toString());

          maps.byAlias.put(aliasId, alias);
        }
      }

      maps.aliasesStr = aliasStr.toString();

      return maps;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  private void processSpecs(final String dtstamp) throws TzException {
    try {
      resetTzs();

      try (DBIterator it = db.iterator()) {
        for(it.seekToFirst(); it.hasNext(); it.next()) {
          final String key = Iq80DBFactory.asString(it.peekNext().getKey());

          if (!key.startsWith(timezoneSpecPrefix)) {
            continue;
          }

          final TzDbSpec spec = getJson(it.peekNext().getValue(),
                                        TzDbSpec.class);

          String dt = spec.getDtstamp();
          if (!dt.endsWith("Z")) {
            // Pretend it's UTC
            dt += "Z";
          }

          processSpec(spec.getName(), spec.getVtimezone(),
                      XcalUtil.getXmlFormatDateTime(dt));
        }
      }
    } catch (final TzException te) {
      throw te;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  private DB getDb() throws TzException {
    if (db != null) {
      return db;
    }

    try {
      if ((lastConfigLevelDbPath == null) ||
          (!lastConfigLevelDbPath.equals(cfg.getLeveldbPath()))) {
        lastConfigLevelDbPath = cfg.getLeveldbPath();

        if (debug) {
          trace("Try to open leveldb at " + lastConfigLevelDbPath);
        }

        final File f = new File(lastConfigLevelDbPath);

        if (!f.isAbsolute()) {
          throw new TzException("levelDbPath must be absolute - found " +
                                lastConfigLevelDbPath);
        }

        levelDbPath = lastConfigLevelDbPath;
      }

      final Options options = new Options();
      options.createIfMissing(true);
      db = Iq80DBFactory.factory.open(new File(levelDbPath), options);
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }

    return db;
  }

  private void closeDb() {
    if (db == null) {
      return;
    }

    try {
      db.close();
      db = null;
    } catch (Throwable t) {
      warn("Error closing db: " + t.getMessage());
      error(t);
    }
  }

  /** ===================================================================
   *                   Json methods
   *  =================================================================== */

  protected void writeJson(final OutputStream out,
                           final Object val) throws TzException {
    try {
      mapper.writeValue(out, val);
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  protected byte[] bytesJson(final Object val) throws TzException {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();

      mapper.writeValue(os, val);

      return os.toByteArray();
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  protected <T> T getJson(final byte[] value,
                          final Class<T> valueType) throws TzException {
    InputStream is = null;
    try {
      is = new ByteArrayInputStream(value);

      return mapper.readValue(is, valueType);
    } catch (final Throwable t) {
      throw new TzException(t);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (final Throwable ignored) {}
      }
    }
  }
}
