/* **********************************************************************
    Copyright 2007 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
package org.bedework.timezones.common;

import java.io.Serializable;

/** This class defines the various properties we need for an Exchange synch
 *
 * @author Mike Douglass
 */
public class TzsvrConfig implements Serializable {
  /* From bedework build */
  private String appType;

  private String tzdataUrl;

  private int refetchInterval;

  private String cacheName;

  private String postId;

  /**
   * @param val
   */
  public void setAppType(final String val) {
    appType = val;
  }

  /**
   * @return String
   */
  public String getAppType() {
    return appType;
  }

  /**
   * @param val    String
   */
  public void setTzdataUrl(final String val) {
    tzdataUrl = val;
  }

  /**
   * @return String
   */
  public String getTzdataUrl() {
    return tzdataUrl;
  }

  /**
   * @param val    int
   */
  public void setRefetchInterval(final int val) {
    refetchInterval = val;
  }

  /**
   * @return int
   */
  public int getRefetchInterval() {
    return refetchInterval;
  }

  /**
   * @param val    String
   */
  public void setCacheName(final String val) {
    cacheName = val;
  }

  /**
   * @return String
   */
  public String getCacheName() {
    return cacheName;
  }

  /**
   * @param val    String
   */
  public void setPostId(final String val) {
    postId = val;
  }

  /**
   * @return String
   */
  public String getPostId() {
    return postId;
  }
}
