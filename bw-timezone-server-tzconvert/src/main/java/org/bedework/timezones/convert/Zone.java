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
from pycalendar.icalendar.vtimezone import VTimezone
from pycalendar.icalendar.vtimezonestandard import Standard
from pycalendar.utcoffsetvalue import UTCOffsetValue
import rule
import utils

*/

import org.bedework.timezones.convert.LineReader.LineReaderIterator;
import org.bedework.timezones.convert.Utils.DateTimeWrapper;

import edu.rpi.sss.util.ToString;

import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.Standard;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.TzId;
import net.fortuna.ical4j.model.property.XProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bedework.timezones.convert.Utils.Offsets;

/**
Class that maintains a TZ data Zone.
 A tzdata Zone object containing a set of ZoneRules
*/
class Zone {
  String name;
  private final List<ZoneRule> rules = new ArrayList<>();

  /**
   """
   Parse the Zone lines from tzdata.

    *
    * @param line zone line
   */
  void parse(final String line,
             final LineReaderIterator lri) {
    // Parse one line at a time

    boolean first = true;
    String nextLine = line;

    while (nextLine != null) {
      if (nextLine.length() == 0) {
        nextLine = lri.next();
        continue;
      }

      if (first) {
        // First line is special - has ZONE<sp>name<sp><rule>

        name = Utils.untab(nextLine).get(1);
      }

      final List<String> splits = Utils.untab(nextLine);

      if (splits.size() == 0) {
        // Assume comment line
        nextLine = lri.next();
        continue;
      }

      final ZoneRule rule = new ZoneRule(this);
      final boolean hasUntil = rule.parse(splits, nextLine, first);
      if (!rule.gmtoff.equals("#")) {
        rules.add(rule);
      }
      first = false;

      if (!hasUntil) {
        return;
      }
      nextLine = lri.next();
    }
  }

  /** Generate a partial Zone line.

   *
   * @ return the Rule
   * /
    String generate() {
        lines = [];
        for (count, rule in enumerate(self.rules)) {
            if (count == 0) {
                items = (
                    "Zone " + self.name,
                    rule.generate(),
                );
            } else {
                items = (
                    "",
                    "",
                    "",
                    rule.generate(),
                );
            }
            lines.append("\t".join(items));
        }
        return "\n".join(lines);
    }
  */

  static class ExpandResult {
    DateTime transition;
    int offsetFrom;
    int offsetTo;
    ZoneRule zonerule;
    Rule rule;

    ExpandResult(final DateTime transition,
                 final int offsetFrom,
                 final int offsetTo,
                 final ZoneRule zonerule,
                 final Rule rule) {
      this.transition = transition;
      this.offsetFrom = offsetFrom;
      this.offsetTo = offsetTo;
      this.zonerule = zonerule;
      this.rule = rule;
    }

    @Override
    public String toString() {
      final ToString ts = new ToString(this);

      ts.append("transition", transition);
      ts.append("offsetTo", offsetTo);
      ts.append("offsetFrom", offsetFrom);

      return ts.toString();
    }
  }

  /**   Expand this zone into a set of transitions.

   @param ruleSets: parsed Rules for the tzdb
   @param minYear: starting year
   @param maxYear: ending year

   @return C{list} of C{tuple} for (
   transition date-time,
   offset to,
   offset from,
   associated rule,
   )
   */
  List<ExpandResult> expand(final Map<String, RuleSet> ruleSets,
                            final int minYear,
                            final int maxYear) {
    // Start at 1/1/1800 with the offset from the initial zone rule

    final DateTime start = new DateTime(1800, 1, 1, 0, 0, 0);
    final int start_offset = rules.get(0).getUTCOffset();
    final int start_stdoffset = rules.get(0).getUTCOffset();
    final DateTime startdt = start.duplicate();

    // Now add each zone rules dates
    final List<ZoneRule.ExpandResult> transitions = new ArrayList<>();
    DateTime lastUntilDateUTC = start.duplicate();

    Offsets lastOffsets = new Offsets(start_offset, start_stdoffset);
    boolean first = true;

    for (final ZoneRule zonerule: rules) {
      lastOffsets =
              zonerule.expand(ruleSets,
                              transitions,
                              lastUntilDateUTC,
                              lastOffsets,
                              maxYear);
      final DateTimeWrapper lastUntilDate = zonerule.getUntilDate();
      lastUntilDateUTC = lastUntilDate.getUTC(lastOffsets);

      // We typically don't care about the initial one
      if (first && rules.size() > 1) {
        transitions.clear();
        first = false;
      }
    }

    // Sort the results by date
    Collections.sort(transitions);

    // Now scan transitions looking for real changes and note those
    final ArrayList<ExpandResult> results = new ArrayList<>();
    ExpandResult last_transition = new ExpandResult(startdt,
                                                    start_offset,
                                                    start_offset,
                                                    null,
                                                    null);
    for (final ZoneRule.ExpandResult transition: transitions) {
      //dtutc, to_offset, zonerule, rule = transition;
      final DateTime dtutc = transition.utc.getDt();
      final DateTime dt = dtutc.duplicate();
      dt.offsetSeconds(last_transition.offsetFrom);

      if (dtutc.getYear() >= minYear) {
        if (dt.compareTo(last_transition.transition) > 0) {
          results.add(new ExpandResult(dt,
                                       last_transition.offsetFrom,
                                       transition.offset,
                                       transition.zonerule,
                                       transition.rule));
        } else {
          if (results.size() > 0) {
            final ExpandResult lastOne = results.get(results.size() - 1);

            lastOne.offsetTo = transition.offset;
            lastOne.zonerule = transition.zonerule;
          } else {
            results.add(new ExpandResult(last_transition.transition,
                                         last_transition.offsetFrom,
                                         last_transition.offsetTo,
                                         transition.zonerule,
                                         null));
          }
        }
      }
      last_transition = new ExpandResult(dt,
                                         transition.offset,
                                         last_transition.offsetTo,
                                         last_transition.zonerule,
                                         transition.rule);
    }

    return results;
  }

