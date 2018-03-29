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

import org.bedework.timezones.common.ExpandedMapEntry;
import org.bedework.timezones.common.Stat;
import org.bedework.timezones.common.TzServerUtil;

import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle GET.
 *
 *   @author Mike Douglass
 */
public class GetMethod extends MethodBase {
  private static final CapabilitiesHandler capabilities;
  private static final ListHandler lists;
  private static final TzidHandler tzids;

  static {
    try {
      capabilities = new CapabilitiesHandler();
      lists = new ListHandler();
      tzids = new TzidHandler();
    } catch (final ServletException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * @throws ServletException
   */
  public GetMethod() throws ServletException {
    super();
  }

  private static final String tzsEl = "timezones";

  /* Draft 3
   *
5.1.  "capabilities" Action
   Request-URI Template:  {/service-prefix}/capabilities
5.2.  "list" Action
   Request-URI Template:  {/service-prefix,data-
      prefix}/zones{?changedsince}
5.3.  "get" Action
   Request-URI Template:  {/service-prefix,data-
      prefix}/zones{/tzid}{?start,end}
5.4.  "expand" Action
   Request-URI Template:  {/service-prefix,data-prefix}/observances
       {/tzid}{?start,end,changedsince}
5.5.  "find" Action
   Request-URI Template:  {/service-prefix,data-prefix}/zones{?pattern}

*/
  private static final String capabilitiesEl = "capabilities";
  private static final String zonesEl = "zones";
  private static final String observancesEl = "observances";

  // TODO - fix stylesheets to parse json from list
  private static final String namesReqPar = "names";

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException {
    final ResourceUri ruri = getResourceUri(req);
    final long start = System.currentTimeMillis();

    if (debug) {
      trace("GetMethod: doMethod  path=" + ruri.uri);
    }

    try {
      if (req.getParameter("names") != null) {
        if (ifNoneMatchTest(req, resp)) {
          return;
        }

        doNames(resp);
        return;
      }

      if (ruri.uriElements.size() == 0) {
        return;
      }

      final String el0 = ruri.getPathElement(0);
      if (el0.equals(capabilitiesEl)) {
        capabilities.doMethod(req, resp);
        return;
      }

      final int dataPrefixElements = dataPrefixElements(ruri);

      final String el = ruri.getPathElement(dataPrefixElements);

      if (el == null) {
        return;
      }

      if (el.equals(zonesEl)) {
        doZones(req,resp, ruri, dataPrefixElements);
        return;
      }

      if (el.equals(observancesEl)) {
        doExpand(req, resp, ruri, dataPrefixElements);
      }
    } finally {
      if (debug) {
        trace("GET exit. Took " + TzServerUtil.printableTime(
                System.currentTimeMillis() - start));
      }
    }
  }

  private int dataPrefixElements(@SuppressWarnings(
          "UnusedParameters") final ResourceUri ruri) {
    // Will determine how many elements are data-prefix elements.
    return 0;
  }

  /** Handle pre-rest action parameter style. For backward compatibility
   * with deployed clients.
   *
   * @param req http request
   * @param resp http response
   * @param action non-null action parameter
   * @throws ServletException
   */
  private void doAction(final HttpServletRequest req,
                       final HttpServletResponse resp,
                       final String action) throws ServletException {
    if ("capabilities".equals(action)) {
      capabilities.doMethod(req, resp);
      return;
    }

    if ("list".equals(action)) {
      lists.doMethod(req, resp);
      return;
    }

    if ("expand".equals(action)) {
      doExpand(req,resp, null, 0);
      return;
    }

    if ("get".equals(action)) {
      tzids.doMethod(req, resp);
      return;
    }

    if ("find".equals(action)) {
      doFind(req, resp, req.getParameter("name"));
    }
  }

  private void doZones(final HttpServletRequest req,
                       final HttpServletResponse resp,
                       final ResourceUri ruri,
                       final int dataPrefixSize) throws ServletException {
    if (req.getParameter("pattern") != null) {
      doFind(req, resp, req.getParameter("pattern"));
      return;
    }

    final String tzid = ruri.getElements(dataPrefixSize + 1);

    if (tzid == null) {
      lists.doMethod(req, resp);
      return;
    }

    tzids.doTzid(resp, tzid);
  }

  private void doFind(final HttpServletRequest req,
                      final HttpServletResponse resp,
                      final String pattern) throws ServletException {
    try {
      if (pattern == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      listResponse(resp, util.findTimezones(pattern));
    } catch (final ServletException se) {
      throw se;
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }

  /*
   *    Possible Error Codes

      invalid-tzid  The "tzid" query parameter is not present, or
         appears more than once.

      missing-tzid  The "tzid" query parameter value does not map to a
         timezone identifier known to the server.

      invalid-start  The "start" query parameter has an incorrect value,
         or appears more than once.

      invalid-end  The "end" query parameter has an incorrect value, or
         appears more than once, or has a value less than our equal to
         the "start" query parameter.

      invalid-changedsince  The "changedsince" query parameter has an
         incorrect value, or appears more than once.

   */
  private void doExpand(final HttpServletRequest req,
                        final HttpServletResponse resp,
                        final ResourceUri ruri,
                        final int dataPrefixSize) throws ServletException {
    try {
      resp.setContentType("application/json; charset=UTF-8");

      final String tzid;

      if (ruri == null) {
        // Old style
        tzid = req.getParameter("tzid");
      } else {
        tzid = ruri.getElements(dataPrefixSize + 1);
      }

      if (tzid == null) {
        errorResponse(resp,
                      HttpServletResponse.SC_BAD_REQUEST,
                      missingTzid);
        return;
      }

      final String start = req.getParameter("start");
      final String end = req.getParameter("end");
      final String changedsince = req.getParameter("changedsince");
      final ExpandedMapEntry tzs = util.getExpanded(tzid, start, end,
                                                    ruri == null);

      if (tzs == null) {
        errorResponse(resp,
                      HttpServletResponse.SC_NOT_FOUND,
                      missingTzid);
        return;
      }

      resp.setHeader("ETag", tzs.getEtag());

      writeJson(resp, tzs.getTzs());
    } catch (final ServletException se) {
      throw se;
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doNames(final HttpServletResponse resp) throws ServletException {
    try {
      resp.setContentType("text/plain; charset=UTF-8");
      resp.setHeader("ETag", util.getEtag());

      Writer wtr = resp.getWriter();

      for (String s: util.getNames()) {
        wtr.write(s);
        wtr.write("\n");
      }
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doStats(final HttpServletResponse resp) throws ServletException {
    try {
      resp.setContentType("text/html; charset=UTF-8");
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
      resp.setContentType("text/plain; charset=UTF-8");

      Writer wtr = resp.getWriter();

      wtr.write(util.getAliasesStr());
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  /*
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
  */

  private void doInfo(final HttpServletResponse resp) throws ServletException {
    try {
      resp.setHeader("ETag", util.getEtag());
      resp.setContentType("text/html; charset=UTF-8");

      Writer wtr = resp.getWriter();

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

      infoLine(wtr, "dtstamp", util.getDtstamp());

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

  /* Return true if data unchanged - status is set */
  private boolean ifNoneMatchTest(final HttpServletRequest req,
                                  final HttpServletResponse resp) throws ServletException {
    try {
      String inEtag = req.getHeader("If-None-Match");

      if (inEtag == null) {
        return false;
      }

      if (!inEtag.equals(util.getEtag())) {
        return false;
      }

      resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return true;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }
}
