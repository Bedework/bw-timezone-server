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

import org.apache.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Utils {
  /**
   * @param month - in Cyrus form 1->12
   * @param year - actual year
   * @return days in month
   */
  static int daysInMonth(final int month, final int year) {
    final Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(Calendar.YEAR, year);
    calendar.set(Calendar.MONTH, month - 1);
    return calendar.getActualMaximum(Calendar.DATE);
  }

  static List<String> untab(final String s) {
    final String s1 = s.replaceAll("\t", " ");

    return split(s1);
  }

  /** This is not correct. For example the # character is a comment
   * OUTSIDE of quotes. We don't deal with that nor a field that
   * ends with #.
   *
   * @param s to split
   * @return fields
   */
  static List<String> split(final String s) {
    final List<String> res = new ArrayList<>();

    int len1 = s.length();
    String s1 = s;
    while (true) {
      final String s2 = s1.replace("  "," ");
      final int len2 = s2.length();
      s1 = s2;
      if (len2 == len1) {
        break;
      }

      len1 = len2;
    }

    for (final String split: s1.split(" ")) {
      if (split.length() > 0) {
        if (split.startsWith("#")) {
          break;
        }
        res.add(split);
      }
    }

    return res;
  }

  static class Offsets {
    int offset;

    int stdoffset;

    Offsets(final int offset,
            final int stdoffset) {
      this.offset = offset;
      this.stdoffset = stdoffset;
    }
  }

  /**
    A date-time object that wraps the tzdb wall-clock/utc style date-time information
    and that can generate appropriate localtime or UTC offsets based on Zone/Rule offsets.
   */
  static class DateTimeWrapper implements Comparable<DateTimeWrapper> {
    final private DateTime dt;
    final private String mode;

    /**
     * @param dt the date/time
     * @param mode  the special tzdata mode character
     */
    DateTimeWrapper(final DateTime dt,
                    final String mode) {

      this.dt = dt;
      this.mode = mode;
    }

    DateTime getDt() {
      return dt;
    }

    DateTimeWrapper duplicate() {
      return new DateTimeWrapper(dt.duplicate(),
                                 mode);
    }

    @SuppressWarnings("UnusedDeclaration")
    DateTime getLocaltime(final Offsets offsets) {
      final DateTime newDt = dt.duplicate();

      if (mode == null) {
        return newDt;
      }

      switch (mode) {
        case "u":
          newDt.offsetSeconds(offsets.offset);
          break;
        case "s":
          newDt.offsetSeconds(-offsets.stdoffset + offsets.offset);
          break;
        default:
          throw new RuntimeException("Bad mode");
      }

      return newDt;
    }

    DateTime getUTC(final Offsets offsets) {
      final DateTime newDt = dt.duplicate();

      if (mode == null) {
        newDt.offsetSeconds(-offsets.offset);
      } else if (mode.equals("u")) {
        return newDt;
      } else if (mode.equals("s")) {
        newDt.offsetSeconds(-offsets.stdoffset);
      } else {
        throw new RuntimeException("Bad mode");
      }

      return newDt;
    }

    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") final DateTimeWrapper o) {
      return dt.compareTo(o.dt);
    }
  }

  public static Path createFile(final String path) throws Throwable {
    final Path pathToFile = Paths.get(path);
    Files.createDirectories(pathToFile.getParent());
    return Files.createFile(pathToFile);
  }

  public static boolean empty(final String path) {
    return delete(new File(path), false);
  }

  public static boolean delete(final File file,
                               final boolean deleteThis) {
    final File[] flist;

    if(file == null){
      return false;
    }

    if (file.isFile()) {
      return file.delete();
    }

    if (!file.isDirectory()) {
      return false;
    }

    flist = file.listFiles();
    if (flist != null && flist.length > 0) {
      for (final File f : flist) {
        if (!delete(f, true)) {
          return false;
        }
      }
    }

    if (!deleteThis) {
      return true;
    }
    return file.delete();
  }

  static String formatTzname(final String format,
                             final String letter) {
    if (!format.contains("%")) {
      return format;
    }

    final Formatter f = new Formatter();
    final String param;

    if (letter.equals("-")) {
      param = "";
    } else {
      param = letter;
    }

    return f.format(format, param).toString();
  }

  static void print(final String fmt,
                    final Object... params) {
    final Formatter f = new Formatter();

    info(f.format(fmt, params).toString());
  }

  static void info(final String msg) {
    Logger.getLogger(Utils.class).info(msg);
  }

  static void warn(final String msg) {
    Logger.getLogger(Utils.class).warn(msg);
  }

  static void assertion(final boolean test,
                        final String fmt,
                        final Object... params) {
    if (test) {
      return;
    }

    final Formatter f = new Formatter();

    throw new RuntimeException(f.format(fmt, params).toString());
  }

  static int getInt(final String val) {
    try {
      return Integer.valueOf(val);
    } catch (Throwable ignored) {
      throw new RuntimeException("Failed to parse as Integer " + val);
    }
  }

  final static Map<Integer, Integer> leapDaysMap = new HashMap<>();

  static int leapDaysSince1970(final int yearOffset) {
    Integer r = leapDaysMap.get(yearOffset);

    if (r != null) {
      return r;
    }

    int result;

    if (yearOffset > 2) {
      result = (yearOffset + 1) / 4;
    } else if (yearOffset < -1) {
      // Python will round down negative numbers (i.e. -5/4 = -2, but we want -1), so
      // what is (year_offset - 2) in C code is actually (year_offset - 2 + 3) in Python.
      result = (yearOffset - 2) / 4;
    } else {
      result = 0;
    }

    leapDaysMap.put(yearOffset, result);

    return result;
  }


  final static int[] daysUptoMonth =
          {0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};

  final static int[] daysUptoMonthLeap =
          {0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335};

  static int daysUptoMonth(final int month, final int year) {
    // NB month is 1..12 so use dummy value at start of array to avoid index
    // adjustment
    if (isLeapYear(year)) {
      return daysUptoMonthLeap[month];
    }

    return daysUptoMonth[month];
  }


  private static final Map<Integer, Boolean> leapYears = new HashMap<>();

  static boolean isLeapYear(final int year) {
    Boolean b = leapYears.get(year);

    if (b != null) {
      return b;
    }

    boolean result;

    if (year <= 1752) {
      result = (year % 4 == 0);
    } else {
      result = ((year % 4 == 0) && ((year % 100 != 0)) || (year % 400 == 0));
    }

    leapYears.put(year, result);

    return result;
  }
}

