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

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.TzId;

import org.bedework.timezones.common.CachedData;
import org.bedework.timezones.common.ExpandedMapEntry;
import org.bedework.timezones.common.ExpandedMapEntryKey;
import org.bedework.timezones.common.Stat;
import org.bedework.timezones.common.TzException;
import org.bedework.timezones.common.TzServerUtil;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import ietf.params.xml.ns.timezone_service.AliasType;
import ietf.params.xml.ns.timezone_service.LocalNameType;
import ietf.params.xml.ns.timezone_service.SummaryType;
import ietf.params.xml.ns.timezone_service.TimezoneListType;

import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;

import edu.rpi.cmt.calendar.XcalUtil;
import edu.rpi.cmt.timezones.Timezones;
import edu.rpi.cmt.timezones.TimezonesImpl;
import edu.rpi.cmt.timezones.Timezones.TaggedTimeZone;
import edu.rpi.cmt.timezones.Timezones.TimezonesException;

/** Cached timezone data in a database.
 *
 * @author douglm
 */
public class DbCachedData implements CachedData {
  private boolean debug;

  private static volatile boolean refreshNow = true;

  private transient Logger log;

  /** When we were created for debugging */
  protected Timestamp objTimestamp;

  /** Current hibernate session - exists only across one user interaction
   */
  protected HibSession sess;

  private static SessionFactory sessionFactory;

  /** */
  protected boolean open;

  private String primaryUrl;

  private String dtstamp;
  
  private long reloads;
  private long primaryFetches;
  private long lastFetchCt;

  private Map<String, String> vtzs = new HashMap<String, String>();

  private Map<String, TimeZone> tzs = new HashMap<String, TimeZone>();

  private Map<String, String> aliasedVtzs = new HashMap<String, String>();

  private Map<String, TimeZone> aliasedTzs = new HashMap<String, TimeZone>();

  private SortedSet<String> nameList;

  private Map<ExpandedMapEntryKey, ExpandedMapEntry> expansions =
    new HashMap<ExpandedMapEntryKey, ExpandedMapEntry>();

  private static class AliasMaps {
    String aliasesStr;

    Properties aliases;

    Map<String, List<String>> byTzid;
    Map<String, String> byAlias;
  }

  private AliasMaps aliasMaps;

  private List<SummaryType> summaries;

  /** Throws an exception if db is not set up. Fall back is probably to use the
   * zipped data.
   *
   * @param forAdd - true if we are going to add data
   * @param primaryUrl - if non-null location of primary server
   * @throws TzException
   */
  public DbCachedData(boolean forAdd,
                      String primaryUrl) throws TzException {
    debug = getLogger().isDebugEnabled();

    this.primaryUrl = primaryUrl;
    refreshNow = true;

    if (forAdd) {
      return;
    }

    try {
      if (!reloadData(true)) {
        if (primaryUrl != null) {
          update();
        }

        if (!reloadData(true)) {
          throw new DbEmptyException();
        }
      }
    } catch (ServletException se) {
      if (se.getCause() instanceof TzException) {
        throw (TzException)se.getCause();
      }

      throw new TzException(se);
    }
  }
  
  public List<Stat> getStats() throws ServletException {
    List<Stat> stats = new ArrayList<Stat>(); 

    if (tzs == null) {
      stats.add(new Stat("Db", "Unavailable"));
    }

    stats.add(new Stat("Db #tzs", String.valueOf(tzs.size())));
    stats.add(new Stat("Db dtstamp", dtstamp));
    stats.add(new Stat("Db reloads", String.valueOf(reloads)));
    stats.add(new Stat("Db primary fetches", String.valueOf(primaryFetches)));
    stats.add(new Stat("Db last fetch count",
                       String.valueOf(lastFetchCt)));
    stats.add(new Stat("Db cached expansions", 
                       String.valueOf(expansions.size())));

    return stats;
  }

  /* ====================================================================
   *                   DbCachedData methods
   * ==================================================================== */

  /** Call when the database has been updated - presumably by a refresh process
   * for a secondary server.
   */
  public static void flagUpdated() {
    refreshNow = true;
  }

  /**
   * @throws TzException
   */
  public void startAdd() throws TzException {
    open();
  }

  /**
   *
   */
  public void endAdd() {
    close();
  }

  /**
   *
   */
  public void failAdd() {
    fail();
  }

