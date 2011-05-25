/* **********************************************************************
    Copyright 2010 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.timezones.service;

import org.bedework.timezones.common.Stat;
import org.bedework.timezones.common.TzServerUtil;

import org.apache.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import java.util.List;

/**
 * @author douglm
 *
 */
public class Tzsvc implements TzsvcMBean {
  private transient Logger log;

  private boolean running;

  private TzServerUtil util;

  private Configuration cfg;

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#getName()
   */
  public String getName() {
    /* This apparently must be the same as the name attribute in the
     * jboss service definition
     */
    return "org.bedework:service=Tzsvr";
  }

  public void setAppname(final String val) {
    TzServerUtil.setAppname(val);
  }

  public String getAppname() {
    return TzServerUtil.getAppname();
  }

  /** Tzdata url
   *
   * @param val
   */
  public void setTzdataUrl(String val) {
    try {
      TzServerUtil.setTzdataUrl(val);
    } catch (Throwable t) {
      error("Error setting url");
      error(t);
    }
  }

  /**
   * @return String tzdata url
   */
  public String getTzdataUrl() {
    return TzServerUtil.getTzdataUrl();
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  public List<Stat> getStats() {
    try {
      return TzServerUtil.getStats();
    } catch (Throwable t) {
      error("Error getting stats");
      error(t);
      return null;
    }
  }

  public String refreshData() {
    try {
      util.fireRefresh();
      return "Ok";
    } catch (Throwable t) {
      error(t);
      return "Refresh error: " + t.getLocalizedMessage();
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

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#create()
   */
  public void create() {
    // An opportunity to initialise
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#start()
   */
  public void start() {
    try {
      util = TzServerUtil.getInstance();
      running = true;
    } catch (Throwable t) {
      error("Error getting instance");
      error(t);
    }
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#stop()
   */
  public void stop() {
    running = false;
  }

  /* (non-Javadoc)
   * @see org.bedework.indexer.BwIndexerMBean#isStarted()
   */
  public boolean isStarted() {
    return running;
  }

  /* (non-Javadoc)
   * @see org.bedework.dumprestore.BwDumpRestoreMBean#destroy()
   */
  public void destroy() {
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private String schema(boolean drop) {
    String result = null;

    try {
      SchemaExport se = new SchemaExport(getConfiguration());

      se.setDelimiter(";");

      se.setHaltOnError(false);

      se.execute(false, // script - causes write to System.out if true
                 true,
                 drop,
                 !drop);
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
