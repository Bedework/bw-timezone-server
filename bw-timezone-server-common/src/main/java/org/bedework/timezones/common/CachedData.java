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

import org.bedework.timezones.common.Differ.DiffListEntry;
import org.bedework.timezones.common.db.TzAlias;
import org.bedework.util.timezones.model.TimezoneType;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import net.fortuna.ical4j.model.TimeZone;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

/** Cached data affected by the source data.
 *
 * @author douglm
 */
public interface CachedData extends Serializable {
  /** Stop any running threads.
   *
   * @throws TzException on fatal error
   */
  void stop() throws TzException;

  /**
   * @return String source information for data.
   * @throws TzException on fatal error
   */
  String getSource() throws TzException;

  /**
   * @return stats for the module
   * @throws TzException on fatal error
   */
  List<Stat> getStats() throws TzException;

  /** Update from primary source if any.
   *
   * @throws TzException on fatal error
   */
  void checkData() throws TzException;

  /** Update the stored data using the given update list. Note that the update
   * may be transient if the data cache has no or an unchangable backing.
   *
   * @param dtstamp lastmod for change
   * @param dles diff list
   * @throws TzException on fatal error
   */
  void updateData(String dtstamp,
                  List<DiffListEntry> dles) throws TzException;

  /**
   * @return XML formatted UTC dateTime
   * @throws TzException on fatal error
   */
  String getDtstamp() throws TzException;

  /** Given an alias return the tzid for that alias
   *
   * @param val alias
   * @return aliased name(s) or null
   * @throws TzException on fatal error
   */
  @SuppressWarnings("UnusedDeclaration")
  TzAlias fromAlias(String val) throws TzException;

  /**
   * @return String value of aliases file.
   * @throws TzException on fatal error
   */
  String getAliasesStr() throws TzException;

  /**
   * @param tzid for which we want aliases
   * @return list of aliases or null
   * @throws TzException on fatal error
   */
  SortedSet<String> findAliases(String tzid) throws TzException;

  /**
   * @return namelist or null
   * @throws TzException on fatal error
   */
  SortedSet<String> getNameList() throws TzException;

  /**
   * @param key to expanded map
   * @param tzs entries from map
   * @throws TzException on fatal error
   */
  void setExpanded(ExpandedMapEntryKey key,
                   ExpandedMapEntry tzs) throws TzException;

  /**
   * @param key to expanded map
   * @return expanded or null
   * @throws TzException on fatal error
   */
  ExpandedMapEntry getExpanded(ExpandedMapEntryKey key) throws TzException;

  /** Get cached VTIMEZONE specifications
   *
   * @param name tzid
   * @return cached spec or null.
   * @throws TzException on fatal error
   */
  String getCachedVtz(String name) throws TzException;

  /** Get all cached VTIMEZONE specifications
   *
   * @return cached specs or null.
   * @throws TzException on fatal error
   */
  Collection<String> getAllCachedVtzs() throws TzException;

  /** Get a timezone object from the server given the id.
   *
   * @param tzid the id
   * @return TimeZone with id or null
   * @throws TzException on fatal error
   */
  TimeZone getTimeZone(String tzid) throws TzException;

  /* * Get an aliased timezone object from the server given the id.
   *
   * @param tzid
   * @return TimeZone with id or null
   * @throws TzException on fatal error
   * /
  TimeZone getAliasedTimeZone(final String tzid) throws TzException;
  */

  /** Get a timezone object from the server given the id.
   *
   * @param tzid the id
   * @return IcalendarType with id or null
   * @throws TzException on fatal error
   */
  IcalendarType getXTimeZone(String tzid) throws TzException;

  /** Get an aliased timezone object from the server given the id.
   *
   * @param tzid the id
   * @return IcalendarType with id or null
   * @throws TzException on fatal error
   */
  @SuppressWarnings("UnusedDeclaration")
  IcalendarType getAliasedXTimeZone(String tzid) throws TzException;

  /** Get an aliased cached VTIMEZONE specifications
   *
   * @param name tzid
   * @return cached spec or null.
   * @throws TzException on fatal error
   */
  String getAliasedCachedVtz(String name) throws TzException;

  /**
   * @param tzids - to fetch
   * @return list of summary info
   * @throws TzException on fatal error
   */
  List<TimezoneType> getTimezones(String[] tzids) throws TzException;

  /**
   * @param changedSince - null or dtstamp value
   * @return list of summary info
   * @throws TzException on fatal error
   */
  List<TimezoneType> getTimezones(String changedSince) throws TzException;

  /**
   * @param name to be partially matched
   * @return list of matching summary info
   * @throws TzException on fatal error
   */
  List<TimezoneType> findTimezones(String name) throws TzException;
}
