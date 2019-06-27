/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.timezones.convert;

import net.fortuna.ical4j.model.DateTime;

/**
 * User: mike Date: 5/3/19 Time: 11:35
 */
public class Onset implements Comparable<Onset> {
  final String observanceName;
  final boolean standard;
  net.fortuna.ical4j.model.DateTime onset;
  long offset;

  public Onset(final String observanceName,
               final boolean standard,
               final DateTime onset,
               final long offset) {
    this.observanceName = observanceName;
    this.standard = standard;
    this.onset = onset;
    this.offset = offset;

    onset.setUtc(true);
  }

  public boolean equals(final Object o) {
    final Onset on = (Onset)o;

    return (onset.equals(on.onset)) &&
            (offset == on.offset);
  }

  @Override
  public int compareTo(final Onset o) {
    int res = observanceName.compareTo(o.observanceName);

    if (res != 0) {
      return res;
    }

    if (standard != o.standard) {
      if (standard) {
        return 1;
      }

      return -1;
    }

    res = onset.compareTo(o.onset);

    if (res != 0) {
      return res;
    }

    if (offset > o.offset) {
      return 1;
    }

    if (offset < o.offset) {
      return -1;
    }

    return 0;
  }

  public String toString() {
    final String type;
    if (standard) {
      type = "standard";
    } else {
      type = "daylight";
    }

    return observanceName + "(" + type + "): " +
            onset.toString() + " " + offset;
  }
}

