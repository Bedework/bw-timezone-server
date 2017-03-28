/* ********************************************************************
    Appropriate copyright notice
*/
package org.bedework.timezones.common.es;

import org.bedework.timezones.common.TzConfig;
import org.bedework.timezones.common.db.TzAlias;
import org.bedework.timezones.common.db.TzDbSpec;
import org.bedework.util.elasticsearch.DocBuilderBase.UpdateInfo;
import org.bedework.util.elasticsearch.EsDocInfo;
import org.bedework.util.elasticsearch.EsUtil;
import org.bedework.util.indexing.IndexException;
import org.bedework.util.misc.Logged;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;

/**
 * User: mike
 * Date: 3/22/17
 * Time: 22:57
 */
public class Indexer extends Logged {
  private final EsUtil utils;
  private final TzConfig config;
  
  private Client client;
  
  public Indexer(final TzConfig config) throws IndexException {
    utils = new EsUtil(config);
    this.config = config;
  }
  
  public void index(final Object val) throws IndexException {
    EsDocInfo edi = null;
    final DocBuilder db = new DocBuilder();

    if (val instanceof UpdateInfo) {
      edi = db.makeDoc((UpdateInfo)val);
    }

    if (val instanceof TzAlias) {
      edi = db.makeDoc((TzAlias)val);
    }

    if (val instanceof TzDbSpec) {
      edi = db.makeDoc((TzDbSpec)val);
    }

    if (edi == null) {
      throw new IndexException("Unknown class: " + val.getClass());
    }

    IndexResponse resp = utils.indexDoc(edi, config.getIndexName());

    if (debug) {
      if (resp == null) {
        debug("IndexResponse: resp=null");
      } else {
        debug("IndexResponse: index=" + resp.getIndex() +
                      " id=" + resp.getId() +
                      " type=" + resp.getType() +
                      " version=" + resp.getVersion());
      }
    }
  }
  
  private Client getClient() throws IndexException {
    if (client != null) {
      return client;
    }
    
    client = utils.getClient();
    return client;
  }
}
