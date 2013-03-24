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

import edu.rpi.cmt.config.ConfigBase;
import edu.rpi.sss.util.ToString;
import edu.rpi.sss.util.Util;

import java.util.List;

import javax.xml.namespace.QName;

/** This class defines the various properties we need for a carddav server
 *
 * @author Mike Douglass
 * @param <T>
 */
public class TzConfig<T extends TzConfig> extends ConfigBase<T> {
  /** */
  public final static QName confElement = new QName(ns, "bwtz-confinfo");

  private static final QName dtstampProperty = new QName(ns, "dtstamp");

  private static final QName versionProperty = new QName(ns, "version");

  private static final QName primaryUrlProperty = new QName(ns, "primaryUrl");

  private static final QName primaryServerProperty = new QName(ns, "primaryServer");

  private static final QName tzdataUrlProperty = new QName(ns, "tzdataUrl");

  private static final QName leveldbPathProperty = new QName(ns, "leveldbPath");

  private static final QName refreshDelayProperty = new QName(ns, "refreshDelay");

  private static final QName hibernateProperty = new QName(ns, "hibernateProperty");

  @Override
  public QName getConfElement() {
    return confElement;
  }

  /**
   * @param val
   */
  public void setDtstamp(final String val) {
    setProperty(dtstampProperty, val);
  }

  /**
   * @return String XML format dtstamp
   */
  public String getDtstamp() {
    return getPropertyValue(dtstampProperty);
  }

  /**
   * @param val
   */
  public void setVersion(final String val) {
    setProperty(versionProperty, val);
  }

  /**
   * @return String version
   */
  public String getVersion() {
    return getPropertyValue(versionProperty);
  }

  /** Url for the primary server.
   *
   * @param val    String
   */
  public void setPrimaryUrl(final String val) {
    setProperty(primaryUrlProperty, val);
  }

  /** Url for the primary server.
   *
   * @return String, null for unset
   */
  public String getPrimaryUrl() {
    return getPropertyValue(primaryUrlProperty);
  }

  /** Are we a primary server?
   *
   * @param val    Boolean
   */
  public void setPrimaryServer(final Boolean val) {
    setBooleanProperty(primaryServerProperty, val);
  }

  /** Are we a primary server?
   *
   * @return Boolean, null for unset
   */
  public Boolean getPrimaryServer() {
    return getBooleanPropertyValue(primaryServerProperty);
  }

  /** Location of the (backup) zip file.
   *
   * @param val    String
   */
  public void setTzdataUrl(final String val) {
    setProperty(tzdataUrlProperty, val);
  }

  /** Location of the (backup) zip file.
   *
   * @return String, null for unset
   */
  public String getTzdataUrl() {
    return getPropertyValue(tzdataUrlProperty);
  }

  /** Location of the leveldb data.
   *
   * @param val    String
   */
  public void setLeveldbPath(final String val) {
    setProperty(leveldbPathProperty, val);
  }

  /** Location of the leveldb data.
   *
   * @return String, null for unset
   */
  public String getLeveldbPath() {
    return getPropertyValue(leveldbPathProperty);
  }

  /** Refresh delay - seconds
   *
   * @param val
   */
  public void setRefreshDelay(final Long val) {
    setLongProperty(refreshDelayProperty, val);
  }

  /** Refresh delay - seconds
   *
   * @return Long refreshDelay - null for unset
   */
  public Long getRefreshDelay() {
    return getLongPropertyValue(refreshDelayProperty);
  }

  /** Add a hibernate property
   *
   * @param name
   * @param val
   */
  public void addHibernateProperty(final String name,
                                   final String val) {
    addProperty(hibernateProperty, name + "=" + val);
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

      getConfig().removeProperty(hibernateProperty, name + "=" + v);
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

  /**
   *
   * @return String val
   */
  public List<String> getHibernateProperties() {
    try {
      return getConfig().getAll(hibernateProperty);
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

    ts.append("appName", getAppName());
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
