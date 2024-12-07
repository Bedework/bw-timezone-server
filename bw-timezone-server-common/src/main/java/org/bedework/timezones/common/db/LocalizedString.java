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
package org.bedework.timezones.common.db;

import org.bedework.util.misc.ToString;
import org.bedework.util.misc.Util;

import java.util.Collection;

/**
 *.
 *  @version 1.0
 */
public class LocalizedString extends TzDbentity<LocalizedString> {
  private String lang;

  private String value;

  /** Constructor
   */
  public LocalizedString() {
    super();
  }

  /** Create a string by specifying all its fields
   *
   * @param lang        String language code
   * @param value       String value
   */
  public LocalizedString(final String lang,
                      final String value) {
    super();
    this.lang = lang;
    this.value = value;
  }

  /** Set the lang
   *
   * @param val    String lang
   */
  public void setLang(final String val) {
    lang = val;
  }

  /** Get the lang
   *
   * @return String   lang
   */
  public String getLang() {
    return lang;
  }

  /** Set the value
   *
   * @param val    String value
   */
  public void setValue(final String val) {
    value = val;
  }

  /** Get the value
   *
   *  @return String   value
   */
  public String getValue() {
    return value;
  }

  /* ====================================================================
   *                        Convenience methods
   * ==================================================================== */

  /** Search the collection for a string that matches the given language code.
   *
   * @param lang code
   * @param c collection of localized strings
   * @return LocalizedString or null if no strings.
   */
  protected static LocalizedString findLang(final String lang,
                                            final Collection<? extends LocalizedString> c) {
    if (c == null) {
      return null;
    }

    for (final LocalizedString s: c) {
      if (s.getLang().equals(lang)) {
        return s;
      }
    }

    return null;
  }

  /** Figure out what's different and update it. This should reduce the number
   * of spurious changes to the db.
   *
   * @param from localized string
   * @return true if we changed something.
   */
  public boolean update(final LocalizedString from) {
    boolean changed = false;

    if (!Util.equalsString(getLang(), from.getLang())) {
      setLang(from.getLang());
      changed = true;
    }

    if (!Util.equalsString(getValue(), from.getValue())) {
      setValue(from.getValue());
      changed = true;
    }

    return changed;
  }

  /** Check this is properly trimmed
   *
   * @return boolean true if changed
   */
  public boolean checkNulls() {
    boolean changed = false;

    String str = Util.checkNull(getLang());
    if (!Util.equalsString(str, getLang())) {
      setLang(str);
      changed = true;
    }

    str = Util.checkNull(getValue());
    if (!Util.equalsString(str, getValue())) {
      setValue(str);
      changed = true;
    }

    return changed;
  }

  /* ==============================================================
   *                        Object methods
   * ============================================================== */

  @Override
  public int compareTo(final LocalizedString that) {
    if (that == this) {
      return 0;
    }

    if (that == null) {
      return -1;
    }

    final int res = Util.cmpObjval(getLang(), that.getLang());

    if (res != 0) {
      return res;
    }

    return Util.cmpObjval(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    int hc = 7;

    if (getLang() != null) {
      hc *= getLang().hashCode();
    }

    if (getValue() != null) {
      hc *= getValue().hashCode();
    }

    return hc;
  }

  @Override
  protected void toStringSegment(final ToString ts) {
    super.toStringSegment(ts);
    ts.append("lang", getLang());
    ts.append("value", getValue());
  }

  @Override
  public Object clone() {
    return new LocalizedString(getLang(),
                        getValue());
  }
}
