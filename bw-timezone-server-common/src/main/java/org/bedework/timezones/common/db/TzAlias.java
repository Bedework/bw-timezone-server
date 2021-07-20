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

import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;

/**
 *.
 *  @version 1.0
 */
public class TzAlias extends TzDbentity<TzAlias> {
  private String aliasId;

  private String targetId;

  /** Constructor for jackson
   */
  @SuppressWarnings("unused")
  public TzAlias() {
    super();
  }

  /** Constructor
   */
  public TzAlias(final String aliasId,
                 final String targetId) {
    super();

    this.aliasId = aliasId;
    this.targetId = targetId;
  }

  /** Get the alias - this should be unique.
   *
   * @return String   alias
   */
  public String getAliasId() {
    return aliasId;
  }

  /** set the target id
   *
   * @param val target id
   */
  public void setTargetId(final String val) {
    targetId = val;
  }

  /** Get the targetId
   *
   *  @return String
   */
  public String getTargetId() {
    return targetId;
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

    final int res = Util.cmpObjval(getAliasId(), that.getAliasId());

    if (res != 0) {
      return res;
    }

    return Util.cmpObjval(getTargetId(), that.getTargetId());
  }

  @Override
  public int hashCode() {
    int hc = 7 * getAliasId().hashCode();

    if (getTargetId() != null) {
      hc *= getTargetId().hashCode();
    }

    return hc;
  }

  @Override
  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);
    ts.append("aliasId", getAliasId());
    ts.append("targetId", getTargetId());
  }

  @Override
  public Object clone() {
    return new TzAlias(getAliasId(),
                       getTargetId());
  }
}
