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
import org.bedework.timezones.common.TzConfig;
import org.bedework.util.jmx.ConfBaseMBean;
import org.bedework.util.jmx.MBeanInfo;

import java.util.List;

/** Run the timezones service
 *
 * @author douglm
 */
@SuppressWarnings("unused")
public interface TzConfMBean extends TzConfig, ConfBaseMBean {
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
   * @param tzdataUrl to compare with
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

  /** Update the data from the data at the given url
   *
   * @param tzdataUrl reference to data
   * @return completion code.
   */
  @MBeanInfo("Update the data from the data at the given url.")
  String updateData(@MBeanInfo("Url of the data for update") String tzdataUrl);

  /** Check with primary source
   *
   * @return completion code.
   */
  @MBeanInfo("Check with primary source.")
  String checkData();

  /** (Re)load the configuration
   *
   * @return status
   */
  @MBeanInfo("(Re)load the configuration")
  String loadConfig();
}
