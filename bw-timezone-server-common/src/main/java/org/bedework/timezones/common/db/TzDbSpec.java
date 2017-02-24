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

import java.util.Set;

/** Type for db info.
 *
 * @author Mike Douglass
 * @version 1.0
 */
public class TzDbSpec extends TzDbentity<TzDbSpec> {
  private String name;

  private String etag;

  private String dtstamp;

  private String source;

  private boolean active;

  private String vtimezone;

  private Set<LocalizedString> displayNames;

  /**
   * @param val
   */
  public void setName(final String val) {
    name = val;
  }

  /**
   * @return Name of the spec
   */
  public String getName() {
    return name;
  }

  /** Etag value for last fetch from primary
   *
   * @param val the etag
   */
  public void setEtag(final String val) {
    etag = val;
  }

  /**
   * @return etag or null
   */
  public String getEtag() {
    return etag;
  }

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

  /** Source of the timezone - a url
   *
   * @param val
   */
  public void setSource(final String val) {
    source = val;
  }

  /**
   * @return String source or null
   */
  public String getSource() {
    return source;
  }

  /**
   * @param val
   */
  public void setActive(final boolean val) {
    active = val;
  }

  /**
   * @return boolean true for active
   */
  public boolean getActive() {
    return active;
  }

  /**
   * @param val
   */
  public void setVtimezone(final String val) {
    vtimezone = val;
  }

  /**
   * @return Name of the spec
   */
  public String getVtimezone() {
    return vtimezone;
  }

  /**
   * @param val
   */
  public void setDisplayNames(final Set<LocalizedString> val) {
    displayNames = val;
  }

  /**
   * @return display names
   */
  public Set<LocalizedString> getDisplayNames() {
    return displayNames;
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  @Override
  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);

    ts.append("name", getName());
    ts.append("etag", getEtag());
    ts.append("dtstamp", getDtstamp());
    ts.append("source", getSource());
    ts.append("active", getActive());
    ts.append("vtimezone", getVtimezone());
  }

  /* ====================================================================
   *                   Object methods
   * The following are required for a db object.
   * ==================================================================== */

  @Override
  public int compareTo(final TzDbSpec that) {
    int res = getName().compareTo(that.getName());

    if (res != 0) {
      return res;
    }

    return getDtstamp().compareTo(that.getDtstamp());
  }

  @Override
  public int hashCode() {
    return dtstamp.hashCode();
  }
}
