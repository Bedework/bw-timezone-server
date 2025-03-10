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
package org.bedework.timezones.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Class called to handle POST. This is used to provoke the server into doing a
 * refresh and is not part of the timezone server specification.
 *
 * <p>This method should ensure that the request is from a known ip address and
 * that some sort of key is provided.
 *
 *   @author Mike Douglass
 */
public class PostMethod extends MethodBase {
  /**
   * @throws ServletException on error
   */
  public PostMethod() throws ServletException {
    super();
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException {
    if (debug()) {
      debug("PostMethod: doMethod");
    }

    try {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }
}
