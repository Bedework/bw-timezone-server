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
</pre>
 */
public class RuleData extends Data {
  private String name;

  private int from;

  private int to;

  private String type;

  private int in;

  private DayInfo on;

  private TimeInfo at;

  private long save;

  private String letters;

  /**
   * @param val
   */
  public void setName(final String val) {
    name = val;
  }

  /**
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * @param val - year
   */
  public void setFrom(final int val) {
    from = val;
  }

  /**
   * @return year
   */
  public int getFrom() {
    return from;
  }

  /**
   * @param val - year
   */
  public void setTo(final int val) {
    to = val;
  }

  /**
   * @return year
   */
  public int getTo() {
    return to;
  }

  /**
   * @param val
   */
  public void setType(final String val) {
    type = val;
  }

  /**
   * @return name
   */
  public String getType() {
    return type;
  }

  /**
   * @param val - month 0 - 12
   */
  public void setIn(final int val) {
    in = val;
  }

  /**
   * @return month 0 - 12
   */
  public int getIn() {
    return in;
  }

  /**
   * @param val
   */
  public void setOn(final DayInfo val) {
    on = val;
  }

  /**
   * @return day info
   */
  public DayInfo getOn() {
    return on;
  }

  /**
   * @param val
   */
  public void setAt(final TimeInfo val) {
    at = val;
  }

  /**
   * @return day info
   */
  public TimeInfo getAt() {
    return at;
  }

  /**
   * @param save - seconds
   */
  public void setSave(final long save) {
    this.save = save;
  }

  /**
   * @return seconds
   */
  public long getSave() {
    return save;
  }

  /**
   * @param val
   */
  public void setLetters(final String val) {
    letters = val;
  }

  /**
   * @return letters
   */
  public String getLetters() {
    return letters;
  }

  /** Create a Rule object based on the input String or throw an exception
   *
   * @param val
   * @return Rule object
   * @throws TzdataException for invalid input
   */
  public static RuleData fromString(final String val) throws TzdataException {
    if (val == null) {
      return null;
    }

    String[] fields = tokenize(val);

    if (fields.length != 10) {
      throw new TzdataException("Bad rule - incorrect number of fields");
    }

    if (!fields[0].equals("Rule")) {
      throw new TzdataException("Bad rule - not Rule line");
    }

    RuleData r = new RuleData();

    r.name = fields[1];

    r.from = getYear(fields[2]);

    if ("only".equals(fields[3])) {
      r.to = r.from;
    } else {
      r.to = getYear(fields[3]);
    }

    if (!fields[4].equals("-")) {
      r.type = fields[4];
    }

    r.in = getMonth(fields[5]);

    r.on = DayInfo.fromString(fields[6]);

    r.at = TimeInfo.fromString(fields[7]);

    r.save = TimeInfo.fromString(fields[8]).getValue();

    if (!fields[9].equals("-")) {
      r.letters = fields[9];
    }

    return r;
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    ts.append("name", getName());
    ts.append("from", getFrom());
    ts.append("to", getTo());
    ts.append("type", getType());
    ts.append("in", getIn());
    ts.append("on", getOn());
    ts.append("at", getAt());

    return ts.toString();
  }
}
