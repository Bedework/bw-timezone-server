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

import org.bedework.timezones.common.TzServerUtil;

import edu.rpi.cmt.config.ConfigurationType;
import edu.rpi.cmt.jmx.ConfBase;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

/** This bootstraps the annotated jmx bean.
 *
 * @author douglm
 *
 */
public class Tzsvc extends ConfBase implements TzsvcMBean {
  private static volatile String hostsConfigDir;

  private static Set<ObjectName> registeredMBeans = new CopyOnWriteArraySet<ObjectName>();

  private String serviceName = "org.bedework.timezones:service=TzSvr";

  private TzConf tzConf;

  private TzServerUtil tzutil;

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /**
   * @param val
   */
  @Override
  public void setHostsConfigDir(final String val) {
    hostsConfigDir = val;
  }

  /**
   * @return String path to configs
   */
  @Override
  public String getHostsConfigDir() {
    return hostsConfigDir;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

  @Override
  public String getName() {
    /* This apparently must be the same as the name attribute in the
     * jboss service definition
     */
    return getServiceName();
  }

  @Override
  protected Set<ObjectName> getRegisteredMBeans() {
    return registeredMBeans;
  }

  @Override
  public ConfigurationType getConfigObject() {
    return tzConf.getConfigObject();
  }

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  @Override
  public void create() {
    try {
      /* Register the carddav mbean and load the configs. */

      getManagementContext().start();

      tzConf = new TzConf(getHostsConfigDir());
      register("tzConf", "tzConf", tzConf);
      tzConf.loadConfig();
    } catch (Throwable t) {
      error("Failed to create service");
      error(t);
    }
  }

  @Override
  public synchronized void start() {
    try {
      tzutil = TzServerUtil.getInstance();
    } catch (Throwable t) {
      error("Error starting service");
      error(t);
    }
  }

  @Override
  public synchronized void stop() {
    try {
      tzutil.stop();
    } catch (Throwable t) {
      error("Error stopping service");
      error(t);
    } finally {
      tzutil = null;
    }
  }

  @Override
  public boolean isStarted() {
    return tzutil != null;
  }

  @Override
  public void destroy() {
    try {
      getManagementContext().stop();
    } catch (Throwable t) {
      error("Failed to stop management context");
      error(t);
    }
  }
}
