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

import org.bedework.util.misc.Util;

import net.fortuna.ical4j.model.Recur;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
  Class that builds a recurrence
*/
public class Recurrence {
  private Integer byMonth;

  private List<Integer> byMonthDay;

  private List<Integer> byYearDay;

  static class ByDay {
    int byDayOffset;

    int byDay;

    ByDay(final int byDayOffset,
          final int byDay) {
      this.byDayOffset = byDayOffset;
      this.byDay = byDay;
    }
  }

  private final List<ByDay> byDay = new ArrayList<>();

  private static final String[] byDayVals = {
          "SU", "MO", "TU", "WE", "TH", "FR", "SA"
  };

  private DateTime until;
  private boolean useUntil;

  public void setByMonth(final Integer val) {
    byMonth = val;
  }

  public void setByMonthDay(final List<Integer> val) {
    byMonthDay = val;
  }

  public void addByDay(final int offset,
                       final int dayNum) {
    byDay.add(new ByDay(offset, dayNum));
  }

  public void addByYearDay(final int dayNum) {
    if (byYearDay == null) {
      byYearDay = new ArrayList<>();
    }
    byYearDay.add(dayNum);
  }

  void setUseUntil(final boolean val) {
    useUntil = val;
  }

  void setUntil(final DateTime val) {
    until = val;
  }

  Recur getRecur() {
    final StringBuilder sb = new StringBuilder("FREQ=");
    sb.append(Recur.YEARLY);

    if (useUntil) {
      sb.append(';');
      sb.append("UNTIL=");
      sb.append(until.utcTime());
    }

    if (!Util.isEmpty(byDay)) {
      sb.append(';');
      sb.append("BYDAY=");
      boolean comma = false;
      for (final ByDay bd: byDay) {
        if (comma) {
          sb.append(',');
        }
        comma = true;

        if (bd.byDayOffset != 0) {
          sb.append(bd.byDayOffset);
        }

        sb.append(byDayVals[bd.byDay]);
      }
    }

    if (!Util.isEmpty(byMonthDay)) {
      sb.append(';');
      sb.append("BYMONTHDAY=");
      boolean comma = false;
      for (final Integer i: byMonthDay) {
        if (comma) {
          sb.append(',');
        }
        comma = true;
        sb.append(i);
      }
    }

    if (!Util.isEmpty(byYearDay)) {
      sb.append(';');
      sb.append("BYYEARDAY=");
      boolean comma = false;
      for (final Integer i: byYearDay) {
        if (comma) {
          sb.append(',');
        }
        comma = true;
        sb.append(i);
      }
    }

    if (byMonth != null) {
      sb.append(';');
      sb.append("BYMONTH=");
      sb.append(byMonth);
    }

    try {
      return new Recur(sb.toString());
    } catch (final ParseException pe) {
      System.err.println(sb.toString());
      throw new RuntimeException(pe);
    }
  }
}