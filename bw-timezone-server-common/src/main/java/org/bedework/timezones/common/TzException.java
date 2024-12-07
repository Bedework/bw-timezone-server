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

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

/** Base exception thrown by tz classes
 *
 *   @author Mike Douglass   douglm rpi.edu
 */
public class TzException extends RuntimeException {
  /** > 0 if set
   */
  int statusCode;
  QName errorTag;

  /** Constructor
   *
   * @param msg a message
   */
  public TzException(final String msg) {
    super(msg);
    statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
  }

  /** Constructor
   *
   * @param t throwable
   */
  public TzException(final Throwable t) {
    super(t);
    statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
  }

  /** Constructor
   *
   * @param st status
   */
  public TzException(final int st) {
    statusCode = st;
  }

  /** Constructor
   *
   * @param st status
   * @param msg a message
   */
  public TzException(final int st, final String msg) {
    super(msg);
    statusCode = st;
  }

  /** Constructor
   *
   * @param st status
   * @param errorTag xml tage
   */
  public TzException(final int st, final QName errorTag) {
    statusCode = st;
    this.errorTag = errorTag;
  }

  /** Constructor
   *
   * @param st status
   * @param errorTag xml tage
   * @param msg a message
   */
  public TzException(final int st,
                     final QName errorTag,
                     final String msg) {
    super(msg);
    statusCode = st;
    this.errorTag = errorTag;
  }

  /** Set the status
   * @param val int status
   */
  public void setStatusCode(final int val) {
    statusCode = val;
  }

  /** Get the status
   *
   * @return int status
   */
  public int getStatusCode() {
    return statusCode;
  }

  /** Get the errorTag
   *
   * @return QName
   */
  public QName getErrorTag() {
    return errorTag;
  }
}
