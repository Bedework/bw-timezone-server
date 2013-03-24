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
import org.bedework.timezones.common.TzConfigHolder;
import org.bedework.timezones.common.TzServerUtil;

import edu.rpi.cmt.config.ConfigurationFileStore;
import edu.rpi.cmt.config.ConfigurationStore;
import edu.rpi.cmt.config.ConfigurationType;
import edu.rpi.cmt.jmx.ConfBase;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

/**
 * @author douglm
 *
 */
public class TzConf extends ConfBase implements TzConfMBean, TzConfigHolder {
  private static Set<ObjectName> registeredMBeans = new CopyOnWriteArraySet<ObjectName>();

  private String serviceName = "org.bedework.timezones:service=Server";

  // private Configuration config;

  private static TzConfig cfg;

  /*
  private class SchemaThread extends Thread {
    InfoLines infoLines = new InfoLines();

    SchemaThread() {
      super("BuildSchema");
    }

    @Override
    public void run() {
      infoLines.addLn("Started drop of tables");

      String res = schema(true);

      if (res != null) {
        infoLines.addLn("Failed: message was:");
        infoLines.addLn(res);
        return;
      }

      infoLines.addLn("Started export of schema");

      res = schema(false);

      if (res != null) {
        infoLines.addLn("Failed: message was:");
        infoLines.addLn(res);
        return;
      }

      infoLines.addLn("Action complete: check logs");
    }

    private String schema(final boolean drop) {
      String result = null;

      try {
        SchemaExport se = new SchemaExport(getConfiguration());

        se.setDelimiter(";");

        se.setHaltOnError(false);

        se.execute(false, // script - causes write to System.out if true
                   true,  // export
                   drop, // justDrop
                   !drop); // justCreate
      } catch (Throwable t) {
        error(t);
        result = "Exception: " + t.getLocalizedMessage();
      }

      return result;
    }
  }

  private SchemaThread buildSchema = new SchemaThread();
  */

  /**
   * @param configDir
   */
  public TzConf(final String configDir) {
    setConfigDir(configDir);

    TzServerUtil.setTzConfigHolder(this);
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

  @Override
  protected Set<ObjectName> getRegisteredMBeans() {
    return registeredMBeans;
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
    geTzConfig().setTzdataUrl(val);
  }

  @Override
  public String getTzdataUrl() {
    return geTzConfig().getTzdataUrl();
  }

  @Override
  public void setLeveldbPath(final String val) {
    geTzConfig().setLeveldbPath(val);
  }

  @Override
  public String getLeveldbPath() {
    return geTzConfig().getLeveldbPath();
  }

  @Override
  public void setPrimaryUrl(final String val) {
    geTzConfig().setPrimaryUrl(val);
  }

  @Override
  public String getPrimaryUrl() {
      return geTzConfig().getPrimaryUrl();
  }

  @Override
  public void setPrimaryServer(final boolean val) {
    geTzConfig().setPrimaryServer(val);
  }

  @Override
  public boolean getPrimaryServer() {
    return geTzConfig().getPrimaryServer();
  }

  @Override
  public void setRefreshInterval(final long val) {
    geTzConfig().setRefreshDelay(val);
  }

  @Override
  public long getRefreshInterval() {
    return geTzConfig().getRefreshDelay();
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
      geTzConfig().setDtstamp(null);
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

  /*
  @Override
  public String recreateDb() {
    try {
      buildSchema.start();

      return "OK";
    } catch (Throwable t) {
      error(t);

      return "Exception: " + t.getLocalizedMessage();
    }
  }

  @Override
  public synchronized List<String> recreateStatus() {
    if (buildSchema == null) {
      InfoLines infoLines = new InfoLines();

      infoLines.addLn("Schema build has not been started");

      return infoLines;
    }

    return buildSchema.infoLines;
  }

  @Override
  public String listHibernateProperties() {
    StringBuilder res = new StringBuilder();

    @SuppressWarnings("unchecked")
    List<String> ps = geTzConfig().getHibernateProperties();

    for (String p: ps) {
      res.append(p);
      res.append("\n");
    }

    return res.toString();
  }

  @Override
  public String displayHibernateProperty(final String name) {
    String val = geTzConfig().getHibernateProperty(name);

    if (val != null) {
      return val;
    }

    return "Not found";
  }

  @Override
  public void removeHibernateProperty(final String name) {
    geTzConfig().removeHibernateProperty(name);
  }

  @Override
  public void addHibernateProperty(final String name,
                                   final String value) {
    geTzConfig().addHibernateProperty(name, value);
  }

  @Override
  public void setHibernateProperty(final String name,
                                   final String value) {
    geTzConfig().setHibernateProperty(name, value);
  }
  */

  @Override
  public String loadConfig() {
    try {
      /* Load up the config */

      ConfigurationStore cfs = new ConfigurationFileStore(getConfigDir());

      Collection<String> configNames = cfs.getConfigs();

      if (configNames.isEmpty()) {
        return "No configuration";
      }

      if (configNames.size() != 1) {
        return "1 and only 1 configuration allowed";
      }

      String configName = configNames.iterator().next();

      cfg = getConfigInfo(cfs, configName);

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
  public TzConfig geTzConfig() {
    return cfg;
  }

  /** Save the configuration.
   *
   */
  @Override
  public void saveTzConfig() {
    saveConfig();
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  /**
   * @return current state of config
   */
  private synchronized TzConfig getConfigInfo(final ConfigurationStore cfs,
                                              final String configName) {
    try {
      /* Try to load it */

      ConfigurationType config = cfs.getConfig(configName);

      if (config == null) {
        return null;
      }

      TzConfig cfg =
          (TzConfig)makeObject(TzConfig.class.getCanonicalName());

      cfg.setConfig(config);

      return cfg;
    } catch (Throwable t) {
      error(t);
      return null;
    }
  }

  /*
  private synchronized Configuration getConfiguration() {
    if (config == null) {
      config = new Configuration().configure();
    }

    return config;
  }
  */

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
