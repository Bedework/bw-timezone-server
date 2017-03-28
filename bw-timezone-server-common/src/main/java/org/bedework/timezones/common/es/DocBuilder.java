/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.timezones.common.es;

import org.bedework.timezones.common.db.LocalizedString;
import org.bedework.timezones.common.db.TzAlias;
import org.bedework.timezones.common.db.TzDbSpec;
import org.bedework.util.elasticsearch.DocBuilderBase;
import org.bedework.util.elasticsearch.EsDocInfo;
import org.bedework.util.indexing.IndexException;
import org.bedework.util.misc.Util;

import java.util.Set;

/**
 * User: mike
 * Date: 3/22/17
 * Time: 22:16
 */
public class DocBuilder extends DocBuilderBase {
  public static final String docTypeTzAlias = "tzalias";

  public static final String docTypeTzSpec = "tzspec";

  /**
   *
   */
  protected DocBuilder() throws IndexException {
  }

  /* Return the docinfo for the indexer */
  EsDocInfo makeDoc(final TzAlias ent) throws IndexException {
    try {
      startObject();

      makeField(Properties.aliasId, ent.getAliasId());
      makeField(Properties.targetIds, ent.getTargetIds());

      endObject();

      return makeDocInfo(docTypeTzAlias, 0, ent.getAliasId());
    } catch (final IndexException cfe) {
      throw cfe;
    } catch (final Throwable t) {
      throw new IndexException(t);
    }
  }

  /* Return the docinfo for the indexer */
  EsDocInfo makeDoc(final TzDbSpec ent) throws IndexException {
    try {
      startObject();

      makeField(Properties.name, ent.getName());
      makeField(Properties.etag, ent.getEtag());
      makeField(Properties.dtstamp, ent.getDtstamp());
      makeField(Properties.source, ent.getSource());
      makeField(Properties.active, ent.getActive());
      makeField(Properties.vtimezone, ent.getVtimezone());
      makeField(Properties.displayNames, ent.getDisplayNames());

      endObject();

      return makeDocInfo(docTypeTzSpec, 0, ent.getName());
    } catch (final IndexException cfe) {
      throw cfe;
    } catch (final Throwable t) {
      throw new IndexException(t);
    }
  }

  protected void makeField(final String id,
                           final Set<LocalizedString> val) 
          throws IndexException {
    if (Util.isEmpty(val)) {
      return;
    }

    startArray(id);

    for (final LocalizedString ls: val) {
      makeField(null, ls);
    }

    endArray();
  }

  /*
  private void indexGeo(final BwGeo val) throws IndexException {
    try {
      if (val == null) {
        return;
      }

      builder.startObject(getJname(PropertyInfoIndex.GEO));
      builder.field("lat", val.getLatitude().toPlainString());
      builder.field("lon", val.getLongitude().toPlainString());
      builder.endObject();
    } catch (IOException e) {
      throw new IndexException(e);
    }
  }*/

  private void makeField(final String id,
                         final LocalizedString val) throws IndexException {
    if (val == null) {
      return;
    }

    if (id == null) {
      startObject();
    } else {
      startObject(id);
    }
      
    makeField(Properties.lang, val.getLang());
    makeField(Properties.value, val.getValue());
    endObject();
  }
}
