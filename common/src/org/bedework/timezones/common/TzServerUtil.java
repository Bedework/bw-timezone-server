/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/
package org.bedework.timezones.common;

import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.Observance;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.util.TimeZones;

import org.bedework.timezones.common.db.DbCachedData;
import org.bedework.timezones.common.db.DbEmptyException;
import org.bedework.timezones.common.db.TzAlias;
import org.bedework.timezones.common.db.TzDbInfo;
import org.bedework.timezones.common.db.TzDbSpec;

import org.apache.log4j.Logger;

import ietf.params.xml.ns.timezone_service.AliasType;
import ietf.params.xml.ns.timezone_service.ObservanceType;
import ietf.params.xml.ns.timezone_service.SummaryType;
import ietf.params.xml.ns.timezone_service.TimezonesType;
import ietf.params.xml.ns.timezone_service.TzdataType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.xml.datatype.XMLGregorianCalendar;

import edu.rpi.cmt.calendar.XcalUtil;
import edu.rpi.sss.util.DateTimeUtil;
import edu.rpi.sss.util.OptionsException;
import edu.rpi.sss.util.OptionsI;

/** Common code for the timezone service.
 *
 *   @author Mike Douglass
 */
public class TzServerUtil {
  /* Temp - allows disabling of db */
  private static boolean tryDb = true;

  private static String appname = "tzsvr";

  private static TzsvrConfig config;

  private static TzServerUtil instance;

  /* A URL for retrieving the tzdata jar */
  static String tzdataUrl;

  static String primaryUrl;

  private static Object locker = new Object();

  /* ======================= Stats ======================= */

  static long gets;
  static long cacheHits;
  static long reads;
  static long nameLists;
  static long aliasReads;
  static long conversions;
  static long conversionsMillis;
  static long tzfetches;
  static long reloads;
  static long reloadsMillis;
  static long expandFetches;
  static long expandHits;
  static long expands;
  static long expandsMillis;

  /* ======================= Error codes ======================= */

  /** Unable to retrieve the data */
  public static final String errorNodata = "org.tserver.no.data";

  /* ======================= Caching ======================= */

  private CachedData cache;

  /** Time we last fetched the data */
  public static long lastDataFetch;

  static XMLGregorianCalendar dtstamp;

  /**
   * @throws ServletException
   */
  private TzServerUtil() throws ServletException {
    /* Note that the options factory returns a static object and we should
     * initialise the config once only
     */
    OptionsI opts;
    try {
      opts = TzsvrOptionsFactory.getOptions(false);
      config = (TzsvrConfig)opts.getAppProperty(appname);
      if (config == null) {
        config = new TzsvrConfig();
      }
    } catch (OptionsException e) {
      throw new ServletException(e);
    }

    if (tzdataUrl == null) {
      tzdataUrl = config.getTzdataUrl();
    }

    getcache();
  }

  /* ====================================================================
   *                   Static methods
   * ==================================================================== */

  /**
   * @return a singleton instance
   * @throws ServletException
   */
  public static TzServerUtil getInstance() throws ServletException {
    if (instance != null) {
      return instance;
    }

    synchronized (locker) {
      if (instance != null) {
        return instance;
      }

      instance = new TzServerUtil();
    }

    return instance;
  }

  /** Set before calling getInstance
   *
   * @param val
   */
  public static void setAppname(final String val) {
    appname = val;
  }

  /**
   * @return appname
   */
  public static String getAppname() {
    return appname;
  }

  /** Set before calling getInstance if overriding config
   *
   * @param val
   */
  public static void setTzdataUrl(final String val) {
    tzdataUrl = val;
  }

  /**
   * @return tzdataUrl
   */
  public static String getTzdataUrl() {
    return tzdataUrl;
  }

  /** Url of server we refresh from. Null if we are a primary server
   *
   * @param val    String
   */
  public static void setPrimaryUrl(final String val) {
    primaryUrl = val;
  }

