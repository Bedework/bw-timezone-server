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

import java.io.Serializable;

/**
 Define parameters that drive the tz conversion package
*/
public interface TzConvertParamsI extends Serializable {
  /**
   * @param val uri of server used for comparisons
   */
  void setTzServerUri(String val);

  /**
   * @return uri of server used for comparisons
   */
  String getTzServerUri();

  /**
   * @param val true if we do comparisons
   */
  void setCompare(boolean val);

  /**
   * @return true if we do comparisons
   */
  boolean getCompare();

  /**
   * @param val end year for conversion
   */
  void setEndYear(int val);

  /**
   * @return end year for conversion
   */
  int getEndYear();

  /**
   * @param val start year for conversion
   */
  void setStartYear(int val);

  /**
   * @return start year for conversion
   */
  int getStartYear();

  /**
   * @param val where we find and build data
   */
  void setRootdir(String val);

  /**
   * @return where we find and build data
   */
  String getRootdir();

  /**
   * @param val prodid for resulting output
   */
  void setProdid(String val);

  /**
   * @return prodid for resulting output
   */
  String getProdid();

  /**
   * @param val value for source in tz server capabilities e.g. "IANA 2014d"
   */
  void setSource(String val);

  /**
   * @return value for source in tz server capabilities e.g. "IANA 2014d"
   */
  String getSource();

  /**
   * @param val true to generate output
   */
  void setGenerate(boolean val);

  /**
   * @return true to generate output
   */
  boolean getGenerate();

  /**
   * @param val Directory containing an Olson tzdata directory to  compare with
   */
  void setCompareWithPath(String val);

  /**
   * @return Directory containing an Olson tzdata directory to  compare with
   */
  String getCompareWithPath();

  /**
   * @param val Tzid for verbose output
   */
  void setVerboseId(String val);

  /**
   * @return Tzid for verbose output
   */
  String getVerboseId();

  /**
   * @param val Path to property file defining extra aliases
   */
  void setAliasesPath(String val);

  /**
   * @return Path to property file defining extra aliases
   */
  String getAliasesPath();
}