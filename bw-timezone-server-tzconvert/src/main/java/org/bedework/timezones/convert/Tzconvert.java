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

/*
from __future__ import with_statement

from difflib import unified_diff
from pycalendar.icalendar.calendar import Calendar
import cStringIO as StringIO
import getopt
import os
import rule
import sys
import zone
*/

import org.bedework.util.args.Args;
import org.bedework.util.jmx.InfoLines;
import org.bedework.util.misc.Util;

/**
 *
 */
class Tzconvert {
  static void usage(final String error_msg) {
    if (error_msg != null) {
      Utils.print(error_msg);
    }

    Utils.print("Usage: tzconvert [options] [DIR]\n" +
                        "Options:\n" +
                        "    -h            Print this help and exit\n" +
                        "    --prodid      PROD-ID string to use\n" +
                        "    --start       Start year\n" +
                        "    --end         End year\n" +
                        "\n" +
                        "Arguments:\n" +
                        "    DIR      Directory containing an Olson tzdata directory to read, also\n" +
                        "             where zoneinfo data will be written\n" +
                        "\n" +
                        "Description:\n" +
                        "    This utility convert Olson-style timezone data in iCalendar.\n" +
                        "    VTIMEZONE objects, one .ics file per-timezone.\n");

    if (error_msg != null) {
      throw new RuntimeException(error_msg);
    }

    System.exit(0);
  }

  static boolean processArgs(final Args args,
                             final Processor proc) throws Throwable {
    if (args == null) {
      return true;
    }

    while (args.more()) {
      if (args.ifMatch("")) {
        continue;
      }

      if (args.ifMatch("-h")) {
        usage(null);
      } else if (args.ifMatch("--prodid")) {
        proc.setProdid(args.next());
      } else if (args.ifMatch("--root")) {
        proc.setRootdir(args.next());
      } else if (args.ifMatch("--start")) {
        proc.setStartYear(Integer.valueOf(args.next()));
      } else if (args.ifMatch("--end")) {
        proc.setEndYear(Integer.valueOf(args.next()));
      } else if (args.ifMatch("--generate")) {
        proc.setGenerate(Boolean.valueOf(args.next()));
      } else if (args.ifMatch("--tzserver")) {
        proc.setTzServerUri(args.next());
      } else if (args.ifMatch("--comparewith")) {
        proc.setCompareWithPath(args.next());
      } else if (args.ifMatch("--compare")) {
        proc.setCompare(Boolean.valueOf(args.next()));
      } else if (args.ifMatch("--aliases")) {
        proc.setAliasesPath(args.next());
      } else if (args.ifMatch("--source")) {
        proc.setSource(args.next());
      } else {
        usage("Unrecognized option: " + args.current());
        return false;
      }
    }

    return true;
  }

  public static void main(final String[] args) {
    final Processor proc = new Processor();

    try {
      if (!processArgs(new Args(args), proc)) {
        return;
      }

      //Calendar.sProdID = prodid

      proc.parse();

      if (proc.getGenerate()) {
        proc.generateZoneinfoFiles(Util.buildPath(true,
                                                  proc.getRootdir(),
                                                  "/", "zoneinfo"),
                                   true);  // doLinks
      }

      final InfoLines msgs = new InfoLines();

      if (proc.getCompare()) {
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
