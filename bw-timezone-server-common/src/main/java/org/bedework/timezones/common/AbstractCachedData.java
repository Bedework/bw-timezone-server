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

import org.bedework.timezones.common.db.TzAlias;
import org.bedework.util.caching.FlushMap;
import org.bedework.util.calendar.IcalToXcal;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.timezones.DateTimeUtil;
import org.bedework.util.timezones.model.TimezoneType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.TzId;
import net.fortuna.ical4j.model.property.TzidAliasOf;

import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

/** Abstract class to help simplify implementation
 *
 * @author douglm
 */
public abstract class AbstractCachedData implements Logged, CachedData {
  protected String msgPrefix;

  /** When we were created for debugging */
  protected Timestamp objTimestamp;

  /** XML formatted UTC dtstamp (i.e. separators) for the data */
  protected String dtstamp;

  private Map<String, String> vtzs = new HashMap<>();

  private Map<String, TimeZone> timeZones = new FlushMap<>();

  private Map<String, IcalendarType> xtzs = new HashMap<>();

  private Map<String, String> aliasedVtzs = new HashMap<>();

//  private Map<String, TimeZone> aliasedTzs = new HashMap<String, TimeZone>();

  private Map<String, IcalendarType> aliasedXtzs = new HashMap<>();

  private SortedSet<String> nameList;

  protected Map<ExpandedMapEntryKey, ExpandedMapEntry> expansions =
    new HashMap<>();

  /** */
  public static class AliasMaps {
    /** */
    public String aliasesStr;

    /** */
    public Properties aliases;

    /** */
    public Map<String, SortedSet<String>> byTzid;
    /** */
    public Map<String, TzAlias> byAlias;
  }

  protected AliasMaps aliasMaps;

  protected TzConfig cfg;

  private List<TimezoneType> timezones;

  private Map<String, TimezoneType> timezonesMap;

  /**
   * @param cfg
   * @param msgPrefix - for messages
   * @throws TzException
   */
  public AbstractCachedData(final TzConfig cfg,
                            final String msgPrefix) throws TzException {
    if (cfg == null) {
      throw new TzException("No configuration data");
    }

    this.cfg = cfg;
    this.msgPrefix = msgPrefix;
  }

  @Override
  public List<Stat> getStats() throws TzException {
    final List<Stat> stats = new ArrayList<>();

    if (vtzs == null) {
      stats.add(new Stat(msgPrefix, " #tzs  Unavailable"));
    } else {
      stats.add(new Stat(msgPrefix + " #tzs", String.valueOf(vtzs.size())));
    }

    stats.add(new Stat(msgPrefix + " dtstamp", dtstamp));
    stats.add(new Stat(msgPrefix + " cached expansions",
                       String.valueOf(expansions.size())));

    return stats;
  }

  /** Find tz identifiers or alias names that (partially) match the given value
   * @param val
   * @return list of strings - never null
   * @throws TzException
   */
  public abstract List<String> findIds(String val) throws TzException;

  /* ====================================================================
   *                   CachedData methods
   * ==================================================================== */

  @Override
  public String getDtstamp() throws TzException {
    return dtstamp;
  }

  @Override
  public TzAlias fromAlias(final String val) throws TzException {
    return aliasMaps.byAlias.get(val);
  }

  @Override
  public String getAliasesStr() throws TzException {
    return aliasMaps.aliasesStr;
  }

  @Override
  public SortedSet<String> findAliases(final String tzid) throws TzException {
    return aliasMaps.byTzid.get(tzid);
  }

  @Override
  public SortedSet<String> getNameList() throws TzException {
    return nameList;
  }

  @Override
  public void setExpanded(final ExpandedMapEntryKey key,
                          final ExpandedMapEntry tzs) throws TzException {
    expansions.put(key, tzs);
  }

  @Override
  public ExpandedMapEntry getExpanded(final ExpandedMapEntryKey key) throws TzException {
    return expansions.get(key);
  }

  @Override
  public String getCachedVtz(final String name) throws TzException {
    return vtzs.get(name);
  }

  @Override
  public Collection<String> getAllCachedVtzs() throws TzException {
    return vtzs.values();
  }