  /**
   * @return String
   */
  public static String getPrimaryUrl() {
    return primaryUrl;
  }

  /** Cause a refresh of the data
   *
   * @throws ServletException
   */
  public static void fireRefresh() throws ServletException {
    getInstance().getcache();
  }

  /** Cause an update of the data
   *
   * @throws ServletException
   */
  public static void fireUpdate() throws ServletException {
    getInstance().cache.update();
  }

  /**
   * @return stats for the service
   * @throws ServletException
   */
  public static List<Stat> getStats() throws ServletException {
    List<Stat> stats = new ArrayList<Stat>();

    stats.add(new Stat("Gets", String.valueOf(gets)));
    stats.add(new Stat("Hits", String.valueOf(cacheHits)));
    stats.add(new Stat("Name lists", String.valueOf(nameLists)));
    stats.add(new Stat("Reads", String.valueOf(reads)));
    stats.add(new Stat("conversions",
                       String.valueOf(conversions),
                       String.valueOf(conversionsMillis)));
    stats.add(new Stat("tzfetches", String.valueOf(tzfetches)));
    stats.add(new Stat("tzreloads",
                       String.valueOf(reloads),
                       String.valueOf(reloadsMillis)));
    stats.add(new Stat("expands",
                       String.valueOf(expands),
                       String.valueOf(expandsMillis)));

    return stats;
  }

  /* ====================================================================
   *                   Instance methods
   * ==================================================================== */

  /**
   * @return the data dtsamp
   * @throws ServletException
   */
  public XMLGregorianCalendar getDtstamp() throws ServletException {
    if (dtstamp == null) {
      String dtst = cache.getDtstamp();

      if (dtst == null) {
        DtStamp dt =  new DtStamp(new DateTime(lastDataFetch));

        dtst = dt.getValue();
      }

      try {
        dtstamp = XcalUtil.fromDtval(dtst);
      } catch (Throwable t) {
        throw new ServletException(t);
      }
    }

    return dtstamp;
  }

  /**
   * @return config
   */
  public TzsvrConfig getConfig() {
    return config;
  }

  /**
   * @return names from the zip file.
   * @throws ServletException
   */
  public SortedSet<String> getNames() throws ServletException {
    nameLists++;

    return cache.getNameList();
  }

  /**
   * @param name
   * @return spec
   * @throws ServletException
   */
  public String getTz(final String name) throws ServletException {
    return cache.getCachedVtz(name);
  }

  /**
   * @return all specs
   * @throws ServletException
   */
  public Collection<String> getAllTzs() throws ServletException {
    return cache.getAllCachedVtzs();
  }

  /**
   * @param name
   * @return spec
   * @throws ServletException
   */
  public String getAliasedTz(final String name) throws ServletException {
    return cache.getAliasedCachedVtz(name);
  }

  /* *
   * @param tzid - possible alias
   * @return actual timezone id
   * @throws ServletException
   * /
  public String unalias(String tzid) throws ServletException {
    if (tzid == null) {
      throw new ServletException("Null id for unalias");
    }

    /* First transform the name if it follows a known pattern, for example
     * we used to get     /mozilla.org/20070129_1/America/New_York
     * /

    tzid = transformTzid(tzid);

    // Allow chains of aliases

    String target = tzid;

    for (int i = 0; i < 100; i++) {   // Just in case we get a circular chain
      String unaliased = cache.fromAlias(target);

      if (unaliased == null) {
        return target;
      }

      if (unaliased.equals(tzid)) {
        break;
      }

      target = unaliased;
    }

    error("Possible circular alias chain looking for " + tzid);

    return null;
  } */

  /**
   * @return String value of aliases file.
   * @throws ServletException
   */
  public String getAliasesStr() throws ServletException {
    return cache.getAliasesStr();
  }

