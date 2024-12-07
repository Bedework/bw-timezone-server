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
import org.bedework.util.calendar.XcalUtil;
import org.bedework.util.timezones.FileTzFetcher;
import org.bedework.util.timezones.TzFetcher;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.property.Version;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

/** Cached data obtained from a set of files and directories..
 *
 * @author douglm
 */
public class FileCachedData extends AbstractCachedData {
  private String source;

  /**
   * @param cfg configuration file
   */
  public FileCachedData(final TzConfig cfg) {
    super(cfg, "File");
    loadData();
  }

  @Override
  public void stop() {
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public void checkData() {
    loadData();
  }

  @Override
  public void updateData(final String dtstamp,
                         final List<DiffListEntry> dles) {
    // XXX ??
  }

  @Override
  public List<String> findIds(final String val) {
    return new ArrayList<>();
  }

  private synchronized void loadData() {
    try {
      final long smillis = System.currentTimeMillis();

      /* ======================== First get the data file =================== */
      final File dataDir = getdata();

      TzServerUtil.lastDataFetch = System.currentTimeMillis();

      /* ========================= get the data info ======================== */

      /* This MUST be stored in the directory as file info.properties */

      final Properties info = new Properties();

      info.load(getFileRdr(dataDir, "info.properties"));

      dtstamp = XcalUtil.getXmlFormatDateTime(info.getProperty(
              "buildTime"));
      if (info.getProperty("prodid") != null) {
        TzServerUtil.setProdid(info.getProperty("prodid"));
      }

      source = info.getProperty("source");

      /* ===================== Rebuild the alias maps ======================= */

      aliasMaps = buildAliasMaps(dataDir);

      /* ===================== All tzs into the table ======================= */

      fetchTzs(dtstamp);
      expansions.clear();

      cfg.setDtstamp(dtstamp);
      cfg.setSource(source);

      TzServerUtil.saveConfig();

      TzServerUtil.reloadsMillis += System.currentTimeMillis() - smillis;
      TzServerUtil.reloads++;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  /** We store the aliases as a bunch of properties of the form <br/>
   * alias=val<br/>
   * the alias is the name and val is a comma separated list of
   * target ids.
   *
   * @param parent - root directory
   * @return mapped aliases
   */
  private AliasMaps buildAliasMaps(final File parent) {
    try {
      final AliasMaps maps = new AliasMaps();

      maps.byTzid = new HashMap<>();
      maps.byAlias = new HashMap<>();
      maps.aliases = new Properties();

      maps.aliases.load(getFileRdr(parent, "aliases.properties"));

      final StringBuilder aliasStr = new StringBuilder();

      for (final String aliasId: maps.aliases.stringPropertyNames()) {
        final String val = maps.aliases.getProperty(aliasId);

        if (val == null) {
          continue;
        }

        final SortedSet<String> as = maps.byTzid
                .computeIfAbsent(val, k -> new TreeSet<>());

        as.add(aliasId);

        aliasStr.append(escape(aliasId));
        aliasStr.append('=');
        aliasStr.append(escape(val));
        aliasStr.append('\n');

        maps.byAlias.put(aliasId, val);
      }

      maps.aliasesStr = aliasStr.toString();

      return maps;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  private void fetchTzs(final String dtstamp) {
    try {
      resetTzs();

      final Path dataPath = Paths.get(cfg.getTzdataUrl(),"zoneinfo");
      try (final TzFetcher tzFetcher =
                   new FileTzFetcher(dataPath.toString())) {

        for (final String id: tzFetcher.getTzids()) {
          final Calendar cal = new Calendar();
          cal.getComponents().add(tzFetcher.getTz(id));
          cal.getProperties().add(new Version());

          processSpec(id, cal, null, dtstamp);
        }
      }
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  /** Return the File object which must represent a directory.
   *
   * @return File
   */
  private File getdata() {
    try {
      final String dataUrl = cfg.getTzdataUrl();
      if (dataUrl == null) {
        throw new TzException("No data url defined");
      }

      final File f = new File(dataUrl);

      if (!f.isDirectory()) {
        throw new TzException(dataUrl + " is not a directory");
      }

      return f;
    } catch (final TzException tze) {
      throw tze;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  private FileReader getFileRdr(final File parent,
                                final String name) {
    try {
      final File theFile = new File(parent.getAbsolutePath(), name);

      if (!theFile.exists() || !theFile.isFile()) {
        throw new TzException(name + " does not exist or is not a file. Path: " +
                                      theFile.getAbsolutePath());
      }

      return new FileReader(theFile);
    } catch (final TzException tze) {
      throw tze;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }
}
