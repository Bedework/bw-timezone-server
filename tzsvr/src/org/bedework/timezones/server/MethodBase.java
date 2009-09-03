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

import org.apache.log4j.Logger;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author douglm
 *
 */
public abstract class MethodBase {
  protected boolean debug;

  protected transient Logger log;

  /**
   * @param debug
   */
  public MethodBase(boolean debug) {
    this.debug = debug;
  }

  /**
   * @param req
   * @param resp
   * @param props
   * @throws ServletException
   */
  public abstract void doMethod(HttpServletRequest req,
                                HttpServletResponse resp,
                                Properties props)
        throws ServletException;

  /** Get the decoded and fixed resource URI
   *
   * @param req      Servlet request object
   * @return String  fixed up uri
   * @throws ServletException
   */
  public String getResourceUri(HttpServletRequest req)
      throws ServletException {
    String uri = req.getServletPath();

    if ((uri == null) || (uri.length() == 0)) {
      /* No path specified - set it to root. */
      uri = "/";
    }

    if (debug) {
      trace("uri: " + uri);
    }

    String resourceUri = fixPath(uri);

    if (debug) {
      trace("resourceUri: " + resourceUri);
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
  public static String fixPath(String path) throws ServletException {
    if (path == null) {
      return null;
    }

    String decoded;
    try {
      decoded = URLDecoder.decode(path, "UTF8");
    } catch (Throwable t) {
      throw new ServletException("bad path: " + path);
    }

    if (decoded == null) {
      return (null);
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
    while (decoded.indexOf("//") >= 0) {
      decoded = decoded.replaceAll("//", "/");
    }

    if (decoded.indexOf("/.") < 0) {
      return decoded;
    }

    /** Somewhere we may have /./ or /../
     */

    StringTokenizer st = new StringTokenizer(decoded, "/");

    ArrayList<String> al = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      String s = st.nextToken();

      if (s.equals(".")) {
        // ignore
      } else if (s.equals("..")) {
        // Back up 1
        if (al.size() == 0) {
          // back too far
          return null;
        }

        al.remove(al.size() - 1);
      } else {
        al.add(s);
      }
    }

    /** Reconstruct */
    StringBuilder sb = new StringBuilder();
    for (String s: al) {
      sb.append('/');
      sb.append(s);
    }

    return sb.toString();
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

  protected void debugMsg(String msg) {
    getLogger().debug(msg);
  }

  protected void error(Throwable t) {
    getLogger().error(this, t);
  }

  protected void error(String msg) {
    getLogger().error(msg);
  }

  protected void warn(String msg) {
    getLogger().warn(msg);
  }

  protected void logIt(String msg) {
    getLogger().info(msg);
  }

  protected void trace(String msg) {
    getLogger().debug(msg);
  }
}
