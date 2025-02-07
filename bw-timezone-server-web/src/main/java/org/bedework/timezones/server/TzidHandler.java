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

import org.bedework.timezones.common.TzServerUtil;

import java.io.Writer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Class called to handle GET action=get.
 *
 *   @author Mike Douglass
 */
public class TzidHandler extends MethodBase {
  /**
   * @throws ServletException on error
   */
  public TzidHandler() throws ServletException {
    super();
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException {
    if (debug()) {
      debug("TzidHandler: doMethod");
    }

    doTzid(resp, req.getParameter("tzid"));
  }

  void doTzid(final HttpServletResponse resp,
              final String tzid) throws ServletException {
    if (tzid == null) {
      errorResponse(resp,
                    HttpServletResponse.SC_BAD_REQUEST,
                    invalidTzid);
      return;
    }

    try {
      resp.setContentType("text/calendar; charset=UTF-8");

      final Writer wtr = resp.getWriter();

      String tz = util.getTz(tzid);

      if (tz == null) {
        tz = util.getAliasedTz(tzid);
      }

      if (tz == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } else {
        resp.setHeader("ETag", "\"" + util.getDtstamp() +
                       "\"");
        writeCalHdr(wtr);

        wtr.write(tz);

        writeCalTlr(wtr);
      }
    } catch (final ServletException se) {
      throw se;
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }

  private void writeCalHdr(final Writer wtr) throws Throwable  {
    wtr.write(TzServerUtil.getCalHdr());
  }

  private void writeCalTlr(final Writer wtr) throws Throwable  {
    wtr.write(TzServerUtil.getCalTlr());
  }
}
