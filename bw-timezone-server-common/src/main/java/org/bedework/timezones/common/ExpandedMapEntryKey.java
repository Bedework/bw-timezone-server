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

import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;

import java.io.Serializable;


/** Allows us to cache expansions
 *
 * @author douglm
 */
public class ExpandedMapEntryKey implements Comparable<ExpandedMapEntryKey>, Serializable {
  private final String tzid;
  private final String start;
  private final String end;

  /**
   * @param tzid id
   * @param start of expansion
   * @param end of expansion
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
    if (!(o instanceof final ExpandedMapEntryKey key)) {
      return false;
    }
    return compareTo(key) == 0;
  }

  @Override
  public String toString() {
    return new ToString(this).append("id", getTzid())
                             .append("start", getStart())
                             .append("end", getEnd())
                             .toString();
  }
}
