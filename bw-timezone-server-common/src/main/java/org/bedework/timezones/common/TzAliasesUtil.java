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
package org.bedework.timezones.common;

import org.bedework.util.args.Args;
import org.bedework.util.logging.BwLogger;
import org.bedework.util.logging.Logged;
import org.bedework.util.timezones.model.aliases.AliasInfoType;
import org.bedework.util.timezones.model.aliases.TimezoneAliasInfoType;
import org.bedework.util.timezones.model.aliases.TimezonesAliasInfoType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

/** Common code for timezones.
 *
 *   @author Mike Douglass
 */
public class TzAliasesUtil implements Logged {
  private String infileName;

  private String outfileName;

  private String propsfileName;

  private URI currentURI;

  private String currentURIStr;

  /**
   * @throws TzException
   */
  private TzAliasesUtil() throws TzException {
  }

  /** Merge properties into the given object. If that object is null a new one
   * will be created.
   *
   * @param tzsai
   * @param p
   * @param source a URI representing the source of the information
   * @return merged object.
   */
  public TimezonesAliasInfoType mergeProperties(final TimezonesAliasInfoType tzsai,
                                                final Properties p,
                                                final URI source) {
    if (source == null) {
      currentURIStr = null;
    } else {
      currentURI = source;
      currentURIStr = currentURI.toString();
    }

    TimezonesAliasInfoType tzsaires = tzsai;
    if (tzsaires == null) {
      tzsaires = new TimezonesAliasInfoType();
    }

    List<TimezoneAliasInfoType> atai = tzsaires.getTimezoneAliasInfo();
    if (atai == null) {
      atai = new ArrayList<TimezoneAliasInfoType>();
      tzsaires.setTimezoneAliasInfo(atai);
    }

    Map<String, TimezoneAliasInfoType> tzmap = getTzMap(atai);

    for (Object o: p.keySet()) {
      String alias = (String)o;

      String tzid = p.getProperty(alias);

      TimezoneAliasInfoType tai = tzmap.get(tzid);

      if (tai == null) {
        tai = new TimezoneAliasInfoType();
        tai.setTzid(tzid);
        tzmap.put(tzid, tai);
        atai.add(tai);
      }

      addAlias(tai, tzid, alias);
    }

    return tzsaires;
  }

  /**
   * @return true if processing went ok
   * @throws Throwable
   */
  public boolean process() throws Throwable {
    TimezonesAliasInfoType tai = null;

    if (infileName != null) {
      InputStream in = new FileInputStream(new File(infileName));

      tai = (TimezonesAliasInfoType)unmarshal(in);
    }

    if (propsfileName == null) {
      error("Must provide properties file");
      usage();
      return false;
    }

    InputStream propStream = new FileInputStream(new File(propsfileName));

    Properties props = new Properties();

    props.load(propStream);

    OutputStream out;

    if (outfileName == null) {
      out = System.out;
    } else {
      out = new FileOutputStream(new File(outfileName));
    }

    tai = mergeProperties(tai, props, null);

    marshal(tai, out);

    out.close();

    return true;
  }

  /** Main
   *
   * @param args
   */
  public static void main(final String[] args) {
    TzAliasesUtil tzau = null;

    try {
      tzau = new TzAliasesUtil();

      if (!tzau.processArgs(new Args(args))) {
        return;
      }

      tzau.process();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  /* ====================================================================
   *                   Private methods
   * ==================================================================== */

  @SuppressWarnings("unchecked")
  protected void marshal(final TimezonesAliasInfoType tai,
                         final OutputStream out) throws Throwable {
    JAXBContext contextObj = JAXBContext.newInstance(tai.getClass());

    Marshaller marshaller = contextObj.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

    //ObjectFactory of = new ObjectFactory();
    marshaller.marshal(new JAXBElement(new QName("urn:ietf:params:xml:ns:timezone-service-aliases",
                                                 "TimezonesAliasInfoType"),
                                                 tai.getClass(), tai), out);
  }

  protected Object unmarshal(final InputStream in) throws Throwable {
    JAXBContext jc = JAXBContext.newInstance(TimezonesAliasInfoType.class);
    Unmarshaller u = jc.createUnmarshaller();
    return u.unmarshal(in);
  }

  private boolean processArgs(final Args args) throws Throwable {
    if (args == null) {
      return true;
    }

    while (args.more()) {
      if (args.ifMatch("")) {
        continue;
      }

      if (args.ifMatch("-f")) {
        infileName = args.next();
      } else if (args.ifMatch("-o")) {
        outfileName = args.next();
      } else if (args.ifMatch("-p")) {
        propsfileName = args.next();
      } else {
        error("Illegal argument: " + args.current());
        usage();
        return false;
      }
    }

    return true;
  }

  private void usage() {
    System.out.println("Usage:");
    System.out.println("args   -f <filename>");
    System.out.println("            specify file containing xml data");
    System.out.println("       -p <filename>");
    System.out.println("            specify file containing properties");
    System.out.println("       -o <filename>");
    System.out.println("            specify file for XML output");
    System.out.println("");
  }

  private Map<String, TimezoneAliasInfoType> getTzMap(final List<TimezoneAliasInfoType> atai) {
    Map<String, TimezoneAliasInfoType> tzmap =
        new HashMap<String, TimezoneAliasInfoType>();

    if (atai == null) {
      return tzmap;
    }

    for (TimezoneAliasInfoType tai: atai) {
      tzmap.put(tai.getTzid(), tai);
    }

    return tzmap;
  }

  private void addAlias(final TimezoneAliasInfoType tai,
                        final String tzid,
                        final String alias) {
    List<AliasInfoType> aai = tai.getAliases();

    if (aai == null) {
      aai = new ArrayList<AliasInfoType>();
      tai.setAliases(aai);
    }

    for (AliasInfoType ai: aai) {
      if (ai.getAlias().equals(alias)) {
        // Already there
        return;
      }
    }

    // Not found
    AliasInfoType ai = new AliasInfoType();
    aai.add(ai);

    ai.setAlias(alias);
    ai.setSource(currentURIStr);
  }

  /* ====================================================================
   *                   Logged methods
   * ==================================================================== */

  private BwLogger logger = new BwLogger();

  @Override
  public BwLogger getLogger() {
    if ((logger.getLoggedClass() == null) && (logger.getLoggedName() == null)) {
      logger.setLoggedClass(getClass());
    }

    return logger;
  }
}
