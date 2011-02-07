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

import java.util.List;

/** Run the timezones service
 *
 * @author douglm
 */
public interface TzsvcMBean {
  /* ========================================================================
   * Attributes
   * ======================================================================== */

  /** Name apparently must be the same as the name attribute in the
   * jboss service definition
   *
   * @return Name
   */
  public String getName();

  /** Application name - for config info
   *
   * @param val
   */
  public void setAppname(String val);

  /**
   * @return String application namee
   */
  public String getAppname();

  /** Tzdata url
   *
   * @param val
   */
  public void setTzdataUrl(String val);

  /**
   * @return String tzdata url
   */
  public String getTzdataUrl();

  /* ========================================================================
   * Operations
   * ======================================================================== */

  /** Get the current stats
   *
   * @return List of Stat
   */
  public List<Stat> getStats();

  /** Refresh the data from the source
   *
   * @return completion code.
   */
  public String refreshData();

  /* ========================================================================
   * Lifecycle
   * ======================================================================== */

  /** Lifecycle
   *
   */
  public void create();

  /** Lifecycle
   *
   */
  public void start();

  /** Lifecycle
   *
   */
  public void stop();

  /** Lifecycle
   *
   * @return true if started
   */
  public boolean isStarted();

  /** Lifecycle
   *
   */
  public void destroy();
}
