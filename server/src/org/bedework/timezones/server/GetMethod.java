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

import org.bedework.timezones.common.Stat;
import org.bedework.timezones.common.TzServerUtil;

import java.io.Writer;
import java.util.Collection;

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
   * @throws ServletException
   */
  public GetMethod(final boolean debug) throws ServletException {
    super(debug);
  }

  private static final String tzspath = "/timezones";

  /* (non-Javadoc)
   * @see org.bedework.timezones.server.MethodBase#doMethod(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException {
    String path = getResourceUri(req);

    if (debug) {
      trace("GetMethod: doMethod  path=" + path);
    }

    if (path == null) {
      path = "";
    }

    if (req.getParameter("names") != null) {
      if (ifNoneMatchTest(req, resp)) {
        return;
      }

      doNames(resp);
    } else if (req.getParameter("stats") != null) {
      doStats(resp);
    } else if (req.getParameter("info") != null) {
      doInfo(resp);
    } else if (req.getParameter("aliases") != null) {
      if (ifNoneMatchTest(req, resp)) {
        return;
      }

      doAliases(resp);
    } else if (req.getParameter("unalias") != null) {
      doUnalias(resp, req.getParameter("id"));
    } else if (req.getParameter("convert") != null) {
      doConvert(resp, req.getParameter("dt"),
                req.getParameter("fromtzid"),
                req.getParameter("totzid"));
    } else if (req.getParameter("utc") != null) {
      doToUtc(resp, req.getParameter("dt"),
                req.getParameter("fromtzid"));
    } else if (path.equals(tzspath) ||
               path.equals(tzspath + "/")) {
      doNames(resp);
    } else if (path.startsWith(tzspath + "/")) {
      String[] ids = {
         path.substring(tzspath.length() + 1)
      };
      doTzids(resp, ids);
    } else {
      doTzids(resp, req.getParameterValues("tzid"));
    }
  }

  private void doNames(final HttpServletResponse resp) throws ServletException {
    try {
      resp.setContentType("text/plain");
      resp.setHeader("ETag", util.getEtag());

      Writer wtr = resp.getWriter();

      for (String s: util.getNames()) {
        wtr.write(s);
        wtr.write("\n");
      }
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doStats(final HttpServletResponse resp) throws ServletException {
    try {
      resp.setContentType("text/html");
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

      for (Stat s: TzServerUtil.getStats()) {
        statLine(wtr, s.getName(), s.getValue1(), s.getValue2());
      }

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

  private void doAliases(final HttpServletResponse resp) throws ServletException {
    try {
      resp.setHeader("ETag", util.getEtag());
      resp.setContentType("text/plain");

      Writer wtr = resp.getWriter();

      wtr.write(util.getAliases());
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doUnalias(final HttpServletResponse resp,
                         String id) throws ServletException {
    try {
      resp.setContentType("text/plain");

      Writer wtr = resp.getWriter();

      String tzid = util.unalias(id);
      if (tzid == null) {
        tzid = "** error **";
      }
      wtr.write(tzid);
      wtr.write("\n");
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doInfo(final HttpServletResponse resp) throws ServletException {
    try {
      resp.setHeader("ETag", util.getEtag());
      resp.setContentType("text/html");

      Writer wtr = resp.getWriter();

      Collection<String> info = util.getInfo();

      wtr.write("<html>\r\n");
      wtr.write("  <head>\r\n");

      /* Need some styles I guess */

      wtr.write("    <title>Timezone server information</title>\r\n");

      wtr.write("</head>\r\n");
      wtr.write("<body>\r\n");

      wtr.write("    <h1>Timezone server information</title></h1>\r\n");

      wtr.write("  <hr/>\r\n");

      wtr.write("  <table width=\"30%\" " +
                "cellspacing=\"0\"" +
                " cellpadding=\"4\">\r\n");

      for (String s: info) {
        String[] nameVal = s.split("=");
        infoLine(wtr, nameVal[0], nameVal[1]);
      }

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

  private void infoLine(final Writer wtr,
                        final String name,
                        final String val) throws Throwable {
    wtr.write("<tr>\r\n");

    wtr.write("  <td align=\"right\">");
    wtr.write(name);
    wtr.write("</td>");

    wtr.write("  <td align=\"right\">");
    wtr.write(val);
    wtr.write("</td>");

    wtr.write("</tr>\r\n");
  }

  private void statLine(final Writer wtr,
                        final String name, final String val,
                        final String millis) throws Throwable {
    wtr.write("<tr>\r\n");

    wtr.write("  <td align=\"right\">");
    wtr.write(name);
    wtr.write("</td>");

    wtr.write("  <td align=\"right\">");
    wtr.write(String.valueOf(val));
    wtr.write("</td>");

    wtr.write("<td>");
    if (millis == null) {
      wtr.write("&nbsp;");
    } else {
      String s = millis;
      while (s.length() < 4) {
        s = "0" + s;
      }

      wtr.write(s.substring(0, s.length() - 3));
      wtr.write(".");
      wtr.write(s.substring(s.length() - 3));
      wtr.write(" seconds");
    }

    wtr.write("</td>");

    wtr.write("</td>\r\n");

    wtr.write("</tr>\r\n");
  }

  private void doConvert(final HttpServletResponse resp,
                         final String dateTime,
                         final String fromTzid,
                         final String toTzid) throws ServletException {
    try {
      Writer wtr = resp.getWriter();

      String cnvDdateTime = util.convertDateTime(dateTime,
                                                 fromTzid,
                                                 toTzid);

      if (cnvDdateTime != null) {
        wtr.write(cnvDdateTime);
      } else {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doToUtc(final HttpServletResponse resp,
                       final String dateTime,
                       final String fromTzid) throws ServletException {
    try {
      Writer wtr = resp.getWriter();

      String cnvDdateTime = util.getUtc(dateTime, fromTzid);

      if (cnvDdateTime != null) {
        wtr.write(cnvDdateTime);
      } else {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doTzids(final HttpServletResponse resp,
                       final String[] tzids) throws ServletException {
    if ((tzids == null) || (tzids.length == 0)) {
      return;
    }

    try {
      Writer wtr = resp.getWriter();

      boolean found = false;

      for (String tzid: tzids) {
        String tz = util.getTz(util.unalias(tzid));

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

  /* Return true if data unchanged - status is set */
  private boolean ifNoneMatchTest(final HttpServletRequest req,
                                  final HttpServletResponse resp) throws ServletException {
    String inEtag = req.getHeader("If-None-Match");

    if (inEtag == null) {
      return false;
    }

    if (!inEtag.equals(util.getEtag())) {
      return false;
    }

    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    return true;
  }
}