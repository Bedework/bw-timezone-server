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
import org.bedework.timezones.common.h2db.H2dbCachedData;
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.jmx.ConfigHolder;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.timezones.DateTimeUtil;
import org.bedework.util.timezones.model.ExpandedTimezoneType;
import org.bedework.util.timezones.model.ObservanceType;
import org.bedework.util.timezones.model.TimezoneType;

import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.TemporalAmountAdapter;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.Observance;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.util.TimeZones;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletResponse;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/** Common code for the timezone service.
 *
 *   @author Mike Douglass
 */
public class TzServerUtil {
  private static final BwLogger logger =
          new BwLogger().setLoggedClass(TzServerUtil.class);

  private static final String appname = "tzsvr";

  private static TzServerUtil instance;

  static ConfigHolder<TzConfigImpl> cfgHolder;

  private static final Object locker = new Object();

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
   */
  private TzServerUtil() {
  }

  /* ====================================================================
   *                   Static methods
   * ==================================================================== */

  /**
   * @return a singleton instance
   */
  public static TzServerUtil getInstance() {
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
   * @param val config holder
   */
  public static void setTzConfigHolder(final ConfigHolder<TzConfigImpl> val) {
    cfgHolder = val;
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

    return minutes + ":" + (seconds - minutes * 60);
  }

  /** Cause a refresh of the data
   *
   */
  public static void fireRefresh(final boolean clear) {
    final TzServerUtil tzutil = getInstance();

    if (tzutil.cache != null) {
      try {
        tzutil.cache.stop();
      } catch (final Throwable t) {
        logger.error(t);
        logger.error("Error stopping cache");
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
   */
  public static void fireCheck() {
    getInstance().getcache().checkData();
  }

  /** Compare data pointed to by tzdataUrl with the given data then update.
   *
   * @param tzdataUrl - references source
   * @return info lines.
   */
  public static List<String> updateData(final String tzdataUrl) {
    final TzServerUtil util = getInstance();

    final Differ diff = new Differ();

    final TzConfig newConfig = new TzConfigImpl();
    final var cfg = ((TzConfigImpl)getTzConfig());
    if (cfg == null) {
      throw new TzException("Null configuration");
    }

    cfg.copyTo(newConfig);
    newConfig.setTzdataUrl(tzdataUrl);

    final CachedData cd = getDataSource(newConfig);

    if (cd == null) {
      throw new TzException(
              "Unable to read datasource with config " +
                      newConfig);
    }

    final List<DiffListEntry> dles = diff.compare(cd, util.getcache());

    final List<String> out = new ArrayList<>();

    if (dles == null) {
      out.add("No data returned");
      return out;
    }

    for (final DiffListEntry dle: dles) {
      out.add(dle.toShortString());
    }

    if (dles.isEmpty()) {
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
   */
  public static List<String> compareData(final String tzdataUrl) {
    final TzServerUtil util = getInstance();

    final Differ diff = new Differ();

    final TzConfig newConfig = new TzConfigImpl();
    final var cfg = ((TzConfigImpl)getTzConfig());
    if (cfg == null) {
      throw new TzException("Null configuration");
    }

    cfg.copyTo(newConfig);
    newConfig.setTzdataUrl(tzdataUrl);

    final CachedData cd = getDataSource(newConfig);

    if (cd == null) {
      throw new TzException(
              "Unable to read datasource with config " +
                      newConfig);
    }

    final List<DiffListEntry> dles = diff.compare(cd, util.getcache());

    final List<String> out = new ArrayList<>();

    if (dles == null) {
      out.add("No data returned");
      return out;
    }

    for (final DiffListEntry dle: dles) {
      out.add(dle.toShortString());
    }

    return out;
  }

  public static CachedData getDataSource(final TzConfig config) {
    final String tzdataUrl = config.getTzdataUrl();

    if (tzdataUrl == null) {
      logger.error("No tz data url");
      return null;
    }

    if (tzdataUrl.endsWith(".zip")) {
      return new ZipCachedData(config);
    }

    return new FileCachedData(config);
  }

  /**
   * @return stats for the service
   */
  public static List<Stat> getStats() {
    final List<Stat> stats = new ArrayList<>();

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
   */
  public void stop() {
    if (cache != null) {
      cache.stop();
    }
  }

  /**
   * @return the data dtstamp
   */
  public String getDtstamp() {
    final String dtst = getcache().getDtstamp();
    if (dtst != null) {
      return dtst;
    }

    return DateTimeUtil.rfcDateTimeUTC(new DateTime(lastDataFetch));
  }

  /**
   * @return names from the zip file.
   */
  public SortedSet<String> getNames() {
    nameLists++;

    return getcache().getNameList();
  }

  /**
   * @param name of tz
   * @return spec
   */
  public String getTz(final String name) {
    return getcache().getCachedVtz(name);
  }

  /**
   * @return all specs
   */
  public Collection<String> getAllTzs() {
    return getcache().getAllCachedVtzs();
  }

  /**
   * @param name alias
   * @return spec
   */
  public String getAliasedTz(final String name) {
    return getcache().getAliasedCachedVtz(name);
  }

  /**
   * @return String value of aliases file.
   */
  public String getAliasesStr() {
    return getcache().getAliasesStr();
  }

  /**
   * @param tzid timezone id
   * @return set of aliases or null
   */
  public SortedSet<String> findAliases(final String tzid) {
    return getcache().findAliases(tzid);
  }

  /**
   * @param time to convert to UTC
   * @param tzid timezone id
   * @return String utc date
   */
  public String getUtc(final String time,
                       final String tzid) {
    if (DateTimeUtil.isISODateTimeUTC(time)) {
      // Already UTC
      return time;
    }

    if (!DateTimeUtil.isISODateTime(time)) {
      return null;  // Bad datetime
    }

    conversions++;
    final long smillis = System.currentTimeMillis();

    final TimeZone tz = fetchTimeZone(tzid);

    final DateFormat formatTd  = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    formatTd.setTimeZone(tz);

    final Date date;
    try {
      date = formatTd.parse(time);
    } catch (final ParseException pe) {
      throw new TzException(pe);
    }
    final String utc;

    synchronized (cal) {
      cal.clear();
      cal.setTime(date);

      //formatTd.setTimeZone(utctz);
      //debug("formatTd with utc: " + formatTd.format(date));

      final StringBuilder sb = new StringBuilder();
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
   * @param dateTime local time
   * @param fromTzid tz converting from
   * @param toTzid tz converting to
   * @return String time in given timezone
   */
  public String convertDateTime(final String dateTime, final String fromTzid,
                                final String toTzid) {
    final String UTCdt;
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
   */
  public List<TimezoneType> getTimezones(final String[] tzids) {
    return getcache().getTimezones(tzids);
  }

  /**
   * @param changedSince - null or dtstamp value
   * @return list of summary info
   */
  public List<TimezoneType> getTimezones(final String changedSince) {
    return getcache().getTimezones(changedSince);
  }

  /**
   * @param name - non null name for partial match
   * @return list of summary info
   */
  public List<TimezoneType> findTimezones(final String name) {
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
   * @param tzid for expansion
   * @param start of range
   * @param end of range
   * @return expansion or null
   */
  public ExpandedMapEntry getExpanded(final String tzid,
                                      final String start,
                                      final String end,
                                      final boolean oldForm) {
    expandFetches++;

    final ExpandedMapEntryKey emek;
    if (oldForm) {
      emek = makeExpandedKey(tzid, start, end);
    } else {
      emek = new ExpandedMapEntryKey(tzid,
                                     XcalUtil.getIcalFormatDateTime(start),
                                     XcalUtil.getIcalFormatDateTime(end));
    }

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

    final DateTime dtstart;
    final DateTime dtend;
    try {
      dtstart = new DateTime(emek.getStart());
      dtend = new DateTime(emek.getEnd());
    } catch (final ParseException pe) {
      throw new TzException(pe);
    }

    dtstart.setTimeZone(tz);
    dtend.setTimeZone(tz);

    final Period p = new Period(dtstart, dtend);

    final ComponentList<Observance> cl = vtz.getObservances();

    final TreeSet<ObservanceWrapper> obws = new TreeSet<>();

    for (final Observance ob: cl) {
      final PeriodList pl = ob.calculateRecurrenceSet(p);

      for (final Object po: pl) {
        final Period onsetPer = (Period)po;

        final ObservanceType ot = new ObservanceType();

        ot.setName(ob.getName());
        ot.setOnset(XcalUtil.getXmlFormatDateTime(
                onsetPer.getStart().toString()));

        ot.setUtcOffsetFrom(
                ob.getOffsetFrom().getOffset().getTotalSeconds());

        ot.setUtcOffsetTo(
                ob.getOffsetTo().getOffset().getTotalSeconds());

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
        etzt.setObservances(new ArrayList<>());
      }
      etzt.getObservances().add(ow.ot);
    }

    tzs = new ExpandedMapEntry(String.valueOf(smillis), etzt);

    getcache().setExpanded(emek, tzs);

    expandsMillis += System.currentTimeMillis() - smillis;
    expands++;

    return tzs;
  }

  /** Get a timezone object from the server given the id.
   *
   * @param tzid to fetch
   * @return TimeZone with id or null
   */
  public TimeZone fetchTimeZone(final String tzid) {
    tzfetches++;

    return getcache().getTimeZone(tzid);
  }

  /* ==============================================================
   *                   Convenience methods
   * ============================================================== */

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
   */
  public String getEtag() {
    final StringBuilder val = new StringBuilder();

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
                                              final String end) {
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
          final String val) {
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
          final String val) {
    if (val == null) {
      final TemporalAmountAdapter dur = new TemporalAmountAdapter(
              Duration.ofDays(520 * 7)); //520 weeks

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

  private CachedData getcache() {
    if (cache == null) {
      getcache(false);
    }

    return cache;
  }

  private void getcache(final boolean clear) {
    final TzConfig cfg = getTzConfig();

    if (cfg == null) {
      logger.error("No config data");
      return;
    }

    try {
      cache = new H2dbCachedData(cfg, clear);
    } catch (final TzException te) {
      logger.error(te);
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

  private static void digit2(final StringBuilder sb, final int val) {
    if (val > 99) {
      throw new TzException("Bad date");
    }
    if (val < 10) {
      sb.append("0");
    }
    sb.append(val);
  }

  private static void digit4(final StringBuilder sb, final int val) {
    if (val > 9999) {
      throw new TzException("Bad date");
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