  private static class RuleMapEntry {
    DateTime dt;
    int offsetFrom;
    int offsetTo;

    private RuleMapEntry(final DateTime dt,
                         final int offsetFrom,
                         final int offsetTo) {
      this.dt = dt;
      this.offsetFrom = offsetFrom;
      this.offsetTo = offsetTo;
    }

    public boolean offsetsEqual(final RuleMapEntry that) {
      return (offsetFrom == that.offsetFrom) &&
              (offsetTo == that.offsetTo);
    }

    @Override
    public String toString() {
      final ToString ts = new ToString(this);

      ts.append("offsetFrom", offsetFrom);
      ts.append("offsetTo", offsetTo);
      ts.append("dt", dt);

      return ts.toString();
    }
  }

  /**
   Generate a VTIMEZONE for this Zone.

   @return vtimezone component.
   */
  VTimeZone vtimezone(final Map<String, RuleSet> rules,
                      final int minYear,
                      final int maxYear) {
    // Get a VTIMEZONE component
    final VTimeZone vtz = new VTimeZone();

    final PropertyList pl = vtz.getProperties();

    // Add TZID property
    pl.add(new TzId(name));
    pl.add(new XProperty("X-LIC-LOCATION", name));

    final List<ExpandResult> transitions = expand(rules, minYear, maxYear);

    // Group rules
    ZoneRule lastZoneRule = null;
    final List<Rule> ruleorder = new ArrayList<>();
    final Map<Rule, List<RuleMapEntry>> rulemap = new HashMap<>();

    for (final ExpandResult expr: transitions) {
      // for (dt, offsetfrom, offsetto, zonerule, rule in transitions) {
      // Check for change of rule - we ignore LMT's
      if (!expr.zonerule.getFormat().equals(("LMT"))) {
        if ((lastZoneRule != null) && !lastZoneRule.equals(expr.zonerule)) {
          generateRuleData(vtz, lastZoneRule, ruleorder, rulemap);
        }
        if (!ruleorder.contains(expr.rule)) {
          ruleorder.add(expr.rule);
        }

        List<RuleMapEntry> lrme = rulemap.get(expr.rule);
        if (lrme == null) {
          lrme = new ArrayList<>();
          rulemap.put(expr.rule, lrme);
        }

        lrme.add(new RuleMapEntry(expr.transition,
                                  expr.offsetFrom,
                                  expr.offsetTo));
      }
      lastZoneRule = expr.zonerule;
    }

    // Do left overs
    generateRuleData(vtz, lastZoneRule, ruleorder, rulemap);

    compressRDateComponents(vtz);

    return vtz;
  }

  private void generateRuleData(final VTimeZone vtz,
                                final ZoneRule zonerule,
                                final List<Rule> ruleorder,
                                final Map<Rule, List<RuleMapEntry>> rulemap) {
    // Generate VTIMEZONE component for last set of rules
    for (final Rule rule: ruleorder) {
      if (rule != null) {
        // Accumulate rule portions with the same offset pairs
        final List<RuleMapEntry> rme = rulemap.get(rule);
        RuleMapEntry lastOffsetPair = rme.get(0);
        int startIndex = 0;
        for (int index = 1; index < rme.size(); index++) {
          final RuleMapEntry offsetPair = rme.get(index);
          if (!offsetPair.offsetsEqual(lastOffsetPair)) {
            final RuleMapEntry startRme = rme.get(startIndex);
            rule.vtimezone(
                    vtz,
                    zonerule,
                    startRme.dt,
                    rme.get(index - 1).dt,
                    startRme.offsetFrom,
                    startRme.offsetTo,
                    index - startIndex);
            lastOffsetPair = rme.get(index);
            startIndex = index;
          }
        }

        final RuleMapEntry startRme = rme.get(startIndex);
        rule.vtimezone(
                vtz,
                zonerule,
                startRme.dt,
                rme.get(rme.size() - 1).dt,
                startRme.offsetFrom,
                startRme.offsetTo,
                rme.size());
      } else {
        final List<RuleMapEntry> rme = rulemap.get(null);
        zonerule.vtimezone(
                        vtz,
                        rme.get(0).dt,
                        rme.get(rme.size() - 1).dt,
                        rme.get(0).offsetFrom,
                        rme.get(0).offsetTo);
      }
    }
    ruleorder.clear();
    rulemap.clear();
  }

