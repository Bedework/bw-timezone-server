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

/** This interface defines methods to access and update the config
 *
 * @author Mike Douglass
 */
public interface TzConfigHolder {
  /**
   * @return path of config directory.
   */
  String getConfigDir();

  /**
   * @return the current state of the configuration.
   */
  TzConfig geTzConfig();

  /** Save the configuration.
   *
   */
  void saveTzConfig();
}
