/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

    Redistribution and use of this distribution in source and binary forms,
    with or without modification, are permitted provided that:
       The above copyright notice and this permission notice appear in all
        copies and supporting documentation;

        The name, identifiers, and trademarks of Rensselaer Polytechnic
        Institute are not used in advertising or publicity without the
        express prior written permission of Rensselaer Polytechnic Institute;

    DISCLAIMER: The software is distributed" AS IS" without any express or
    implied warranty, including but not limited to, any implied warranties
    of merchantability or fitness for a particular purpose or any warrant)'
    of non-infringement of any current or pending patent rights. The authors
    of the software make no representations about the suitability of this
    software for any particular purpose. The entire risk as to the quality
    and performance of the software is with the user. Should the software
    prove defective, the user assumes the cost of all necessary servicing,
    repair or correction. In particular, neither Rensselaer Polytechnic
    Institute, nor the authors of the software are liable for any indirect,
    special, consequential, or incidental damages related to the software,
    to the maximum extent the law permits.
*/
package org.bedework.timezones.common;

import net.fortuna.ical4j.model.TimeZone;
import ietf.params.xml.ns.timezone_service.SummaryType;
import ietf.params.xml.ns.timezone_service.TimezonesType;

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
  /**
   *
   */
  void refresh();

  /**
   * @return data info
   * @throws ServletException
   */
  Collection<String> getDataInfo() throws ServletException;

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
                   TimezonesType tzs) throws ServletException;

  /**
   * @param key
   * @return expanded or null
   * @throws ServletException
   */
  TimezonesType getExpanded(ExpandedMapEntryKey key) throws ServletException;

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
   * @return list of summary info
   * @throws ServletException
   */
  List<SummaryType> getSummaries() throws ServletException;
}
