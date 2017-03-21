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
from pycalendar.icalendar.recurrence import Recurrence
from pycalendar.icalendar.vtimezonedaylight import Daylight
from pycalendar.icalendar.vtimezonestandard import Standard
from pycalendar.utcoffsetvalue import UTCOffsetValue
from pycalendar.utils import daysInMonth
import utils
*/

import org.bedework.timezones.convert.Utils.DateTimeWrapper;
import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.UtcOffset;
import net.fortuna.ical4j.model.component.Daylight;
import net.fortuna.ical4j.model.component.Observance;
import net.fortuna.ical4j.model.component.Standard;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.TzName;
import net.fortuna.ical4j.model.property.TzOffsetFrom;
import net.fortuna.ical4j.model.property.TzOffsetTo;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tzdata Rule
 *
 * <pre>
 A rule line has the form

 Rule  NAME  FROM  TO    TYPE  IN   ON       AT    SAVE  LETTER/S

 For example:

 Rule  US    1967  1973  -     Apr  lastSun  2:00  1:00  D

 The fields that make up a rule line are:

 NAME    Gives the (arbitrary) name of the set of rules this
 rule is part of.

 FROM    Gives the first year in which the rule applies.  Any
 integer year can be supplied; the Gregorian calendar
 is assumed.  The word minimum (or an abbreviation)
 means the minimum year representable as an integer.
 The word maximum (or an abbreviation) means the
 maximum year representable as an integer.  Rules can
 describe times that are not representable as time
 values, with the unrepresentable times ignored; this
 allows rules to be portable among hosts with
 differing time value types.

 TO      Gives the final year in which the rule applies.  In
 addition to minimum and maximum (as above), the word
 only (or an abbreviation) may be used to repeat the
 value of the FROM field.

 TYPE    Gives the type of year in which the rule applies.
 If TYPE is - then the rule applies in all years
 between FROM and TO inclusive.  If TYPE is something
 else, then zic executes the command
 yearistype year type
 to check the type of a year: an exit status of zero
 is taken to mean that the year is of the given type;
 an exit status of one is taken to mean that the year
 is not of the given type.

 IN      Names the month in which the rule takes effect.
 Month names may be abbreviated.

 ON      Gives the day on which the rule takes effect.
 Recognized forms include:

 5        the fifth of the month
 lastSun  the last Sunday in the month
 lastMon  the last Monday in the month
 Sun>=8   first Sunday on or after the eighth
 Sun<=25  last Sunday on or before the 25th

 Names of days of the week may be abbreviated or
 spelled out in full.  Note that there must be no
 spaces within the ON field.
 AT      Gives the time of day at which the rule takes
 effect.  Recognized forms include:

 2        time in hours
 2:00     time in hours and minutes
 15:00    24-hour format time (for times after noon)
 1:28:14  time in hours, minutes, and seconds
 -        equivalent to 0

 where hour 0 is midnight at the start of the day,
 and hour 24 is midnight at the end of the day.  Any
 of these forms may be followed by the letter w if
 the given time is local "wall clock" time, s if the
 given time is local "standard" time, or u (or g or
 z) if the given time is universal time; in the
 absence of an indicator, wall clock time is assumed.

 SAVE    Gives the amount of time to be added to local
 standard time when the rule is in effect.  This
 field has the same format as the AT field (although,
 of course, the w and s suffixes are not used).

 LETTER/S
 Gives the "variable part" (for example, the "S" or
 "D" in "EST" or "EDT") of time zone abbreviations to
 be used when this rule is in effect.  If this field
 is -, the variable part is null.
 * </pre>
 */
class Rule {
  String name;
  private String fromYear;
  private String toYear;
  private String type = "-";
  private String inMonth;
  private String onDay;
  private String atTime;
  private String saveTime;
  private String letter;

  // Some useful mapping tables

  static final Map<String, Integer> LASTDAY_NAME_TO_DAY =
          new HashMap<>();

  static {
    LASTDAY_NAME_TO_DAY.put("lastSun", DateTime.SUNDAY);
    LASTDAY_NAME_TO_DAY.put("lastMon", DateTime.MONDAY);
    LASTDAY_NAME_TO_DAY.put("lastTue", DateTime.TUESDAY);
    LASTDAY_NAME_TO_DAY.put("lastWed", DateTime.WEDNESDAY);
    LASTDAY_NAME_TO_DAY.put("lastThu", DateTime.THURSDAY);
    LASTDAY_NAME_TO_DAY.put("lastFri", DateTime.FRIDAY);
    LASTDAY_NAME_TO_DAY.put("lastSat", DateTime.SATURDAY);
  }

