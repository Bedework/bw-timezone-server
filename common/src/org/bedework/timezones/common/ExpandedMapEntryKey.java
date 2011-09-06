/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/
package org.bedework.timezones.common;

import java.io.Serializable;

import edu.rpi.sss.util.Util;

/** Allos us to cache expansions
 *
 * @author douglm
 */
public class ExpandedMapEntryKey implements Comparable<ExpandedMapEntryKey>, Serializable {
  private String tzid;
  private String start;
  private String end;

  /**
   * @param tzid
   * @param start
   * @param end
   */
  public ExpandedMapEntryKey(final String tzid,
                          final String start,
                          final String end) {
    this.tzid = tzid;
    this.start = start;
    this.end = end;
  }

  /**
   * @return tzid
   */
  public String getTzid() {
    return tzid;
  }

  /**
   * @return start
   */
  public String getStart() {
    return start;
  }

  /**
   * @return end
   */
  public String getEnd() {
    return end;
  }
  
  public int hashCode() {
    return getTzid().hashCode() * getStart().hashCode() * getEnd().hashCode();
  }

  @Override
  public int compareTo(ExpandedMapEntryKey o) {
    int res = getTzid().compareTo(o.getTzid());

    if (res != 0) {
      return res;
    }

    res = Util.compareStrings(getStart(), o.getStart());
    if (res != 0) {
      return res;
    }

    return Util.compareStrings(getEnd(), o.getEnd());
  }
  
  public boolean equals(Object o) {
    return compareTo((ExpandedMapEntryKey)o) == 0;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    sb.append(getTzid());
    sb.append(", start=");
    sb.append(getStart());
    sb.append(", end=");
    sb.append(getEnd());

    sb.append("}");

    return sb.toString();
  }
}
