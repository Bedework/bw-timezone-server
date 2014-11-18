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
package org.bedework.timezones.common;

import org.bedework.timezones.common.Differ.DiffListEntry;
import org.bedework.timezones.common.leveldb.LdbCachedData;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.jmx.ConfigHolder;
import org.bedework.util.timezones.DateTimeUtil;
import org.bedework.util.timezones.model.ExpandedTimezoneType;
import org.bedework.util.timezones.model.ObservanceType;
import org.bedework.util.timezones.model.TimezoneType;

import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.UtcOffset;
import net.fortuna.ical4j.model.component.Observance;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.util.TimeZones;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/** Common code for the timezone service.
 *
 *   @author Mike Douglass
 */
public class TzServerUtil {
  private static String appname = "tzsvr";

  private static TzServerUtil instance;

  static ConfigHolder<TzConfig> cfgHolder;

  private static Object locker = new Object();

  private static String prodid = "/bedework.org//NONSGML Bedework//EN";

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

  /**
   * @throws TzException
   */
  private TzServerUtil() throws TzException {
  }

  /* ====================================================================
   *                   Static methods
   * ==================================================================== */

  /**
   * @return a singleton instance
   * @throws TzException
   */
  public static TzServerUtil getInstance() throws TzException {
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

  /**
   * @return appname
   */
  public static String getAppname() {
    return appname;
  }

  /**
   * @param val
   */
  public static void setTzConfigHolder(final ConfigHolder<TzConfig> val) {
    cfgHolder = val;
  }

  /**
   * @return path of config directory.
   */
  public static String getConfigDir() {
    if (cfgHolder == null) {
      return null;
    }

    return cfgHolder.getConfigUri();
  }

  /**
   * @return current state of the configuration
   */
  public static TzConfig getTzConfig() {
    if (cfgHolder == null) {
      return null;
    }

    return cfgHolder.getConfig();
  }

  /**
   *
   */
  public static void saveConfig() {
    if (cfgHolder != null) {
      cfgHolder.putConfig();
    }
  }

  /**
   * @param millis long
   * @return String minutes:seconds
   */
  public static String printableTime(final long millis) {
    final long seconds = millis / 1000;
    final long minutes = seconds / 60;

    return String.valueOf(minutes) + ":" + (seconds - minutes * 60);
  }

  /** Cause a refresh of the data
   *
   * @throws TzException
   */
  public static void fireRefresh(final boolean clear) throws TzException {
    final TzServerUtil tzutil = getInstance();

    if (tzutil.cache != null) {
      try {
        tzutil.cache.stop();
      } catch (final Throwable t) {
        error(t);
        error("Error stopping cache");
      }

      tzutil.cache = null;
    }
    tzutil.getcache(clear);
  }

  /**
   * @param val prodid for generated calendar data
   */
  public static void setProdid(final String val) {
    prodid = val;
  }

  /** Cause data to be checked against primary
   *
   * @throws TzException
   */
  public static void fireCheck() throws TzException {
    getInstance().getcache().checkData();
  }

  /** Compare data pointed to by tzdataUrl with the given data then update.
   *
   * @param tzdataUrl - references source
   * @return info lines.
   * @throws TzException
   */
  public static List<String> updateData(final String tzdataUrl) throws TzException {
    TzServerUtil util = getInstance();

    Differ diff = new Differ();

    final TzConfig newConfig = new TzConfig();
    getTzConfig().copyTo(newConfig);
    newConfig.setTzdataUrl(tzdataUrl);

    final CachedData cd = getDataSource(newConfig);

    final List<DiffListEntry> dles = diff.compare(cd, util.getcache());

    final List<String> out = new ArrayList<>();

    if (dles == null) {
      out.add("No data returned");
      return out;
    }

    for (final DiffListEntry dle: dles) {
      out.add(dle.toShortString());
    }

    if (dles.size() == 0) {
      return out;
    }

    try {
      final DatatypeFactory dtf = DatatypeFactory.newInstance();

      final XMLGregorianCalendar xgc = dtf.newXMLGregorianCalendar(new GregorianCalendar());
      xgc.setFractionalSecond(null);
      final String dtstamp = xgc.normalize().toXMLFormat();

      util.getcache().updateData(dtstamp, dles);

      /* Now do a reload to ensure we have the new data in the cache */

      fireRefresh(false);

      return out;
    } catch (final TzException te) {
      throw te;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  /** Compare data pointed to by tzdataUrl with the given data.
   *
   * @param tzdataUrl - reference to data
   * @return info lines.
   * @throws TzException
   */
  public static List<String> compareData(final String tzdataUrl) throws TzException {
    final TzServerUtil util = getInstance();

    final Differ diff = new Differ();

    final TzConfig newConfig = new TzConfig();
    getTzConfig().copyTo(newConfig);
    newConfig.setTzdataUrl(tzdataUrl);

    final CachedData cd = getDataSource(newConfig);

    final List<DiffListEntry> dles = diff.compare(cd, util.getcache());

    final List<String> out = new ArrayList<String>();

    if (dles == null) {
      out.add("No data returned");
      return out;
    }

    for (final DiffListEntry dle: dles) {
      out.add(dle.toShortString());
    }

    return out;
  }

  public static CachedData getDataSource(final TzConfig config) throws TzException {
    final String tzdataUrl = config.getTzdataUrl();

    if (tzdataUrl == null) {
      error("No tz data url");
      return null;
    }

    if (tzdataUrl.endsWith(".zip")) {
      return new ZipCachedData(config);
    }

    return new FileCachedData(config);
  }

  /**
   * @return stats for the service
   * @throws TzException
   */
  public static List<Stat> getStats() throws TzException {
    List<Stat> stats = new ArrayList<>();

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

    if (getInstance().getcache() != null) {
      stats.addAll(getInstance().getcache().getStats());
    }

    return stats;
  }

  /* ====================================================================
   *                   Instance methods
   * ==================================================================== */

  /** Stop any running threads.
   *
   * @throws TzException
   */
  public void stop() throws TzException {
    if (cache != null) {
      cache.stop();
    }
  }

  /**
   * @return the data dtstamp
   * @throws TzException
   */
  public String getDtstamp() throws TzException {
    String dtst = getcache().getDtstamp();
    if (dtst != null) {
      return dtst;
    }

    return DateTimeUtil.rfcDateTimeUTC(new DateTime(lastDataFetch));
  }

  /**
   * @return names from the zip file.
   * @throws TzException
   */
  public SortedSet<String> getNames() throws TzException {
    nameLists++;

    return getcache().getNameList();
  }

  /**
   * @param name
   * @return spec
   * @throws TzException
   */
  public String getTz(final String name) throws TzException {
    return getcache().getCachedVtz(name);
  }

  /**
   * @return all specs
   * @throws TzException
   */
  public Collection<String> getAllTzs() throws TzException {
    return getcache().getAllCachedVtzs();
  }

  /**
   * @param name
   * @return spec
   * @throws TzException
   */
  public String getAliasedTz(final String name) throws TzException {
    return getcache().getAliasedCachedVtz(name);
  }

  /**
   * @return String value of aliases file.
   * @throws TzException
   */
  public String getAliasesStr() throws TzException {
    return getcache().getAliasesStr();
  }

  /**
   * @param tzid
   * @return list of aliases or null
   * @throws TzException
   */
  public SortedSet<String> findAliases(final String tzid) throws TzException {
    return getcache().findAliases(tzid);
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
    final long smillis = System.currentTimeMillis();

    // Convert to time in toTzid

    final Date dt = DateTimeUtil.fromISODateTimeUTC(UTCdt);

    final TimeZone tz = fetchTimeZone(toTzid);
    if (tz == null) {
      return null;
    }

    final String cdt = DateTimeUtil.isoDateTime(dt, tz);
    conversionsMillis += System.currentTimeMillis() - smillis;

    return cdt;
  }

  /**
   * @param tzids - to fetch
   * @return list of summary info
   * @throws TzException
   */
  public List<TimezoneType> getTimezones(final String[] tzids) throws TzException {
    return getcache().getTimezones(tzids);
  }

  /**
   * @param changedSince - null or dtstamp value
   * @return list of summary info
   * @throws TzException
   */
  public List<TimezoneType> getTimezones(final String changedSince) throws TzException {
    return getcache().getTimezones(changedSince);
  }

  /**
   * @param name - non null name for partial match
   * @return list of summary info
   * @throws TzException
   */
  public List<TimezoneType> findTimezones(final String name) throws TzException {
    return getcache().findTimezones(name);
  }

  private static class ObservanceWrapper implements Comparable<ObservanceWrapper> {
    ObservanceType ot;

    ObservanceWrapper(final ObservanceType ot) {
      this.ot = ot;
    }

    @Override
    public int compareTo(final ObservanceWrapper o) {
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
  public ExpandedMapEntry getExpanded(final String tzid,
                                      final String start,
                                      final String end,
                                      final boolean oldForm) throws Throwable {
    expandFetches++;

    final ExpandedMapEntryKey emek = makeExpandedKey(tzid, start, end);

    ExpandedMapEntry tzs = getcache().getExpanded(emek);
    if (tzs != null) {
      expandHits++;
      return tzs;
    }

    final long smillis = System.currentTimeMillis();

    final TimeZone tz = fetchTimeZone(tzid);
    if (tz == null) {
      return null;
    }

    final VTimeZone vtz = tz.getVTimeZone();

    final DateTime dtstart = new DateTime(emek.getStart());
    final DateTime dtend = new DateTime(emek.getEnd());

    dtstart.setTimeZone(tz);
    dtend.setTimeZone(tz);

    final Period p = new Period(dtstart, dtend);

    final ComponentList cl = vtz.getObservances();

    final TreeSet<ObservanceWrapper> obws = new TreeSet<>();

    for (final Object o: cl) {
      final Observance ob = (Observance)o;

      final PeriodList pl = ob.calculateRecurrenceSet(p);

      for (final Object po: pl) {
        final Period onsetPer = (Period)po;

        final ObservanceType ot = new ObservanceType();

        ot.setName(ob.getName());
        ot.setOnset(XcalUtil.getXmlFormatDateTime(
                onsetPer.getStart().toString()));

        ot.setUtcOffsetFrom(
                (int)(ob.getOffsetFrom().getOffset().getOffset() / 1000));

        ot.setUtcOffsetTo(
                (int)(ob.getOffsetTo().getOffset().getOffset() / 1000));

        obws.add(new ObservanceWrapper(ot));
      }
    }

    final ExpandedTimezoneType etzt = new ExpandedTimezoneType();

    etzt.setDtstamp(getDtstamp());
    if (!oldForm) {
      etzt.setTzid(tzid);
    }

    for (final ObservanceWrapper ow: obws) {
      if (etzt.getObservances() == null) {
        etzt.setObservances(new ArrayList<ObservanceType>());
      }
      etzt.getObservances().add(ow.ot);
    }

    tzs = new ExpandedMapEntry(String.valueOf(smillis), etzt);

    getcache().setExpanded(emek, tzs);

    expandsMillis += System.currentTimeMillis() - smillis;
    expands++;

    return tzs;
  }

  private String delimited(final UtcOffset val) {
    String offset = val.toString();

    int pos = 0;
    StringBuilder sb = new StringBuilder();

    if (offset.startsWith("-") || offset.startsWith("+")) {
      sb.append(offset.charAt(0));
      pos = 1;
    }

    sb.append(offset.substring(pos, pos + 2));
    sb.append(':');
    pos += 2;
    sb.append(offset.substring(pos, pos + 2));
    pos += 2;

    if (pos < offset.length()) {
      sb.append(':');
      sb.append(offset.substring(pos, pos + 2));
    }

    return sb.toString();
  }

  /** Get a timezone object from the server given the id.
   *
   * @param tzid
   * @return TimeZone with id or null
   * @throws TzException
   */
  public TimeZone fetchTimeZone(final String tzid) throws TzException {
    tzfetches++;

    return getcache().getTimeZone(tzid);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /**
   * @return an ical Calendar prefix
   */
  public static String getCalHdr() {
    return "BEGIN:VCALENDAR\n" +
           "VERSION:2.0\n" +
           "CALSCALE:GREGORIAN\n" +
           "PRODID:" + prodid + "\n";
  }

  /**
   * @return an ical Calendar trailer
   */
  public static String getCalTlr() {
    return "END:VCALENDAR\n";
  }

  /**
   * @return an etag based on when we refreshed data
   * @throws TzException
   */
  public String getEtag() throws TzException {
    StringBuilder val = new StringBuilder();

    val.append("\"");
    val.append(getDtstamp());
    val.append("\"");

    return val.toString();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private ExpandedMapEntryKey makeExpandedKey(final String tzid,
                                              final String start,
                                              final String end) throws TzException {
    /* Start and end may be null or intergers represeting start/end year
     */

    final net.fortuna.ical4j.model.Date startDate =
        makeStartDateFromYear(start);
    final String st = startDate + "T000000Z";

    final String e = makeEndDateFromYear(startDate, end) + "T000000Z";

    if (st.compareTo(e) >= 0) {
      throw new TzException(HttpServletResponse.SC_BAD_REQUEST,
                            "badly formed date range");
    }

    return new ExpandedMapEntryKey(tzid, st, e);
  }

  private net.fortuna.ical4j.model.Date makeStartDateFromYear(
          final String val) throws TzException {
    if (val == null) {
      return new net.fortuna.ical4j.model.Date();
    }

    try {
      return new net.fortuna.ical4j.model.Date(
              val + "0101");
    } catch (final Throwable t) {
      throw new TzException(HttpServletResponse.SC_BAD_REQUEST,
                            "badly formed date " + val);
    }
  }

  private net.fortuna.ical4j.model.Date makeEndDateFromYear(
          final net.fortuna.ical4j.model.Date start,
          final String val) throws TzException {
    if (val == null) {
      final Dur dur = new Dur("P520W");

      return new net.fortuna.ical4j.model.Date(dur.getTime(
              new Date(start.getTime())
      ));
    }

    try {
      return new net.fortuna.ical4j.model.Date(
              val + "0101");
    } catch (final Throwable t) {
      throw new TzException(HttpServletResponse.SC_BAD_REQUEST,
                            "badly formed date " + val);
    }
  }

  private CachedData getcache() throws TzException {
    if (cache == null) {
      getcache(false);
    }

    return cache;
  }

  private void getcache(final boolean clear) throws TzException {
    final TzConfig cfg = getTzConfig();

    if (cfg == null) {
      error("No config data");
      return;
    }

    try {
      cache = new LdbCachedData(cfg, clear);
    } catch (final TzException te) {
      error(te);
      cache = null;
    }

    if (cache == null) {
      cache = getDataSource(cfg);
    }
  }

  private static final Calendar cal = Calendar.getInstance();
  private static final java.util.TimeZone utctz;

  static {
    try {
      utctz = TimeZone.getTimeZone(TimeZones.UTC_ID);
    } catch (final Throwable t) {
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

  static void info(final String msg) {
    getLogger().info(msg);
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
