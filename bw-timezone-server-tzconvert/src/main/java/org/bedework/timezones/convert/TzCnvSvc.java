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
package org.bedework.timezones.convert;

import org.bedework.util.jmx.ConfBase;
import org.bedework.util.jmx.InfoLines;
import org.bedework.util.misc.Util;

/**
 * @author douglm
 *
 */
public class TzCnvSvc extends ConfBase<TzConvertParams>
        implements TzCnvSvcMBean {
  /* Name of the directory holding the config data */
  private static final String confDirName = "tzs";

  private Processor proc;

  /**
   */
  public TzCnvSvc() {
    super("org.bedework.timezones:service=Convert",
          confDirName,
          "cnvconfig",
          "config");
  }

  /* ========================================================================
   * Attributes
   * ======================================================================== */

  @Override
  public void setTzServerUri(final String val) {
    getConfig().setTzServerUri(val);
  }

  @Override
  public String getTzServerUri() {
    return getConfig().getTzServerUri();
  }

  @Override
  public void setCompare(final boolean val) {
    getConfig().setCompare(val);
  }

  @Override
  public boolean getCompare() {
    return getConfig().getCompare();
  }

  @Override
  public void setEndYear(final int val) {
    getConfig().setEndYear(val);
  }

  @Override
  public int getEndYear() {
    return getConfig().getEndYear();
  }

  @Override
  public void setStartYear(final int val) {
    getConfig().setStartYear(val);
  }

  @Override
  public int getStartYear() {
    return getConfig().getStartYear();
  }

  @Override
  public void setRootdir(final String val) {
    getConfig().setRootdir(val);
  }

  @Override
  public String getRootdir() {
    return getConfig().getRootdir();
  }

  @Override
  public void setProdid(final String val) {
    getConfig().setProdid(val);
  }

  @Override
  public String getProdid() {
    return getConfig().getProdid();
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
  public void setGenerate(final boolean val) {
    getConfig().setGenerate(val);
  }

  @Override
  public boolean getGenerate() {
    return getConfig().getGenerate();
  }

  @Override
  public void setCompareWithPath(final String val) {
    getConfig().setCompareWithPath(val);
  }

  @Override
  public String getCompareWithPath() {
    return getConfig().getCompareWithPath();
  }

  @Override
  public void setVerboseId(final String val) {
    getConfig().setVerboseId(val);
  }

  @Override
  public String getVerboseId() {
    return getConfig().getVerboseId();
  }

  @Override
  public void setAliasesPath(final String val) {
    getConfig().setAliasesPath(val);
  }

  @Override
  public String getAliasesPath() {
    return getConfig().getAliasesPath();
  }

  /* ========================================================================
   * Operations
   * ======================================================================== */

  @Override
  public String doConvert() {
    final InfoLines msgs = new InfoLines();

    try {
      final Processor proc = getProc();

      proc.parse();

      if (getGenerate()) {
        proc.generateZoneinfoFiles(Util.buildPath(true,
                                                  getRootdir(),
                                                  "/", "zoneinfo"),
                                   true);  // doLinks
      }

      if (getCompare()) {
        proc.compare(msgs);
      }

      for (final String msg: msgs) {
        System.out.print(msg);
      }
    } catch (final Throwable t) {
      t.printStackTrace();
      msgs.exceptionMsg(t);
    }

    return msgs.toString();
  }

  @Override
  public String loadConfig() {
    return loadConfig(TzConvertParams.class);
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  private Processor getProc() {
    if (proc != null) {
      return proc;
    }

    proc = new Processor(this);

    return proc;
  }
}
