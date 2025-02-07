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

import org.bedework.timezones.convert.TzCnvSvc;
import org.bedework.timezones.service.TzConf;
import org.bedework.util.jmx.ConfBase;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.servlet.HttpServletUtils;

import java.util.Enumeration;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/** Serve up timezone information. This server is an intermediate step towards
 * a real global timezone service being promoted in CalConnect
 *
 * <p>This server provides timezones stored in a zip file at a given remote
 * location accessible over http. This allows easy replacement of timezone
 * definitions.
 *
 * @author Mike Douglass
 *
 */
public class TzServer extends HttpServlet
        implements Logged, HttpSessionListener, ServletContextListener {
  protected boolean dumpContent;

  @Override
  public void init(final ServletConfig config) throws ServletException {
    try {
      super.init(config);

      dumpContent = "true".equals(config.getInitParameter("dumpContent"));
    } catch (final ServletException se) {
      throw se;
    } catch (final Throwable t) {
      throw new ServletException(t);
    }
  }

  @Override
  protected void service(final HttpServletRequest req,
                         final HttpServletResponse resp) throws ServletException {
    try {
      final String methodName = req.getMethod();

      if (debug()) {
        debug("entry: " + methodName);
        dumpRequest(req);
      }

      switch (methodName) {
        case "OPTIONS":
          new OptionsMethod().doMethod(req, resp);
          break;
        case "GET":
          new GetMethod().doMethod(req, resp);
          break;
        case "POST":
          new PostMethod().doMethod(req, resp);
          break;

        default:
          resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      }
    } finally {
      /* We're stateless - toss away any session */
      try {
        final HttpSession sess = req.getSession(false);
        if (sess != null) {
          sess.invalidate();
        }
      } catch (final Throwable ignored) {}
    }
  }

  /** Debug
   *
   * @param req http request
   */
  public void dumpRequest(final HttpServletRequest req) {
    try {
      String title = "Request headers";

      debug(title);

      HttpServletUtils.dumpHeaders(req);

      final Enumeration<String> names = req.getParameterNames();

      title = "Request parameters";

      debug(title + " - global info and uris");
      debug("getRemoteAddr = " + req.getRemoteAddr());
      debug("getRequestURI = " + req.getRequestURI());
      debug("getRemoteUser = " + req.getRemoteUser());
      debug("getRequestedSessionId = " + req.getRequestedSessionId());
      debug("HttpUtils.getRequestURL(req) = " + req.getRequestURL());
      debug("contextPath=" + req.getContextPath());
      debug("query=" + req.getQueryString());
      debug("contentlen=" + req.getContentLength());
      debug("request=" + req);
      debug("parameters:");

      debug(title);

      while (names.hasMoreElements()) {
        final String key = names.nextElement();
        final String val = req.getParameter(key);
        debug("  " + key + " = \"" + val + "\"");
      }
    } catch (final Throwable ignored) {
    }
  }

  class Configurator extends ConfBase {
    TzConf tzConf;

    TzCnvSvc cnv;

    public Configurator() {
      super("org.bedework.timezones:service=TzSvr",
            (String)null,
            null);
    }

    @Override
    public String loadConfig() {
      return null;
    }

    @Override
    public void start() {
      try {
        getManagementContext().start();

        tzConf = new TzConf();
        register("tzConf", "tzConf", tzConf);
        tzConf.loadConfig();

        cnv = new TzCnvSvc();
        register("tzCnvSvc", "tzCnvSvc", cnv);
        cnv.loadConfig();
      } catch (final Throwable t){
        t.printStackTrace();
      }
    }

    @Override
    public void stop() {
      try {
        getManagementContext().stop();
      } catch (final Throwable t){
        t.printStackTrace();
      }
    }
  }

  private final Configurator conf = new Configurator();

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    conf.start();
  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    conf.stop();
  }

  @Override
  public void sessionCreated(final HttpSessionEvent se) {
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
   */
  @Override
  public void sessionDestroyed(final HttpSessionEvent se) {
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private final BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
