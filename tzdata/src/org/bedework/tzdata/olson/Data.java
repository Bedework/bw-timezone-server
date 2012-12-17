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

import java.io.Serializable;

/** Base class for zone data

 */
public class Data implements Serializable {
  /** Tokenize the given String
   *
   * @param val
   * @return array of fields
   * @throws TzdataException for invalid input
   */
  public static String[] tokenize(final String val) throws TzdataException {
    if (val == null) {
      return null;
    }

    if (val.indexOf("\"") >= 0) {
      throw new TzdataException("Not handling quote yet");
    }

    return val.split("\\s+");
  }

  protected static int getYear(final String val) {
    if ("minimum".startsWith(val)) {
      return Integer.MIN_VALUE;
    }

    if ("maximum".startsWith(val)) {
      return Integer.MAX_VALUE;
    }

    return Integer.valueOf(val);
  }

  private static final String[] months = {
    "january",
    "february",
    "march",
    "april",
    "may",
    "june",
    "july",
    "august",
    "september",
    "october",
    "november",
    "december"
  };

  protected static int getMonth(final String val) throws TzdataException {
    int i = 0;

    String lcval = val.toLowerCase();

    for (String m: months) {
      if (m.startsWith(lcval)) {
        return i;
      }

      i++;
    }

    throw new TzdataException("Bad month - val");
  }

  private static String[] days = {
    "sunday",
    "monday",
    "tuesday",
    "wednesday",
    "thursday",
    "friday",
    "saturday"
  };

  protected static int getWeekday(final String val) throws TzdataException {
    int i = 0;

    String lcval = val.toLowerCase();

    for (String d: days) {
      if (d.startsWith(lcval)) {
        return i;
      }

      i++;
    }

    throw new TzdataException("Bad day - val");
  }
}
