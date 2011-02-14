/* **********************************************************************
    Copyright 2010 Rensselaer Polytechnic Institute. All worldwide rights reserved.

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

import edu.rpi.sss.util.OptionsException;
import edu.rpi.sss.util.OptionsFactory;
import edu.rpi.sss.util.OptionsI;

/** Obtain an options object.
 *
 */
public class TzsvrOptionsFactory extends OptionsFactory {
  /* Options class if we've already been called */
  private static volatile OptionsI opts;

  /** Location of the options file */
  private static final String optionsFile = "/properties/tzsvr/options.xml";

  private static final String outerTag = "bedework-options";

  /** Global properties have this prefix.
   */
  public static final String globalPrefix = "org.bedework.global.";

  /** App properties have this prefix.
   */
  public static final String appPrefix = "org.bedework.app.";

  /** Obtain and initialise an options object.
   *
   * @param debug
   * @return OptionsI
   * @throws OptionsException
   */
  public static OptionsI getOptions(final boolean debug) throws OptionsException {
    if (opts != null) {
      return opts;
    }

    opts = getOptions(globalPrefix, appPrefix, optionsFile, outerTag);
    return opts;
  }
}
