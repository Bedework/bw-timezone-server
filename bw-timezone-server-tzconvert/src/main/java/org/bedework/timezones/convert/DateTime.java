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

import org.bedework.base.ToString;

/**
  Extracted from pycalendar.datetime
*/
public class DateTime implements Comparable<DateTime> {
  public static final int SUNDAY = 0;
  public static final int MONDAY = 1;
  public static final int TUESDAY = 2;
  public static final int WEDNESDAY = 3;
  public static final int THURSDAY = 4;
  public static final int FRIDAY = 5;
  public static final int SATURDAY = 6;

  private int year = 1970;
  private int month;
  private int day;

  private boolean dateOnly = false;

  private int hours;
  private int minutes;
  private int seconds;

  private boolean isUtc;

  DateTime() {
  }

  public DateTime(final int year,
                  final int month,
                  final int day) {
    this.year = year;
    this.month = month;
    this.day = day;
    dateOnly = true;
  }

  public DateTime(final int year,
                  final int month,
                  final int day,
                  final int hours,
                  final int minutes,
                  final int seconds) {
    this.year = year;
    this.month = month;
    this.day = day;
    this.hours = hours;
    this.minutes = minutes;
    this.seconds = seconds;
  }

  void offsetSeconds(final int val) {
    seconds += val;
    normalise();
  }

  void setTimezoneUTC(final boolean val) {
    isUtc = val;
  }

  int getDayOfWeek() {
    /*
    normalise();

    final Calendar cal = Calendar.getInstance();
    cal.clear();
    //noinspection MagicConstant
    cal.set(year, month - 1, day);

    return cal.get(Calendar.DAY_OF_WEEK) - 1;
    */

    // Count days since 01-Jan-1970 which was a Thursday
    int result = THURSDAY + daysSince1970();
    result %= 7;
    if (result < 0) {
      result += 7;
    }

    return result;
  }

  void setDay(final int val) {
    day = val;
  }

  int getDay() {
    return day;
  }

  void setMonth(final int val) {
    month = val;
  }

  int getMonth() {
    return month;
  }

  void setYear(final int val) {
    year = val;
  }

  int getYear() {
    return year;
  }

  void setHours(final int val) {
    hours = val;
  }

  void setMinutes(final int val) {
    minutes = val;
  }

  void setSeconds(final int val) {
    seconds = val;
  }

  /**
   * @param offset of day in month eg 1 for first occurrence -1 for last
   * @param dayNum the day number
   */
  void setDayOfWeekInMonth(final int offset,
                           final int dayNum) {
    // Set to first day in month
    day = 1;

    // Determine first weekday in month
    int firstDay = getDayOfWeek();

    if (offset > 0) {
      int cycle = (offset - 1) * 7 + dayNum;
      cycle -= firstDay;
      if (firstDay > dayNum) {
        cycle += 7;
      }
      day = cycle + 1;
    } else if (offset < 0) {
      final int days_in_month = Utils.daysInMonth(month, year);
      firstDay += days_in_month - 1;
      firstDay %= 7;

      int cycle = (-offset - 1) * 7 - dayNum;
      cycle += firstDay;
      if (dayNum > firstDay ) {
        cycle += 7;
      }
      day = days_in_month - cycle;
    }

    normalise();
  }

  void setNextDayOfWeek(final int start,
                        final int dayNum) {
    //if (year == 1944) {
    //  final int x = year;
    //}
    // Set to first day in month
    day = start;

    // Determine first weekday in month
    final int first_day = getDayOfWeek();

    if (first_day > dayNum) {
      day += 7;
    }

    day += dayNum - first_day;

    normalise();
  }

