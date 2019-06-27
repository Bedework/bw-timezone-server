/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.timezones.convert;

import org.bedework.util.misc.ToString;

/**
 * User: mike Date: 5/3/19 Time: 12:05
 */
public class RuleMapEntry {
  final DateTime dt;
  final int offsetFrom;
  final int offsetTo;
  final boolean standard;

  public RuleMapEntry(final DateTime dt,
                      final int offsetFrom,
                      final int offsetTo,
                      final boolean standard) {
    this.dt = dt;
    this.offsetFrom = offsetFrom;
    this.offsetTo = offsetTo;
    this.standard = standard;
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
    ts.append("standard", standard);

    return ts.toString();
  }
}
