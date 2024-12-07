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

import org.bedework.util.logging.BwLogger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle GET action=list.
 *
 *   @author Mike Douglass
 */
public class ListHandler extends MethodBase {
  /**
   * @throws javax.servlet.ServletException
   */
  public ListHandler() throws ServletException {
    super();
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException {
    if (debug()) {
      debug("ListHandler: doMethod");
    }

    try {
      final String changedsince = req.getParameter("changedsince");

      // TODO - this is non-standard?
      final String[] tzids = req.getParameterValues("tzid");

      if ((tzids != null) && (tzids.length > 0)) {
        if (changedsince != null) {
          errorResponse(resp,
                        HttpServletResponse.SC_BAD_REQUEST,
                        invalidListTzid);
          return;
        }

        listResponse(resp, util.getTimezones(tzids));
        return;
      }

      listResponse(resp, util.getTimezones(changedsince));

      if (changedsince != null) {
        new BwLogger().setLoggedName("org.bedework.timezones.refresh.logger")
                      .info("Refresh call from " + req.getRemoteHost());
      }
    } catch (final ServletException se) {
      throw se;
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }
}
