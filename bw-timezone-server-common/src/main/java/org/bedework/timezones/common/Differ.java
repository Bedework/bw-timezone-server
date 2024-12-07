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

import org.bedework.util.calendar.diff.XmlIcalCompare;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;

import ietf.params.xml.ns.icalendar_2.IcalendarType;
import org.oasis_open.docs.ws_calendar.ns.soap.ComponentSelectionType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/** This class provides support for diffing timezone data to determine if
 * updates need to be made to stored data.
 *
 * @author douglm
 */
public class Differ implements Logged {
  /**
   */
  public static class DiffListEntry {
    /** Tzid changed
     */
    public String tzid;

    /** If not add or delete then update.
     */
    public boolean add;

    /**  We should never delete even though we check for it here */
    public boolean deleted;

    /** The spec
     */
    public String tzSpec;

    /** The XML spec
     */
    public IcalendarType xcal;

    /** True if only aliases changed
     */
    public boolean aliasChangeOnly;
    /**
     */
    public SortedSet<String> aliases = new TreeSet<>();

    /** Add our stuff to the StringBuilder
     *
     * @param sb    StringBuilder for result
     */
    protected void toStringSegment(final StringBuilder sb,
                                   final boolean full,
                                   final String indent) {
      sb.append("tzid = ");
      sb.append(tzid);

      if (add) {
        sb.append(", add");
      } else if (deleted) {
        sb.append(", deleted");
      } else {
        sb.append(", update");
      }

      if (aliasChangeOnly) {
        sb.append(", aliasChangeOnly");
      }
    }

    /**
     * @return short form.
     */
    public String toShortString() {
      final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

      toStringSegment(sb, false, "  ");

      sb.append("}");
      return sb.toString();
    }
  }

  /**
   */
  public Differ() {
  }

  /** Compares the new set of data with the supplied current set of data.
   *
   * @param newTzdata new set
   * @param currentTzdata current set
   * @return possibly empty list - never null.
   */
  public List<DiffListEntry> compare(final CachedData newTzdata,
                                     final CachedData currentTzdata) {
    final List<DiffListEntry> res = new ArrayList<>();

    final SortedSet<String> newNames = newTzdata.getNameList();

    final NameChanges nc = getNameChanges(newNames, currentTzdata.getNameList());

    if (!nc.deletedNames.isEmpty()) {
      warn("Following ids appear to have been deleted");
      for (final String id: nc.deletedNames) {
        warn("   " + id);
      }
    }

    if (debug()) {
      debug("Following ids appear to have been added");
      for (final String id: nc.addedNames) {
        debug("   " + id);
      }
    }

    /* Get each timezone that exists in new and current and compare to see if
     * it's changed.
     */

    final XmlIcalCompare comp = new XmlIcalCompare(XmlIcalCompare.defaultSkipList,
                                                   null); // Shouldn't need any tzs

    for (final String tzid: newNames) {
      if (nc.addedNames.contains(tzid)) {
        final DiffListEntry dle = new DiffListEntry();

        dle.tzid = tzid;
        dle.add = true;
        dle.tzSpec = newTzdata.getCachedVtz(tzid);
        dle.aliases = newTzdata.findAliases(tzid);

        res.add(dle);
        continue;
      }

      if (nc.deletedNames.contains(tzid)) {
        final DiffListEntry dle = new DiffListEntry();

        dle.tzid = tzid;
        dle.deleted = true;
        dle.tzSpec = currentTzdata.getCachedVtz(tzid);
        dle.aliases = currentTzdata.findAliases(tzid);

        res.add(dle);
        continue;
      }

      /* compare */

      final IcalendarType newXcal = newTzdata.getXTimeZone(tzid);
      final IcalendarType currentXcal = currentTzdata.getXTimeZone(tzid);

      final ComponentSelectionType cst = comp.diff(newXcal, currentXcal);

      if (cst == null) {
        continue;
      }

      if (debug()) {
        debug("Adding " + tzid);
      }

      final DiffListEntry dle = new DiffListEntry();

      dle.tzid = tzid;
      dle.tzSpec = newTzdata.getCachedVtz(tzid);
      dle.aliases = newTzdata.findAliases(tzid);
      dle.xcal = newXcal;

      res.add(dle);
    }

    return res;
  }

  private static class NameChanges {
    SortedSet<String> addedNames = new TreeSet<>();
    SortedSet<String> deletedNames = new TreeSet<>();
  }

  private NameChanges getNameChanges(final SortedSet<String> newList,
                                     final SortedSet<String> currentList) {
    final NameChanges nc = new NameChanges();

    final Iterator<String> nit = newList.iterator();
    final Iterator<String> cit = currentList.iterator();
    String nid = nit.next();
    String cid = cit.next();

    while ((nid != null) || (cid != null)) {
      boolean advanceNew = false;
      boolean advanceCur = false;

      test: {
        if (cid == null) {
          nc.addedNames.add(nid);
          advanceNew = true;
          break test;
        }

        if (nid == null) {
          nc.deletedNames.add(cid);
          advanceCur = true;
          break test;
        }

        final int cmp = nid.compareTo(cid);

        if (cmp == 0) {
          advanceNew = true;
          advanceCur = true;

          break test;
        }

        if (cmp < 0) {
          nc.addedNames.add(nid);
          advanceNew = true;

          break test;
        }

        nc.deletedNames.add(cid);
        advanceCur = true;
      } // test

      if (advanceCur) {
        if (cit.hasNext()) {
          cid = cit.next();
        } else {
          cid = null;
        }
      }

      if (advanceNew) {
        if (nit.hasNext()) {
          nid = nit.next();
        } else {
          nid = null;
        }
      }
    }

    return nc;
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