  /**
   * @param val
   * @throws TzException
   */
  public void addTzInfo(TzDbInfo val) throws TzException {
    sess.save(val);
  }

  /**
   * @param val
   * @throws TzException
   */
  public void updateTzInfo(TzDbInfo val) throws TzException {
    sess.update(val);
  }

  /**
   * @param val
   * @throws TzException
   */
  public void addTzAlias(TzAlias val) throws TzException {
    sess.save(val);
  }

  /**
   * @param val
   * @throws TzException
   */
  public void removeTzAlias(TzAlias val) throws TzException {
    sess.delete(val);
  }

  /**
   * @param val
   * @throws TzException
   */
  public void addTzSpec(TzDbSpec val) throws TzException {
    sess.save(val);
  }

  /**
   * @param val
   * @throws TzException
   */
  public void updateTzSpec(TzDbSpec val) throws TzException {
    sess.save(val);
  }

  /* ====================================================================
   *                   CachedData methods
   * ==================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#refresh()
   */
  public void refresh() {
    refreshNow = true;
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#update()
   */
  public void update() throws ServletException {
    updateData();
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getDtstamp()
   */
  public String getDtstamp() throws ServletException {
    reloadData();
    return dtstamp;
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#fromAlias(java.lang.String)
   */
  public String fromAlias(String val) throws ServletException {
    reloadData();
    return aliasMaps.byAlias.get(val);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getAliasesStr()
   */
  public String getAliasesStr() throws ServletException {
    reloadData();
    return aliasMaps.aliasesStr;
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#findAliases(java.lang.String)
   */
  public List<String> findAliases(String tzid) throws ServletException {
    reloadData();
    return aliasMaps.byTzid.get(tzid);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getNameList()
   */
  public SortedSet<String> getNameList() throws ServletException {
    reloadData();
    return nameList;
  }

  public void setExpanded(ExpandedMapEntryKey key,
                          ExpandedMapEntry tzs) throws ServletException {
    reloadData();
    expansions.put(key, tzs);
  }

  public ExpandedMapEntry getExpanded(ExpandedMapEntryKey key) throws ServletException {
    reloadData();
    return expansions.get(key);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getCachedVtz(java.lang.String)
   */
  public String getCachedVtz(final String name) throws ServletException {
    reloadData();
    return vtzs.get(name);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getAllCachedVtzs()
   */
  public Collection<String> getAllCachedVtzs() throws ServletException {
    reloadData();
    return vtzs.values();
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getTimeZone(java.lang.String)
   */
  public TimeZone getTimeZone(final String tzid) throws ServletException {
    reloadData();
    return tzs.get(tzid);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getAliasedCachedVtz(java.lang.String)
   */
  public String getAliasedCachedVtz(final String name) throws ServletException {
    reloadData();
    return aliasedVtzs.get(name);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getAliasedTimeZone(java.lang.String)
   */
  public TimeZone getAliasedTimeZone(final String tzid) throws ServletException {
    reloadData();
    return aliasedTzs.get(tzid);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getSummaries(java.lang.String)
   */
  public List<SummaryType> getSummaries(String changedSince) throws ServletException {
    reloadData();

    if (changedSince == null) {
      return summaries;
    }

    List<SummaryType> ss = new ArrayList<SummaryType>();

    for (SummaryType sum: summaries) {
      if (sum.getLastModified() == null) {
        ss.add(sum);
        continue;
      }

      String lm = sum.getLastModified().toXMLFormat();

      /*
       * cs > lm +
       * cs = lm 0
       * cs < lm -
       */

      if (changedSince.compareTo(lm) < 0) {
        ss.add(sum);
      }
    }

    return ss;
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
   * @param aliasesStr
   * @throws ServletException
   */
  private synchronized void reloadData() throws ServletException {
    reloadData(false);
  }

  /**
   * @param aliasesStr
   * @throws ServletException
   */
  private synchronized boolean reloadData(boolean emptyReturn) throws ServletException {
    if (!refreshNow) {
      return true;
    }

    reloads++;
    
    try {
      open();

      TzDbInfo inf = getDbInfo();

      if (inf == null) {
        if (emptyReturn) {
          return false;
        }
        throw new TzException("Empty tz db");
      }

      dtstamp = inf.getDtstamp();

      TzServerUtil.lastDataFetch = System.currentTimeMillis();
      refreshNow = false;

      /* ===================== Rebuild the alias maps ======================= */

      aliasMaps = buildAliasMaps();

      /* ===================== All tzs into the table ======================= */

      processSpecs(dtstamp);

      expansions.clear();

      return true;
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    } finally {
      close();
    }
  }

  /** Call the primary server and get a list of data that's changed since we last
   * looked. Then fetch each changed timezone and update the db.
   * 
   * @throws ServletException
   */
  private void updateData() throws ServletException {
    if (primaryUrl == null) {
      return;
    }

    try {
      Timezones tzs = new TimezonesImpl();
      tzs.init(primaryUrl);
      open();

      TzDbInfo inf = getDbInfo();

      String changedSince = null;
      if (inf != null) {
        changedSince = inf.getDtstamp();
      }

      TimezoneListType tzl = tzs.getList(changedSince);

      String svrCs = tzl.getDtstamp().toXMLFormat();

      if (inf == null) {
        inf = new TzDbInfo();

        inf.setDtstamp(svrCs);
        inf.setVersion("1.0");

        addTzInfo(inf);
      } else if ((changedSince == null) ||
                 !svrCs.equals(changedSince)) {
        inf.setDtstamp(svrCs);

        updateTzInfo(inf);
      }
      
      primaryFetches++;
      lastFetchCt = tzl.getSummary().size();

      /* Go through the returned timezones and try to update.
       * If we have the timezone and it has an etag do a conditional fetch.
       * If we don't have the timezone do an unconditional fetch.
       */

      Map<String, TzAlias> dbaliases = getDbAliases();

      for (SummaryType sum: tzl.getSummary()) {
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

        boolean add = dbspec == null;

        if (add) {
          // Create a new one
          dbspec = new TzDbSpec();
        }

        dbspec.setName(id);
        dbspec.setEtag(ttz.etag);
        dbspec.setDtstamp(sum.getLastModified().toXMLFormat());
        dbspec.setSource(primaryUrl);
        dbspec.setActive(true);
        dbspec.setVtimezone(ttz.vtz);

        if (sum.getLocalName().size() > 0) {
          Set<LocalizedString> dns = new TreeSet<LocalizedString>();

          if (add) {
            dns = new TreeSet<LocalizedString>();
            dbspec.setDisplayNames(dns);
          } else {
            dns = dbspec.getDisplayNames();
            dns.clear(); // XXX not good - forces delete and recreate
          }
          for (LocalNameType ln: sum.getLocalName()) {
            LocalizedString ls = new LocalizedString(ln.getLang(), ln.getValue());

            dns.add(ls);
          }
        }

        if (add) {
          addTzSpec(dbspec);
        } else {
          updateTzSpec(dbspec);
        }

        for (AliasType a: sum.getAlias()) {
          TzAlias alias = dbaliases.get(a.getValue());

          if (alias == null) {
            alias = new TzAlias();

            alias.setFromId(a.getValue());
            alias.setToId(id);

            addTzAlias(alias);

            continue;
          }

          dbaliases.remove(a.getValue());
        }

        /* remaining aliases should be deleted */
        for (TzAlias alias: dbaliases.values()) {
          removeTzAlias(alias);
        }
      }
    } catch (TimezonesException te) {
      fail();
      throw new ServletException(te);
    } catch (Throwable t) {
      fail();
      throw new ServletException(t);
    } finally {
      close();
    }
  }

  private TzDbInfo getDbInfo() throws TzException {
    sess.createQuery("from " + TzDbInfo.class.getName());
    List infos = sess.getList();

    if (infos == null) {
      return null;
    }

    if (infos.size() == 0) {
      return null;
    }

    if (infos.size() != 1) {
      throw new TzException("Expected a single info entry");
    }

    return (TzDbInfo)infos.get(0);
  }

  private TzDbSpec getSpec(String id) throws TzException {
    StringBuilder sb = new StringBuilder();

    sb.append("from ");
    sb.append(TzDbSpec.class.getName());
    sb.append(" where name=:name");

    sess.createQuery(sb.toString());

    sess.setParameter("name", id);

    return (TzDbSpec)sess.getUnique();
  }

  @SuppressWarnings("unchecked")
  private AliasMaps buildAliasMaps() throws ServletException {
    try {
      AliasMaps maps = new AliasMaps();

      maps.byTzid = new HashMap<String, List<String>>();
      maps.byAlias = new HashMap<String, String>();
      maps.aliases = new Properties();

      sess.createQuery("from " + TzAlias.class.getName());
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

        List<String> as = maps.byTzid.get(id);

        if (as == null) {
          as = new ArrayList<String>();
          maps.byTzid.put(id, as);
        }

        as.add(from);
      }

      maps.aliasesStr = aliasStr.toString();

      return maps;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, TzAlias> getDbAliases() throws ServletException {
    try {
      Map<String, TzAlias> dbaliases = new HashMap<String, TzAlias>();

      sess.createQuery("from " + TzAlias.class.getName());
      List<TzAlias> aliases = sess.getList();
      if (aliases == null) {
        return dbaliases;
      }

      for (TzAlias alias: aliases) {
        dbaliases.put(alias.getFromId(), alias);
      }

      return dbaliases;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  @SuppressWarnings("unchecked")
  private void processSpecs(String dtstamp) throws ServletException {
    try {
      nameList = new TreeSet<String>();

      sess.createQuery("from " + TzDbSpec.class.getName());
      List<TzDbSpec> specs = sess.getList();

      summaries = new ArrayList<SummaryType>();

      for (TzDbSpec spec: specs) {
        String id = spec.getName();

        nameList.add(id);

        String tzdef = spec.getVtimezone();

        CalendarBuilder cb = new CalendarBuilder();

        UnfoldingReader ufrdr = new UnfoldingReader(new StringReader(tzdef), true);

        net.fortuna.ical4j.model.Calendar cal = cb.build(ufrdr);
        VTimeZone vtz = (VTimeZone)cal.getComponents().getComponent(Component.VTIMEZONE);
        if (vtz == null) {
          throw new Exception("Incorrectly stored timezone");
        }

        tzs.put(id, new TimeZone(vtz));
        vtzs.put(id, vtz.toString());

        /* ================== Build summary info ======================== */
        SummaryType st = new SummaryType();

        st.setTzid(id);

        LastModified lm = vtz.getLastModified();
        if (lm!= null) {
          st.setLastModified(XcalUtil.getXMlUTCCal(lm.getValue()));
        } else if (spec.getDtstamp() != null) {
          st.setLastModified(XcalUtil.getXMlUTCCal(spec.getDtstamp()));
        } else {
          st.setLastModified(XcalUtil.getXMlUTCCal(dtstamp));
        }

        List<String> aliases = findAliases(id);

        // XXX Need to have list of local names per timezone
        //String ln = vtz.
        if (aliases != null) {
          for (String a: aliases) {
            AliasType at = new AliasType();

            // XXX Need locale as well as name
            at.setValue(a);
            st.getAlias().add(at);

            /* Construct a new vtimezone with the alias as id and then
             * add it and the string version to the alias table.
             */
            VTimeZone avtz = (VTimeZone)vtz.copy();

            TzId tzid = avtz.getTimeZoneId();
            tzid.setValue(a);

            aliasedTzs.put(a, new TimeZone(avtz));
            aliasedVtzs.put(a, avtz.toString());
          }
        }

        summaries.add(st);
      }
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private String escape(String val) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < val.length(); i++) {
      char ch = val.charAt(i);

      if ((ch > 61) && (ch < 127)) {
        if (ch == '\\') {
          sb.append("\\\\");
          continue;
        }

        sb.append(ch);
        continue;
      }

      switch(ch) {
      case ' ':
        if (i == 0) {
          sb.append('\\');
        }

        sb.append(' ');
        break;
      case '\f':
        sb.append("\\f");
        break;
      case '\n':
        sb.append("\\n");
        break;
      case '\r':
        sb.append("\\r");
        break;
      case '\t':
        sb.append("\\t");
        break;
      case '=':
      case ':':
      case '#':
      case '!':
        sb.append('\\');
        sb.append(ch);
        break;
      default:
        if ((ch < 0x0020) || (ch > 0x007e)) {
          sb.append("\\u");

          sb.append(hex[(ch >> 12) & 0xF]);
          sb.append(hex[(ch >>  8) & 0xF]);
          sb.append(hex[(ch >>  4) & 0xF]);
          sb.append(hex[ch & 0xF]);
        } else {
          sb.append(ch);
        }
      }
    }
    return sb.toString();
  }

  private static final char[] hex = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
   };

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

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  /**
   * @param t
   */
  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  /**
   * @param msg
   */
  protected void warn(final String msg) {
    getLogger().warn(msg);
  }

  /**
   * @param msg
   */
  protected void trace(final String msg) {
    getLogger().debug(msg);
  }
}