  /**
   * @param tzid
   * @return list of aliases or null
   * @throws ServletException
   */
  public List<String> findAliases(String tzid) throws ServletException {
    return cache.findAliases(tzid);
  }

  /**
   * @param time
   * @param tzid
   * @return String utc date
   * @throws Throwable
   */
  public String getUtc(final String time,
                       final String tzid) throws Throwable {
    if (DateTimeUtil.isISODateTimeUTC(time)) {
      // Already UTC
      return time;
    }

    if (!DateTimeUtil.isISODateTime(time)) {
      return null;  // Bad datetime
    }

    conversions++;
    long smillis = System.currentTimeMillis();

    TimeZone tz = fetchTimeZone(tzid);

    DateFormat formatTd  = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    formatTd.setTimeZone(tz);

    Date date = formatTd.parse(time);
    String utc;

    synchronized (cal) {
      cal.clear();
      cal.setTime(date);

      //formatTd.setTimeZone(utctz);
      //trace("formatTd with utc: " + formatTd.format(date));

      StringBuilder sb = new StringBuilder();
      digit4(sb, cal.get(Calendar.YEAR));
      digit2(sb, cal.get(Calendar.MONTH) + 1); // Month starts at 0
      digit2(sb, cal.get(Calendar.DAY_OF_MONTH));
      sb.append('T');
      digit2(sb, cal.get(Calendar.HOUR_OF_DAY));
      digit2(sb, cal.get(Calendar.MINUTE));
      digit2(sb, cal.get(Calendar.SECOND));
      sb.append('Z');

      utc = sb.toString();
    }

    conversionsMillis += System.currentTimeMillis() - smillis;

    return utc;
  }

  /** Convert from local time in fromTzid to local time in toTzid. If dateTime is
   * already an iso utc date time fromTzid may be null.
   *
   * @param dateTime
   * @param fromTzid
   * @param toTzid
   * @return String time in given timezone
   * @throws Throwable
   */
  public String convertDateTime(final String dateTime, final String fromTzid,
                                final String toTzid) throws Throwable {
    String UTCdt = null;
    if (DateTimeUtil.isISODateTimeUTC(dateTime)) {
      // Already UTC
      UTCdt = dateTime;
    } else if (!DateTimeUtil.isISODateTime(dateTime)) {
      return null;  // Bad datetime
    } else if (toTzid == null) {
      return null;  // Bad toTzid
    } else {
      UTCdt = getUtc(dateTime, fromTzid);
      conversions--; // avoid double inc
    }

    conversions++;
    long smillis = System.currentTimeMillis();

    // Convert to time in toTzid

    Date dt = DateTimeUtil.fromISODateTimeUTC(UTCdt);

    TimeZone tz = fetchTimeZone(toTzid);
    if (tz == null) {
      return null;
    }

    String cdt = DateTimeUtil.isoDateTime(dt, tz);
    conversionsMillis += System.currentTimeMillis() - smillis;

    return cdt;
  }

  /**
   * @param changedSince - null or dtstamp value
   * @return list of summary info
   * @throws ServletException
   */
  public List<SummaryType> getSummaries(String changedSince) throws ServletException {
    return cache.getSummaries(changedSince);
  }

  private static class ObservanceWrapper implements Comparable<ObservanceWrapper> {
    ObservanceType ot;

    ObservanceWrapper(ObservanceType ot) {
      this.ot = ot;
    }

    @Override
    public int compareTo(ObservanceWrapper o) {
      return ot.getOnset().toXMLFormat().compareTo(o.ot.getOnset().toXMLFormat());
    }
  }

