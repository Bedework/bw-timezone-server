/*
#    Copyright (c) 2007-2013 Cyrus Daboo. All rights reserved.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
*/
package org.bedework.timezones.convert;

/*
from pycalendar.datetime import DateTime
from pycalendar.icalendar import definitions
from pycalendar.icalendar.property import Property
from pycalendar.icalendar.vtimezone import VTimezone
from pycalendar.icalendar.vtimezonestandard import Standard
from pycalendar.utcoffsetvalue import UTCOffsetValue
import rule
import utils
*/

import org.bedework.timezones.convert.Rule.DateOffset;
import org.bedework.timezones.convert.Utils.DateTimeWrapper;
import org.bedework.base.ToString;
import org.bedework.util.misc.Util;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.UtcOffset;
import net.fortuna.ical4j.model.component.Daylight;
import net.fortuna.ical4j.model.component.Observance;
import net.fortuna.ical4j.model.component.Standard;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.TzName;
import net.fortuna.ical4j.model.property.TzOffsetFrom;
import net.fortuna.ical4j.model.property.TzOffsetTo;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.bedework.timezones.convert.Utils.Offsets;

/** A specific rule for a portion of a Zone
 *
 * <pre>
 A zone line has the form

 Zone  NAME                GMTOFF  RULES/SAVE  FORMAT  [UNTILYEAR [MONTH [DAY [TIME]]]]

 For example:

 Zone  Australia/Adelaide  9:30    Aus         CST     1971 Oct 31 2:00

 The fields that make up a zone line are:

 NAME  The name of the time zone.  This is the name used in
 creating the time conversion information file for the
 zone.

 GMTOFF
 The amount of time to add to UTC to get standard time
 in this zone.  This field has the same format as the
 AT and SAVE fields of rule lines; begin the field with
 a minus sign if time must be subtracted from UTC.

 RULES/SAVE
 The name of the rule(s) that apply in the time zone
 or, alternately, an amount of time to add to local
 standard time.  If this field is - then standard time
 always applies in the time zone.

 FORMAT
 The format for time zone abbreviations in this time
 zone.  The pair of characters %s is used to show where
 the "variable part" of the time zone abbreviation
 goes.  Alternately, a slash (/) separates standard and
 daylight abbreviations.

 UNTILYEAR [MONTH [DAY [TIME]]]
 The time at which the UTC offset or the rule(s) change
 for a location.  It is specified as a year, a month, a
 day, and a time of day.  If this is specified, the
 time zone information is generated from the given UTC
 offset and rule change until the time specified.  The
 month, day, and time of day have the same format as
 the IN, ON, and AT fields of a rule; trailing fields
 can be omitted, and default to the earliest possible
 value for the missing fields.

 The next line must be a "continuation" line; this has
 the same form as a zone line except that the string
 "Zone" and the name are omitted, as the continuation
 line will place information starting at the time
 specified as the "until" information in the previous
 line in the file used by the previous line.
 Continuation lines may contain "until" information,
 just as zone lines do, indicating that the next line
 is a further continuation.
 * </pre>
 */
class ZoneRule {
  private final Zone zone;
  String gmtoff;
  private String rule;
  private String format;
  private String until;

  private DateTimeWrapper cached_until;
  private Integer cached_utc_offset;

  ZoneRule(final Zone zone) {
    this.zone = zone;
  }

  /**
   Parse the Zone line from tzdata.

   * @param splits: split line to parse.
   * @param line      the line for messages.
   * @param zoneLine  true if this is the first line
   * @return true if we had an until part
   */
  boolean parse(final List<String> splits,
                final String line,
                final boolean zoneLine) {
    final int offset;
    if (zoneLine){
      offset = 2;
    } else {
      offset = 0;
    }

    Utils.assertion(splits.size() - offset >= 3, "Help: %s", line);
    gmtoff = splits.get(offset);
    rule = splits.get(1 + offset);
    format = splits.get(2 + offset);
    if (splits.size() - offset < 4) {
      return false; // No until
    }

    until = "";
    String delim = " ";
    for (int i = 3 + offset; i < splits.size(); i++) {
      until += splits.get(i) + delim;
      delim = " ";
    }

    return true;
  }

  public String getFormat() {
    return format;
  }

  /**
   Generate a partial Zone line.

   *
   * @return the rule
   */
  String generate() {
    final StringBuilder sb = new StringBuilder();

    sb.append(gmtoff);
    sb.append("t");
    sb.append(rule);
    sb.append("t");
    sb.append(format);

    if (until != null) {
      sb.append("t");
      sb.append(until);
    }

    return sb.toString();
  }

