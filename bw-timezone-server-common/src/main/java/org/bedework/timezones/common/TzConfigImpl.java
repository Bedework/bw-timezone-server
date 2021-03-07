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

import org.bedework.util.config.ConfInfo;
import org.bedework.util.config.ConfigBase;
import org.bedework.util.misc.ToString;

/** This class defines the various properties we need for a carddav server
 *
 * @author Mike Douglass
 */
@ConfInfo(elementName = "bwtz-confinfo",
        type = "org.bedework.timezones.TzConfig")
public class TzConfigImpl
        extends ConfigBase<TzConfigImpl>
        implements TzConfig {
  private String dtstamp;

  private String version;

  private String primaryUrl;

  private boolean primaryServer;

  private String source;

  private String tzdataUrl;

  private String leveldbPath;

  private long refreshDelay;

  /**
   * @param val the dtstamp
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
   * @param val version
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

  /** Url for the primary server.
   *
   * @param val    String
   */
  public void setPrimaryUrl(final String val) {
    primaryUrl = val;
  }

  /** Url for the primary server.
   *
   * @return String, null for unset
   */
  public String getPrimaryUrl() {
    return primaryUrl;
  }

  /** Are we a primary server?
   *
   * @param val    boolean
   */
  public void setPrimaryServer(final boolean val) {
    primaryServer = val;
  }

  /** Are we a primary server?
   *
   * @return boolean
   */
  public boolean getPrimaryServer() {
    return primaryServer;
  }

  /**
   * @param val source of the data
   */
  public void setSource(final String val) {
    source = val;
  }

  /**
   * @return source of the data
   */
  public String getSource() {
    return source;
  }

  /** Location of the (backup) zip file.
   *
   * @param val    String
   */
  public void setTzdataUrl(final String val) {
    tzdataUrl = val;
  }

  /** Location of the (backup) zip file.
   *
   * @return String, null for unset
   */
  public String getTzdataUrl() {
    return tzdataUrl;
  }

  /** Location of the leveldb data.
   *
   * @param val    String
   */
  public void setLeveldbPath(final String val) {
    leveldbPath = val;
  }

  /** Location of the leveldb data.
   *
   * @return String, null for unset
   */
  public String getLeveldbPath() {
    return leveldbPath;
  }

  /** Refresh delay - seconds
   *
   * @param val delay
   */
  public void setRefreshDelay(final long val) {
    refreshDelay = val;
  }

  /** Refresh delay - seconds
   *
   * @return long refreshDelay
   */
  public long getRefreshDelay() {
    return refreshDelay;
  }

  /** Add our stuff to the StringBuilder
   *
   * @param ts    ToString for result
   */
  @Override
  public void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);

    ts.append("dtstamp", getDtstamp());
    ts.append("version", getVersion());
    ts.append("primaryUrl", getPrimaryUrl());
    ts.append("primaryServer", getPrimaryServer());
    ts.append("tzdataUrl", getTzdataUrl());
    ts.append("refreshDelay", getRefreshDelay());
  }

  /** init copy of the config
   *
   * @param newConf other config
   */
  public void copyTo(final TzConfig newConf) {
    newConf.setDtstamp(getDtstamp());
    newConf.setVersion(getVersion());
    newConf.setPrimaryUrl(getPrimaryUrl());
    newConf.setPrimaryServer(getPrimaryServer());
    newConf.setTzdataUrl(getTzdataUrl());
    newConf.setRefreshDelay(getRefreshDelay());

    ((TzConfigImpl)newConf).setName(getName());
  }
}
