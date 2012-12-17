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

import edu.rpi.sss.util.ToString;

/**<pre>
     A link line has the form

          Link  LINK-FROM        LINK-TO

     For example:

          Link  Europe/Istanbul  Asia/Istanbul

     The LINK-FROM field should appear as the NAME field in some
     zone line; the LINK-TO field is used as an alternate name
     for that zone.

</pre>
 */
public class LinkData extends Data {
  private String from;

  private String to;

  /**
   * @return from
   */
  public String getFrom() {
    return from;
  }

  /**
   * @return to
   */
  public String getTo() {
    return to;
  }

  /** Create a LinkData object based on the input String or throw an exception
   *
   * @param val
   * @return LinkData object
   * @throws TzdataException for invalid input
   */
  public static LinkData fromString(final String val) throws TzdataException {
    if (val == null) {
      return null;
    }

    String[] fields = tokenize(val);

    if (fields.length != 3) {
      throw new TzdataException("Bad Link - incorrect number of fields");
    }

    if (!fields[0].equals("Link")) {
      throw new TzdataException("Bad Link - not Link line");
    }

    LinkData l = new LinkData();

    l.from = fields[1];
    l.to = fields[2];

    return l;
  }

  @Override
  public String toString() {
    ToString ts = new ToString(this);

    ts.append("from", getFrom());
    ts.append("to", getTo());

    return ts.toString();
  }
}