  DateTimeWrapper getUntilDate() {
    if (cached_until != null) {
      return cached_until;
    }

    int year = 9999;
    int month = 12;
    int day = 1;
    int hours = 0;
    int minutes = 0;
    int seconds = 0;
    String mode = null;

    if (until != null && !until.startsWith("#")) {
      final List<String> splits = Utils.split(until);
      year = Utils.getInt(splits.get(0));
      month = 1;
      day = 1;
      hours = 0;
      minutes = 0;
      seconds = 0;
      mode = null;
      if (splits.size() > 1 && !splits.get(1).startsWith("#")) {
        month = Rule.MONTH_NAME_TO_POS.get(splits.get(1));

        if (splits.size() > 2 && !splits.get(2).startsWith("#")) {
          switch (splits.get(2)) {
            case "lastSun":
              DateTime dt = new DateTime(year, month, 1);
              dt.setDayOfWeekInMonth(-1, DateTime.SUNDAY);
              day = dt.getDay();
              break;
            case "lastSat":
              dt = new DateTime(year, month, 1);
              dt.setDayOfWeekInMonth(-1, DateTime.SATURDAY);
              day = dt.getDay();
              break;
            case "Sun>=1":
              dt = new DateTime(year, month, 1);
              dt.setDayOfWeekInMonth(1, DateTime.SUNDAY);
              day = dt.getDay();
              break;

            default:
              day = Integer.parseInt(splits.get(2));
          }


          if (splits.size() > 3 && !splits.get(3).startsWith(
                  "#")) {
            final String[] splits2 = splits.get(3).split(":");
            hours = Integer.parseInt(splits2[0]);
            minutes = Integer.parseInt(splits2[1].substring(0, 2));
            if (splits2[1].length() > 2) {
              mode = splits2[1].substring(2);
            }
            if (splits2.length > 2) {
              seconds = Integer.parseInt(splits2[2]);
            }
          }
        }
      }
    }

    final DateTime dt = new DateTime(year, month,
                                     day, hours,
                                     minutes, seconds);
    cached_until = new DateTimeWrapper(dt, mode);
    return cached_until;
  }

  Integer getUTCOffset() {
    if (cached_utc_offset != null) {
      return cached_utc_offset;
    }

    final String[] splits = gmtoff.split(":");

    final int hours = posint(splits, 0);
    final int minutes = posint(splits, 1);
    final int seconds = posint(splits, 2);
    final boolean negative = splits[0].startsWith("-");
    cached_utc_offset = ((hours * 60) + minutes) * 60 + seconds;
    if (negative) {
      cached_utc_offset = -cached_utc_offset;
    }
    return cached_utc_offset;
  }

  private int posint(final String[] splits, final int i) {
    if (splits.length <= i) {
      return 0;
    }

    return Math.abs(Integer.parseInt(splits [i]));
  }

  static class ExpandResult implements Comparable<ExpandResult> {
    DateTimeWrapper utc;
    int offset;
    ZoneRule zonerule;
    Rule rule;

    ExpandResult(final DateTimeWrapper utc,
                 final int offset,
                 final ZoneRule zonerule,
                 final Rule rule) {
      this.utc = utc;
      this.offset = offset;
      this.zonerule = zonerule;
      this.rule = rule;
    }

    @Override
    public int compareTo(final ExpandResult o) {
      return utc.compareTo(o.utc);
    }

    @Override
    public String toString() {
      final ToString ts = new ToString(this);

      ts.append("utc", utc);
      ts.append("offset", offset);

      return ts.toString();
    }
  }