  private class SimilarMapKey {
    private final Component comp;

    private SimilarMapKey(final Component comp) {
      this.comp = comp;
    }

    public int hashCode() {
      int res = 1;

      if (comp instanceof Standard) {
        res = 2;
      }

      res *= propHash(Property.TZNAME);
      res *= propHash(Property.TZOFFSETTO);
      res *= propHash(Property.TZOFFSETFROM);

      return res;
    }

    private int propHash(final String name) {
      final Property p = comp.getProperty(name);

      if (p == null) {
        return 1;
      }

      return p.getValue().hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      final Component that = ((SimilarMapKey)o).comp;

      return (comp.getClass().equals((that.getClass()))) &&
              cmpProps(that, Property.TZNAME) &&
              cmpProps(that, Property.TZOFFSETTO) &&
              cmpProps(that, Property.TZOFFSETFROM);
    }

    private boolean cmpProps(final Component that,
                             final String propName) {
      final Property p = comp.getProperty(propName);
      final Property thatp = that.getProperty(propName);

      return(p.getValue().equals(thatp.getValue()));
    }
  }

  /** Compress sub-components with RDATEs into a single component with multiple
   RDATEs assuming all other properties are the same.

   @param vtz: the VTIMEZONE object to compress
   */
  void compressRDateComponents(final VTimeZone vtz) {
    // Map the similar sub-components together
    final Map<SimilarMapKey, List<Component>> similarMap = new HashMap<>();

    for (final Object o: vtz.getObservances()) {
      final Component comp = (Component)o;
      final SimilarMapKey key = new SimilarMapKey(comp);

      if (comp.getProperty(Property.RDATE) != null) {
        List<Component> comps = similarMap.get(key);

        if (comps == null) {
          comps = new ArrayList<>();
          similarMap.put(key, comps);
        }
        comps.add(comp);
      }
    }

    // Merge similar
    for (final List<Component> values: similarMap.values()) {
      if (values.size() > 1) {
        Component mergeTo = null;
        for (final Component mergeFrom: values) {
          if (mergeTo == null) {
            mergeTo = mergeFrom;
            continue;
          }

          // Copy RDATE from to and remove from actual timezone
          final Property prop = mergeFrom.getProperty(Property.RDATE);
          mergeTo.getProperties().add(prop);
          vtz.getObservances().remove(mergeFrom);
        }
      }
    }
  }

  @Override
  public boolean equals(final Object o) {
    final Zone other = (Zone)o;

    return name.equals(other.name) &&
          rules.equals(other.rules);
  }


/*
  if __name__ == '__main__':
    rulesdef = """Rule\tUS\t1918\t1919\t-\tMar\tlastSun\t2:00\t1:00\tD
Rule\tUS\t1918\t1919\t-\tOct\tlastSun\t2:00\t0\tS
Rule\tUS\t1942\tonly\t-\tFeb\t9\t2:00\t1:00\tW # War
Rule\tUS\t1945\tonly\t-\tAug\t14\t23:00u\t1:00\tP # Peace
Rule\tUS\t1945\tonly\t-\tSep\t30\t2:00\t0\tS
Rule\tUS\t1967\t2006\t-\tOct\tlastSun\t2:00\t0\tS
Rule\tUS\t1967\t1973\t-\tApr\tlastSun\t2:00\t1:00\tD
Rule\tUS\t1974\tonly\t-\tJan\t6\t2:00\t1:00\tD
Rule\tUS\t1975\tonly\t-\tFeb\t23\t2:00\t1:00\tD
Rule\tUS\t1976\t1986\t-\tApr\tlastSun\t2:00\t1:00\tD
Rule\tUS\t1987\t2006\t-\tApr\tSun>=1\t2:00\t1:00\tD
Rule\tUS\t2007\tmax\t-\tMar\tSun>=8\t2:00\t1:00\tD
Rule\tUS\t2007\tmax\t-\tNov\tSun>=1\t2:00\t0\tS"""
    rules = {}
    import rule
    ruleset = rule.RuleSet()
    ruleset.parse(rulesdef)
    rules[ruleset.name] = ruleset

    zonedef = """Zone America/New_York -4:56:02\t-\tLMT\t1883 Nov 18 12:03:58
\t\t\t-5:00\tUS\tE%sT\t1920
\t\t\t-5:00\tNYC\tE%sT\t1942
\t\t\t-5:00\tUS\tE%sT\t1946
\t\t\t-5:00\tNYC\tE%sT\t1967
\t\t\t-5:00\tUS\tE%sT"""
    zone = Zone()
    zone.parse(zonedef)
    */
}
