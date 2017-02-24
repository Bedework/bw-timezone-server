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
import org.bedework.util.timezones.model.ErrorResponseType;
import org.bedework.util.timezones.model.TimezoneListType;
import org.bedework.util.timezones.model.TimezoneType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.log4j.Logger;

import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author douglm
 *
 */
public abstract class MethodBase {
  protected static final ErrorResponseType invalidTzid =
      new ErrorResponseType("invalid-tzid",
                            "The \"tzid\" query parameter is not present, or" +
                            " appears more than once.");

  protected static final ErrorResponseType missingTzid =
      new ErrorResponseType("missing-tzid",
                            "The \"tzid\" query parameter value does not map " +
                            "to a timezone identifier known to the server.");

  protected static final ErrorResponseType invalidStart =
      new ErrorResponseType("invalid-start",
                            "The \"start\" query parameter has an incorrect" +
                            " value, or appears more than once.");

  protected static final ErrorResponseType invalidEnd =
      new ErrorResponseType("invalid-end",
                            "The \"end\" query parameter has an incorrect " +
                            "value, or appears more than once, or has a value" +
                            " less than our equal to the \"start\" query " +
                            "parameter.");

  protected static final ErrorResponseType invalidChangedsince =
      new ErrorResponseType("invalid-changedsince",
                            "The \"changedsince\" query parameter has an " +
                            "incorrect value, or appears more than once.");

  protected static final ErrorResponseType invalidListTzid =
          new ErrorResponseType("invalid-tzid",
                                "The \"tzid\" query parameter is present along with the " +
                                        "\"changedsince\", or has an incorrect value.");

  protected boolean debug;

  protected transient Logger log;

  protected ObjectMapper mapper = new ObjectMapper(); // create once, reuse

  protected TzServerUtil util;

  /**
   * @throws ServletException
   */
  public MethodBase() throws ServletException {
    this.debug = getLogger().isDebugEnabled();

    try {
      if (debug) {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      }

      final DateFormat df = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'");

      mapper.setDateFormat(df);

      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

      util = TzServerUtil.getInstance();
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  /**
   * @param req
   * @param resp
   * @throws ServletException
   */
  public abstract void doMethod(HttpServletRequest req,
                                HttpServletResponse resp)
        throws ServletException;

  public static class ResourceUri {
    public String uri;
    public final List<String> uriElements = new ArrayList<>();

    public ResourceUri() {
      this.uri = "";
    }

    public ResourceUri(final String uri) {
      this.uri = uri;
    }

    public void addPathElement(final String val) {
      uriElements.add(val);
      uri += "/" + val;
    }

    /**
     * @param i - index
     * @return indexed element or null for out of range
     */
    public String getPathElement(final int i) {
      if (i > uriElements.size()) {
        return null;
      }
      return uriElements.get(i);
    }

    /**
     *
     * @param i index
     * @return String componsed of elements starting at indexed
     *         separated by "/"
     */
    public String getElements(final int i) {
      if (i >= uriElements.size()) {
        return null;
      }

      String uri = "";
      for (int x = i; x < uriElements.size(); x++) {
        if (x > i) {
          uri += "/";
        }
        uri += uriElements.get(x);
      }

      return uri;
    }
  }

  /** Get the decoded and fixed resource URI
   *
   * @param req      Servlet request object
   * @return resourceUri  fixed up and split uri
   * @throws ServletException
   */
  public ResourceUri getResourceUri(final HttpServletRequest req)
      throws ServletException {
    String uri = req.getPathInfo();

    if ((uri == null) || (uri.length() == 0)) {
      /* No path specified - set it to root. */
      uri = "/";
    }

    if (debug) {
      trace("uri: " + uri);
    }

    final ResourceUri resourceUri = fixPath(uri);

    if (debug) {
      trace("resourceUri: " + resourceUri.uri);
    }

    return resourceUri;
  }

  /** Return a path, beginning with a "/", after "." and ".." are removed.
   * If the parameter path attempts to go above the root we return null.
   *
   * Other than the backslash thing why not use URI?
   *
   * @param path      String path to be fixed
   * @return String   fixed path
   * @throws ServletException
   */
  public static ResourceUri fixPath(final String path) throws ServletException {
    if (path == null) {
      return new ResourceUri();
    }

    String decoded;
    try {
      decoded = URLDecoder.decode(path, "UTF8");
    } catch (final Throwable t) {
      throw new ServletException("bad path: " + path);
    }

    if (decoded == null) {
      return new ResourceUri();
    }

    /** Make any backslashes into forward slashes.
     */
    if (decoded.indexOf('\\') >= 0) {
      decoded = decoded.replace('\\', '/');
    }

    /** Ensure a leading '/'
     */
    if (!decoded.startsWith("/")) {
      decoded = "/" + decoded;
    }

    /** Remove all instances of '//'.
     */
    while (decoded.contains("//")) {
      decoded = decoded.replaceAll("//", "/");
    }
    /** Somewhere we may have /./ or /../
     */

    final StringTokenizer st = new StringTokenizer(decoded, "/");

    final ArrayList<String> al = new ArrayList<>();
    while (st.hasMoreTokens()) {
      final String s = st.nextToken();

      if (s.equals(".")) {
        // ignore
        continue;
      }

      if (s.equals("..")) {
        // Back up 1
        if (al.size() == 0) {
          // back too far
          return new ResourceUri();
        }

        al.remove(al.size() - 1);
        continue;
      }

      al.add(s);
    }

    final ResourceUri ruri = new ResourceUri();

    /** Reconstruct */
    for (final String s: al) {
      ruri.addPathElement(s);
    }

    return ruri;
  }

  /** ===================================================================
   *                   Output methods
   *  =================================================================== */

  protected void listResponse(final HttpServletResponse resp,
                              final List<TimezoneType> tzs) throws ServletException {
    try {
      resp.setContentType("application/json; charset=UTF-8");

      final TimezoneListType tzl = new TimezoneListType();

      tzl.setSynctoken(util.getDtstamp());

      if (tzl.getTimezones() == null) {
        tzl.setTimezones(new ArrayList<TimezoneType>());
      }
      tzl.getTimezones().addAll(tzs);

      writeJson(resp, tzl);
    } catch (final ServletException se) {
      throw se;
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }

  protected void errorResponse(final HttpServletResponse resp,
                               final int servletError,
                               final String errorCode,
                               final String description) throws ServletException {
    errorResponse(resp, servletError,
                  new ErrorResponseType(errorCode, description));
  }

  protected void errorResponse(final HttpServletResponse resp,
                               final int servletError,
                               final ErrorResponseType error) throws ServletException {
    resp.setStatus(servletError);
    writeJson(resp, error);
  }

  protected void writeJson(final HttpServletResponse resp,
                           final Object val) throws ServletException {
    try {
      mapper.writeValue(resp.getOutputStream(), val);
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  /** ===================================================================
   *                   Logging methods
   *  =================================================================== */

  /**
   * @return Logger
   */
  protected Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  protected void warn(final String msg) {
    getLogger().warn(msg);
  }

  protected void logIt(final String msg) {
    getLogger().info(msg);
  }

  protected void trace(final String msg) {
    getLogger().debug(msg);
  }
}