  Offsets expand(final Map<String, RuleSet> rules,
                 final List<ExpandResult> results,
                 final DateTime lastUntilUTC,
                 final Offsets lastOffsets,
                 final ZoneRule lastZoneRule,
                 final TzConvertParamsI params) {
    final boolean isTime =
            Character.getType(rule.charAt(0)) ==
                    Character.DECIMAL_DIGIT_NUMBER ||

                    // Check for negative time
                    (rule.length() > 1 &&
                             rule.charAt(0) == '-' &&
                             Character.getType(rule.charAt(1)) ==
                                     Character.DECIMAL_DIGIT_NUMBER);

    if (rule.equals("-") || isTime) {
      return expandNorule(results, lastUntilUTC);
    }

    // Expand the rule
    final RuleSet ruleset = rules.get(rule);
    Utils.assertion(ruleset != null,
                    "No rule '%s' found in cache. %s for %s",
                    rule, this, zone);

    assert ruleset != null;
    final List<DateOffset> tempresults = ruleset.expand(this,
                                                        params.getEndYear());

    // Sort the results by date
    Collections.sort(tempresults);

    boolean found_one = false;
    boolean foundStart = false;
    final Offsets resOffsets = new Offsets(lastOffsets.offset, lastOffsets.stdoffset);
    final DateTimeWrapper finalUntil = getUntilDate();

    for (final DateOffset doffset /*dt, to_offset, rule*/: tempresults) {
      DateTime dtutc = doffset.dt.getUTC(resOffsets);

      final int cmp = dtutc.compareTo(lastUntilUTC);
      if (cmp >= 0) {
        if (!foundStart && (cmp != 0)) {
          // Insert a start item
          if (!found_one) {
            resOffsets.offset = getUTCOffset();
            resOffsets.stdoffset = getUTCOffset();
            dtutc = doffset.dt.getUTC(resOffsets);
          }

          final ZoneRule inForce;
          if (lastZoneRule == null) {
            inForce = this;
          } else {
            inForce = lastZoneRule;
          }
          results.add(new ExpandResult(new DateTimeWrapper(lastUntilUTC, null),
                                       resOffsets.offset,
                                       inForce,
                                       null));
          // Tried this and trnsition shows up but wrong type
            //          this, doffset.rule));
        }
        foundStart = true;

        if (dtutc.compareTo(finalUntil.getUTC(resOffsets)) >= 0) {
          break;
        }

        results.add(new ExpandResult(new DateTimeWrapper(dtutc, null),
                                     doffset.offset,
                                     this,
                                     doffset.rule));
      }

      resOffsets.offset = doffset.offset;
      resOffsets.stdoffset = getUTCOffset();
      found_one = true;
    }

    if (!foundStart) {
      results.add(new ExpandResult(new DateTimeWrapper(lastUntilUTC, null),
                                   resOffsets.offset,
                                   this, null));
    }

    return resOffsets;
  }

  Offsets expandNorule(final List<ExpandResult> results,
                       final DateTime lastUntil) {
    int to_offset = 0;

    if (!rule.equals("-")) {
      final String ruleNoMinus;
      final boolean neg;

      if (rule.startsWith("-")) {
        neg = true;
        ruleNoMinus = rule.substring(1);
      } else {
        neg = false;
        ruleNoMinus = rule;
      }

      final String[] splits = ruleNoMinus.split(":");
      to_offset = 60 * 60 * Integer.parseInt(splits[0]);
      if (splits.length > 1) {
        to_offset += 60 * Integer.parseInt(splits[1]);
      }

      if (neg) {
        to_offset = -to_offset;
      }
    }

    // Always add a transition for the start of this rule
    results.add(new ExpandResult(new DateTimeWrapper(lastUntil, null),
                                 getUTCOffset() + to_offset,
                                 this,
                                 null));
    return (new Offsets(getUTCOffset() + to_offset, getUTCOffset()));
  }

  void vtimezone(final VTimeZone vtz,
                 final DateTime start,
                 final DateTime end,
                 final int offsetfrom,
                 final int offsetto,
                 final boolean standard) {
    // Determine type of component based on offset
    final Observance comp;

    if (standard) {
      comp = new Standard();
    } else {
      comp = new Daylight();
    }

    final PropertyList<Property> pl = comp.getProperties();

    final String tzname;

    // Do TZNAME
    if (format.contains("%s")) {
      tzname = format.replace("%s", "S");
    } else if (format.contains("/")) {
      final String[] splitTzname = format.split("/");
      if (standard) {
        tzname = splitTzname[0];
      } else {
        tzname = splitTzname[1];
      }
    } else {
      tzname = format;
    }

    pl.add(new TzName(tzname));

    // Do offsets

    pl.add(new TzOffsetFrom(new UtcOffset(offsetfrom * 1000)));
    pl.add(new TzOffsetTo(new UtcOffset(offsetto * 1000)));

    // Do DTSTART
    try {
      pl.add(new DtStart(start.localTime()));

      // Recurrence
      //pl.add(new RDate(new ParameterList(), start.localTime()));
    } catch (final ParseException pe) {
      throw new RuntimeException(pe);
    }

    vtz.getObservances().add(comp);
  }


  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    final ZoneRule other = (ZoneRule)o;

    return Util.equalsString(gmtoff, other.gmtoff) &&
            Util.equalsString(rule, other.rule) &&
            Util.equalsString(format, other.format) &&
            Util.equalsString(until, other.until);
  }
}