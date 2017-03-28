/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.timezones.common.es;

import org.bedework.timezones.common.db.LocalizedString;
import org.bedework.timezones.common.db.TzAlias;
import org.bedework.timezones.common.db.TzDbSpec;
import org.bedework.util.elasticsearch.EntityBuilderBase;
import org.bedework.util.indexing.IndexException;
import org.bedework.util.misc.Util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * User: mike
 * Date: 3/22/17
 * Time: 21:31
 */
public class EntityBuilder extends EntityBuilderBase {
  /**
   * Constructor - 1 use per entity
   *
   * @param fields  map of fields from index
   * @param version of document
   */
  protected EntityBuilder(final Map<String, ?> fields,
                          final long version) throws IndexException {
    super(fields, version);
  }

  TzDbSpec makeSpec() throws IndexException {
    final TzDbSpec spec = new TzDbSpec();

    spec.setName(getString(Properties.name));
    spec.setEtag(getString(Properties.etag));
    spec.setDtstamp(getString(Properties.dtstamp));
    spec.setSource(getString(Properties.source));
    spec.setActive(getBool(Properties.active));
    spec.setVtimezone(getString(Properties.vtimezone));
    spec.setDisplayNames(getLocalizedStrings(Properties.displayNames));
    
    return spec;
  }

  TzAlias makeAlias() throws IndexException {
    final TzAlias alias = new TzAlias();

    alias.setAliasId(getString(Properties.aliasId));
    alias.setTargetIds(getStringList(Properties.targetIds));

    return alias;
  }
  
  private Set<LocalizedString> getLocalizedStrings(final String id) throws IndexException {
    final Collection<Object> vals = getFieldValues(id);
    if (Util.isEmpty(vals)) {
      return null;
    }
    
    final Set<LocalizedString> lss = new TreeSet<>();

    for (final Object o: vals) {
      pushFields(o);
      try {
        final String uid = getString(Properties.lang);
        lss.add(new LocalizedString(getString(Properties.lang),
                                    getString(Properties.value)));
      } finally {
        popFields();
      }
    }
    
    return lss;
  }
}
