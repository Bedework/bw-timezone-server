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
import org.bedework.timezones.common.TzServerUtil;

import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.jboss.system.ServiceMBeanSupport;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @author douglm
 *
 */
public class Tzsvc extends ServiceMBeanSupport implements TzsvcMBean {
  private transient Logger log;

  private Configuration cfg;

  private TzServerUtil tzutil;

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  @Override
  public void setAppname(final String val) {
    TzServerUtil.setAppname(val);
  }

  @Override
  public String getAppname() {
    return TzServerUtil.getAppname();
  }

  /** Tzdata url
   *
   * @param val
   */
  @Override
  public void setTzdataUrl(final String val) {
    try {
      TzServerUtil.setTzdataUrl(val);
    } catch (Throwable t) {
      error("Error setting url");
      error(t);
    }
  }

  @Override
  public String getTzdataUrl() {
    return TzServerUtil.getTzdataUrl();
  }

  @Override
  public void setPrimaryUrl(final String val) {
    try {
      TzServerUtil.getInstance().setPrimaryUrl(val);
    } catch (Throwable t) {
      error("Error setting url");
      error(t);
    }
  }

  @Override
  public String getPrimaryUrl() {
    try {
      return TzServerUtil.getInstance().getPrimaryUrl();
    } catch (Throwable t) {
      error(t);
      return "Error getting url";
    }
  }

  @Override
  public void setPrimaryServer(final boolean val) {
    try {
      TzServerUtil.getInstance().setPrimaryServer(val);
    } catch (Throwable t) {
      error("Error setting url");
      error(t);
    }
  }

  @Override
  public boolean getPrimaryServer() {
    try {
      return TzServerUtil.getInstance().getPrimaryServer();
    } catch (Throwable t) {
      error("Error getting primary flag");
      error(t);

      return false;
    }
  }

  @Override
  public void setRefreshInterval(final long val) {
    try {
      TzServerUtil.getInstance().setRefreshInterval(val);
    } catch (Throwable t) {
      error("Error setting refresh");
      error(t);
    }
  }

  @Override
  public long getRefreshInterval() {
    try {
      return TzServerUtil.getInstance().getRefreshInterval();
    } catch (Throwable t) {
      error("Error getting refresh");
      error(t);
      return -99999;
    }
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
  public String recreateDb() {
    String res = schema(true);

    if (res != null) {
      return res;
    }

    res = schema(false);

    if (res != null) {
      return res;
    }

    return "Action complete: check logs";
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

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private String schema(final boolean drop) {
    String result = null;

    try {
      SchemaExport se = new SchemaExport(getConfiguration());

      se.setDelimiter(";");

      se.setHaltOnError(false);

      se.execute(false, // script - causes write to System.out if true
                 true,
                 drop, // justDrop
                 !drop); // justCreate
    } catch (Throwable t) {
      error(t);
      result = "Exception: " + t.getLocalizedMessage();
    }

    return result;
  }

  private synchronized Configuration getConfiguration() {
    if (cfg == null) {
      cfg = new Configuration().configure();
    }

    return cfg;
  }

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  @Override
  protected ObjectName getObjectName(final MBeanServer server,
                                     final ObjectName name)
      throws MalformedObjectNameException {
    if (name == null) {
      return OBJECT_NAME;
    }

    return name;
   }

  @Override
  public void startService() throws Exception {
    try {
      tzutil = TzServerUtil.getInstance();
    } catch (Throwable t) {
      error("Error starting service");
      error(t);
      throw new Exception(t);
    }
  }

  @Override
  public void stopService() throws Exception {
    try {
      tzutil.stop();
    } catch (Throwable t) {
      error("Error stopping service");
      error(t);
      throw new Exception(t);
    }
  }

  /* ====================================================================
   *                   Protected methods
   * ==================================================================== */

  protected void info(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  /* Get a logger for messages
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }
}
