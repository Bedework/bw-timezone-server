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

import edu.rpi.cmt.timezones.model.TimezoneType;

import net.fortuna.ical4j.model.TimeZone;

import ietf.params.xml.ns.icalendar_2.IcalendarType;

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
   * @throws TzException
   */
  void stop() throws TzException;

  /**
   * @return stats for the module
   * @throws TzException
   */
  List<Stat> getStats() throws TzException;

  /** Update from primary source if any.
   *
   * @throws TzException
   */
  void checkData() throws TzException;

  /** Update the stored data using the given update list. Note that the update
   * may be transient if the data cache has no or an unchangable backing.
   *
   * @param dtstamp
   * @param dles
   * @throws TzException
   */
  void updateData(String dtstamp,
                  List<DiffListEntry> dles) throws TzException;

  /**
   * @return XML formatted UTC dateTime
   * @throws TzException
   */
  String getDtstamp() throws TzException;

  /** Given an alias return the tzid for that alias
   *
   * @param val
   * @return aliased name or null
   * @throws TzException
   */
  String fromAlias(String val) throws TzException;

  /**
   * @return String value of aliases file.
   * @throws TzException
   */
  String getAliasesStr() throws TzException;

  /**
   * @param tzid
   * @return list of aliases or null
   * @throws TzException
   */
  SortedSet<String> findAliases(String tzid) throws TzException;

  /**
   * @return namelist or null
   * @throws TzException
   */
  SortedSet<String> getNameList() throws TzException;

  /**
   * @param key
   * @param tzs
   * @throws TzException
   */
  void setExpanded(ExpandedMapEntryKey key,
                   ExpandedMapEntry tzs) throws TzException;

  /**
   * @param key
   * @return expanded or null
   * @throws TzException
   */
  ExpandedMapEntry getExpanded(ExpandedMapEntryKey key) throws TzException;

  /** Get cached VTIMEZONE specifications
   *
   * @param name
   * @return cached spec or null.
   * @throws TzException
   */
  String getCachedVtz(final String name) throws TzException;

  /** Get all cached VTIMEZONE specifications
   *
   * @return cached specs or null.
   * @throws TzException
   */
  Collection<String> getAllCachedVtzs() throws TzException;

  /** Get a timezone object from the server given the id.
   *
   * @param tzid
   * @return TimeZone with id or null
   * @throws TzException
   */
  TimeZone getTimeZone(final String tzid) throws TzException;

  /* * Get an aliased timezone object from the server given the id.
   *
   * @param tzid
   * @return TimeZone with id or null
   * @throws TzException
   * /
  TimeZone getAliasedTimeZone(final String tzid) throws TzException;
  */

  /** Get a timezone object from the server given the id.
   *
   * @param tzid
   * @return IcalendarType with id or null
   * @throws TzException
   */
  IcalendarType getXTimeZone(final String tzid) throws TzException;

  /** Get an aliased timezone object from the server given the id.
   *
   * @param tzid
   * @return IcalendarType with id or null
   * @throws TzException
   */
  IcalendarType getAliasedXTimeZone(final String tzid) throws TzException;

  /** Get an aliased cached VTIMEZONE specifications
   *
   * @param name
   * @return cached spec or null.
   * @throws TzException
   */
  String getAliasedCachedVtz(final String name) throws TzException;

  /**
   * @param changedSince - null or dtstamp value
   * @return list of summary info
   * @throws TzException
   */
  List<TimezoneType> getTimezones(String changedSince) throws TzException;

  /**
   * @param name to be partially matched
   * @return list of matching summary info
   * @throws TzException
   */
  List<TimezoneType> findTimezones(String name) throws TzException;
}
