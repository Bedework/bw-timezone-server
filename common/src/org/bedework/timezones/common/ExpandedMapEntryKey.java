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

  @Override
  public int hashCode() {
    return getTzid().hashCode() * getStart().hashCode() * getEnd().hashCode();
  }

  @Override
  public int compareTo(final ExpandedMapEntryKey o) {
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

  @Override
  public boolean equals(final Object o) {
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
