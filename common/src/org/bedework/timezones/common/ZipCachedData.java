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

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.TzId;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import ietf.params.xml.ns.timezone_service.AliasType;
import ietf.params.xml.ns.timezone_service.SummaryType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletException;

import edu.rpi.cmt.calendar.XcalUtil;

/** Cached data affected by the source data.
 *
 * @author douglm
 */
public class ZipCachedData implements CachedData {
  private volatile boolean refreshNow = true;

  private String tzdataUrl;

  private ZipFile tzDefsZipFile;

  private File tzDefsFile;

  private String buildTime;

  private Map<String, String> vtzs = new HashMap<String, String>();

  private Map<String, TimeZone> tzs = new HashMap<String, TimeZone>();

  private Map<String, String> aliasedVtzs = new HashMap<String, String>();

  private Map<String, TimeZone> aliasedTzs = new HashMap<String, TimeZone>();

  private SortedSet<String> nameList;

  private Map<ExpandedMapEntryKey, ExpandedMapEntry> expansions =
    new HashMap<ExpandedMapEntryKey, ExpandedMapEntry>();

  private static class AliasMaps {
    String aliasesStr;

    Properties aliases;

    Map<String, List<String>> byTzid;
    Map<String, String> byAlias;
  }

  private AliasMaps aliasMaps;

  private List<SummaryType> summaries;

  /**
   * @param tzdataUrl
   */
  public ZipCachedData(final String tzdataUrl) {
    this.tzdataUrl = tzdataUrl;
  }

  @Override
  public void stop() throws ServletException {
  }

  @Override
  public List<Stat> getStats() throws ServletException {
    List<Stat> stats = new ArrayList<Stat>();

    if (tzs == null) {
      stats.add(new Stat("Db", "Unavailable"));
    }

    stats.add(new Stat("Zip #tzs", String.valueOf(tzs.size())));
    stats.add(new Stat("Zip buildTime", buildTime));
    stats.add(new Stat("Zip cached expansions",
                       String.valueOf(expansions.size())));

    return stats;
  }

