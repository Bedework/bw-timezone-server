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

import org.bedework.timezones.common.TzConfig;
import org.bedework.timezones.common.TzServerUtil;
import org.bedework.util.timezones.model.CapabilitiesAcceptParameterType;
import org.bedework.util.timezones.model.CapabilitiesActionType;
import org.bedework.util.timezones.model.CapabilitiesInfoType;
import org.bedework.util.timezones.model.CapabilitiesTruncatedType;
import org.bedework.util.timezones.model.CapabilitiesType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.lang.String.format;

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
              "/capabilities",
              "This action returns the capabilities of the server, " +
                "allowing clients to determine if a specific feature has been" +
                " deployed and/or enabled.");

    addAction(capabilities, "list",
              "/zones{?changedsince}",
              "This action lists all timezone identifiers, in summary " +
                "format, with optional localized data. In addition, it " +
                "returns a timestamp which is the current server global " +
                "last modification value. ",
              makePar("changedsince",
                      false,
                      false,
                      null,
                      "OPTIONAL, but MUST occur only once. If present, " +
                        "limits the response to timezones changed since " +
                        "the given timestamp."));

    addAction(capabilities, "get",
              "/zones{/tzid}{?start,end}",
              "This action returns a timezone. Clients must be " +
                "prepared to accept a timezone with a different identifier " +
                "if the requested identifier is an alias. ",
              makePar("start",
                      false,
                      false,
                      null,
                      "OPTIONAL, and MUST occur only once.  Specifies " +
                      "the inclusive UTC date-time value at which the returned time " +
                      "zone data is truncated at its start."),
              makePar("end",
                      false,
                      false,
                      null,
                      "OPTIONAL, and MUST occur only once.  Specifies " +
                      "the exclusive UTC date-time value at which the returned time " +
                      "zone data is truncated at its end."));

    addAction(capabilities, "expand",
              "/zones{/tzid}/observances{?start,end}",
              "This action expands the specified timezone(s) into a " +
                "list of onset start date/time and offset. ",
              makePar("start",
                      false,
                      false,
                      null,
                      "OPTIONAL, and MUST occur only once.  Specifies " +
                              "the inclusive UTC date-time value at which the returned time " +
                              "zone data is truncated at its start."),
              makePar("end",
                      false,
                      false,
                      null,
                      "OPTIONAL, and MUST occur only once.  Specifies " +
                              "the exclusive UTC date-time value at which the returned time " +
                              "zone data is truncated at its end."));

    addAction(capabilities, "find",
              "/zones{?pattern}",
              "This action allows a client to query the timezone service " +
                "for a matching identifier, alias or localized name.\n" +
                "Response format is the same as for list with one result " +
                "element per successful match. ",
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
                                final String uriTemplate,
                                @SuppressWarnings(
                                        "UnusedParameters") final String description,
                                final CapabilitiesAcceptParameterType... pars) {
    final CapabilitiesActionType cot = new CapabilitiesActionType();

    cot.setName(action);
    cot.setUriTemplate(uriTemplate);

    if (pars != null) {
      for (final CapabilitiesAcceptParameterType par: pars) {
        cot.getParameters().add(par);
      }
    }

    capabilities.getActions().add(cot);
  }

  private static CapabilitiesAcceptParameterType makePar(final String name,
                                                         final boolean required,
                                                         final boolean multi,
                                                         final String value,
                                                         @SuppressWarnings(
                                                                 "UnusedParameters") final String description) {
    final CapabilitiesAcceptParameterType capt = new CapabilitiesAcceptParameterType();

    capt.setName(name);
    capt.setRequired(required);
    capt.setMulti(multi);
    capt.addValue(value);

    return capt;
  }

  /**
   */
  public CapabilitiesHandler() throws ServletException {
    super();
  }

  @Override
  public void doMethod(final HttpServletRequest req,
                       final HttpServletResponse resp) throws ServletException {
    if (debug()) {
      debug("CapabilitiesHandler: doMethod");
    }

    try {
      resp.setContentType("application/json; charset=UTF-8");

      final CapabilitiesInfoType ci = new CapabilitiesInfoType();

      final TzConfig cfg = TzServerUtil.getTzConfig();

      if (cfg == null) {
        error(format("%1$s\n%1$s\n%1$s\n%2$s\n%1$s\n%1$s\n%1$s\n",
                     "====================================",
                     " Missing configuration - exiting"));
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      }

      if (!cfg.getPrimaryServer()) {
        ci.setPrimarySource(cfg.getPrimaryUrl());
      } else {
        ci.setSource(cfg.getSource());
      }

      ci.getFormats().add("text/calendar");
      ci.getFormats().add("application/calendar+xml");
      ci.getFormats().add("application/calendar+json");

      final CapabilitiesTruncatedType ct = new CapabilitiesTruncatedType();

      ct.setAny(false);
      ct.setUntruncated(true);

      ci.setTruncated(ct);

      //ci.getContacts().add(util.get)
      capabilities.setInfo(ci);

      writeJson(resp, capabilities);
    } catch (final ServletException se) {
      throw se;
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }
}
