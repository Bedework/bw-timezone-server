/* **********************************************************************
    Copyright 2009 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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