  void normalise() {
    // Normalise seconds
    int normalised_secs = seconds % 60;
    int adjustment_mins = seconds / 60;
    if (normalised_secs < 0) {
      normalised_secs += 60;
      adjustment_mins -= 1;
    }
    seconds = normalised_secs;
    minutes += adjustment_mins;

    // Normalise minutes
    int normalised_mins = minutes % 60;
    int adjustment_hours = minutes / 60;
    if (normalised_mins < 0) {
      normalised_mins += 60;
      adjustment_hours -= 1;
    }
    minutes = normalised_mins;
    hours += adjustment_hours;

    // Normalise hours
    int normalised_hours = hours % 24;
    int adjustment_days = hours / 24;
    if (normalised_hours < 0) {
      normalised_hours += 24;
      adjustment_days -= 1;
    }
    hours = normalised_hours;
    day += adjustment_days;

    //# Wipe the time if date only
    if (dateOnly) {
      seconds = minutes = hours = 0;
    }

    // Adjust the month first, since the day adjustment is month dependent

    // Normalise month
    int normalised_month = ((month - 1) % 12) + 1;
    int adjustment_year = (month - 1) / 12;
    if ((normalised_month - 1) < 0) {
      normalised_month += 12;
      adjustment_year -= 1;
    }
    month = normalised_month;
    year += adjustment_year;

    // Now do days
    if (day > 0) {
      while (day > Utils.daysInMonth(month, year)) {
        day -= Utils.daysInMonth(month, year);
        month += 1;
        if (month > 12) {
          month = 1;
          year += 1;
        }
      }
    } else {
      while (day <= 0) {
        month -= 1;
        if (month < 1) {
          month = 12;
          year -= 1;
        }
        day += Utils.daysInMonth(month, year);
      }
    }

    // Always invalidate posix time cache
    //changed();
  }

  @Override
  public int compareTo(final DateTime other) {
    int res = cmp(year, other.year);
    if (res != 0) {
      return res;
    }

    res = cmp(month, other.month);
    if (res != 0) {
      return res;
    }

    res = cmp(day, other.day);
    if (res != 0) {
      return res;
    }

    res = cmp(dateOnly, other.dateOnly);
    if (res != 0) {
      return res;
    }

    if (dateOnly) {
      return 0;
    }

    res = cmp(hours, other.hours);
    if (res != 0) {
      return res;
    }

    res = cmp(minutes, other.minutes);
    if (res != 0) {
      return res;
    }

    res = cmp(seconds, other.seconds);
    if (res != 0) {
      return res;
    }

    return cmp(isUtc, other.isUtc);
  }

  private int cmp(final int a, final int b) {
    return Integer.compare(a, b);
  }

  private int cmp(final boolean a, final boolean b) {
    if (a) {
      if (!b) {
        return -1;
      }

      return 0;
    }

    if (b) {
      return 1;
    }

    return 0;
  }

  String localTime() {
    final StringBuilder sb = new StringBuilder();

    sb.append(year);
    sb.append(digits2(month));
    sb.append(digits2(day));

    if (!dateOnly) {
      sb.append("T");
      sb.append(digits2(hours));
      sb.append(digits2(minutes));
      sb.append(digits2(seconds));
    }

    return sb.toString();
  }

  String utcTime() {
    if (dateOnly) {
      return localTime();
    }
    return localTime() + "Z";
  }

  private String digits2(final int value) {
    final String s = String.valueOf(value);

    if (value < 10) {
      return "0" + s;
    }

    return s;
  }


  private int daysSince1970() {
    // Add days between 1970 and current year (ignoring leap days)
    int result = (year - 1970) * 365;

    // Add leap days between years
    result += Utils.leapDaysSince1970(year - 1970);

    // Add days in current year up to current month (includes leap day for
    // current year as needed)
    result += Utils.daysUptoMonth(month, year);

    // Add days in month
    result += day - 1;

    return result;
  }

  DateTime duplicate() {
    final DateTime ndt = new DateTime();

    ndt.year = year;
    ndt.month = month;
    ndt.day = day;
    ndt.dateOnly = dateOnly;

    if (dateOnly) {
      return ndt;
    }

    ndt.hours = hours;
    ndt.minutes = minutes;
    ndt.seconds = seconds;
    ndt.isUtc = isUtc;

    return ndt;
  }

  @Override
  public String toString() {
    final ToString ts = new ToString(this);

    ts.append("year", year);
    ts.append("month", month);
    ts.append("day", day);
    ts.append("dayOfWeek", getDayOfWeek());
    ts.append("dateOnly", dateOnly);

    if (!dateOnly) {
      ts.append("hours", hours);
      ts.append("minutes", minutes);
      ts.append("seconds", seconds);
      ts.append("isUtc", isUtc);
    }

    return ts.toString();
  }
}