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

import ietf.params.xml.ns.timezone_service.ObservanceType;
import ietf.params.xml.ns.timezone_service.TimezonesType;
import ietf.params.xml.ns.timezone_service.TzdataType;

/** Allows us to cache expansions
 *
 * @author douglm
 */
public class ExpandedMapEntry {
  private String etag;
  private TimezonesType tzs;

  /**
   * @param etag
   * @param tzs
   */
  public ExpandedMapEntry(final String etag,
                          final TimezonesType tzs) {
    this.etag = etag;
    this.tzs = tzs;
  }

  /**
   * @return etag
   */
  public String getEtag() {
    return etag;
  }

  /**
   * @return tzs
   */
  public TimezonesType getTzs() {
    return tzs;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");

    sb.append(", etag=");
    sb.append(getEtag());
    sb.append(",\n   tzs=TimezonesType{");
    sb.append(", dtstamp=");
    sb.append(tzs.getDtstamp().toXMLFormat());

    for (TzdataType tzd: tzs.getTzdata()){
      sb.append(",\n       TzdataType{tzid=");
      sb.append(tzd.getTzid());

      for (ObservanceType ot: tzd.getObservance()) {
        sb.append(",\n      {name=");
        sb.append(ot.getName());
        sb.append(", onset=");
        sb.append(ot.getOnset().toXMLFormat());
        sb.append(", offset-from=");
        sb.append(ot.getUtcOffsetFrom());
        sb.append(", offset-to=");
        sb.append(ot.getUtcOffsetTo());
        sb.append("}");
      }
    }

    sb.append("}");

    return sb.toString();
  }
}