  static final Map<String, Integer> DAY_NAME_TO_DAY =
          new HashMap<>();

  static {
    DAY_NAME_TO_DAY.put("Sun", DateTime.SUNDAY);
    DAY_NAME_TO_DAY.put("Mon", DateTime.MONDAY);
    DAY_NAME_TO_DAY.put("Tue", DateTime.TUESDAY);
    DAY_NAME_TO_DAY.put("Wed", DateTime.WEDNESDAY);
    DAY_NAME_TO_DAY.put("Thu", DateTime.THURSDAY);
    DAY_NAME_TO_DAY.put("Fri", DateTime.FRIDAY);
    DAY_NAME_TO_DAY.put("Sat", DateTime.SATURDAY);
  }

  static final Map<String, Integer> LASTDAY_NAME_TO_RDAY =
          new HashMap<>();

  static {
    LASTDAY_NAME_TO_RDAY.put("lastSun", DateTime.SUNDAY);
    LASTDAY_NAME_TO_RDAY.put("lastMon", DateTime.MONDAY);
    LASTDAY_NAME_TO_RDAY.put("lastTue", DateTime.TUESDAY);
    LASTDAY_NAME_TO_RDAY.put("lastWed", DateTime.WEDNESDAY);
    LASTDAY_NAME_TO_RDAY.put("lastThu", DateTime.THURSDAY);
    LASTDAY_NAME_TO_RDAY.put("lastFri", DateTime.FRIDAY);
    LASTDAY_NAME_TO_RDAY.put("lastSat", DateTime.SATURDAY);
  }

  static final Map<String, Integer> MONTH_NAME_TO_POS =
          new HashMap<>();

  static {
    MONTH_NAME_TO_POS.put("Jan", 1);
    MONTH_NAME_TO_POS.put("Feb", 2);
    MONTH_NAME_TO_POS.put("Mar", 3);
    MONTH_NAME_TO_POS.put("Apr", 4);
    MONTH_NAME_TO_POS.put("May", 5);
    MONTH_NAME_TO_POS.put("Jun", 6);
    MONTH_NAME_TO_POS.put("Jul", 7);
    MONTH_NAME_TO_POS.put("Aug", 8);
    MONTH_NAME_TO_POS.put("Sep", 9);
    MONTH_NAME_TO_POS.put("Oct", 10);
    MONTH_NAME_TO_POS.put("Nov", 11);
    MONTH_NAME_TO_POS.put("Dec", 12);
  }

  final static int[] daysBackStartOfMonth = {
          365, 334, 306, 275, 245, 214, 184, 153, 122, 92, 61, 31, 0
          // Does not account for leap year
  };


  /**
   *
   * Parse the Rule line from tzdata.
   * @param line to parse
   */
  void parse(final String line) {
    // Simply split the bits up and store them in various properties
    final List<String> splits = Utils.untab(line);
    Utils.assertion(splits.size() >= 10,
                    "Wrong number of fields in Rule: '%s'", line);
    name = splits.get(1);
    fromYear = splits.get(2);
    toYear = splits.get(3);
    type = splits.get(4);
    inMonth = splits.get(5);
    onDay = splits.get(6);
    atTime = splits.get(7);
    saveTime = splits.get(8);
    letter = splits.get(9);
  }

  /**
   Generate a Rule line.
   *
   * @return string containing rule
   */
  String generate() {
    return "Rule" + "\t" +
            name + "\t" +
            fromYear + "\t" +
            toYear + "\t" +
            type + "\t" +
            inMonth + "\t" +
            onDay + "\t" +
            atTime + "\t" +
            saveTime + "\t" +
            letter;
  }

  /**
   *
   * @return the specified rule offset in seconds.
   */
  private int getOffset() {
    final String[] splits = saveTime.split(":");
    final int hours = Integer.valueOf(splits[0]);
    final int minutes;
    if (splits.length == 2) {
      minutes = Integer.valueOf(splits[1]);
    } else {
      minutes = 0;
    }

    final boolean negative = hours < 0;
    if (negative){
      return -((-hours * 60) + minutes) * 60;
    }

    return ((hours * 60) + minutes) * 60;
  }

  private int startYear() {
    return new Integer(fromYear);
  }

