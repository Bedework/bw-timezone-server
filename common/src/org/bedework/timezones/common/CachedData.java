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

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.LastModified;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import ietf.params.xml.ns.timezone_service.AliasType;
import ietf.params.xml.ns.timezone_service.SummaryType;
import ietf.params.xml.ns.timezone_service.Timezones;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
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

/** Cached data affected by the source data.
 *
 * @author douglm
 */
public class CachedData implements Serializable {
  private volatile boolean refreshNow = true;

  private String tzdataUrl;

  private ZipFile tzDefsZipFile;

  private File tzDefsFile;

  private Collection<String> dataInfo;

  private Map<String, String> vtzs = new HashMap<String, String>();

  private Map<String, TimeZone> tzs = new HashMap<String, TimeZone>();

  private SortedSet<String> nameList;

  private Map<ExpandedMapEntryKey, Timezones> expansions =
    new HashMap<ExpandedMapEntryKey, Timezones>();

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
  public CachedData(String tzdataUrl) {
    this.tzdataUrl = tzdataUrl;
  }

  /**
   *
   * @param val
   */
  public void setTzdataUrl(final String val) {
    tzdataUrl = val;
  }

  /**
   * @return tzdataUrl
   */
  public String getTzdataUrl() {
    return tzdataUrl;
  }

  /**
   *
   */
  public void refresh() {
    refreshNow = true;
  }

  /**
   * @return data info
   * @throws ServletException
   */
  public Collection<String> getDataInfo() throws ServletException {
    reload();
    return dataInfo;
  }

  /** Given an alias return the tzid for that alias
   *
   * @param val
   * @return aliased name or null
   * @throws ServletException
   */
  public String fromAlias(String val) throws ServletException {
    reload();
    return aliasMaps.byAlias.get(val);
  }

  /**
   * @return String value of aliases file.
   * @throws ServletException
   */
  public String getAliasesStr() throws ServletException {
    reload();
    return aliasMaps.aliasesStr;
  }

  /**
   * @param tzid
   * @return list of aliases or null
   * @throws ServletException
   */
  public List<String> findAliases(String tzid) throws ServletException {
    reload();
    return aliasMaps.byTzid.get(tzid);
  }

  /**
   * @return namelist or null
   * @throws ServletException
   */
  public SortedSet<String> getNameList() throws ServletException {
    reload();
    return nameList;
  }

  /** Get cached VTIMEZONE specifications
   *
   * @param name
   * @return cached spec or null.
   * @throws ServletException
   */
  public String getCachedVtz(final String name) throws ServletException {
    reload();
    return vtzs.get(name);
  }

  /**
   * @param key
   * @param tzs
   * @throws ServletException
   */
  public void setExpanded(ExpandedMapEntryKey key,
                          Timezones tzs) throws ServletException {
    reload();
    expansions.put(key, tzs);
  }

  /**
   * @param key
   * @return expanded or null
   * @throws ServletException
   */
  public Timezones getExpanded(ExpandedMapEntryKey key) throws ServletException {
    reload();
    return expansions.get(key);
  }

  /** Get a timezone object from the server given the id.
   *
   * @param tzid
   * @return TimeZone with id or null
   * @throws ServletException
   */
  public TimeZone getTimeZone(final String tzid) throws ServletException {
    reload();
    return tzs.get(tzid);
  }

  /**
   * @return list of summary info
   * @throws ServletException
   */
  public List<SummaryType> getSummaries() throws ServletException {
    reload();
    return summaries;
  }

  /**
   * @param aliasesStr
   * @throws ServletException
   */
  private synchronized void reload() throws ServletException {
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
      dataInfo = new ArrayList<String>();

      for (String s: infoLines) {
        dataInfo.add(s);
      }

      /* ===================== Rebuild the alias maps ======================= */

      aliasMaps = buildAliasMaps(tzDefsZipFile);

      /* ===================== All tzs into the table ======================= */

      unzipTzs(tzDefsZipFile);
      expansions.clear();

      TzServerUtil.reloadsMillis += System.currentTimeMillis() - smillis;
      TzServerUtil.reloads++;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private AliasMaps buildAliasMaps(ZipFile tzDefsZipFile) throws ServletException {
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

  private void unzipTzs(ZipFile tzDefsZipFile) throws ServletException {
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
        vtzs.put(id, tzdef);

        CalendarBuilder cb = new CalendarBuilder();

        UnfoldingReader ufrdr = new UnfoldingReader(new StringReader(tzdef), true);

        net.fortuna.ical4j.model.Calendar cal = cb.build(ufrdr);
        VTimeZone vtz = (VTimeZone)cal.getComponents().getComponent(Component.VTIMEZONE);
        if (vtz == null) {
          throw new Exception("Incorrectly stored timezone");
        }

        TimeZone tz = new TimeZone(vtz);
        tzs.put(id, tz);

        /* ================== Build summary info ======================== */
        SummaryType st = new SummaryType();

        st.setTzid(id);

        LastModified lm = vtz.getLastModified();
        if (lm!= null) {
          st.setLastModified(lm.getValue());
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

      /* Fetch the data */
      HttpClient client = new HttpClient();

      HttpMethod get = new GetMethod(dataUrl);

      client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                                      new DefaultHttpMethodRetryHandler());

      client.executeMethod(get);

      InputStream is = get.getResponseBodyAsStream();

      File f = File.createTempFile("bwtzserver", "zip");

      FileOutputStream fos = new  FileOutputStream(f);

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