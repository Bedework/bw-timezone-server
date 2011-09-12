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

import net.fortuna.ical4j.model.TimeZone;
import ietf.params.xml.ns.timezone_service.SummaryType;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import javax.servlet.ServletException;

/** Cached data affected by the source data.
 *
 * @author douglm
 */
public interface CachedData extends Serializable {
  /** Stop any running threads.
   *
   * @throws ServletException
   */
  void stop() throws ServletException;

  /**
   * @return stats for the module
   * @throws ServletException
   */
  List<Stat> getStats() throws ServletException;

  /** Flag a refresh..
   */
  void refresh();

  /** Update fromprimary source if any.
   *
   * @throws ServletException
   */
  void update() throws ServletException;

  /**
   * @return XML formatted UTC dateTime
   * @throws ServletException
   */
  String getDtstamp() throws ServletException;

  /** Given an alias return the tzid for that alias
   *
   * @param val
   * @return aliased name or null
   * @throws ServletException
   */
  String fromAlias(String val) throws ServletException;

  /**
   * @return String value of aliases file.
   * @throws ServletException
   */
  String getAliasesStr() throws ServletException;

  /**
   * @param tzid
   * @return list of aliases or null
   * @throws ServletException
   */
  List<String> findAliases(String tzid) throws ServletException;

  /**
   * @return namelist or null
   * @throws ServletException
   */
  SortedSet<String> getNameList() throws ServletException;

  /**
   * @param key
   * @param tzs
   * @throws ServletException
   */
  void setExpanded(ExpandedMapEntryKey key,
                   ExpandedMapEntry tzs) throws ServletException;

  /**
   * @param key
   * @return expanded or null
   * @throws ServletException
   */
  ExpandedMapEntry getExpanded(ExpandedMapEntryKey key) throws ServletException;

  /** Get cached VTIMEZONE specifications
   *
   * @param name
   * @return cached spec or null.
   * @throws ServletException
   */
  String getCachedVtz(final String name) throws ServletException;

  /** Get all cached VTIMEZONE specifications
   *
   * @return cached specs or null.
   * @throws ServletException
   */
  Collection<String> getAllCachedVtzs() throws ServletException;

  /** Get a timezone object from the server given the id.
   *
   * @param tzid
   * @return TimeZone with id or null
   * @throws ServletException
   */
  TimeZone getTimeZone(final String tzid) throws ServletException;

  /** Get an aliased cached VTIMEZONE specifications
   *
   * @param name
   * @return cached spec or null.
   * @throws ServletException
   */
  String getAliasedCachedVtz(final String name) throws ServletException;

  /** Get an aliased timezone object from the server given the id.
   *
   * @param tzid
   * @return TimeZone with id or null
   * @throws ServletException
   */
  TimeZone getAliasedTimeZone(final String tzid) throws ServletException;

  /**
   * @param changedSince - null or dtstamp value
   * @return list of summary info
   * @throws ServletException
   */
  List<SummaryType> getSummaries(String changedSince) throws ServletException;
}