  private int endYear() {
    if (toYear.equals("only")) {
      return startYear();
    }

    if (toYear.equals("max")) {
      return 9999;
    }

    return Integer.valueOf(toYear);
  }

  /**
   Given a specific year, determine the actual date/time of the transition
   *
   * @param year the year to determine the transition for
   * @return C{tuple} of L{DateTime} and C{str} (which is the special
  tzdata mode character
   */
  private Utils.DateTimeWrapper datetimeForYear(final int year) {
    // Create a floating date-time
    final DateTime dt = new DateTime();

    // Setup base year/month/day
    dt.setYear(year);
    dt.setMonth(Rule.MONTH_NAME_TO_POS.get(inMonth));
    dt.setDay(1);

    // Setup base hours/minutes
    String[] splits = atTime.split(":");
    final String minval;
    if (splits.length == 1) {
      minval = "0";
    } else {
      Utils.assertion(splits.length == 2,
                      "atTime format is wrong: %s, %s", atTime, this);
      minval = splits[1];
    }

    final int hours = Integer.valueOf(splits[0]);
    final int minutes;
    final String special;

    if (minval.length() > 2) {
      minutes = Integer.valueOf(minval.substring(0, 2));
      special = minval.substring(2);
    } else {
      minutes = Integer.valueOf(minval);
      special = null;
    }

    // Special case for 24:00
    if ((hours == 24) & (minutes == 0)) {
      dt.setHours(23);
      dt.setMinutes(59);
      dt.setSeconds(59);
    } else {
      dt.setHours(hours);
      dt.setMinutes(minutes);
    }

    // Now determine the actual start day
    if (LASTDAY_NAME_TO_DAY.containsKey(onDay)) {
      dt.setDayOfWeekInMonth(-1, LASTDAY_NAME_TO_DAY.get(onDay));
    } else if (onDay.contains(">=")) {
      splits = onDay.split(">=");
      dt.setNextDayOfWeek(Integer.valueOf(splits[1]),
                          DAY_NAME_TO_DAY.get(splits[0]));
    } else {
      try {
        final int day = Integer.valueOf(onDay);
        dt.setDay(day);
      } catch (final Throwable t) {
        Utils.assertion(false, "onDay value is not recognized: %s" , onDay);
      }
    }

    return new Utils.DateTimeWrapper(dt, special);
  }

  static class OnDayDetails {
    int day;
    int offset;
    List<Integer> bymday;
  }

  /**  Get RRULE BYxxx part items from the Rule data.
   *
   * @param start start date-time for the recurrence set
   * @param indicatedDay the day that the Rule indicates for recurrence
   * @param indicatedOffset the offset that the Rule indicates for recurrence
   */
  private OnDayDetails getOnDayDetails(final DateTime start,
                                       final int indicatedDay,
                                       final int indicatedOffset) {
    final OnDayDetails res = new OnDayDetails();

    int month = start.getMonth();
    final int year = start.getYear();
    final int dayOfWeek = start.getDayOfWeek();
    res.day = dayOfWeek;
    int offset = indicatedOffset;

    // Need to check whether day has changed due to time shifting
    // e.g. if "u" mode is specified, the actual localtime may be
    // shifted to the previous day if the offset is negative
    if (indicatedDay != dayOfWeek) {
      final int difference = dayOfWeek - indicatedDay;
      // ??? if ((difference in (1, -6,)) {
      if ((difference == 1) || (difference == -6)) {
        offset += 1;

        // Adjust the month down too if needed
        if (start.getDay() == 1) {
          month -= 1;
          if (month < 1) {
            month = 12;
          }
        }
        // ???      } else if difference in (-1, 6,):
      } else if ((difference == -1) || (difference == 6)) {
        Utils.assertion(offset != 1, "Bad RRULE adjustment");
        offset -= 1;
      } else {
        Utils.assertion(false, "Unknown RRULE adjustment");
      }
    }

    try {
      // Form the appropriate RRULE bits

      if (offset == 1) {
        offset = 1;
      } else if (offset == 8) {
        offset = 2;
      } else if (offset == 15) {
        offset = 3;
      } else if (offset == 22) {
        offset = 4;
      } else {
        final int days_in_month = Utils.daysInMonth(month, year);
        if (days_in_month - offset == 6) {
          offset = -1;
        } else if (days_in_month - offset == 13) {
          offset = -2;
        } else if (days_in_month - offset == 20) {
          offset = -3;
        } else {
          //bymday = [offset + i for i in range(7) if (offset + i) <= days_in_month];
          res.bymday = new ArrayList<>();

          for (int i = 0; i < 7; i++) {
            if ((offset + i) <= days_in_month) {
              res.bymday.add(offset + i);
            }
          }

          offset = 0;
        }
      }
    } catch (final Throwable ignored) {
      ignored.printStackTrace();
      Utils.assertion(false, "onDay value is not recognized: %s", onDay);
    }

    res.offset = offset;

    return res;
  }

