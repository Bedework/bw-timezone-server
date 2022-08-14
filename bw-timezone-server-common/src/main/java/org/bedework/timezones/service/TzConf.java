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
package org.bedework.timezones.service;

import org.bedework.timezones.common.Stat;
import org.bedework.timezones.common.TzConfigImpl;
import org.bedework.timezones.common.TzServerUtil;
import org.bedework.util.jmx.ConfBase;
import org.bedework.util.jmx.ConfigHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author douglm
 *
 */
public class TzConf extends ConfBase<TzConfigImpl> implements TzConfMBean, ConfigHolder<TzConfigImpl> {
  /* Name of the directory holding the config data */
  private static final String confDirName = "tzs";

  /**
   */
  public TzConf() {
    super("org.bedework.timezones:service=Server",
          confDirName,
          "config");

    TzServerUtil.setTzConfigHolder(this);
  }

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  @Override
  public void setDtstamp(final String val) {
    getConfig().setDtstamp(val);
  }

  @Override
  public String getDtstamp() {
    return getConfig().getDtstamp();
  }

  @Override
  public void setVersion(final String val) {
    getConfig().setVersion(val);
  }

  @Override
  public String getVersion() {
    return getConfig().getVersion();
  }

  @Override
  public void setTzdataUrl(final String val) {
    getConfig().setTzdataUrl(val);
  }

  @Override
  public String getTzdataUrl() {
    return getConfig().getTzdataUrl();
  }

  @Override
  public void setDbPath(final String val) {
    getConfig().setDbPath(val);
  }

  @Override
  public String getDbPath() {
    return getConfig().getDbPath();
  }

  @Override
  public void setPrimaryUrl(final String val) {
    getConfig().setPrimaryUrl(val);
  }

  @Override
  public String getPrimaryUrl() {
    return getConfig().getPrimaryUrl();
  }

  @Override
  public void setPrimaryServer(final boolean val) {
    getConfig().setPrimaryServer(val);
  }

  @Override
  public boolean getPrimaryServer() {
    return getConfig().getPrimaryServer();
  }

  @Override
  public void setSource(final String val) {
    getConfig().setSource(val);
  }

  @Override
  public String getSource() {
    return getConfig().getSource();
  }

  @Override
  public void setRefreshDelay(final long val) {
    getConfig().setRefreshDelay(val);
  }

  @Override
  public long getRefreshDelay() {
    return getConfig().getRefreshDelay();
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  @Override
  public List<Stat> getStats() {
    try {
      return TzServerUtil.getStats();
    } catch (final Throwable t) {
      error("Error getting stats");
      error(t);
      return null;
    }
  }

  @Override
  public String refreshData() {
    try {
      getConfig().setDtstamp(null);
      saveConfig();
      TzServerUtil.fireRefresh(true);
      return "Ok";
    } catch (final Throwable t) {
      error(t);
      return "Refresh error: " + t.getLocalizedMessage();
    }
  }

  @Override
  public String checkData() {
    try {
      TzServerUtil.fireCheck();
      return "Ok";
    } catch (final Throwable t) {
      error(t);
      return "Update error: " + t.getLocalizedMessage();
    }
  }

  @Override
  public String compareData(final String tzdataUrl) {
    final StringWriter sw = new StringWriter();

    try {
      final PrintWriter pw = new PrintWriter(sw);

      final List<String> chgs = TzServerUtil.compareData(tzdataUrl);

      for (final String s: chgs) {
        pw.println(s);
      }

    } catch (final Throwable t) {
      t.printStackTrace(new PrintWriter(sw));
    }

    return sw.toString();
  }

  @Override
  public String updateData(final String tzdataUrl) {
    final StringWriter sw = new StringWriter();

    try {
      final PrintWriter pw = new PrintWriter(sw);

      final List<String> chgs = TzServerUtil.updateData(tzdataUrl);

      for (final String s: chgs) {
        pw.println(s);
      }

    } catch (final Throwable t) {
      t.printStackTrace(new PrintWriter(sw));
    }

    return sw.toString();
  }

  @Override
  public String loadConfig() {
    return loadConfig(TzConfigImpl.class);
  }

  /** Save the configuration.
   *
   */
  @Override
  public void putConfig() {
    saveConfig();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */
}
