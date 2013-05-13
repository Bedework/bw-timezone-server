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

import edu.rpi.cmt.config.ConfInfo;
import edu.rpi.cmt.config.ConfigBase;
import edu.rpi.sss.util.ToString;
import edu.rpi.sss.util.Util;

import java.util.ArrayList;
import java.util.List;

/** This class defines the various properties we need for a carddav server
 *
 * @author Mike Douglass
 * @param <T>
 */
@ConfInfo(elementName = "bwtz-confinfo")
public class TzConfig<T extends TzConfig> extends ConfigBase<T> {
  private String dtstamp;

  private String version;

  private String primaryUrl;

  private boolean primaryServer;

  private String tzdataUrl;

  private String leveldbPath;

  private long refreshDelay;

  private List<String> hibernateProperties;

  /**
   * @param val
   */
  public void setDtstamp(final String val) {
    dtstamp = val;
  }

  /**
   * @return String XML format dtstamp
   */
  public String getDtstamp() {
    return dtstamp;
  }

  /**
   * @param val
   */
  public void setVersion(final String val) {
    version = val;
  }

  /**
   * @return String version
   */
  public String getVersion() {
    return version;
  }

  /** Url for the primary server.
   *
   * @param val    String
   */
  public void setPrimaryUrl(final String val) {
    primaryUrl = val;
  }

  /** Url for the primary server.
   *
   * @return String, null for unset
   */
  public String getPrimaryUrl() {
    return primaryUrl;
  }

  /** Are we a primary server?
   *
   * @param val    boolean
   */
  public void setPrimaryServer(final boolean val) {
    primaryServer = val;
  }

  /** Are we a primary server?
   *
   * @return boolean
   */
  public boolean getPrimaryServer() {
    return primaryServer;
  }

  /** Location of the (backup) zip file.
   *
   * @param val    String
   */
  public void setTzdataUrl(final String val) {
    tzdataUrl = val;
  }

  /** Location of the (backup) zip file.
   *
   * @return String, null for unset
   */
  public String getTzdataUrl() {
    return tzdataUrl;
  }

  /** Location of the leveldb data.
   *
   * @param val    String
   */
  public void setLeveldbPath(final String val) {
    leveldbPath = val;
  }

  /** Location of the leveldb data.
   *
   * @return String, null for unset
   */
  public String getLeveldbPath() {
    return leveldbPath;
  }

  /** Refresh delay - seconds
   *
   * @param val
   */
  public void setRefreshDelay(final Long val) {
    refreshDelay = val;
  }

  /** Refresh delay - seconds
   *
   * @return long refreshDelay
   */
  public long getRefreshDelay() {
    return refreshDelay;
  }

  /**
   *
   * @param val
   */
  public void setHibernateProperties(final List<String> val) {
    hibernateProperties = val;
  }

  /**
   *
   * @return String val
   */
  @ConfInfo(collectionElementName = "hibernateProperty")
  public List<String> getHibernateProperties() {
    return hibernateProperties;
  }

  /** Add a hibernate property
   *
   * @param name
   * @param val
   */
  public void addHibernateProperty(final String name,
                                   final String val) {
    List<String> p = getHibernateProperties();
    if (p == null) {
      p = new ArrayList<String>();
      setHibernateProperties(p);
    }
    p.add(name + "=" + val);
  }

  /** Get a hibernate property
   *
   * @param val
   * @return value or null
   */
  public String getHibernateProperty(final String val) {
    List<String> ps = getHibernateProperties();

    String key = val + "=";
    for (String p: ps) {
      if (p.startsWith(key)) {
        return p.substring(key.length());
      }
    }

    return null;
  }

  /** Remove a hibernate property
   *
   * @param name
   */
  public void removeHibernateProperty(final String name) {
    try {
      String v = getHibernateProperty(name);

      if (v == null) {
        return;
      }

      getHibernateProperties().remove(name + "=" + v);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /** Set a hibernate property
   *
   * @param name
   * @param val
   */
  public void setHibernateProperty(final String name,
                                   final String val) {
    try {
      removeHibernateProperty(name);
      addHibernateProperty(name, val);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  /** Add our stuff to the StringBuilder
   *
   * @param ts    ToString for result
   */
  @Override
  public void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);
  }

  /** init copy of the config
   *
   * @param newConf
   */
  public void copyTo(final TzConfig newConf) {
    newConf.setDtstamp(getDtstamp());
    newConf.setVersion(getVersion());
    newConf.setPrimaryUrl(getPrimaryUrl());
    newConf.setPrimaryServer(getPrimaryServer());
    newConf.setTzdataUrl(getTzdataUrl());
    newConf.setRefreshDelay(getRefreshDelay());

    if (!Util.isEmpty(getHibernateProperties())) {
      for (String hp: getHibernateProperties()) {
        String[] hpels = hp.split("=");
        newConf.addHibernateProperty(hpels[0], hpels[1]);
      }
    }
  }
}
