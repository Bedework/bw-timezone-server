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

import ietf.params.xml.ns.timezone_service.CapabilitiesAcceptParameterType;
import ietf.params.xml.ns.timezone_service.CapabilitiesOperationType;
import ietf.params.xml.ns.timezone_service.CapabilitiesType;
import ietf.params.xml.ns.timezone_service.ObjectFactory;
import ietf.params.xml.ns.timezone_service.TimezoneListType;

import java.io.Writer;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/** Class called to handle GET.
 *
 *   @author Mike Douglass
 */
public class GetMethod extends MethodBase {
  /* Define capabilities as static objects */
  static final CapabilitiesType capabilities = new CapabilitiesType();

  static {
    addCapability(capabilities, "capabilities",
                  "This operation returns the capabilities of the server, " +
                    "allowing clients to determine if a specific feature has been" +
                    " deployed and/or enabled.",
                  makePar("action",
                          true,
                          false,
                          "capabilities",
                          "Specify the action to be carried out."));

    addCapability(capabilities, "list",
                  "This operation lists all timezone identifiers, in summary " +
                    "format, with optional localized data. In addition, it " +
                    "returns a timestamp which is the current server global " +
                    "last modification value. ",
                  makePar("action",
                          true,
                          false,
                          "list",
                          "Specify the action to be carried out."),
                  makePar("lang",
                          false,
                          true,
                          null,
                          "OPTIONAL, but MUST occur only once. If present, " +
                            "indicates that timezone aliases should be returned " +
                            "in the list. "),
                  makePar("returnall",
                          false,
                          false,
                          null,
                          "OPTIONAL, but MUST occur only once. If present, " +
                            "indicates that all, including inactive, timezones " +
                            "should be returned in the response. The " +
                            "TZ:inactive XML element will flag those timezones " +
                            "no longer in use. "),
                  makePar("changedsince",
                          false,
                          false,
                          null,
                          "OPTIONAL, but MUST occur only once. If present, " +
                            "limits the response to timezones changed since " +
                            "the given timestamp."));

    addCapability(capabilities, "get",
                  "    This operation returns a timezone. Clients must be " +
                    "prepared to accept a timezone with a different identifier " +
                    "if the requested identifier is an alias. ",
                  makePar("action",
                          true,
                          false,
                          "get",
                          "Specify the action to be carried out."),
                  makePar("format",
                          false,
                          false,
                          null,
                          "OPTIONAL, but MUST occur only once. Return " +
                            "information using the specified media-type. In " +
                            "the absence of this parameter, the value " +
                            "\"text/calendar\" MUST be assumed."),
                  makePar("lang",
                          false,
                          true,
                          null,
                          "OPTIONAL, but MUST occur only once. If present, " +
                            "indicates that timezone aliases should be returned " +
                            "in the list. "),
                  makePar("tzid",
                          true,
                          true,
                          null,
                          "REQUIRED, but MAY occur multiple times. Identifies " +
                            "the timezone for which information is returned. " +
                            "Alternatively, if a single value of \"*\" is " +
                            "given, returns information for all timezones. " +
                            "The \"*\" option will typically be used by servers" +
                            "that wish to retrieve the entire set of timezones " +
                            "supported by another server to re-synchronize " +
                            "their entire data cache. Clients will typically " +
                            "only retrieve individual timezone data on a " +
                            "case-by-case basis."));

    addCapability(capabilities, "expand",
                  "    This operation expands the specified timezone(s) into a " +
                    "list of onset start date/time and offset. ",
                  makePar("action",
                          true,
                          false,
                          "expand",
                          "Specify the action to be carried out."),
                  makePar("tzid",
                          true,
                          true,
                          null,
                          "REQUIRED, but MAY occur multiple times. Identifies " +
                            "the timezones for which information is returned. " +
                            "The value \"*\", which has a special meaning in " +
                            "the \"get\" operation, is not supported by this " +
                            "operation."),
                  makePar("lang",
                          false,
                          true,
                          null,
                          "OPTIONAL, but MUST occur only once. If present, " +
                            "indicates that timezone aliases should be returned " +
                            "in the list. "),
                  makePar("start",
                          false,
                          true,
                          "date or date-time",
                          "OPTIONAL, but MUST occur only once. If present, " +
                            "specifies the start of the period of interest. " +
                            "If omitted, the current year is assumed. "),
                  makePar("end",
                          false,
                          true,
                          "date or date-time",
                          "OPTIONAL, but MUST occur only once. If present, " +
                            "specifies the end of the period of interest. " +
                            "If omitted, the current year + 10 is assumed. "));
  }

