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

import org.apache.log4j.Logger;

import ietf.params.xml.ns.timezone_service.ObservanceType;
import ietf.params.xml.ns.timezone_service.SummaryType;
import ietf.params.xml.ns.timezone_service.Timezones;
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

import edu.rpi.sss.util.DateTimeUtil;
import edu.rpi.sss.util.OptionsException;
import edu.rpi.sss.util.OptionsI;

/** Common code for the timezone service.
 *
 *   @author Mike Douglass
 */
public class TzServerUtil {
  private static String appname = "tzsvr";

  private TzsvrConfig config;

  private static TzServerUtil instance;

  /* ======================= Error codes ======================= */

  /** Unable to retrieve the data */
  public static final String errorNodata = "org.tserver.no.data";

  /* ======================= Caching ======================= */

  private CachedData cache;

  /** Time we last fetched the data */
  public static long lastDataFetch;

  static String etagValue;

  static String dtstamp;

  static String tzdataUrl;

  /* ======================= TimeZone objects ======================= */

  private static Object zipLock = new Object();

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

    cache = new CachedData(tzdataUrl);
  }

  /**
   * @return a singleton instance
   * @throws ServletException
   */
  public static TzServerUtil getInstance() throws ServletException {
    if (instance != null) {
      return instance;
    }

    synchronized (zipLock) {
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

    if (instance != null) {
      instance.cache.setTzdataUrl(val);
    }
  }

  /**
   * @return tzdataUrl
   */
  public static String getTzdataUrl() {
    return tzdataUrl;
  }

  /** Cause a refresh of the data
   */
  public void fireRefresh() {
    cache.refresh();
  }

  /**
   * @return an etag based on when we refreshed data
   * @throws ServletException
   */
  public String getEtag() throws ServletException {
    if (etagValue == null) {
      Collection<String> info = cache.getDataInfo();

      for (String s: info) {
        if (s.startsWith("buildTime=")) {
          etagValue = s.substring("buildTime=".length());
          break;
        }
      }

      if (etagValue == null) {
        etagValue = String.valueOf(lastDataFetch);
      }
    }

    StringBuilder val = new StringBuilder();

    val.append("\"");
    val.append(etagValue);
    val.append("\"");

    return val.toString();
  }

  /**
   * @return the data dtsamp
   * @throws ServletException
   */
  public String getDtstamp() throws ServletException {
    if (dtstamp == null) {
      Collection<String> info = cache.getDataInfo();

      for (String s: info) {
        if (s.startsWith("buildTime=")) {
          dtstamp = s.substring("buildTime=".length());
          break;
        }
      }

      if (dtstamp == null) {
        DtStamp dt =  new DtStamp(new DateTime(lastDataFetch));

        dtstamp = dt.getValue();
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
   * @param tzid - possible alias
   * @return actual timezone id
   * @throws ServletException
   */
  public String unalias(String tzid) throws ServletException {
    if (tzid == null) {
      throw new ServletException("Null id for unalias");
    }

    /* First transform the name if it follows a known pattern, for example
     * we used to get     /mozilla.org/20070129_1/America/New_York
     */

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
  }

  /**
   * @return data info
   * @throws ServletException
   */
  public Collection<String> getDataInfo() throws ServletException {
    return cache.getDataInfo();
  }

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
   * @return list of summary info
   * @throws ServletException
   */
  public List<SummaryType> getSummaries() throws ServletException {
    return cache.getSummaries();
  }

  private static class ObservanceWrapper implements Comparable<ObservanceWrapper> {
    ObservanceType ot;

    ObservanceWrapper(ObservanceType ot) {
      this.ot = ot;
    }

    @Override
    public int compareTo(ObservanceWrapper o) {
      return ot.getOnset().compareTo(o.ot.getOnset());
    }
  }

  /**
   * @param tzid
   * @param start
   * @param end
   * @return expansion or null
   * @throws Throwable
   */
  public Timezones getExpanded(String tzid,
                               String start,
                               String end) throws Throwable {
    expandFetches++;

    ExpandedMapEntryKey emek = makeExpandedKey(tzid, start, end);

    Timezones tzs = cache.getExpanded(emek);
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
        ot.setOnset(onsetPer.getStart().toString());

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
      tzd.getObservances().add(ow.ot);
    }

    tzs = new Timezones();

    tzs.setDtstamp(getDtstamp());
    tzs.getTzdatas().add(tzd);

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
