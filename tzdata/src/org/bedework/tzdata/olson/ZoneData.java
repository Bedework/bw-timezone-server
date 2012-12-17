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

/**<pre>
     A zone line has the form

   Zone  NAME       GMTOFF  RULES/SAVE  FORMAT  [UNTILYEAR [MONTH [DAY [TIME]]]]

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

</pre>
 */
public class ZoneData extends Data {
  private String name;

  private TimeInfo gmtoff;

  private String rules;

  private long save;

  private String format;

  private Integer untilYear;

  private Integer untilMonth;

  private DayInfo untilDay;

  private TimeInfo untilTime;

  /* Information for formatting */

  private boolean formatParsed;

  /* Split on "/" */
  private String[] formatStdAndDaylight;

  /* Split on %s */
  private String[] formatParts;

  /**
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * @return time info
   */
  public TimeInfo getGmtoff() {
    return gmtoff;
  }

  /**
   * @return rules name or null
   */
  public String getRules() {
    return rules;
  }

  /**
   * @return seconds
   */
  public long getSave() {
    return save;
  }

  /**
   * @return name
   */
  public String getFormat() {
    return format;
  }

  /**
   * @return until year or null for no until
   */
  public Integer getUntilYear() {
    return untilYear;
  }

  /**
   * @return until month or null
   */
  public Integer getUntilMonth() {
    return untilMonth;
  }

  /**
   * @return day info
   */
  public DayInfo getUntilDay() {
    return untilDay;
  }

  /**
   * @return time info
   */
  public TimeInfo getUntilTime() {
    return untilTime;
  }

  /**
   * @param letters
   * @param daylight
   * @return formatted name
   * @throws TzdataException
   */
  public String getFormattedName(final String letters,
                                 final boolean daylight) throws TzdataException {
    if (!formatParsed) {
      int pos = format.indexOf("%s");
      if (pos > 0) {
        formatParts = new String[2];
        formatParts[0] = format.substring(0, pos);
        formatParts[1] = format.substring(pos + 2);
      } else {
        pos = format.indexOf("/");
        if (pos > 0) {
          formatStdAndDaylight = new String[2];
          formatStdAndDaylight[0] = format.substring(0, pos);
          formatStdAndDaylight[1] = format.substring(pos + 1);
        }
      }
    }

    if (formatParts != null) {
      if (letters == null) {
        throw new TzdataException("Zone - no letters for name");
      }

      return formatParts[0] + letters + formatParts[1];
    }

    if (formatStdAndDaylight != null) {
      if (daylight) {
        return formatStdAndDaylight[1];
      }

      return formatStdAndDaylight[0];
    }

    return format;
  }

  /** Create a ZoneData object based on the input String or throw an exception
   *
   * @param val
   * @param continuation
   * @return Rule object
   * @throws TzdataException for invalid input
   */
  public static ZoneData fromString(final String val,
                                    final boolean continuation) throws TzdataException {
    if (val == null) {
      return null;
    }

    String[] fields;

    if (!continuation) {
      fields = tokenize(val);
    } else {
      fields = tokenize("Zone same/name " + val);
    }

    if ((fields.length < 5) ||
        (fields.length > 9)) {
      throw new TzdataException("Bad Zone - incorrect number of fields");
    }

    if (!fields[0].equals("Zone")) {
      throw new TzdataException("Bad Zone - not Zone line");
    }

    ZoneData z = new ZoneData();

    if (!continuation) {
      z.name = fields[1];
    }

    z.gmtoff = TimeInfo.fromString(fields[2]);

    if (fields[3].equals("-")) {
      z.save = 0;
    } else {
      char c = fields[3].charAt(0);

      if ((c == '-') || ((c >= '0') && (c <= '9'))) {
        try {
          z.save = TimeInfo.fromString(fields[3]).getValue();
        } catch (Throwable t) {
          throw new TzdataException("Bad Zone - bad save value");
        }
      } else {
        z.rules = fields[3];
      }
    }

    z.format = fields[4];

    if (fields.length > 5) {
      z.untilYear = getYear(fields[5]);
    }

    if (fields.length > 6) {
      z.untilMonth = getMonth(fields[6]);
    }

    if (fields.length > 7) {
      z.untilDay = DayInfo.fromString(fields[7]);
    }

    if (fields.length > 8) {
      z.untilTime = TimeInfo.fromString(fields[8]);
    }

    return z;
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    ts.append("name", getName());
    ts.append("gmtoff", getGmtoff());
    if (getRules() != null) {
      ts.append("rules", getRules());
    } else {
      ts.append("save", getSave());
    }
    ts.append("format", getFormat());

    if (getUntilYear() != null) {
      ts.append("untilYear", getUntilYear());

      if (getUntilMonth() != null) {
        ts.append("untilMonth", getUntilMonth());

        if (getUntilDay() != null) {
          ts.append("untilDay", getUntilDay());

          if (getUntilTime() != null) {
            ts.append("untilTime", getUntilTime());
          }
        }
      }
    }

    return ts.toString();
  }
}