  /* *
   *
   * @param val
   * /
  public void setTzdataUrl(final String val) {
    tzdataUrl = val;
  }

  /* *
   * @return tzdataUrl
   * /
  public String getTzdataUrl() {
    return tzdataUrl;
  }*/

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#refresh()
   */
  @Override
  public void refresh() {
    refreshNow = true;
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#update()
   */
  @Override
  public void update() {
    refreshNow = true;
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getDtstamp()
   */
  @Override
  public String getDtstamp() throws ServletException {
    reloadData();
    return buildTime;
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#fromAlias(java.lang.String)
   */
  @Override
  public String fromAlias(final String val) throws ServletException {
    reloadData();
    return aliasMaps.byAlias.get(val);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getAliasesStr()
   */
  @Override
  public String getAliasesStr() throws ServletException {
    reloadData();
    return aliasMaps.aliasesStr;
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#findAliases(java.lang.String)
   */
  @Override
  public List<String> findAliases(final String tzid) throws ServletException {
    reloadData();
    return aliasMaps.byTzid.get(tzid);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getNameList()
   */
  @Override
  public SortedSet<String> getNameList() throws ServletException {
    reloadData();
    return nameList;
  }

  @Override
  public void setExpanded(final ExpandedMapEntryKey key,
                          final ExpandedMapEntry tzs) throws ServletException {
    reloadData();
    expansions.put(key, tzs);
  }

  @Override
  public ExpandedMapEntry getExpanded(final ExpandedMapEntryKey key) throws ServletException {
    reloadData();
    return expansions.get(key);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getCachedVtz(java.lang.String)
   */
  @Override
  public String getCachedVtz(final String name) throws ServletException {
    reloadData();
    return vtzs.get(name);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getAllCachedVtzs()
   */
  @Override
  public Collection<String> getAllCachedVtzs() throws ServletException {
    reloadData();
    return vtzs.values();
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getTimeZone(java.lang.String)
   */
  @Override
  public TimeZone getTimeZone(final String tzid) throws ServletException {
    reloadData();
    return tzs.get(tzid);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getAliasedCachedVtz(java.lang.String)
   */
  @Override
  public String getAliasedCachedVtz(final String name) throws ServletException {
    reloadData();
    return aliasedVtzs.get(name);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getAliasedTimeZone(java.lang.String)
   */
  @Override
  public TimeZone getAliasedTimeZone(final String tzid) throws ServletException {
    reloadData();
    return aliasedTzs.get(tzid);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.common.CachedData#getSummaries(java.lang.String)
   */
  @Override
  public List<SummaryType> getSummaries(final String changedSince) throws ServletException {
    reloadData();

    if (changedSince == null) {
      return summaries;
    }

    List<SummaryType> ss = new ArrayList<SummaryType>();

    for (SummaryType sum: summaries) {
      if (sum.getLastModified() == null) {
        ss.add(sum);
        continue;
      }

      String lm = sum.getLastModified().toXMLFormat();

      /*
       * cs > lm +
       * cs = lm 0
       * cs < lm -
       */

      if (changedSince.compareTo(lm) < 0) {
        ss.add(sum);
      }
    }

    return ss;
  }

  /**
   * @param aliasesStr
   * @throws ServletException
   */
  private synchronized void reloadData() throws ServletException {
    if (!refreshNow) {
      return;
    }

    try {
      long smillis = System.currentTimeMillis();

      /* ======================== First get the data file =================== */
      File f = getdata();

      /* ============================ open a zip file ======================= */
      ZipFile zf = new ZipFile(f);

      if (tzDefsZipFile != null) {
        try {
          tzDefsZipFile.close();
        } catch (Throwable t) {
        }
      }

      if (tzDefsFile != null) {
        try {
          tzDefsFile.delete();
        } catch (Throwable t) {
        }
      }

      tzDefsFile = f;
      tzDefsZipFile = zf;

      TzServerUtil.lastDataFetch = System.currentTimeMillis();
      refreshNow = false;

      /* ========================= get the data info ======================== */

      ZipEntry ze = tzDefsZipFile.getEntry("info.txt");

      String info = entryToString(ze);

      String[] infoLines = info.split("\n");

      for (String s: infoLines) {
        if (s.startsWith("buildTime=")) {
          buildTime = s.substring("buildTime=".length());
        }
      }

      /* ===================== Rebuild the alias maps ======================= */

      aliasMaps = buildAliasMaps(tzDefsZipFile);

      /* ===================== All tzs into the table ======================= */

      unzipTzs(tzDefsZipFile, buildTime);
      expansions.clear();

      TzServerUtil.reloadsMillis += System.currentTimeMillis() - smillis;
      TzServerUtil.reloads++;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private AliasMaps buildAliasMaps(final ZipFile tzDefsZipFile) throws ServletException {
    try {
      AliasMaps maps = new AliasMaps();
      ZipEntry ze = tzDefsZipFile.getEntry("aliases.txt");

      maps.aliasesStr = entryToString(ze);

      maps.byTzid = new HashMap<String, List<String>>();
      maps.byAlias = new HashMap<String, String>();
      maps.aliases = new Properties();

      StringReader sr = new StringReader(maps.aliasesStr);

      maps.aliases.load(sr);

      for (String a: maps.aliases.stringPropertyNames()) {
        String id = maps.aliases.getProperty(a);

        maps.byAlias.put(a, id);

        List<String> as = maps.byTzid.get(id);

        if (as == null) {
          as = new ArrayList<String>();
          maps.byTzid.put(id, as);
        }

        as.add(a);
      }

      return maps;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void unzipTzs(final ZipFile tzDefsZipFile,
                        final String buildTime) throws ServletException {
    try {
      nameList = new TreeSet<String>();

      Enumeration<? extends ZipEntry> zes = tzDefsZipFile.entries();

      summaries = new ArrayList<SummaryType>();

      while (zes.hasMoreElements()) {
        ZipEntry ze = zes.nextElement();

        if (ze.isDirectory()) {
          continue;
        }

        String n = ze.getName();

        if (!(n.startsWith("zoneinfo/") && n.endsWith(".ics"))) {
          continue;
        }

        String id = n.substring(9, n.length() - 4);
        nameList.add(id);

        String tzdef = entryToString(ze);

        CalendarBuilder cb = new CalendarBuilder();

        UnfoldingReader ufrdr = new UnfoldingReader(new StringReader(tzdef), true);

        net.fortuna.ical4j.model.Calendar cal = cb.build(ufrdr);
        VTimeZone vtz = (VTimeZone)cal.getComponents().getComponent(Component.VTIMEZONE);
        if (vtz == null) {
          throw new Exception("Incorrectly stored timezone");
        }

        tzs.put(id, new TimeZone(vtz));
        vtzs.put(id, vtz.toString());

        /* ================== Build summary info ======================== */
        SummaryType st = new SummaryType();

        st.setTzid(id);

        LastModified lm = vtz.getLastModified();
        if (lm!= null) {
          st.setLastModified(XcalUtil.getXMlUTCCal(lm.getValue()));
        } else {
          st.setLastModified(XcalUtil.getXMlUTCCal(buildTime));
        }

        List<String> aliases = findAliases(id);

        // XXX Need to have list of local names per timezone
        //String ln = vtz.
        if (aliases != null) {
          for (String a: aliases) {
            AliasType at = new AliasType();

            // XXX Need locale as well as name
            at.setValue(a);
            st.getAlias().add(at);

            /* Construct a new vtimezone with the alias as id and then
             * add it and the string version to the alias table.
             */
            VTimeZone avtz = (VTimeZone)vtz.copy();

            TzId tzid = avtz.getTimeZoneId();
            tzid.setValue(a);

            aliasedTzs.put(a, new TimeZone(avtz));
            aliasedVtzs.put(a, avtz.toString());
          }
        }

        summaries.add(st);
      }
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  /** Retrieve the data and store in a temp file. Return the file object.
   *
   * @return File
   * @throws ServletException
   */
  private File getdata() throws ServletException {
    try {
      String dataUrl = tzdataUrl;
      if (dataUrl == null) {
        throw new ServletException("No data url defined");
      }

      if (dataUrl.startsWith("http:")) {
        /* Fetch the data */
        HttpClient client = new HttpClient();

        HttpMethod get = new GetMethod(dataUrl);

        client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                                        new DefaultHttpMethodRetryHandler());

        client.executeMethod(get);

        InputStream is = get.getResponseBodyAsStream();

        File f = File.createTempFile("bwtzserver", "zip");

        FileOutputStream fos = new FileOutputStream(f);

        byte[] buff = new byte[4096];

        for (;;) {
          int num = is.read(buff);

          if (num < 0) {
            break;
          }

          if (num > 0) {
            fos.write(buff, 0, num);
          }
        }

        fos.close();
        is.close();

        get.releaseConnection();

        return f;
      }

      return new File(dataUrl);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private String entryToString(final ZipEntry ze) throws Throwable {
    InputStreamReader is = new InputStreamReader(tzDefsZipFile.getInputStream(ze),
                                                 "UTF-8");

    StringWriter sw = new StringWriter();

    char[] buff = new char[4096];

    for (;;) {
      int num = is.read(buff);

      if (num < 0) {
        break;
      }

      if (num > 0) {
        sw.write(buff, 0, num);
      }
    }

    is.close();

    return sw.toString();
  }
}
