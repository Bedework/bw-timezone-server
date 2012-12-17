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
package org.bedework.tzdata.olson;

import edu.rpi.sss.util.Args;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utility to process Olson tz data.
 *
 *
 * @author douglm
 */
public class TzUtil {
  private transient Logger log;

  private boolean debug;

  private String infileName;

  private Map<String, List<ZoneData>> zones = new HashMap<String, List<ZoneData>>();

  private Map<String, List<RuleData>> rules = new HashMap<String, List<RuleData>>();

  private Map<String, List<LinkData>> links = new HashMap<String, List<LinkData>>();

  private boolean zoneContinues;
  private String currentZone;

  TzUtil()  throws Throwable {
  }

  String[] dataFiles = {
     "africa",
     "antarctica",
     "asia",
     "australasia",
     "europe",
     "northamerica",
     "southamerica"
  };

  void process() throws Throwable {
    for (String name: dataFiles) {
      info("processing " + name);

      processFile(name);
    }

    info(" zones: " + zones.keySet().size());
    info(" rules: " + rules.keySet().size());
    info(" links: " + links.keySet().size());
  }

  void processFile(final String name) throws Throwable {
    InputStream is;

    if (infileName != null) {
      is = new FileInputStream(infileName.trim() + "/" + name);
    } else {
      is = System.in;
    }

    LineNumberReader lis = new LineNumberReader(new InputStreamReader(is));

    for (;;) {
      String ln = lis.readLine();

      if (ln == null) {
        break;
      }

      ln = ln.trim();

      if (ln.length() == 0) {
        continue;
      }

      if (ln.startsWith("#")) {
        continue;
      }

      int pos = ln.indexOf("#");
      if (pos > 0) {
        ln = ln.substring(0, pos);
      }

      if (!processLine(ln)) {
        error(ln);
        error("At line " + lis.getLineNumber());
      }
    }
  }

  private boolean processLine(final String ln) throws Throwable {
    try {
      if (zoneContinues || (ln.startsWith("Zone"))) {
        ZoneData z = ZoneData.fromString(ln, zoneContinues);
        if (!zoneContinues) {
          currentZone = z.getName();
        }

        List<ZoneData> zs = zones.get(currentZone);
        if (zs == null) {
          if (zoneContinues) {
            error("No zone with name " + currentZone);
            return false;
          }

          zs = new ArrayList<ZoneData>();
          zones.put(currentZone, zs);
        }

        zs.add(z);
        zoneContinues = z.getUntilYear() != null;
        return true;
      }

      if (ln.startsWith("Rule")) {
        RuleData r = RuleData.fromString(ln);

        List<RuleData> rs = rules.get(r.getName());
        if (rs == null) {
          rs = new ArrayList<RuleData>();
          rules.put(r.getName(), rs);
        }

        rs.add(r);
        return true;
      }

      if (ln.startsWith("Link")) {
        LinkData l = LinkData.fromString(ln);

        List<LinkData> ls = links.get(l.getFrom());
        if (ls == null) {
          ls = new ArrayList<LinkData>();
          links.put(l.getFrom(), ls);
        }

        ls.add(l);
        return true;
      }

      return false;
    } catch (TzdataException tzde) {
      error(tzde.getMessage());
      return false;
    } catch (Throwable t) {
      error(t);
      return false;
    }
  }

  boolean processArgs(final Args args) throws Throwable {
    if (args == null) {
      return true;
    }

    while (args.more()) {
      if (args.ifMatch("")) {
        continue;
      }

      if (args.ifMatch("-debug")) {
        debug = true;
      } else if (args.ifMatch("-ndebug")) {
        debug = false;
      } else if (args.ifMatch("-f")) {
        infileName = args.next();
      } else {
        error("Illegal argument: " + args.current());
        usage();
        return false;
      }
    }

    return true;
  }

  void usage() {
    System.out.println("Usage:");
    System.out.println("args   -debug");
    System.out.println("       -ndebug");
    System.out.println("       -f <filename>");
    System.out.println("            specify file containing data");
    System.out.println("");
  }

  protected Logger getLog() {
    if (log == null) {
      log = Logger.getLogger(this.getClass());
    }

    return log;
  }

  protected void info(final String msg) {
    getLog().info(msg);
  }

  protected void error(final String msg) {
    getLog().error(msg);
  }

  protected void error(final Throwable t) {
    getLog().error(this, t);
  }

  protected void trace(final String msg) {
    getLog().debug(msg);
  }

  /** Main
   *
   * @param args
   */
  public static void main(final String[] args) {
    TzUtil mc = null;

    try {
      mc = new TzUtil();

      if (!mc.processArgs(new Args(args))) {
        return;
      }

      mc.process();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
