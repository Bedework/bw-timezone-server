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
import org.bedework.timezones.common.TzConfig;
import org.bedework.timezones.common.TzServerUtil;

import edu.rpi.cmt.config.ConfigurationStore;
import edu.rpi.cmt.config.ConfigurationType;
import edu.rpi.cmt.jmx.ConfBase;
import edu.rpi.cmt.jmx.ConfigHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

/**
 * @author douglm
 *
 */
public class TzConf extends ConfBase<TzConfig> implements TzConfMBean, ConfigHolder<TzConfig> {
  private static TzConfig cfg;

  /* Name of the property holding the location of the config data */
  private static final String datauriPname = "org.bedework.tzs.datauri";

  /**
   */
  public TzConf() {
    super("org.bedework.timezones:service=Server");

    setConfigPname(datauriPname);

    TzServerUtil.setTzConfigHolder(this);
  }

  @Override
  public ConfigurationType getConfigObject() {
    return getCfg().getConfig();
  }

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /** Tzdata url
   *
   * @param val
   */
  @Override
  public void setTzdataUrl(final String val) {
    getConfig().setTzdataUrl(val);
  }

  @Override
  public String getTzdataUrl() {
    return getConfig().getTzdataUrl();
  }

  @Override
  public void setLeveldbPath(final String val) {
    getConfig().setLeveldbPath(val);
  }

  @Override
  public String getLeveldbPath() {
    return getConfig().getLeveldbPath();
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
  public void setRefreshInterval(final long val) {
    getConfig().setRefreshDelay(val);
  }

  @Override
  public long getRefreshInterval() {
    return getConfig().getRefreshDelay();
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  @Override
  public List<Stat> getStats() {
    try {
      return TzServerUtil.getStats();
    } catch (Throwable t) {
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
      TzServerUtil.fireRefresh();
      return "Ok";
    } catch (Throwable t) {
      error(t);
      return "Refresh error: " + t.getLocalizedMessage();
    }
  }

  @Override
  public String checkData() {
    try {
      TzServerUtil.fireCheck();
      return "Ok";
    } catch (Throwable t) {
      error(t);
      return "Update error: " + t.getLocalizedMessage();
    }
  }

  @Override
  public String compareData(final String tzdataUrl) {
    StringWriter sw = new StringWriter();

    try {
      PrintWriter pw = new PrintWriter(sw);

      List<String> chgs = TzServerUtil.compareData(tzdataUrl);

      for (String s: chgs) {
        pw.println(s);
      }

    } catch (Throwable t) {
      t.printStackTrace(new PrintWriter(sw));
    }

    return sw.toString();
  }

  @Override
  public String updateData(final String tzdataUrl) {
    StringWriter sw = new StringWriter();

    try {
      PrintWriter pw = new PrintWriter(sw);

      List<String> chgs = TzServerUtil.updateData(tzdataUrl);

      for (String s: chgs) {
        pw.println(s);
      }

    } catch (Throwable t) {
      t.printStackTrace(new PrintWriter(sw));
    }

    return sw.toString();
  }

  @Override
  public String loadConfig() {
    try {
      /* Load up the config */

      ConfigurationStore cs = getStore();

      Collection<String> configNames = cs.getConfigs();

      if (configNames.isEmpty()) {
        return "No configuration";
      }

      if (configNames.size() != 1) {
        return "1 and only 1 configuration allowed";
      }

      String configName = configNames.iterator().next();

      cfg = getConfigInfo(configName, TzConfig.class);

      if (cfg == null) {
        return "Unable to read configuration";
      }

      setConfigName(configName);

      saveConfig(); // Just to ensure we have it for next time

      return "OK";
    } catch (Throwable t) {
      error("Failed to start management context");
      error(t);
      return "failed";
    }
  }

  /**
   * @return the current state of the configuration.
   */
  @Override
  public TzConfig getConfig() {
    return cfg;
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

  /* ====================================================================
   *                   Non-mbean methods
   * ==================================================================== */

  /**
   * @return current state of config
   */
  public TzConfig getCfg() {
    return cfg;
  }

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */
}