  /**
   * @param tzid
   * @param start
   * @param end
   * @return expansion or null
   * @throws Throwable
   */
  public ExpandedMapEntry getExpanded(String tzid,
                                      String start,
                                      String end) throws Throwable {
    expandFetches++;

    ExpandedMapEntryKey emek = makeExpandedKey(tzid, start, end);

    ExpandedMapEntry tzs = cache.getExpanded(emek);
    if (tzs != null) {
      expandHits++;
      return tzs;
    }

    long smillis = System.currentTimeMillis();

    TimeZone tz = fetchTimeZone(tzid);
    if (tz == null) {
      return null;
    }

    VTimeZone vtz = tz.getVTimeZone();

    DateTime dtstart = new DateTime(emek.getStart());
    DateTime dtend = new DateTime(emek.getEnd());

    dtstart.setTimeZone(tz);
    dtend.setTimeZone(tz);

    Period p = new Period(dtstart, dtend);

    ComponentList cl = vtz.getObservances();

    TreeSet<ObservanceWrapper> obws = new TreeSet<ObservanceWrapper>();

    for (Object o: cl) {
      Observance ob = (Observance)o;

      PeriodList pl = ob.calculateRecurrenceSet(p);

      for (Object po: pl) {
        Period onsetPer = (Period)po;

        ObservanceType ot = new ObservanceType();

        ot.setName(ob.getName());
        ot.setOnset(XcalUtil.fromDtval(onsetPer.getStart().toString()));

        String offset = ob.getOffsetFrom().getOffset().toString();

        if (offset.length() > 5) {
          offset = offset.substring(0, offset.length() - 2);
        }
        ot.setUtcOffsetFrom(offset);

        offset = ob.getOffsetTo().getOffset().toString();

        if (offset.length() > 5) {
          offset = offset.substring(0, offset.length() - 2);
        }
        ot.setUtcOffsetTo(offset);

        obws.add(new ObservanceWrapper(ot));
      }
    }

    TzdataType tzd = new TzdataType();

    tzd.setTzid(tzid);
    for (ObservanceWrapper ow: obws) {
      tzd.getObservance().add(ow.ot);
    }

    TimezonesType tzt = new TimezonesType();

    tzt.setDtstamp(getDtstamp());
    tzt.getTzdata().add(tzd);

    tzs = new ExpandedMapEntry(String.valueOf(smillis), tzt);

    cache.setExpanded(emek, tzs);

    expandsMillis += System.currentTimeMillis() - smillis;
    expands++;

    return tzs;
  }

