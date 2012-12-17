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
package org.bedework.tzdata.olson;

import edu.rpi.sss.util.ToString;

import java.io.Serializable;

/**
  Recognized forms include:

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
*
*/
public class TimeInfo implements Serializable {
  /** The time of the day
   *
   */
  public enum TimeType {
    /** */
    wall,

    /** */
    standard,

    /** */
    universal
  }

  private TimeType type;

  private long value;

  /**
   * @param type
   * @param val - seconds
   */
  public TimeInfo(final TimeType type, final long val) {
    value = val;
  }

  /**
   * @return type of day
   */
  public TimeType getType() {
    return type;
  }

  /**
   * @return seconds
   */
  public long getValue() {
    return value;
  }

  /** Create a TimeInfo object based on the input String or throw an exception
   *
   * @param val
   * @return TimeInfo object
   * @throws TzdataException for invalid input
   */
  public static TimeInfo fromString(final String val) throws TzdataException {
    if ((val == null) || (val.equals("-"))) {
      return new TimeInfo(TimeType.wall, 0);
    }

    int start = 0;
    int end = val.length() - 1;
    int sign = 1;

    if (val.charAt(0) == '-') {
      sign = -1;
      start++;
    }

    TimeType type;

    if (val.charAt(end) == 'w') {
      type = TimeType.wall;
      end--;
    } else if (val.charAt(end) == 's') {
      type = TimeType.standard;
      end--;
    } else if (val.charAt(end) == 'u') {
      type = TimeType.universal;
      end--;
    } else {
      type = TimeType.wall;
    }

    if (end < start) {
      throw new TzdataException("Bad time");
    }

    String[] parts = val.substring(start, end + 1).split(":");

    if ((parts.length < 1) || (parts.length > 3)) {
      throw new TzdataException("Bad time");
    }

    int hrs = Integer.valueOf(parts[0]);
    int mins = 0;
    int secs = 0;

    if (parts.length > 1) {
      mins = Integer.valueOf(parts[1]);
    }

    if (parts.length > 2) {
      secs = Integer.valueOf(parts[2]);
    }

    if ((hrs < 0) || (hrs > 24) ||
        (mins < 0) || (mins > 59) ||
        (secs < 0) || (secs > 59) ||
        ((hrs == 24) && ((mins != 0) || (secs != 0)))) {
      throw new TzdataException("Bad time");
    }

    /* vzoc seems to adjust time like this - why? */
    if (hrs == 24) {
      hrs = 23;
      mins = 59;
      secs = 59;
    }

    return new TimeInfo(type,
                        (sign * (hrs * 3600)) + (mins * 60) + secs);
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    ts.append("type", getType());
    ts.append("value", getValue());

    return ts.toString();
  }
}
