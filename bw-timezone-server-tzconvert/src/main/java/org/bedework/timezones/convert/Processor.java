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

/*
from __future__ import with_statement

from difflib import unified_diff
from pycalendar.icalendar.calendar import Calendar
import cStringIO as StringIO
import getopt
import os
import rule
import sys
import zone
*/

import org.bedework.timezones.common.TzException;
import org.bedework.timezones.convert.LineReader.LineReaderIterator;

import edu.rpi.cmt.jmx.InfoLines;
import edu.rpi.cmt.timezones.FileTzFetcher;
import edu.rpi.cmt.timezones.ServerTzFetcher;
import edu.rpi.cmt.timezones.TzFetcher;
import edu.rpi.sss.util.Util;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;

import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** Classes to parse a tzdata files and generate VTIMEZONE data.
 *
 * <pre>
 Input lines are made up of fields.  Fields are separated
 from one another by any number of white space characters.
 Leading and trailing white space on input lines is ignored.
 An unquoted sharp character (#) in the input introduces a
 comment which extends to the end of the line the sharp
 character appears on.  White space characters and sharp
 characters may be enclosed in double quotes (") if they're
 to be used as part of a field.  Any line that is blank
 (after comment stripping) is ignored.  Non-blank lines are
 expected to be of one of three types: rule lines, zone
 lines, and link lines.

 A link line has the form

 Link  LINK-FROM        LINK-TO

 For example:

 Link  Europe/Istanbul  Asia/Istanbul

 The LINK-FROM field should appear as the NAME field in some
 zone line; the LINK-TO field is used as an alternate name
 for that zone.

 Lines in the file that describes leap seconds have the
 following form:

 Leap  YEAR  MONTH  DAY  HH:MM:SS  CORR  R/S

 For example:

 Leap  1974  Dec    31   23:59:60  +     S

 The YEAR, MONTH, DAY, and HH:MM:SS fields tell when the leap
 second happened.  The CORR field should be "+" if a second
 was added or "-" if a second was skipped.  The R/S field
 should be (an abbreviation of) "Stationary" if the leap
 second time given by the other fields should be interpreted
 as UTC or (an abbreviation of) "Rolling" if the leap second
 time given by the other fields should be interpreted as
 local wall clock time.
 * </pre>
 */
class Processor {
  private final Map<String, RuleSet> rules = new HashMap<>();
  private final Map<String, Zone> zones = new HashMap<>();

  /* key:to value:from */
  private final Map<String, String> links = new HashMap<>();

  // Set the PRODID value used in generated iCalendar data
  private String prodid = "-//bedework.org//tzsvr//EN";
  private String rootdir = "../../stuff/temp";
  private int startYear = 1800;
  private int endYear = 2040;

  private final Map<String, VTimeZone> vtzs = new HashMap<>();
  private boolean vtzsBuilt;
  private boolean compare;

  private String tzServerUri; // = //"www.bedework.org";
//          "https://demo.calendarserver.org:8443/stdtimezones";
  private String compareWithPath;

  private String aliasesPath;

  private String source;

  private boolean verbose;
  private boolean generate = true;

  /* null for no filtering - otherwise only these */
  private List<String> filterzones;

  private final static String[] zonefiles = {
          "northamerica",
          "southamerica",
          "europe",
          "africa",
          "asia",
          "australasia",
          "antarctica",
          "etcetera",
          "backward",
  };

  Set<String> getZoneNames() {
    return zones.keySet();
  }

  /** Parse timezone data based on properties.
   *
   */
  public void parse() {
    final String zonedir = Util.buildPath(true, getRootdir(),
                                          "/", "tzdata");

    for (final String file: zonefiles) {
      parseFile(Util.buildPath(true, zonedir, "/", file));
    }
  }

  /**
   Expand a zones transition dates up to the specified year.
   */
  List<Zone.ExpandResult> expandZone(final String zonename,
                                     final int minYear,
                                     int maxYear) {
    if (maxYear < 0) {
      maxYear = 2018;
    }

    final Zone zone = zones.get(zonename);
    return zone.expand(rules, minYear, maxYear);
  }

  /**
  Generate iCalendar data for all VTIMEZONEs or just those specified
  */
  public String vtimezones(final int minYear,
                           final int maxYear) {
    final Calendar cal = new Calendar();
    for (final Zone zone: zones.values()) {
      if ((filterzones != null) && (!filterzones.contains(zone.name))) {
        continue;
      }

      final VTimeZone vtz = zone.vtimezone(rules, minYear, maxYear);
      cal.getComponents().add(vtz);
    }

    return cal.toString();
  }

  /**
   * @param outputdir - where to put output
   * @param doLinks - true to create link data
   * @throws Throwable
   */
  public void generateZoneinfoFiles(final String outputdir,
                                    final boolean doLinks) throws Throwable {
    buildVtzs();

    // Empty current directory
    Utils.empty(outputdir);

    for (final String zoneName: vtzs.keySet()) {
      final Calendar cal = new Calendar();
      final ComponentList cl = cal.getComponents();
      final PropertyList pl = cal.getProperties();

      pl.add(new Version());
      pl.add(new ProdId(getProdid()));

      cl.add(vtzs.get(zoneName));

      final String icsdata = cal.toString();

      final Path fpath =
              Utils.createFile(Util.buildPath(false,
                                              outputdir, "/",
                                              zoneName, ".ics"));

      final OutputStream os = Files.newOutputStream(fpath);

      os.write(icsdata.getBytes());

      os.close();

      if (verbose) {
        Utils.print("Write path: %s", fpath);
      }
    }

    generateInfo(outputdir);

    if (!doLinks) {
      return;
    }

    generateLinks(outputdir);
  }

  /** Compare based on settings
   * @param msgs for output
   */
  public void compare(final InfoLines msgs) {
    try {
      if (getTzServerUri() != null) {
        final ServerTzFetcher tzFetcher = new ServerTzFetcher(getTzServerUri());

        compare(tzFetcher, msgs);

        return;
      }

      if (getCompareWithPath() != null) {
        final FileTzFetcher tzFetcher =
                new FileTzFetcher(getCompareWithPath());

        compare(tzFetcher, msgs);

        return;
      }
    } catch (final Throwable t) {
      msgs.exceptionMsg(t);
      return;
    }

    msgs.addLn("No comparison data source");
  }

  /**
   * @param tzFetcher to fetch tzs
   * @param msgs for output
   */
  public void compare(final TzFetcher tzFetcher,
               final InfoLines msgs) {
    buildVtzs();

    new Compare().compare(vtzs, tzFetcher, msgs);
  }

  public String getTzServerUri() {
    return tzServerUri;
  }

  public void setTzServerUri(final String tzServerUri) {
    this.tzServerUri = tzServerUri;
  }

  public boolean getCompare() {
    return compare;
  }

  public void setCompare(final boolean compare) {
    this.compare = compare;
  }

  public int getEndYear() {
    return endYear;
  }

  public void setEndYear(final int endYear) {
    this.endYear = endYear;
  }

  public int getStartYear() {
    return startYear;
  }

  public void setStartYear(final int val) {
    startYear = val;
  }

  public String getRootdir() {
    return rootdir;
  }

  public void setRootdir(final String val) {
    rootdir = val;
  }

  public String getProdid() {
    return prodid;
  }

  public void setProdid(final String val) {
    prodid = val;
  }

  public String getSource() {
    return source;
  }

  public void setSource(final String val) {
    source = val;
  }

  public boolean getGenerate() {
    return generate;
  }

  public void setGenerate(final boolean val) {
    generate = val;
  }

  public String getCompareWithPath() {
    return compareWithPath;
  }

  public void setCompareWithPath(final String val) {
    compareWithPath = val;
  }

  public String getAliasesPath() {
    return aliasesPath;
  }

  public void setAliasesPath(final String val) {
    aliasesPath = val;
  }

  private void buildVtzs() {
    if (vtzsBuilt) {
      return;
    }

    for (final Zone zone: zones.values()) {
      if ((filterzones != null) && (!filterzones.contains(zone.name))) {
        continue;
      }

      final VTimeZone vtz = zone.vtimezone(rules,
                                           getStartYear(),
                                           getEndYear());

      vtzs.put(zone.name, vtz);
    }

    vtzsBuilt = true;
  }

  private void parseFile(final String file) {
    try {
      final LineReader f = new LineReader(file);
      final LineReaderIterator lri = (LineReaderIterator)f.iterator();

      while (lri.hasNext()) {
        final String line = lri.next();

        if (line == null) {
          break;
        }

        if (line.startsWith("#") || (line.length() == 0)) {
          continue;
        }

        if (line.startsWith("Rule")) {
          parseRule(line);
          continue;
        }

        if (line.startsWith("Zone")) {
          parseZone(line, lri);
          continue;
        }

        if (line.startsWith("Link")) {
          parseLink(line);
          continue;
        }

        if (line.trim().length() != 0) {
          Utils.assertion(false,
                          "Could not parse line %d from file %s: '%s'",
                          f.lineNbr, file, line);
        }
      }
    } catch (final Throwable t) {
      t.printStackTrace();
      Utils.assertion(false, "Failed to parse file %s", file);
    }
  }

  private void parseRule(final String line) {
    final Rule ruleitem = new Rule();
    ruleitem.parse(line);
    RuleSet rs = rules.get(ruleitem.name);

    if (rs == null) {
      rs = new RuleSet();
      rules.put(ruleitem.name, rs);
    }

    rs.add(ruleitem);
  }

  private void parseZone(final String line,
                         final LineReaderIterator lri) {
    final Zone zoneitem = new Zone();
    zoneitem.parse(line, lri);
    zones.put(zoneitem.name, zoneitem);
  }

  private void parseLink(final String line) {
    // Split on whitespace
    final List<String> fields = Utils.untab(line);

    links.put(fields.get(2), fields.get(1));
  }

  private void generateLinks(final String outputdir) throws Throwable {
    /* First add links from the aliases file */

    final Properties aliases = new Properties();
    aliases.load(getFileRdr(getAliasesPath()));

    for (final Object pname: aliases.keySet()) {
      links.put((String)pname, aliases.getProperty((String)pname));
    }

    final List<String> linkList = new ArrayList<>();

    for (final String linkTo: links.keySet()) {
      final String linkFrom = links.get(linkTo);

      // Check for existing output file
      final String fromPath = Util.buildPath(false, outputdir, "/",
                                             linkFrom, ".ics");
      if (!new File(fromPath).exists()) {
        Utils.print("Missing link from: %s to %s", linkFrom, linkTo);
        continue;
      }

      final List<String> contents = new ArrayList<>();

      final LineReader lr = new LineReader(fromPath);

      for (final String s: lr) {
        contents.add(s.replace(linkFrom, linkTo));
      }

      final String toPath = Util.buildPath(false, outputdir, "/",
                                           linkTo, ".ics");
      final Path outPath = Utils.createFile(toPath);
      final OutputStream os = Files.newOutputStream(outPath);

      for (final String s: contents) {
        os.write(s.getBytes());
        os.write('\n');
      }

      os.close();

      if (verbose) {
        Utils.print("Write link: %s", linkTo);
      }

      linkList.add(linkTo + "\t" + linkFrom);
    }

    Collections.sort(linkList);

    // Generate alias properties
    final String toPath = Util.buildPath(false, outputdir, "/",
                                         "aliases.properties");
    final Path outPath = Utils.createFile(toPath);
    final OutputStream os = Files.newOutputStream(outPath);

    aliases.store(os, "# Timezone aliases file");

    /*
    // Generate link mapping file
    final String toPath = Util.buildPath(false, outputdir, "/",
                                         "links.txt");
    final Path outPath = Utils.createFile(toPath);
    final OutputStream os = Files.newOutputStream(outPath);

    for (final String s: linkList) {
      os.write(s.getBytes());
      os.write('\n');
    }*/

    os.close();
  }

  private void generateInfo(final String outputdir) throws Throwable {
    final Properties info = new Properties();

    final DtStamp dtStamp = new DtStamp();

    info.setProperty("buildTime", dtStamp.getValue());
    info.setProperty("prodid", getProdid());
    info.setProperty("source", getSource());

    final String toPath = Util.buildPath(false, outputdir, "/",
                                         "info.properties");
    final Path outPath = Utils.createFile(toPath);
    final OutputStream os = Files.newOutputStream(outPath);

    info.store(os, "# Timezone server info file");

    os.close();
  }

  private FileReader getFileRdr(final String path) throws TzException {
    try {
      final File theFile = new File(path);

      if (!theFile.exists() || !theFile.isFile()) {
        throw new TzException(path + " does not exist or is not a file");
      }

      return new FileReader(theFile);
    } catch (final TzException tze) {
      throw tze;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }
}
