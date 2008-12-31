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

    if (req.getParameter("names") != null) {
      doNames(resp);
    } else if (req.getParameter("stats") != null) {
      doStats(resp);
    } else if (req.getParameter("aliases") != null) {
      doAliases(resp);
    } else {
      doTzids(resp, req.getParameterValues("tzid"));
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

  private void doStats(HttpServletResponse resp) throws ServletException {
    try {
      Writer wtr = resp.getWriter();

      wtr.write("<html>\r\n");
      wtr.write("  <head>\r\n");

      /* Need some styles I guess */

      wtr.write("    <title>Timezone server statistics</title>\r\n");

      wtr.write("</head>\r\n");
      wtr.write("<body>\r\n");

      wtr.write("    <h1>Timezone server statistics</title></h1>\r\n");

      wtr.write("  <hr/>\r\n");

      wtr.write("  <table width=\"30%\" " +
                "cellspacing=\"0\"" +
                " cellpadding=\"4\">\r\n");

      statLine(wtr, "Gets", TzServerUtil.gets);
      statLine(wtr, "Hits", TzServerUtil.cacheHits);
      statLine(wtr, "Name lists", TzServerUtil.nameLists);
      statLine(wtr, "Reads", TzServerUtil.reads);

      wtr.write("</table>\r\n");

      /* Could use a footer */
      wtr.write("</body>\r\n");
      wtr.write("</html>\r\n");
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doAliases(HttpServletResponse resp) throws ServletException {
    try {
      Writer wtr = resp.getWriter();

      wtr.write(TzServerUtil.getAliases(TzServer.tzDefsZipFile));
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void statLine(Writer wtr, String name, long val) throws Throwable {
    wtr.write("<tr>\r\n");

    wtr.write("  <td align=\"right\">");
    wtr.write(name);
    wtr.write("</td>");

    wtr.write("  <td align=\"right\">");
    wtr.write(String.valueOf(val));
    wtr.write("</td>\r\n");
    wtr.write("</tr>\r\n");
  }

  private void doTzids(HttpServletResponse resp,
                       String[] tzids) throws ServletException {
    if ((tzids == null) || (tzids.length == 0)) {
      return;
    }

    try {
      Writer wtr = resp.getWriter();

      boolean found = false;

      for (String tzid: tzids) {
        String tz = TzServerUtil.getTz(tzid, TzServer.tzDefsZipFile);

        if (tz != null) {
          found = true;
          wtr.write(tz);
        }
      }

      if (!found) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }
}
