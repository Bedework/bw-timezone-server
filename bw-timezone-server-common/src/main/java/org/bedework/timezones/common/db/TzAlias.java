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

import java.util.ArrayList;
import java.util.List;

/**
 *.
 *  @version 1.0
 */
public class TzAlias extends TzDbentity<TzAlias> {
  private String aliasId;

  private List<String> targetIds;

  /** Constructor for jackson
   */
  public TzAlias() {
    super();
  }

  /** Constructor
   */
  public TzAlias(final String aliasId) {
    super();

    this.aliasId = aliasId;
  }

  /** set the alias - this should be unique.
   *
   * @param val   alias
   */
  public void setAliasId(String val) {
    aliasId = val;
  }

  /** Get the alias - this should be unique.
   *
   * @return String   alias
   */
  public String getAliasId() {
    return aliasId;
  }

  /** Add a target id
   *
   * @param val    String targetId
   */
  public void addTargetId(final String val) {
    if (targetIds == null) {
      targetIds = new ArrayList<>();
    }

    targetIds.add(val);
  }

  /** Get the targetIds
   *
   *  @return list String
   */
  public List<String> getTargetIds() {
    return targetIds;
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

    int res = Util.cmpObjval(getAliasId(), that.getAliasId());

    if (res != 0) {
      return res;
    }

    return Util.cmpObjval(getTargetIds(), that.getTargetIds());
  }

  @Override
  public int hashCode() {
    int hc = 7 * getAliasId().hashCode();

    if (getTargetIds() != null) {
      hc *= getTargetIds().hashCode();
    }

    return hc;
  }

  @Override
  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);
    ts.append("aliasId", getAliasId());
    ts.append("targetIds", getTargetIds());
  }

  @Override
  public Object clone() {
    TzAlias a = new TzAlias(getAliasId());

    if (getTargetIds() != null) {
      for (String s: getTargetIds()) {
        a.addTargetId(s);
      }
    }

    return a;
  }
}
