/*
#    Copyright (c) 2007-2013 Cyrus Daboo. All rights reserved.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
*/
package org.bedework.timezones.convert;

import org.bedework.util.config.ConfInfo;
import org.bedework.util.config.ConfigBase;

/**
 Define parameters that drive the tz conversion package
*/
@ConfInfo(elementName = "bwtz-cnvinfo")
public class TzConvertParams extends ConfigBase<TzConvertParams>
        implements TzConvertParamsI {
  // Set the PRODID value used in generated iCalendar data
  private String prodid = "-//bedework.org//tzsvr//EN";
  private String rootdir = "../../stuff/temp";
  private int startYear = 1800;
  private int endYear = 2040;

  private boolean compare;

  private String tzServerUri; // = //"www.bedework.org";
  //          "https://demo.calendarserver.org:8443/stdtimezones";
  private String compareWithPath;

  private String aliasesPath;

  private String source;

  private boolean verbose;
  private boolean generate = true;

  @Override
  public void setTzServerUri(final String val) {
    tzServerUri = val;
  }

  @Override
  public String getTzServerUri() {
    return tzServerUri;
  }

  @Override
  public void setCompare(final boolean compare) {
    this.compare = compare;
  }

  @Override
  public boolean getCompare() {
    return compare;
  }

  @Override
  public void setEndYear(final int endYear) {
    this.endYear = endYear;
  }

  @Override
  public int getEndYear() {
    return endYear;
  }

  @Override
  public void setStartYear(final int val) {
    startYear = val;
  }

  @Override
  public int getStartYear() {
    return startYear;
  }

  @Override
  public void setRootdir(final String val) {
    rootdir = val;
  }

  @Override
  public String getRootdir() {
    return rootdir;
  }

  @Override
  public void setProdid(final String val) {
    prodid = val;
  }

  @Override
  public String getProdid() {
    return prodid;
  }

  @Override
  public void setSource(final String val) {
    source = val;
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public void setGenerate(final boolean val) {
    generate = val;
  }

  @Override
  public boolean getGenerate() {
    return generate;
  }

  @Override
  public void setCompareWithPath(final String val) {
    compareWithPath = val;
  }

  @Override
  public String getCompareWithPath() {
    return compareWithPath;
  }

  @Override
  public void setAliasesPath(final String val) {
    aliasesPath = val;
  }

  @Override
  public String getAliasesPath() {
    return aliasesPath;
  }
}