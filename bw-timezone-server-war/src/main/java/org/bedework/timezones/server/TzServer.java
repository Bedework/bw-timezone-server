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

import org.bedework.timezones.service.TzConf;

import edu.rpi.cmt.jmx.ConfBase;
import edu.rpi.sss.util.servlets.HttpServletUtils;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

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
        implements HttpSessionListener, ServletContextListener {
  private boolean debug;

  protected boolean dumpContent;

  protected transient Logger log;

  @Override
  public void init(final ServletConfig config) throws ServletException {
    try {
      super.init(config);

      debug = getLogger().isDebugEnabled();

      dumpContent = "true".equals(config.getInitParameter("dumpContent"));
    } catch (ServletException se) {
      throw se;
    } catch (Throwable t) {
      throw new ServletException(t);
    }
  }

  @Override
  protected void service(final HttpServletRequest req,
                         final HttpServletResponse resp) throws ServletException, IOException {
    try {
      String methodName = req.getMethod();

      if (debug) {
        debugMsg("entry: " + methodName);
        dumpRequest(req);
      }

      if (methodName.equals("OPTIONS")) {
        new OptionsMethod(debug).doMethod(req, resp);
      } else if (methodName.equals("GET")) {
        new GetMethod(debug).doMethod(req, resp);
      } else if (methodName.equals("POST")) {
        new PostMethod(debug).doMethod(req, resp);
      } else {

      }
    } finally {
      /* We're stateless - toss away any session */
      try {
        HttpSession sess = req.getSession(false);
        if (sess != null) {
          sess.invalidate();
        }
      } catch (Throwable t) {}
    }
  }

  /** Debug
   *
   * @param req
   */
  @SuppressWarnings("unchecked")
  public void dumpRequest(final HttpServletRequest req) {
    Logger log = getLogger();

    try {
      Enumeration<String> names = req.getHeaderNames();

      String title = "Request headers";

      log.debug(title);

      HttpServletUtils.dumpHeaders(req, log);

      names = req.getParameterNames();

      title = "Request parameters";

      log.debug(title + " - global info and uris");
      log.debug("getRemoteAddr = " + req.getRemoteAddr());
      log.debug("getRequestURI = " + req.getRequestURI());
      log.debug("getRemoteUser = " + req.getRemoteUser());
      log.debug("getRequestedSessionId = " + req.getRequestedSessionId());
      log.debug("HttpUtils.getRequestURL(req) = " + req.getRequestURL());
      log.debug("contextPath=" + req.getContextPath());
      log.debug("query=" + req.getQueryString());
      log.debug("contentlen=" + req.getContentLength());
      log.debug("request=" + req);
      log.debug("parameters:");

      log.debug(title);

      while (names.hasMoreElements()) {
        String key = names.nextElement();
        String val = req.getParameter(key);
        log.debug("  " + key + " = \"" + val + "\"");
      }
    } catch (Throwable t) {
    }
  }

  class Configurator extends ConfBase {
    TzConf tzConf;

    public Configurator() {
      super("org.bedework.timezones:service=TzSvr");
    }

    void start() {
      try {
        getManagementContext().start();

        tzConf = new TzConf();
        register("tzConf", "tzConf", tzConf);
        tzConf.loadConfig();
      } catch (Throwable t){
        t.printStackTrace();
      }
    }

    void stop() {
      try {
        getManagementContext().stop();
      } catch (Throwable t){
        t.printStackTrace();
      }
    }
  }

  private Configurator conf = new Configurator();

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    conf.start();
  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    conf.stop();
  }

  /**
   * @return Logger
   */
  public Logger getLogger() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  /** Debug
   *
   * @param msg
   */
  public void debugMsg(final String msg) {
    getLogger().debug(msg);
  }

  /** Info messages
   *
   * @param msg
   */
  public void logIt(final String msg) {
    getLogger().info(msg);
  }

  protected void error(final String msg) {
    getLogger().error(msg);
  }

  protected void error(final Throwable t) {
    getLogger().error(this, t);
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
   */
  @Override
  public void sessionCreated(final HttpSessionEvent se) {
  }

  /* (non-Javadoc)
   * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
   */
  @Override
  public void sessionDestroyed(final HttpSessionEvent se) {
  }
}