  static class DateOffset implements Comparable<DateOffset> {
    DateTimeWrapper dt;
    int offset;
    Rule rule;

    DateOffset(final DateTimeWrapper dt,
               final int offset,
               final Rule rule) {
      this.dt = dt;
      this.offset = offset;
      this.rule = rule;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(final DateOffset o) {
      return dt.compareTo(o.dt);
    }
  }
  /**
   """
   Expand the Rule into a set of transition date/offset pairs

   * @param results set of transition date/offset pairs
   * @param zoneinfo the Zone in which this RuleSet is being used
   * @param maxYear the maximum year to expand out to
   */
  public void expand(final List<DateOffset> results,
                      final ZoneRule zoneinfo,
                      final int maxYear) {
    if (startYear() >= maxYear) {
      return;
    }

    final int zoneoffset = zoneinfo.getUTCOffset();
    final int offset = getOffset();
    for (final DateTimeWrapper dt: fullExpand(maxYear)) {
      results.add(new DateOffset(dt.duplicate(), zoneoffset + offset, this));
    }
  }

  private List<DateTimeWrapper> dtCache;

  /**
   """
   Do a full recurrence expansion for each year in the Rule's range, upto
   a specified maximum.

   *
   * @param maxYear maximum year to expand to
   */
  private List<DateTimeWrapper> fullExpand(final int maxYear) {
    if (dtCache != null) {
      return dtCache;
    }

    final int start = startYear();
    int end = endYear();
    if (end > maxYear) {
      end = maxYear - 1;
    }

    dtCache = new ArrayList<>();

    for (int year = start; year <= end; year++) {
      dtCache.add(datetimeForYear(year));
    }

    return dtCache;
  }

