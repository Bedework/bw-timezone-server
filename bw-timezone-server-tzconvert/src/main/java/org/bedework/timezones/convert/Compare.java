/*
#    Copyright (c) 2007-2013 Cyrus Daboo. All rights reserved.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
*/
package org.bedework.timezones.convert;

import org.bedework.util.jmx.InfoLines;
import org.bedework.util.logging.Logged;
import org.bedework.util.timezones.TzFetcher;

import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.Observance;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.util.Dates;
import net.fortuna.ical4j.util.TimeZones;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class Compare implements Logged {
  /**
   * @param vtzs timezones to compare
   * @param tzFetcher fetcher to compare against
   * @param msgs for output
   * @return tzs that differ - null for exception
   */
  List<String> compare(final Map<String, VTimeZone> vtzs,
                       final TzFetcher tzFetcher,
                       final InfoLines msgs) {
    try {
      final Date endDate = new DateTime("20200101T000000Z");
      final List<String> tzids = new ArrayList<>();

      for (final String tzid: vtzs.keySet()) {
        final VTimeZone ourVtz = vtzs.get(tzid);
        final VTimeZone otherTz = tzFetcher.getTz(tzid);

        if (otherTz == null) {
          continue;
        }

        if (!compareTz(ourVtz, otherTz, endDate, msgs)) {
          tzids.add(tzid);
        }
      }

      return tzids;
    } catch (final Throwable t) {
      msgs.exceptionMsg(t);
      return null;
    }
  }

  boolean compareTz(final VTimeZone tz,
                    final VTimeZone thatTz,
                    final Date endDate,
                    final InfoLines msgs) {
    final Set<Onset> onsets = getOnsets(tz, endDate, msgs);
    final Set<Onset> thatOnsets = getOnsets(thatTz, endDate, msgs);

    final String tzid = tz.getTimeZoneId().getValue();

    boolean matches = missing(onsets, thatOnsets,
                              "For " + tzid + " in this but not that", msgs);

    if (!missing(thatOnsets, onsets,
                 "For " + tzid + " in that but not this", msgs)) {
      matches = false;
    }

    if (debug()) {
      debug("Timezone " + tzid + " match=" + matches);
    }

    if (!matches) {
      msgs.addLn("Timezone " + tzid + " does not match");
    }

    return matches;
  }

  @SuppressWarnings("unchecked")
  private static boolean missing(final Set<Onset> a,
                                 final Set<Onset> b,
                                 final String msgPrefix,
                                 final InfoLines msgs) {
    final Set<Onset> notInThat = new TreeSet<>();

    notInThat.addAll(a);
    notInThat.removeAll(b);

    if (notInThat.isEmpty()) {
      return true;
    }

    for (final Object o: notInThat) {
      msgs.addLn(msgPrefix + " " + o);
    }

    return false;
  }

  private Set<Onset> getOnsets(final VTimeZone tz,
                                final Date endDate,
                                final InfoLines msgs) {
    final ComponentList observances = tz.getObservances();

    final Set<Onset> onsets = new TreeSet<>();

    for (final Object o: observances) {
      final Set<Onset> dl = getOnsets((Observance)o, endDate, msgs);

      //noinspection unchecked
      onsets.addAll(dl);
    }

//    Collections.sort(onsets);

    return onsets;
  }

  /**
   * Used for parsing times in a UTC date-time representation.
   */
  private static final String UTC_PATTERN = "yyyyMMdd'T'HHmmss";
  private static final DateFormat UTC_FORMAT = new SimpleDateFormat(
          UTC_PATTERN);

  static {
    UTC_FORMAT.setTimeZone(TimeZone.getTimeZone(TimeZones.UTC_ID));
    UTC_FORMAT.setLenient(false);
  }

  private static class Onset implements Comparable<Onset> {
    DateTime onset;
    long offset;

    private Onset(final DateTime onset,
                  final long offset) {
      this.onset = onset;
      this.offset = offset;

      onset.setUtc(true);
    }

    public boolean equals(final Object o) {
      final Onset on = (Onset)o;

      return (onset.equals(on.onset)) &&
              (offset == on.offset);
    }

    @Override
    public int compareTo(final Onset o) {
      int res = onset.compareTo(o.onset);

      if (res != 0) {
        return res;
      }

      if (offset > o.offset) {
        return 1;
      }

      if (offset < o.offset) {
        return -1;
      }

      return 0;
    }

    public String toString() {
      return onset.toString() + " " + offset;
    }
  }

  private Set<Onset> getOnsets(final Observance o,
                                final Date endDate,
                                final InfoLines msgs) {
    final DateTime initialOnset;
    final Set<Onset> onsets = new TreeSet<>();
    final long offset = o.getOffsetTo().getOffset().getOffset();

    try {
      initialOnset = applyOffsetFrom(o, calculateOnset(
              ((DtStart)o.getProperty(Property.DTSTART)).getDate()
                      .toString()));
    } catch (final ParseException e) {
      msgs.add("Unexpected error calculating initial onset " + e);
      return null;
    }

    onsets.add(new Onset(initialOnset, offset));

    final Date initialOnsetUTC;
    // get first onset without adding TZFROM as this may lead to a day boundary
    // change which would be incompatible with BYDAY RRULES
    // we will have to add the offset to all cacheable onsets
    try {
      initialOnsetUTC = calculateOnset(((DtStart)o.getProperty(
              Property.DTSTART)).getDate().toString());
    } catch (final ParseException e) {
      msgs.add("Unexpected error calculating initial onset " + e);
      return null;
    }

    // check rdates for latest applicable onset..
    final PropertyList rdates = o.getProperties(Property.RDATE);
    for (final Object rdate1 : rdates) {
      final RDate rdate = (RDate)rdate1;
      for (final Object o1 : rdate.getDates()) {
        try {
          final DateTime rdateOnset =
                  applyOffsetFrom(o, calculateOnset(
                          o1.toString()));
          if (!rdateOnset.after(endDate)) {
            onsets.add(new Onset(rdateOnset, offset));
          }
        } catch (final ParseException e) {
          msgs.add("Unexpected error calculating onset" + e);
        }
      }
    }

    // check recurrence rules for latest applicable onset..
    final PropertyList rrules = o.getProperties(Property.RRULE);
    for (final Object rrule1 : rrules) {
      final RRule rrule = (RRule)rrule1;
      // include future onsets to determine onset period..
      final Calendar cal = Dates.getCalendarInstance(endDate);
      cal.setTime(endDate);
      cal.add(Calendar.YEAR, 10);
      final Date onsetLimit = Dates.getInstance(cal.getTime(),
                                                Value.DATE_TIME);
      final DateList recurrenceDates =
              rrule.getRecur().getDates(initialOnsetUTC,
                                        onsetLimit, Value.DATE_TIME);
      for (final Object recurrenceDate : recurrenceDates) {
        final DateTime rruleOnset =
                applyOffsetFrom(o,
                                (DateTime)recurrenceDate);
        if (!rruleOnset.after(endDate)) {
          onsets.add(new Onset(rruleOnset, offset));
        }
      }
    }

//    Collections.sort(onsets);

    return onsets;
  }

  private static DateTime calculateOnset(final String dateStr) throws ParseException {

    // Translate local onset into UTC time by parsing local time
    // as GMT and adjusting by TZOFFSETFROM if required
    final long utcOnset;

    synchronized (UTC_FORMAT) {
      utcOnset = UTC_FORMAT.parse(dateStr).getTime();
    }

    // return a UTC
    final DateTime onset = new DateTime(true);
    onset.setTime(utcOnset);
    return onset;
  }

  private DateTime applyOffsetFrom(
          final Observance o,
          final DateTime orig) {
    final DateTime withOffset = new DateTime(true);
    withOffset.setTime(orig.getTime() - o.getOffsetFrom().getOffset()
            .getOffset());
    withOffset.setUtc(true);

    return withOffset;
  }
}

