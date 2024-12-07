/*
#    Copyright (c) 2007-2013 Cyrus Daboo. All rights reserved.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
*/
package org.bedework.timezones.convert;

import org.bedework.util.args.Args;
import org.bedework.util.jmx.InfoLines;
import org.bedework.util.misc.Util;

/** Converts the data obtainable from IANA into VTIMEZONE format.
 * Data is available from IANA via the FTP server. Latest data at
 * <code>ftp://ftp.iana.org/tz/tzdata-latest.tar.gz</code>
 *
 * <p></p>For a specific version replace 'latest' with the version, e.g.
 * <code>ftp://ftp.iana.org/tz/releases/tzcode2012e.tar.gz</code>
 *
 * <p>This code is a rewrite of that written by Cyrus Daboo and
 * available from <a href="http://calendarserver.org">http://calendarserver.org</a> as part of their open source
 * calendar server.</p>
 *
 */
class Tzconvert {
  static void usage(final String error_msg) {
    if (error_msg != null) {
      Utils.print(error_msg);
    }

    Utils.print("""
      Usage: tzconvert [options] [DIR]
      Options:
          -h            Print this help and exit
          --prodid      PROD-ID string to use
          --start       Start year
          --end         End year
          --root        Directory containing an \
      Olson tzdata directory to read, also
                   where zoneinfo data will be written
          --generate    true/false
          --comparewith Directory containing an " +
       Olson tzdata directory to  compare with
          --tzserver    Server providing tz data to compare with" +
          --aliases     Path to property file defining extra aliases
          --source      Value to be supplied in info.properties \
      e.g. IANA 2014d
      
      Description:
          This utility convert Olson-style timezone data in iCalendar.
          VTIMEZONE objects, one .ics file per-timezone.
      """);

    if (error_msg != null) {
      throw new RuntimeException(error_msg);
    }

    System.exit(0);
  }

  static boolean processArgs(final Args args,
                             final TzConvertParamsI params) {
    if (args == null) {
      return true;
    }

    try {
      while (args.more()) {
        if (args.ifMatch("")) {
          continue;
        }

        if (args.ifMatch("-h")) {
          usage(null);
        } else if (args.ifMatch("--prodid")) {
          params.setProdid(args.next());
        } else if (args.ifMatch("--root")) {
          params.setRootdir(args.next());
        } else if (args.ifMatch("--start")) {
          params.setStartYear(Integer.parseInt(args.next()));
        } else if (args.ifMatch("--end")) {
          params.setEndYear(Integer.parseInt(args.next()));
        } else if (args.ifMatch("--generate")) {
          params.setGenerate(Boolean.parseBoolean(args.next()));
        } else if (args.ifMatch("--tzserver")) {
          params.setTzServerUri(args.next());
          params.setCompare(true);
        } else if (args.ifMatch("--comparewith")) {
          params.setCompareWithPath(args.next());
          params.setCompare(true);
        } else if (args.ifMatch("--verboseid")) {
          params.setVerboseId(args.next());
        } else if (args.ifMatch("--aliases")) {
          params.setAliasesPath(args.next());
        } else if (args.ifMatch("--source")) {
          params.setSource(args.next());
        } else {
          usage("Unrecognized option: " + args.current());
          return false;
        }
      }
    } catch (final Exception e) {
        throw new RuntimeException(e);
    }

    return true;
  }

  public static void main(final String[] args) {
    final TzConvertParams params = new TzConvertParams();

    try {
      if (!processArgs(new Args(args), params)) {
        return;
      }

      final Processor proc = new Processor(params);

      proc.parse();

      if (params.getGenerate()) {
        proc.generateZoneinfoFiles(Util.buildPath(true,
                                                  params.getRootdir(),
                                                  "/", "zoneinfo"),
                                   true);  // doLinks
      }

      final InfoLines msgs = new InfoLines();

      if (params.getCompare()) {
        proc.compare(msgs);
      }

      for (final String msg: msgs) {
        System.out.print(msg);
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }
}