  private static void addCapability(CapabilitiesType capabilities,
                                    String action,
                                    String description,
                                    CapabilitiesAcceptParameterType... pars) {
    CapabilitiesOperationType cot = new CapabilitiesOperationType();

    cot.setAction(action);
    cot.setDescription(description);

    if (pars != null) {
      for (CapabilitiesAcceptParameterType par: pars) {
        cot.getAcceptParameter().add(par);
      }
    }

    capabilities.getOperation().add(cot);
  }

  private static CapabilitiesAcceptParameterType makePar(String name,
                                                         boolean required,
                                                         boolean multi,
                                                         String value,
                                                         String description) {
    CapabilitiesAcceptParameterType capt = new CapabilitiesAcceptParameterType();

    capt.setName(name);
    capt.setRequired(required);
    capt.setMulti(multi);
    capt.setValue(value);
    capt.setDescription(description);

    return capt;
  }

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

    String action = req.getParameter("action");

    if ("capabilities".equals(action)) {
      doCapabilities(resp);
      return;
    }

    if ("list".equals(action)) {
      doList(req, resp);
      return;
    }

    if ("expand".equals(action)) {
      doExpand(req,resp);
      return;
    }

    if ("get".equals(action)) {
      doTzid(resp, req.getParameter("tzid"));
      return;
    }

    /* Follow all old and non-standard actions */

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
    //} else if (req.getParameter("unalias") != null) {
    //  doUnalias(resp, req.getParameter("id"));
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
      doTzid(resp, path.substring(tzspath.length() + 1));
    } else {
      doTzid(resp, req.getParameter("tzid"));
    }
  }

  private void doCapabilities(final HttpServletResponse resp) throws ServletException {
    try {
      resp.setContentType("application/xml; charset=UTF-8");

      Marshaller m = getJc().createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.marshal(getOf().createCapabilities(capabilities), resp.getOutputStream());
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doList(final HttpServletRequest req,
                      final HttpServletResponse resp) throws ServletException {
    try {
      resp.setContentType("application/xml; charset=UTF-8");

      TimezoneListType tzl = new TimezoneListType();

      tzl.setDtstamp(util.getDtstamp());

      tzl.getSummary().addAll(util.getSummaries(req.getParameter("changedsince")));

      Marshaller m = getJc().createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.marshal(getOf().createTimezoneList(tzl), resp.getOutputStream());
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void doExpand(final HttpServletRequest req,
                        final HttpServletResponse resp) throws ServletException {
    try {
      resp.setContentType("application/xml; charset=UTF-8");

      String tzid = req.getParameter("tzid");
      String start = req.getParameter("start");
      String end = req.getParameter("end");
      ExpandedMapEntry tzs = util.getExpanded(tzid, start, end);

      if (tzs == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      resp.setHeader("ETag", tzs.getEtag());

      Marshaller m = getJc().createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.marshal(getOf().createTimezones(tzs.getTzs()), resp.getOutputStream());
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
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
    } catch (ServletException se) {
      throw se;
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
    } catch (ServletException se) {
      throw se;
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

      infoLine(wtr, "dtstamp", util.getDtstamp().toXMLFormat());

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

  private void doTzid(final HttpServletResponse resp,
                      final String tzid) throws ServletException {
    if (tzid == null) {
      return;
    }

    try {
      resp.setContentType("text/calendar; charset=UTF-8");

      Writer wtr = resp.getWriter();

      if ("*".equals(tzid)) {
        // Return all
        Collection<String> vtzs = util.getAllTzs();

        writeCalHdr(wtr);

        for (String s: vtzs) {
          wtr.write(s);
        }

        writeCalTlr(wtr);
      }

      String tz = util.getTz(tzid);

      if (tz == null) {
        tz = util.getAliasedTz(tzid);
      }

      if (tz == null) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } else {
        resp.setHeader("ETag", "\"" + util.getDtstamp().toXMLFormat() + "\"");
        writeCalHdr(wtr);

        wtr.write(tz);

        writeCalTlr(wtr);
      }
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private void writeCalHdr(Writer wtr) throws Throwable  {
    wtr.write(util.getCalHdr());
  }

  private void writeCalTlr(Writer wtr) throws Throwable  {
    wtr.write(util.getCalTlr());
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

  private static JAXBContext jc;

  private JAXBContext getJc() throws ServletException {
    try {
      if (jc != null) {
        return jc;
      }

      synchronized (this) {
        if (jc != null) {
          return jc;
        }

        jc = JAXBContext.newInstance("ietf.params.xml.ns.timezone_service");
      }

      return jc;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  private static ObjectFactory of;

  private ObjectFactory getOf() throws ServletException {
    try {
      if (of != null) {
        return of;
      }

      synchronized (this) {
        if (of != null) {
          return of;
        }

        of = new ObjectFactory();
      }

      return of;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }
}