  @Override
  public TimeZone getTimeZone(final String tzid) throws TzException {
    TimeZone tz = timeZones.get(tzid);

    if (tz != null) {
      return tz;
    }

    Calendar cal = parseDef(TzServerUtil.getCalHdr() +
                                    getCachedVtz(tzid) +
                                    TzServerUtil.getCalTlr());

    tz = new TimeZone(vtzFromCal(cal));


    timeZones.put(tzid, tz);

    return tz;
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getAliasedTimeZone(java.lang.String)
   * /
  @Override
  public TimeZone getAliasedTimeZone(final String tzid) throws TzException {
    return aliasedTzs.get(tzid);
  }*/

  @Override
  public IcalendarType getXTimeZone(final String tzid) throws TzException {
    return xtzs.get(tzid);
  }

  @Override
  public IcalendarType getAliasedXTimeZone(final String tzid) throws TzException {
    return aliasedXtzs.get(tzid);
  }

  @Override
  public String getAliasedCachedVtz(final String name) throws TzException {
    return aliasedVtzs.get(name);
  }

  @Override
  public List<TimezoneType> getTimezones(final String[] tzids) throws TzException {
    List<TimezoneType> ss = new ArrayList<>();

    for (String tzid: tzids) {
      TimezoneType t = timezonesMap.get(tzid);

      if (t != null) {
        ss.add(t);
      }
    }

    return ss;
  }

  @Override
  public List<TimezoneType> getTimezones(final String changedSince) throws TzException {
    if (changedSince == null) {
      return timezones;
    }

    final List<TimezoneType> ss = new ArrayList<>();

    for (final TimezoneType tz: timezones) {
      if (tz.getLastModified() == null) {
        ss.add(tz);
        continue;
      }

      final String lm = DateTimeUtil.rfcDateTimeUTC(tz.getLastModified());

      /*
       * cs > lm +
       * cs = lm 0
       * cs < lm -
       */

      if (changedSince.compareTo(lm) < 0) {
        ss.add(tz);
      }
    }

    return ss;
  }

  @Override
  public List<TimezoneType> findTimezones(final String name) throws TzException {
    List<TimezoneType> sums = new ArrayList<>();

    List<String> ids = findIds(name);

    for (TimezoneType tz: timezones) {
      if (ids.contains(tz.getTzid())) {
        sums.add(tz);
      }
    }

    return sums;
  }

  /* ====================================================================
   *                   protected methods
   * ==================================================================== */

  /**
   * @param id of tz
   * @param caldef a tz spec in the form of a String VCALENDAR representation
   * @param etag for entry
   * @param storedDtstamp to set last mod
   * @throws TzException
   */
  protected void processSpec(final String id,
                             final String caldef,
                             final String etag,
                             final String storedDtstamp) throws TzException {
    processSpec(id, parseDef(caldef), etag, storedDtstamp);
  }


  /**
   * @param id of tz
   * @param cal a tz spec in the form of a CALENDAR component
   * @param etag for entry
   * @param storedDtstamp to set last mod
   * @throws TzException
   */
  protected void processSpec(final String id,
                             final Calendar cal,
                             final String etag,
                             final String storedDtstamp) throws TzException {
    try {
      nameList.add(id);

      final VTimeZone vtz = vtzFromCal(cal);

      vtzs.put(id, vtz.toString());

      /* Now build the XML version */

      IcalendarType xcal = IcalToXcal.fromIcal(cal, null, true);

      xtzs.put(id, xcal);

      /* ================== Build summary info ======================== */
      final TimezoneType tz = new TimezoneType();

      tz.setTzid(id);

      final LastModified lm = vtz.getLastModified();
      if (lm!= null) {
        tz.setLastModified(DateTimeUtil.fromRfcDateTimeUTC(lm.getValue()));
      } else if (storedDtstamp != null) {
        tz.setLastModified(DateTimeUtil.fromRfcDateTimeUTC(storedDtstamp));
      } else {
        tz.setLastModified(DateTimeUtil.fromRfcDateTimeUTC(dtstamp));
      }

      if (etag != null) {
        tz.setEtag(etag);
      } else if (storedDtstamp != null) {
        tz.setEtag(storedDtstamp);
      } else {
        tz.setEtag(dtstamp);
      }

      final SortedSet<String> aliases = findAliases(id);

      // XXX Need to have list of local names per timezone
      //String ln = vtz.
      if (aliases != null) {
        for (String a: aliases) {
          if (tz.getAliases() == null) {
            tz.setAliases(new ArrayList<String>());
          }
          tz.getAliases().add(a);

          List<String> aliasedIds = null;

          if (aliasMaps != null) {
            final TzAlias alias = aliasMaps.byAlias.get(a);
            if (alias != null) {
              aliasedIds = alias.getTargetIds();
            }
          }

          final VTimeZone avtz = addAlias(a, vtz, aliasedIds);

          cal.getComponents().clear();
          cal.getComponents().add(avtz);

          xcal = IcalToXcal.fromIcal(cal, null, true);

          aliasedXtzs.put(id, xcal);
        }
      }

      timezones.add(tz);
      timezonesMap.put(tz.getTzid(), tz);
    } catch (final TzException te) {
      throw te;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  protected Calendar parseDef(final String caldef) throws TzException {
    try {
      CalendarBuilder cb = new CalendarBuilder();

      UnfoldingReader ufrdr = new UnfoldingReader(new StringReader(caldef), true);

      return cb.build(ufrdr);
    } catch (Throwable t) {
      throw new TzException(t);
    }
  }

  protected VTimeZone vtzFromCal(final Calendar cal) throws TzException {
    VTimeZone vtz = (VTimeZone)cal.getComponents().getComponent(Component.VTIMEZONE);
    if (vtz == null) {
      throw new TzException("Incorrectly stored timezone");
    }

    return vtz;
  }

  protected void resetTzs() {
    nameList = new TreeSet<>();
    timezones = new ArrayList<>();
    timezonesMap = new HashMap<>();
  }

  /* Construct a new vtimezone with the alias as id and then
   * add it and the string version to the alias table.
   */
  protected VTimeZone addAlias(final String alias,
                               final VTimeZone vtz,
                               final List<String> tzids) throws TzException {
    try {
      VTimeZone avtz = (VTimeZone)vtz.copy();

      TzId tzidProp = avtz.getTimeZoneId();
      tzidProp.setValue(alias);

      if (tzids != null) {
        for (String tzid: tzids) {
          avtz.getProperties().add(new TzidAliasOf(tzid));
        }
      }

//      aliasedTzs.put(alias, new TimeZone(avtz));
      aliasedVtzs.put(alias, avtz.toString());

      return avtz;
    } catch (Throwable t) {
      throw new TzException(t);
    }
  }

  protected String escape(final String val) {
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

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
