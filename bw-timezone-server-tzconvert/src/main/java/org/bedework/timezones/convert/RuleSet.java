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

import java.util.ArrayList;
import java.util.List;

import static org.bedework.timezones.convert.Rule.DateOffset;

/**
  Class that maintains a TZ data Rule.
  A set of tzdata rules tied to a specific Rule name
*/
public class RuleSet extends ArrayList<Rule> {
  private String name;

  /**
   Parse the set of Rule lines from tzdata.

   * @param lines the lines to parse.
   */
  void parse(final String lines) {
    final String[] splitlines = lines.split("\n");

    for (final String line: splitlines) {
      final String[] splits = line.replace("\t", " ").split(" ");
      final String nm = splits[1];

      if (nm == null) {
        Utils.print("Must have a zone name: '%s'", lines);
      }

      if (name == null) {
        name = nm;
      }

      if (!nm.equals(name)) {
        Utils.print("Different zone names %s and %s: %s", name, nm);
      }

      final Rule rule = new Rule();
      rule.parse(line);
      add(rule);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    String delim = "";

    for (final Rule rl: this) {
      sb.append(delim);
      sb.append(rl.generate());
      delim = "\n";
    }

    return sb.toString();
  }

  /**
   * Expand the set of rules into transition/offset pairs for the entire RuleSet
   * starting at the beginning and going up to maxYear at most.

   *  @param zoneinfo: the Zone in which this RuleSet is being used
   *  @param maxYear: the maximum year to expand out to
   */
  List<DateOffset> expand(final ZoneRule zoneinfo,
              final int maxYear) {
    final List<DateOffset> results = new ArrayList<>();

    for (final Rule rule: this) {
      rule.expand(results, zoneinfo, maxYear);
    }

    return results;
  }

  @Override
  public boolean equals(final Object o) {
    final RuleSet other = (RuleSet)o;

    return name.equals(other.name) &&
            super.equals(other);

  }
}