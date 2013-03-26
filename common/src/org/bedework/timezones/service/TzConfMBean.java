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

import edu.rpi.cmt.jmx.ConfBaseMBean;
import edu.rpi.cmt.jmx.MBeanInfo;

import java.util.List;

/** Run the timezones service
 *
 * @author douglm
 */
public interface TzConfMBean extends ConfBaseMBean {
  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /** Tzdata url
   *
   * @param val
   */
  void setTzdataUrl(String val);

  /**
   * @return String tzdata url
   */
  @MBeanInfo("Location of the (backup) zip file.")
  String getTzdataUrl();

  /** Location of the leveldb data.
   *
   * @param val    String
   */
  void setLeveldbPath(final String val);

  /** Location of the leveldb data.
   *
   * @return String, null for unset
   */
  @MBeanInfo("Location of the leveldb data - a directory. If relative will be in config dir")
  String getLeveldbPath();

  /** Primary url
   *
   * @param val
   */
  void setPrimaryUrl(String val);

  /**
   * @return String Primary url
   */
  @edu.rpi.cmt.jmx.MBeanInfo("The URL of a primary server, e.g. www.bedework.org")
  String getPrimaryUrl();

  /** Are we a primary server?
   *
   * @param val    boolean
   */
  void setPrimaryServer(final boolean val);

  /** Are we a primary server?
   * @return boolean
   */
  @MBeanInfo("Is this a primary server?")
  boolean getPrimaryServer();

  /** Refresh interval - seconds
   *
   * @param val
   */
  void setRefreshInterval(long val);

  /**
   * @return long Refresh interval - seconds
   */
  @MBeanInfo("How often we attempt to refresh from the primary - seconds.")
  long getRefreshInterval();

  /* ========================================================================
   * Operations
   * ======================================================================== */

  /** Get the current stats
   *
   * @return List of Stat
   */
  @MBeanInfo("Provide some statistics.")
  List<Stat> getStats();

  /** Compare data pointed to by tzdataUrl with the current data.
   *
   * @param tzdataUrl
   * @return completion code.
   */
  @MBeanInfo("Compare data pointed to by tzdataUrl with the current data.")
  String compareData(@MBeanInfo("Url of the zipped data to compare against") String tzdataUrl);

  /** Refresh all the data - almost a restart
   *
   * @return completion code.
   */
  @MBeanInfo("Refresh all the data - almost a restart.\n" +
             "May take a while if it updates from a primary.")
  String refreshData();

  /** Update the data from the zipped data at the given url
   *
   * @param tzdataUrl
   * @return completion code.
   */
  @MBeanInfo("Update the data from the zipped data at the given url.")
  String updateData(@MBeanInfo("Url of the zipped data for update") String tzdataUrl);

  /** Check with primary source
   *
   * @return completion code.
   */
  @MBeanInfo("Check with primary source.")
  String checkData();

  // No sql
  /* * Recreate the tzdb
   *
   * @return completion code.
   * /
  @MBeanInfo("Recreate the database - drop tables, rewrite the schema.")
  String recreateDb();
  */

  /* * Returns status of the schema build.
   *
   * @return Completion messages
   * /
  @MBeanInfo("Status of the database recreate.")
  public List<String> recreateStatus();
  */

  /* * List the hibernate properties
   *
   * @return properties
   * /
  @MBeanInfo("List the hibernate properties")
  String listHibernateProperties();
  */

  /* * Display the named property
   *
   * @param name
   * @return value
   * /
  @MBeanInfo("Display the named hibernate property")
  String displayHibernateProperty(@MBeanInfo("name") final String name);
  */

  /* * Remove the named property
   *
   * @param name
   * /
  @MBeanInfo("Remove the named hibernate property")
  void removeHibernateProperty(@MBeanInfo("name") final String name);
  */

  /* *
   * @param name
   * @param value
   * /
  @MBeanInfo("Add a hibernate property")
  void addHibernateProperty(@MBeanInfo("name") final String name,
                              @MBeanInfo("value") final String value);
  */

  /* *
   * @param name
   * @param value
   * /
  @MBeanInfo("Set a hibernate property")
  void setHibernateProperty(@MBeanInfo("name") final String name,
                            @MBeanInfo("value") final String value);
  */

  /** (Re)load the configuration
   *
   * @return status
   */
  @MBeanInfo("(Re)load the configuration")
  String loadConfig();
}
