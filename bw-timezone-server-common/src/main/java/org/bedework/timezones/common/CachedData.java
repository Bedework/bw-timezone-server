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
   */
  void stop();

  /**
   * @return String source information for data.
   */
  String getSource();

  /**
   * @return stats for the module
   */
  List<Stat> getStats();

  /** Update from primary source if any.
   *
   */
  void checkData();

  /** Update the stored data using the given update list. Note that the update
   * may be transient if the data cache has no or an unchangable backing.
   *
   * @param dtstamp lastmod for change
   * @param dles diff list
   */
  void updateData(String dtstamp,
                  List<DiffListEntry> dles);

  /**
   * @return XML formatted UTC dateTime
   */
  String getDtstamp();

  /** Given an alias return the tzid for that alias
   *
   * @param val alias
   * @return aliased name or null
   */
  @SuppressWarnings("UnusedDeclaration")
  String fromAlias(String val);

  /**
   * @return String value of aliases file.
   */
  String getAliasesStr();

  /**
   * @param tzid for which we want aliases
   * @return set of aliases or null
   */
  SortedSet<String> findAliases(String tzid);

  /**
   * @return namelist or null
   */
  SortedSet<String> getNameList();

  /**
   * @param key to expanded map
   * @param tzs entries from map
   */
  void setExpanded(ExpandedMapEntryKey key,
                   ExpandedMapEntry tzs);

  /**
   * @param key to expanded map
   * @return expanded or null
   */
  ExpandedMapEntry getExpanded(ExpandedMapEntryKey key);

  /** Get cached VTIMEZONE specifications
   *
   * @param name tzid
   * @return cached spec or null.
   */
  String getCachedVtz(String name);

  /** Get all cached VTIMEZONE specifications
   *
   * @return cached specs or null.
   */
  Collection<String> getAllCachedVtzs();

  /** Get a timezone object from the server given the id.
   *
   * @param tzid the id
   * @return TimeZone with id or null
   */
  TimeZone getTimeZone(String tzid);

  /* * Get an aliased timezone object from the server given the id.
   *
   * @param tzid
   * @return TimeZone with id or null
   * /
  TimeZone getAliasedTimeZone(final String tzid);
  */

  /** Get a timezone object from the server given the id.
   *
   * @param tzid the id
   * @return IcalendarType with id or null
   */
  IcalendarType getXTimeZone(String tzid);

  /** Get an aliased timezone object from the server given the id.
   *
   * @param tzid the id
   * @return IcalendarType with id or null
   */
  @SuppressWarnings("UnusedDeclaration")
  IcalendarType getAliasedXTimeZone(String tzid);

  /** Get an aliased cached VTIMEZONE specifications
   *
   * @param name tzid
   * @return cached spec or null.
   */
  String getAliasedCachedVtz(String name);

  /**
   * @param tzids - to fetch
   * @return list of summary info
   */
  List<TimezoneType> getTimezones(String[] tzids);

  /**
   * @param changedSince - null or dtstamp value
   * @return list of summary info
   */
  List<TimezoneType> getTimezones(String changedSince);

  /**
   * @param name to be partially matched
   * @return list of matching summary info
   */
  List<TimezoneType> findTimezones(String name);
}
