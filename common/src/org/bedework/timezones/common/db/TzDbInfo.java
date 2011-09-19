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
package org.bedework.timezones.common.db;

import org.bedework.timezones.common.TzException;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

/** Type for db info.
 *
 * @author Mike Douglass
 * @version 1.0
 */
public class TzDbInfo extends TzDbentity<TzDbInfo> {
  private String dtstamp;

  private String version = "1.0";

  private String serverProperties;

  /* Loaded from the serialized form */
  private Properties properties;

  private boolean changed;

  /** Remote primary server url
   */
  public static final String propnamePrimaryUrl = "primaryUrl";

  /** Are we a primary server?
   */
  public static final String propnamePrimaryServer = "primaryServer";

  /** Refresh period for polling primary (seconds) */
  public static final String propnameRefreshDelay = "refreshDelay";

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

  /**
   * @param val serialized properties
   */
  public void setServerProperties(final String val) {
    serverProperties = val;
  }

  /**
   * @return serialized properties
   * @throws Throwable
   */
  public String getServerProperties() throws Throwable {
    if (changed) {
      Writer wtr = new StringWriter();

      properties.store(wtr, null);
      serverProperties = wtr.toString();
    }

    return serverProperties;
  }

  /** Set the changed flag
   *
   * @param val
   */
  public void setChanged(final boolean val) {
    changed = val;
  }

  /**
   * @return changed flag.
   */
  public boolean getChanged() {
    return changed;
  }

  /* ====================================================================
   *                   properties methods
   * ==================================================================== */

  /** Url for the primary server.
   *
   * @param val    String
   * @throws TzException
   */
  public void setPrimaryUrl(final String val) throws TzException {
    setProperty(propnamePrimaryUrl, val);
  }

  /** Url for the primary server.
   *
   * @return String, null for unset
   * @throws TzException
   */
  public String getPrimaryUrl() throws TzException {
    return getProperty(propnamePrimaryUrl);
  }

  /** Are we a primary server?
   *
   * @param val    Boolean
   * @throws TzException
   */
  public void setPrimaryServer(final Boolean val) throws TzException {
    setProperty(propnamePrimaryServer, val.toString());
  }

  /** Are we a primary server?
   *
   * @return Boolean, null for unset
   * @throws TzException
   */
  public Boolean getPrimaryServer() throws TzException {
    String s = getProperty(propnamePrimaryServer);

    if (s == null) {
      return null;
    }

    return Boolean.valueOf(s);
  }

  /** Refresh delay - seconds
   *
   * @param val
   * @throws TzException
   */
  public void setRefreshDelay(final Long val) throws TzException {
    setProperty(propnameRefreshDelay, val.toString());
  }

  /** Refresh delay - seconds
   *
   * @return Long refreshDelay - null for unset
   * @throws TzException
   */
  public Long getRefreshDelay() throws TzException {
    String s = getProperty(propnameRefreshDelay);

    if (s == null) {
      return null;
    }

    return Long.valueOf(s);
  }

  /* ====================================================================
   *                   properties support methods
   * ==================================================================== */

  /** Load the properties from the serialized form.
   *
   * @throws TzException
   */
  public synchronized void loadProperties() throws TzException {
    try {
      if (properties == null) {
        properties = new Properties();
      }

      if (getServerProperties() != null) {
        properties.load(new StringReader(getServerProperties()));
      }
    } catch (Throwable t) {
      throw new TzException(t);
    }
  }

  /** Set a property in the internal properties - loading them from the
   * external value first if necessary.
   *
   * @param name
   * @param val
   * @throws TzException
   */
  public void setProperty(final String name,
                          final String val) throws TzException {
    try {
      if (properties == null) {
        loadProperties();
      }

      if (val == null) {
        properties.remove(name);
      } else {
        properties.setProperty(name, val);
      }
      changed = true;
    } catch (Throwable t) {
      throw new TzException(t);
    }
  }

  /** Get a property from the internal properties - loading them from the
   * external value first if necessary.
   *
   * @param name
   * @return val
   * @throws TzException
   */
  public synchronized String getProperty(final String name) throws TzException {
    if (properties == null) {
      loadProperties();
    }

    return properties.getProperty(name);
  }

  /* ====================================================================
   *                   Convenience methods
   * ==================================================================== */

  /** Add our stuff to the StringBuilder
   *
   * @param sb    StringBuilder for result
   */
  @Override
  protected void toStringSegment(final StringBuilder sb) {
    super.toStringSegment(sb);

    sb.append("dtstamp=");
    sb.append(getDtstamp());

    sb.append("version=");
    sb.append(getVersion());
  }

  /* ====================================================================
   *                   Object methods
   * The following are required for a db object.
   * ==================================================================== */

  @Override
  public int compareTo(final TzDbInfo that) {
    return getDtstamp().compareTo(that.getDtstamp());
  }

  @Override
  public int hashCode() {
    return dtstamp.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("BwUser{");

    toStringSegment(sb);

    return sb.toString();
  }
}
