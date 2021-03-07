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
import org.bedework.util.jmx.MBeanInfo;

/** This interface defines the various properties we need for a 
 * timezone server
 *
 * @author Mike Douglass
 */
@ConfInfo(elementName = "bwtz-confinfo")
public interface TzConfig {
  /**
   * @param val the dtstamp
   */
  void setDtstamp(String val);

  /**
   * @return String XML format dtstamp
   */
  @MBeanInfo("Timestamp from last data load.")
  String getDtstamp();

  /**
   * @param val version
   */
  void setVersion(String val);

  /**
   * @return String version
   */
  String getVersion();

  /** Tzdata url
   *
   * @param val Location of the primary data
   */
  void setTzdataUrl(String val);

  /**
   * @return String tzdata url
   */
  @MBeanInfo("Location of the primary data.")
  String getTzdataUrl();

  /** Location of the leveldb data.
   *
   * @param val    String
   */
  void setLeveldbPath(String val);

  /** Location of the leveldb data.
   *
   * @return String, null for unset
   */
  @MBeanInfo("Location of the leveldb data - a directory. If relative will be in config dir")
  String getLeveldbPath();

  /** Primary url
   *
   * @param val The URL of a primary server, e.g. www.bedework.org
   */
  void setPrimaryUrl(String val);

  /**
   * @return String Primary url
   */
  @MBeanInfo("The URL of a primary server, e.g. www.bedework.org")
  String getPrimaryUrl();

  /** Are we a primary server?
   *
   * @param val    boolean
   */
  void setPrimaryServer(boolean val);

  /** Are we a primary server?
   * @return boolean
   */
  @MBeanInfo("Is this a primary server?")
  boolean getPrimaryServer();

  /**
   * @param val source of the data
   */
  void setSource(String val);

  /**
   * @return source of the data
   */
  @MBeanInfo("Source of the data - usually derived from data")
  String getSource();

  /** Refresh interval - seconds
   *
   * @param val interval
   */
  void setRefreshDelay(long val);

  /**
   * @return long Refresh interval - seconds
   */
  @MBeanInfo("How often we attempt to refresh from the primary - seconds.")
  long getRefreshDelay();
}
