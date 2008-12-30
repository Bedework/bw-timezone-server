/* **********************************************************************
    Copyright 2006 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

package org.bedework.timezones.server;

import java.io.Writer;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle GET.
 *
 *   @author Mike Douglass
 */
public class GetMethod extends MethodBase {
  /**
   * @param debug
   */
  public GetMethod(boolean debug) {
    super(debug);
  }

  /* (non-Javadoc)
   * @see org.bedework.timezones.server.MethodBase#doMethod(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Properties)
   */
  public void doMethod(HttpServletRequest req,
                        HttpServletResponse resp,
                        Properties props) throws ServletException {
    if (debug) {
      trace("GetMethod: doMethod");
    }

    String names = req.getParameter("names");
    String[] tzids = req.getParameterValues("tzid");

    if (names != null) {
      doNames(resp);
    } else {
      doTzids(resp, tzids);
    }
  }

  private void doNames(HttpServletResponse resp) throws ServletException {
    try {
      Writer wtr = resp.getWriter();

      for (String s: TzServerUtil.getNames(TzServer.tzDefsZipFile)) {
        wtr.write(s);
        wtr.write("\n");
      }
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doTzids(HttpServletResponse resp,
                       String[] tzids) throws ServletException {
    if ((tzids == null) || (tzids.length == 0)) {
      return;
    }

    try {
      Writer wtr = resp.getWriter();

      for (String tzid: tzids) {
        String tz = TzServerUtil.getTz(tzid, TzServer.tzDefsZipFile);

        if (tz != null) {
          wtr.write(tz);
        }
      }
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }
}
