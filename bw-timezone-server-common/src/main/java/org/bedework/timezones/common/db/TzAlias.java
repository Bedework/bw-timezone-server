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
package org.bedework.timezones.common.db;

import org.bedework.util.misc.Util;

/**
 *.
 *  @version 1.0
 */
public class TzAlias extends TzDbentity<TzAlias> {
  private String fromId;

  private String toId;

  /** Constructor
   */
  public TzAlias() {
    super();
  }

  /** Create a string by specifying all its fields
   *
   * @param fromId     String alias
   * @param toId       String id
   */
  public TzAlias(final String fromId,
                 final String toId) {
    super();
    this.fromId = fromId;
    this.toId = toId;
  }

  /** Set the fromId
   *
   * @param val    String fromId
   */
  public void setFromId(final String val) {
    fromId = val;
  }

  /** Get the fromId - this should be unique.
   *
   * @return String   fromId
   */
  public String getFromId() {
    return fromId;
  }

  /** Set the toId
   *
   * @param val    String toId
   */
  public void setToId(final String val) {
    toId = val;
  }

  /** Get the toId - this should be a valid tzid
   *
   *  @return String   toId
   */
  public String getToId() {
    return toId;
  }

  /* ====================================================================
   *                        Object methods
   * ==================================================================== */

  @Override
  public int compareTo(final TzAlias that) {
    if (that == this) {
      return 0;
    }

    if (that == null) {
      return -1;
    }

    int res = Util.cmpObjval(getFromId(), that.getFromId());

    if (res != 0) {
      return res;
    }

    return Util.cmpObjval(getToId(), that.getToId());
  }

  @Override
  public int hashCode() {
    int hc = 7;

    if (getFromId() != null) {
      hc *= getFromId().hashCode();
    }

    if (getToId() != null) {
      hc *= getToId().hashCode();
    }

    return hc;
  }

  @Override
  protected void toStringSegment(final StringBuilder sb) {
    super.toStringSegment(sb);
    sb.append(", fromId=");
    sb.append(getFromId());
    sb.append(", toId=");
    sb.append(getToId());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TzAlias{");

    toStringSegment(sb);
    sb.append("}");

    return sb.toString();
  }

  @Override
  public Object clone() {
    return new TzAlias(getFromId(),
                       getToId());
  }
}
