/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
