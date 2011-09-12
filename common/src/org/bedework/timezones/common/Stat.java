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
package org.bedework.timezones.common;

import java.io.Serializable;

/** Provide a way to get named values.
 *
 * @author douglm
 */
public class Stat implements Serializable {
  private String name;
  private String value1;
  private String value2;

  /**
   * @param name
   * @param value
   */
  public Stat(final String name,
              final String value) {
    this.name = name;
    value1 = value;
  }

  /**
   * @param name
   * @param value1
   * @param value2
   */
  public Stat(final String name,
              final String value1,
              final String value2) {
    this.name = name;
    this.value1 = value1;
    this.value2 = value2;
  }

  /**
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * @return value
   */
  public String getValue1() {
    return value1;
  }

  /**
   * @return value
   */
  public String getValue2() {
    return value2;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(getName());
    sb.append(" = ");
    sb.append(getValue1());

    if (getValue2() != null) {
      sb.append(", ");
      sb.append(getValue2());
    }

    sb.append("\n");

    return sb.toString();
  }
}
