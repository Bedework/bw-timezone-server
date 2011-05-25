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

/** Type for db info.
 *
 * @author Mike Douglass
 * @version 1.0
 */
public class TzDbInfo extends TzDbentity<TzDbInfo> {
  private String dtstamp;

  private String version = "1.0";

  /**
   * @param val
   */
  public void setDtstamp(final String val) {
    dtstamp = val;
  }

  /**
   * @return String XML format dtstamp
   */
  public String getDtstamp() {
    return dtstamp;
  }

  /**
   * @param val
   */
  public void setVersion(final String val) {
    version = val;
  }

  /**
   * @return String version
   */
  public String getVersion() {
    return version;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the StringBuilder
   *
   * @param sb    StringBuilder for result
   */
  @Override
  protected void toStringSegment(final StringBuilder sb) {
    super.toStringSegment(sb);

    sb.append("dtstamp=");
    sb.append(getDtstamp());

    sb.append("version=");
    sb.append(getVersion());
  }

  /* ====================================================================
   *                   Object methods
   * The following are required for a db object.
   * ==================================================================== */

  @Override
  public int compareTo(final TzDbInfo that) {
    return getDtstamp().compareTo(that.getDtstamp());
  }

  @Override
  public int hashCode() {
    return dtstamp.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BwUser{");

    toStringSegment(sb);

    return sb.toString();
  }
}
