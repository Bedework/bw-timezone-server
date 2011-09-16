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

import java.util.ArrayList;
import java.util.List;

/**
 * @author douglm
 *
 */
public class Tzsvc implements TzsvcMBean {
  private transient Logger log;

  private boolean running;

  private Configuration cfg;

  @SuppressWarnings("unused")
  private TzServerUtil tzutil;

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#getName()
   */
  @Override
  public String getName() {
    /* This apparently must be the same as the name attribute in the
     * jboss service definition
     */
    return "org.bedework:service=Tzsvr";
  }

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
    TzServerUtil.setPrimaryUrl(val);
  }

  @Override
  public String getPrimaryUrl() {
    return TzServerUtil.getPrimaryUrl();
  }

  @Override
  public void setPrimaryServer(final boolean val) {
    TzServerUtil.setPrimaryServer(val);
  }

  @Override
  public boolean getPrimaryServer() {
    return TzServerUtil.getPrimaryServer();
  }

  @Override
  public void setRefreshInterval(final long val) {
    TzServerUtil.setRefreshInterval(val);
  }

  @Override
  public long getRefreshInterval() {
    return TzServerUtil.getRefreshInterval();
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
  public String updateData() {
    try {
      TzServerUtil.fireUpdate();
      return "Ok";
    } catch (Throwable t) {
      error(t);
      return "Update error: " + t.getLocalizedMessage();
    }
  }

  /* an example say's we need this  - we should probably implement some system
   * independent jmx support which will build this using introspection and/or lists
  public MBeanInfo getMBeanInfo() throws Exception {
    InitialContext ic = new InitialContext();
    RMIAdaptor server = (RMIAdaptor) ic.lookup("jmx/rmi/RMIAdaptor");

    ObjectName name = new ObjectName(MBEAN_OBJ_NAME);

    // Get the MBeanInfo for this MBean
    MBeanInfo info = server.getMBeanInfo(name);
    return info;
  }
  */

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
  public List<String> compareData() {
    try {
      return TzServerUtil.compareData();
    } catch (Throwable t) {
      error(t);
      List <String> out = new ArrayList<String>();
      out.add(t.getLocalizedMessage());
      return out;
    }
  }

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#create()
   */
  @Override
  public void create() {
    // An opportunity to initialise
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#start()
   */
  @Override
  public void start() {
    try {
      tzutil = TzServerUtil.getInstance();
      running = true;
    } catch (Throwable t) {
      error("Error setting url");
      error(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#stop()
   */
  @Override
  public void stop() {
    running = false;
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#isStarted()
   */
  @Override
  public boolean isStarted() {
    return running;
  }

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#destroy()
   */
  @Override
  public void destroy() {
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