  /**
   *
   Generate a VTIMEZONE sub-component for this Rule.

   @param vtz: VTIMEZONE to add to
   @param zonerule: the Zone rule line being used
   @param start: the start time for the first instance
   @param end: the start time for the last instance
   @param offsetfrom: the UTC offset-from
   @param offsetto: the UTC offset-to
   @param instanceCount: the number of instances in the set
   */
  void vtimezone(final VTimeZone vtz,
                 final ZoneRule zonerule,
                 final DateTime start,
                 final DateTime end,
                 final int offsetfrom,
                 final int offsetto,
                 final int instanceCount) {
    // Determine type of component based on offset
    final int dstoffset = getOffset();
    final Component comp;

    if (dstoffset == 0) {
      comp = new Standard();
    } else {
      comp = new Daylight();
    }

    // Do offsets
    final UtcOffset tzoffsetfrom = new UtcOffset(offsetfrom * 1000);
    final UtcOffset tzoffsetto = new UtcOffset(offsetto * 1000);

    comp.getProperties().add(new TzOffsetFrom(null, tzoffsetfrom));
    comp.getProperties().add(new TzOffsetTo(null, tzoffsetto));

    // Do TZNAME
    final String tzname = Utils.formatTzname(zonerule.getFormat(),
                                             letter);

    comp.getProperties().add(new TzName(tzname));

    // Do DTSTART
    try {
      comp.getProperties().add(new DtStart(start.localTime()));
    } catch (final ParseException pe) {
      throw new RuntimeException(pe);
    }

    // Now determine the recurrences (use RDATE if only one year or
    // number of instances is one)
    if ((!toYear.equals("only")) && (instanceCount != 1)) {
      final Recurrence rrule = new Recurrence();

      rrule.setByMonth(MONTH_NAME_TO_POS.get(inMonth));
      if (LASTDAY_NAME_TO_RDAY.containsKey(onDay)) {
        // Need to check whether day has changed due to time shifting
        final int dayOfWeek = start.getDayOfWeek();
        final int indicatedDay = LASTDAY_NAME_TO_DAY.get(onDay);

        if (dayOfWeek == indicatedDay) {
          rrule.addByDay(-1, LASTDAY_NAME_TO_RDAY.get(onDay));
        } else if ((dayOfWeek < indicatedDay) ||
                ((dayOfWeek == 6) && (indicatedDay == 0))) {
          // This is OK as we have moved back a day and thus no month transition
          // could have occurred
          final int fakeOffset = Utils.daysInMonth(start.getMonth(), start.getYear()) - 6;

          final OnDayDetails odd = getOnDayDetails(start, indicatedDay, fakeOffset);
          if (odd.bymday != null) {
            rrule.setByMonthDay(odd.bymday);
          }
          rrule.addByDay(odd.offset, odd.day);
        } else {
          // This is bad news as we have moved forward a day possibly into the next month
          // What we do is switch to using a BYYEARDAY rule with offset from the end of the year
          rrule.setByMonth(null);

          for (int i = 0; i < 7; i++) {
            rrule.addByYearDay(-(daysBackStartOfMonth[MONTH_NAME_TO_POS.get(inMonth)] + i));
          }
          rrule.addByDay(0,
                         (Rule.LASTDAY_NAME_TO_DAY.get(onDay) + 1) % 7);
        }
      } else if (onDay.contains(">=")) {
        final String[] split = onDay.split(">=");
        final int indicatedDay;
        final int dayoffset = Integer.valueOf(split[1]);

        // Need to check whether day has changed due to time shifting
        final int dayOfWeek = start.getDayOfWeek();
        indicatedDay = DAY_NAME_TO_DAY.get(split[0]);

        if (dayOfWeek == indicatedDay) {
          final OnDayDetails odd = getOnDayDetails(start, indicatedDay, dayoffset);
          if (odd.bymday != null) {
            rrule.setByMonthDay(odd.bymday);
          }
          rrule.addByDay(odd.offset, odd.day);
        } else if ((dayoffset == 1) &&
                (((dayoffset - indicatedDay) % 7) == 6)) {
          // This is bad news as we have moved backward a day possibly into the next month
          // What we do is switch to using a BYYEARDAY rule with offset from the end of the year
          rrule.setByMonth(null);

          for (int i = 0; i < 7; i++) {
            rrule.addByYearDay(-(daysBackStartOfMonth[MONTH_NAME_TO_POS.get(inMonth)] + i));
          }
          rrule.addByDay(0, indicatedDay + 1 % 7);
        } else {
          // This is OK as we have moved forward a day and thus no month transition
          // could have occurred
          final OnDayDetails odd = getOnDayDetails(start, indicatedDay,
                                                   dayoffset);
          if (odd.bymday != null) {
            rrule.setByMonthDay(odd.bymday);
          }

          rrule.addByDay(odd.offset, odd.day);
        }
      } else {
        try {
          //noinspection ResultOfMethodCallIgnored
          Integer.valueOf(onDay);
        } catch (final Throwable t) {
          Utils.assertion(false,
                          "onDay value is not recognized: %s", onDay);
        }
      }

      // Add any UNTIL
      if ((zonerule.getUntilDate().getDt().getYear() < 9999) ||
              (endYear() < 9999)) {
        final DateTime until = end.duplicate();
        until.offsetSeconds(-offsetfrom);
        until.setTimezoneUTC(true);
        rrule.setUseUntil(true);
        rrule.setUntil(until);
      }

      comp.getProperties().add(new RRule(rrule.getRecur()));
    } else {
      try {
        comp.getProperties().add(new RDate(new ParameterList(), start.localTime()));
      } catch (final ParseException pe) {
        throw new RuntimeException(pe);
      }
    }

    vtz.getObservances().add((Observance)comp);
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }

    final Rule other = (Rule)o;

    return name.equals(other.name) &&
            fromYear.equals(other.fromYear) &&
            toYear.equals(other.toYear) &&
            type.equals(other.type) &&
            inMonth.equals(other.inMonth) &&
            onDay.equals(other.onDay) &&
            atTime.equals(other.atTime) &&
            saveTime.equals(other.saveTime) &&
            Util.equalsString(letter, other.letter);
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    ts.append("name", name);
    ts.append("fromYear", fromYear);
    ts.append("toYear", toYear);
    ts.append("type", type);
    ts.append("inMonth", inMonth);
    ts.append("onDay", onDay);
    ts.append("atTime", atTime);
    ts.append("saveTime", saveTime);
    ts.append("letter", letter);

    return ts.toString();
  }
}