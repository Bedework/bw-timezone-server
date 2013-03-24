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

/** Represent a day in rules.
*
          Recognized forms include:

               5        the fifth of the month
               lastSun  the last Sunday in the month
               lastMon  the last Monday in the month
               Sun>=8   first Sunday on or after the eighth
               Sun<=25  last Sunday on or before the 25th

          Names of days of the week may be abbreviated or
          spelled out in full.  Note that there must be no
          spaces within the ON field.
*/
public class DayInfo extends Data {
  /** The type of the day
   *
   */
  public enum DayType {
    /** e.g.  5        the fifth of the month */
    dayNum,

    /** e.g. lastSun  the last Sunday in the month
     *       lastMon  the last Monday in the month */
    lastWeekday,

    /** e.g.  Sun>=8   first Sunday on or after the eighth */
    firstOnAfter,

    /** e.g.  Sun<=25  last Sunday on or before the 25th  */
    lastOnBefore,
  }

  private DayType type;

  private int dayNum;

  private int date;

  /**
   * @param type
   * @param dayNum
   */
  public DayInfo(final DayType type, final int dayNum) {
    this.dayNum = dayNum;
  }

  /**
   * @param type
   * @param dayNum
   * @param date
   */
  public DayInfo(final DayType type,
                 final int dayNum,
                 final int date) {
    this.dayNum = dayNum;
    this.date = date;
  }

  /**
   * @return type of day
   */
  public DayType getType() {
    return type;
  }

  /**
   * @return 0-6 (Sunday-Saturday) or 1-31 - depending on type
   */
  public int getDayNum() {
    return dayNum;
  }

  /**
   * @return second part in e.g. 8 in Sun>=8
   */
  public int getDate() {
    return date;
  }

  /** Create a DayInfo object based on the input String or throw an exception
   *
   * @param val
   * @return DayInfo object
   * @throws TzdataException for invalid input
   */
  public static DayInfo fromString(final String val) throws TzdataException {
    if (val == null) {
      return new DayInfo(DayType.dayNum, 1);
    }

    if (val.startsWith("last")) {
      return new DayInfo(DayType.lastWeekday, getWeekday(val.substring(4)));
    }

    int pos = val.indexOf("<=");
    if (pos > 0) {
      return new DayInfo(DayType.lastOnBefore,
                         getWeekday(val.substring(0, pos)),
                         Integer.valueOf(val.substring(pos + 2)));
    }

    pos = val.indexOf(">=");
    if (pos > 0) {
      return new DayInfo(DayType.firstOnAfter,
                         getWeekday(val.substring(0, pos)),
                         Integer.valueOf(val.substring(pos + 2)));
    }

    return new DayInfo(DayType.dayNum,
                       Integer.valueOf(val));
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    ts.append("type", getType());
    ts.append("dayNum", getDayNum());

    return ts.toString();
  }
}
