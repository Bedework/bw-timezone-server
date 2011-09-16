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
public interface TzsvcMBean {
  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /** Name apparently must be the same as the name attribute in the
   * jboss service definition
   *
   * @return Name
   */
  public String getName();

  /** Application name - for config info
   *
   * @param val
   */
  public void setAppname(String val);

  /**
   * @return String application namee
   */
  public String getAppname();

  /** Tzdata url
   *
   * @param val
   */
  public void setTzdataUrl(String val);

  /**
   * @return String tzdata url
   */
  public String getTzdataUrl();

  /** Primary url
   *
   * @param val
   */
  public void setPrimaryUrl(String val);

  /**
   * @return String Primary url
   */
  public String getPrimaryUrl();

  /** Are we a primary server?
   *
   * @param val    boolean
   */
  public void setPrimaryServer(final boolean val);

  /** Are we a primary server?
   * @return boolean
   */
  public boolean getPrimaryServer();

  /** Refresh interval - seconds
   *
   * @param val
   */
  public void setRefreshInterval(long val);

  /**
   * @return long Refresh interval - seconds
   */
  public long getRefreshInterval();

  /* ========================================================================
   * Operations
   * ======================================================================== */

  /** Get the current stats
   *
   * @return List of Stat
   */
  public List<Stat> getStats();

  /** Refresh all the data - almost a restart
   *
   * @return completion code.
   */
  public String refreshData();

  /** Try to update the data - may call primary servers
   *
   * @return completion code.
   */
  public String updateData();

  /** Recreate the tzdb
   *
   * @return completion code.
   */
  public String recreateDb();

  /** Compare data pointed to by tzdataUrl with the current data.
   *
   * @return completion code.
   */
  public String compareData();

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  /** Lifecycle
   *
   */
  public void create();

  /** Lifecycle
   *
   */
  public void start();

  /** Lifecycle
   *
   */
  public void stop();

  /** Lifecycle
   *
   * @return true if started
   */
  public boolean isStarted();

  /** Lifecycle
   *
   */
  public void destroy();
}
