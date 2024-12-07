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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Cached data affected by the source data.
 *
 * @author douglm
 */
public class ZipCachedData  extends AbstractCachedData {
  private ZipFile tzDefsZipFile;

  private File tzDefsFile;

  /**
   * @param cfg tz configuration
   */
  public ZipCachedData(final TzConfig cfg) {
    super(cfg, "Zip");
    loadData();
  }

  @Override
  public void stop() {
  }

  @Override
  public String getSource() {
    return null;
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
    final List<String> ids = new ArrayList<>();

    return ids;
  }

  private synchronized void loadData() {
    try {
      final long smillis = System.currentTimeMillis();

      /* ======================== First get the data file =================== */
      final File f = getdata();

      /* ============================ open a zip file ======================= */
      final ZipFile zf = new ZipFile(f);

      if (tzDefsZipFile != null) {
        try {
          tzDefsZipFile.close();
        } catch (final Throwable ignored) {
        }
      }

      if (tzDefsFile != null) {
        try {
          tzDefsFile.delete();
        } catch (final Throwable ignored) {
        }
      }

      tzDefsFile = f;
      tzDefsZipFile = zf;

      TzServerUtil.lastDataFetch = System.currentTimeMillis();

      /* ========================= get the data info ======================== */

      final ZipEntry ze = tzDefsZipFile.getEntry("info.txt");

      final String info = entryToString(ze);

      final String[] infoLines = info.split("\n");

      for (final String s: infoLines) {
        if (s.startsWith("buildTime=")) {
          String bt = s.substring("buildTime=".length());
          if (!bt.endsWith("Z")) {
            // Pretend it's UTC
            bt += "Z";
          }
          dtstamp = XcalUtil.getXmlFormatDateTime(bt);
        }
      }

      /* ===================== Rebuild the alias maps ======================= */

      aliasMaps = buildAliasMaps(tzDefsZipFile);

      /* ===================== All tzs into the table ======================= */

      unzipTzs(tzDefsZipFile, dtstamp);
      expansions.clear();

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
   * @param tzDefsZipFile a zip file
   * @return mapped aliases
   */
  private AliasMaps buildAliasMaps(final ZipFile tzDefsZipFile) {
    try {
      final ZipEntry ze = tzDefsZipFile.getEntry("aliases.txt");

      final AliasMaps maps = new AliasMaps();
      maps.aliasesStr = entryToString(ze);

      maps.byTzid = new HashMap<>();
      maps.byAlias = new HashMap<>();
      maps.aliases = new Properties();

      final StringReader sr = new StringReader(maps.aliasesStr);

      maps.aliases.load(sr);

      for (final String aliasId: maps.aliases.stringPropertyNames()) {
        final String val = maps.aliases.getProperty(aliasId);

        if (val == null) {
          continue;
        }

        final SortedSet<String> as = maps.byTzid
                .computeIfAbsent(val, k -> new TreeSet<>());

        as.add(aliasId);

        maps.byAlias.put(aliasId, val);
      }

      return maps;
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  private void unzipTzs(final ZipFile tzDefsZipFile,
                        final String dtstamp) {
    try {
      resetTzs();

      final Enumeration<? extends ZipEntry> zes = tzDefsZipFile.entries();

      while (zes.hasMoreElements()) {
        final ZipEntry ze = zes.nextElement();

        if (ze.isDirectory()) {
          continue;
        }

        final String n = ze.getName();

        if (!(n.startsWith("zoneinfo/") && n.endsWith(".ics"))) {
          continue;
        }

        final String id = n.substring(9, n.length() - 4);

        processSpec(id, entryToString(ze), null, null);
      }
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  /** Retrieve the data and store in a temp file. Return the file object.
   *
   * @return File
   */
  private File getdata() {
    try {
      final String dataUrl = cfg.getTzdataUrl();
      if (dataUrl == null) {
        throw new TzException("No data url defined");
      }

      if (!dataUrl.startsWith("http:")) {
        return new File(dataUrl);
      }

      /* Fetch the data */
      final HttpClient client = new DefaultHttpClient();

      final HttpRequestBase get = new HttpGet(dataUrl);

      final HttpResponse resp = client.execute(get);

      InputStream is = null;
      FileOutputStream fos = null;

      try {
        is = resp.getEntity().getContent();

        final File f = File.createTempFile("bwtzserver", "zip");

        fos = new FileOutputStream(f);

        final byte[] buff = new byte[4096];

        for (;;) {
          final int num = is.read(buff);

          if (num < 0) {
            break;
          }

          if (num > 0) {
            fos.write(buff, 0, num);
          }
        }

        return f;
      } finally {
        try {
          fos.close();
        } finally {}

        try {
          is.close();
        } finally {}
      }
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  private String entryToString(final ZipEntry ze) {
    try (final InputStreamReader is =
                 new InputStreamReader(tzDefsZipFile.getInputStream(ze),
                                       StandardCharsets.UTF_8)) {

      final StringWriter sw = new StringWriter();

      final char[] buff = new char[4096];

      for (; ; ) {
        final int num = is.read(buff);

        if (num < 0) {
          break;
        }

        if (num > 0) {
          sw.write(buff, 0, num);
        }
      }

      return sw.toString();
    } catch (final IOException ie) {
      throw new TzException(ie);
    }
  }
}
