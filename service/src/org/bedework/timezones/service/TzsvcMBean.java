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
package org.bedework.timezones.service;

import org.bedework.timezones.common.Stat;

import java.util.List;

/** Run the timezones service
 *
 * @author douglm
 */
public interface TzsvcMBean /*  extends ServiceMBean */ {
  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /** Name apparently must be the same as the name attribute in the
   * jboss service definition
   *
   * @return Name
   */
  String getName();

  /* * From ServiceMBean
   * @return int state
   * /
  int getState();

  /** From ServiceMBean
   * @return string state
   * /
  String getStateString();*/

  /** Application name - for config info
   *
   * @param val
   */
  void setAppname(String val);

  /**
   * @return String application namee
   */
  String getAppname();

  /** Tzdata url
   *
   * @param val
   */
  void setTzdataUrl(String val);

  /**
   * @return String tzdata url
   */
  String getTzdataUrl();

  /** Primary url
   *
   * @param val
   */
  void setPrimaryUrl(String val);

  /**
   * @return String Primary url
   */
  String getPrimaryUrl();

  /** Are we a primary server?
   *
   * @param val    boolean
   */
  void setPrimaryServer(final boolean val);

  /** Are we a primary server?
   * @return boolean
   */
  boolean getPrimaryServer();

  /** Refresh interval - seconds
   *
   * @param val
   */
  void setRefreshInterval(long val);

  /**
   * @return long Refresh interval - seconds
   */
  long getRefreshInterval();

  /* ========================================================================
   * Operations
   * ======================================================================== */

  /** Get the current stats
   *
   * @return List of Stat
   */
  List<Stat> getStats();

  /** Compare data pointed to by tzdataUrl with the current data.
   *
   * @param tzdataUrl
   * @return completion code.
   */
  String compareData(String tzdataUrl);

  /** Refresh all the data - almost a restart
   *
   * @return completion code.
   */
  String refreshData();

  /** Update the data from the zipped data at the given url
   *
   * @param tzdataUrl
   * @return completion code.
   */
  String updateData(String tzdataUrl);

  /** Check with primary source
   *
   * @return completion code.
   */
  String checkData();

  /** Recreate the tzdb
   *
   * @return completion code.
   */
  String recreateDb();

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  /** Lifecycle
   *
   */
  void create();

  /** Lifecycle
   *
   */
  void start();

  /** Lifecycle
   *
   */
  void stop();

  /** Lifecycle
   *
   * @return true if started
   */
  boolean isStarted();

  /** Lifecycle
   *
   */
  void destroy();

  /**
   * @param method
   */
  void jbossInternalLifecycle(String method);
}