  /** Get a timezone object from the server given the id.
   *
   * @param tzid
   * @return TimeZone with id or null
   * @throws ServletException
   */
  public TimeZone fetchTimeZone(final String tzid) throws ServletException {
    tzfetches++;

    return cache.getTimeZone(tzid);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  public String getCalHdr() {
    return "BEGIN:VCALENDAR\n" +
           "VERSION:2.0\n" +
           "CALSCALE:GREGORIAN\n" +
           "PRODID:/bedework.org//NONSGML Bedework//EN\n";
  }

  public String getCalTlr() {
    return "END:VCALENDAR\n";
  }

  /**
   * @return an etag based on when we refreshed data
   * @throws ServletException
   */
  public String getEtag() throws ServletException {
    StringBuilder val = new StringBuilder();

    val.append("\"");
    val.append(getDtstamp().toXMLFormat());
    val.append("\"");

    return val.toString();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private ExpandedMapEntryKey makeExpandedKey(String tzid,
                                              String start,
                                              String end) throws ServletException {
    String st = start;

    if (st == null) {
      String date = new net.fortuna.ical4j.model.Date().toString();

      st = date + "T000000Z";
    }

    String e = end;
    if (e == null) {
      Dur dur = new Dur("P520W");

      String date = new net.fortuna.ical4j.model.Date(dur.getTime(new Date())).toString();
      e = date + "T000000Z";
    }

    return new ExpandedMapEntryKey(tzid, st, e);
  }

  private static String transformTzid(String tzid) {
    int len = tzid.length();

    if ((len > 13) && (tzid.startsWith("/mozilla.org/"))) {
      int pos = tzid.indexOf('/', 13);

      if ((pos < 0) || (pos == len - 1)) {
        return tzid;
      }
      return tzid.substring(pos + 1);
    }

    /* Special to get James Andrewartha going */
    String ss = "/softwarestudio.org/Tzfile/";

    if ((len > ss.length()) &&
        (tzid.startsWith(ss))) {
      return tzid.substring(ss.length());
    }

    return tzid;
  }

  private void getcache() throws ServletException {
    if (tryDb) {
      try {
        cache = new DbCachedData(false, primaryUrl);
      } catch (DbEmptyException dbee) {
        /* try to populate from zipped data */

        if (addDbData()) {
          try {
            cache = new DbCachedData(false, primaryUrl);
          } catch (TzException te) {
            error(te);
          }
        }
      } catch (TzException te) {
        error(te);
      }
    }

    if (cache == null) {
      cache = new ZipCachedData(tzdataUrl);
    }
  }

  private boolean addDbData() {
    DbCachedData db = null;

    try {
      CachedData z = new ZipCachedData(tzdataUrl);

      db = new DbCachedData(true, primaryUrl);

      db.startAdd();

      TzDbInfo dbi = new TzDbInfo();

      dbi.setDtstamp(z.getDtstamp());
      dbi.setVersion("1.0");
      db.addTzInfo(dbi);

      List<SummaryType> sums = z.getSummaries(null);

      for (SummaryType sum: sums) {
        if (sum.getAlias() != null) {
          for (AliasType at: sum.getAlias()) {
            TzAlias alias = new TzAlias();

            alias.setFromId(at.getValue());
            alias.setToId(sum.getTzid());

            db.addTzAlias(alias);
          }
        }

        TzDbSpec spec = new TzDbSpec();

        spec.setName(sum.getTzid());

        spec.setVtimezone(getCalHdr() +
                          z.getCachedVtz(sum.getTzid()) +
                          getCalTlr());
        if (spec.getVtimezone() == null) {
          error("No timezone spec for " + sum.getTzid());
        }

        spec.setDtstamp(z.getDtstamp());
        spec.setActive(true);

        db.addTzSpec(spec);
      }

      db.endAdd();

      db = null;

      return true;
    } catch (ServletException se) {
      getLogger().error("Unable to add tz data to db", se);
      if (db !=  null) {
        db.failAdd();
      }
      return false;
    } catch (TzException te) {
      getLogger().error("Unable to add tz data to db", te);
      if (db !=  null) {
        db.failAdd();
      }
      return false;
    } finally {
      if (db !=  null) {
        db.endAdd();
      }
    }
  }

  private static Calendar cal = Calendar.getInstance();
  private static java.util.TimeZone utctz;

  static {
    try {
      utctz = TimeZone.getTimeZone(TimeZones.UTC_ID);
    } catch (Throwable t) {
      throw new RuntimeException("Unable to initialise UTC timezone");
    }
    cal.setTimeZone(utctz);
  }

  /**
   * @return Logger
   */
  static Logger getLogger() {
    return Logger.getLogger(TzServerUtil.class);
  }

  /** Debug
   *
   * @param msg
   */
  static void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  /** Info messages
   *
   * @param msg
   */
  static void logIt(final String msg) {
    getLogger().info(msg);
  }

  static void error(final String msg) {
    getLogger().error(msg);
  }

  static void error(final Throwable t) {
    getLogger().error(TzServerUtil.class, t);
  }

  private static void digit2(final StringBuilder sb, final int val) throws Throwable {
    if (val > 99) {
      throw new Exception("Bad date");
    }
    if (val < 10) {
      sb.append("0");
    }
    sb.append(val);
  }

  private static void digit4(final StringBuilder sb, final int val) throws Throwable {
    if (val > 9999) {
      throw new Exception("Bad date");
    }
    if (val < 10) {
      sb.append("000");
    } else if (val < 100) {
      sb.append("00");
    } else if (val < 1000) {
      sb.append("0");
    }
    sb.append(val);
  }
}
