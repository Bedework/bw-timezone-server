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
import org.bedework.util.timezones.model.CapabilitiesAcceptParameterType;
import org.bedework.util.timezones.model.CapabilitiesActionType;
import org.bedework.util.timezones.model.CapabilitiesInfoType;
import org.bedework.util.timezones.model.CapabilitiesTruncatedType;
import org.bedework.util.timezones.model.CapabilitiesType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Class called to handle GET action=capabilities.
 *
 *   @author Mike Douglass
 */
public class CapabilitiesHandler extends MethodBase {
  /* Define capabilities as static objects */
  static final CapabilitiesType capabilities = new CapabilitiesType();

  static {
    capabilities.setVersion(1);

    addAction(capabilities, "capabilities",
              "This action returns the capabilities of the server, " +
                "allowing clients to determine if a specific feature has been" +
                " deployed and/or enabled.",
              makePar("action",
                      true,
                      false,
                      "capabilities",
                      "Specify the action to be carried out."));

    addAction(capabilities, "list",
              "This action lists all timezone identifiers, in summary " +
                "format, with optional localized data. In addition, it " +
                "returns a timestamp which is the current server global " +
                "last modification value. ",
              makePar("action",
                      true, // required
                      false, // multi
                      "list", // Value(s)
                      "Specify the action to be carried out."),
              makePar("lang",
                      false,
                      true,
                      null,
                      "OPTIONAL, but MAY occur multiple times. "),
              makePar("changedsince",
                      false,
                      false,
                      null,
                      "OPTIONAL, but MUST occur only once. If present, " +
                        "limits the response to timezones changed since " +
                        "the given timestamp."),
              makePar("tzid",
                      false,
                      true,
                      null,
                      "OPTIONAL, and MAY occur multiple times.  MUST " +
                          "NOT be present if the \"changedsince\" parameter is present.  The " +
                          "value of the \"dtstamp\" member corresponds to the entire set of " +
                          "data and allows the client to determine if it should refresh " +
                          "its full set."));

    addAction(capabilities, "get",
              "This action returns a timezone. Clients must be " +
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
                      "text/calendar",
                      "OPTIONAL, but MUST occur only once. Return " +
                        "information using the specified media-type. In " +
                        "the absence of this parameter, the value " +
                        "\"text/calendar\" MUST be assumed."),
              makePar("lang",
                      false,
                      true,
                      null,
                      "OPTIONAL, but MAY occur multiple times."),
              makePar("tzid",
                      true,
                      false,
                      null,
                      "REQUIRED, and MUST occur only once. Identifies " +
                        "the timezone for which information is returned. " +
                        "The server MUST return an Etag header. "));

    addAction(capabilities, "expand",
              "This action expands the specified timezone(s) into a " +
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

    addAction(capabilities, "find",
              "This action allows a client to query the timezone service " +
                "for a matching identifier, alias or localized name.\n" +
                "Response format is the same as for list with one result " +
                "element per successful match. ",
                makePar("action",
                        true,
                        false,
                        "find",
                        "Specify the action to be carried out."),
                makePar("name",
                        true,
                        false,
                        null,
                        "REQUIRED, but MUST only occur once. Identifies the " +
                        "name to search for. Only partial matching is " +
                        "supported."),
                makePar("lang",
                        false,
                        true,
                        null,
                        "OPTIONAL, but MUST occur only once. If present, " +
                        "indicates that timezone aliases should be returned " +
                        "in the list. "));
  }

  private static void addAction(final CapabilitiesType capabilities,
                                final String action,
                                final String description,
                                final CapabilitiesAcceptParameterType... pars) {
    CapabilitiesActionType cot = new CapabilitiesActionType();

    cot.setName(action);

    if (pars != null) {
      for (CapabilitiesAcceptParameterType par: pars) {
        cot.getParameters().add(par);
      }
    }

    capabilities.getActions().add(cot);
  }

  private static CapabilitiesAcceptParameterType makePar(final String name,
                                                         final boolean required,
                                                         final boolean multi,
                                                         final String value,
                                                         final String description) {
    CapabilitiesAcceptParameterType capt = new CapabilitiesAcceptParameterType();

    capt.setName(name);
    capt.setRequired(required);
    capt.setMulti(multi);
    capt.addValue(value);

    return capt;
  }

  /**
   * @throws javax.servlet.ServletException
   */
  public CapabilitiesHandler() throws ServletException {
    super();
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException {
    if (debug) {
      trace("CapabilitiesHandler: doMethod");
    }

    try {
      resp.setContentType("application/json; charset=UTF-8");

      CapabilitiesInfoType ci = new CapabilitiesInfoType();

      if (!TzServerUtil.getTzConfig().getPrimaryServer()) {
        ci.setPrimarySource(TzServerUtil.getTzConfig().getPrimaryUrl());
      } else if (TzServerUtil.getTzConfig() != null) {
        ci.setSource(TzServerUtil.getTzConfig().getTzdataUrl());
      }

      CapabilitiesTruncatedType ct = new CapabilitiesTruncatedType();

      ct.setAny(false);
      ct.setUntruncated(true);

      ci.setTruncated(ct);

      //ci.getContacts().add(util.get)
      capabilities.setInfo(ci);

      writeJson(resp, capabilities);
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }
}
