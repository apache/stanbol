package org.apache.stanbol.enhancer.engines.lucenefstlinking.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.common.SolrException;

/** See LUCENE-4541 or {@link org.apache.solr.response.transform.ValueSourceAugmenter}. */
public class ValueSourceAccessor {
  // implement FunctionValues ?
  private final List<AtomicReaderContext> readerContexts;
  private final FunctionValues[] docValuesArr;
  private final ValueSource valueSource;
  private final Map fContext;

  private int localId;
  private FunctionValues values;

  public ValueSourceAccessor(IndexSearcher searcher, ValueSource valueSource) {
    readerContexts = searcher.getIndexReader().leaves();
    this.valueSource = valueSource;
    docValuesArr = new FunctionValues[readerContexts.size()];
    fContext = ValueSource.newContext(searcher);
  }

  private void setState(int docid) {
    int idx = ReaderUtil.subIndex(docid, readerContexts);
    AtomicReaderContext rcontext = readerContexts.get(idx);
    values = docValuesArr[idx];
    if (values == null) {
      try {
        docValuesArr[idx] = values = valueSource.getValues(fContext, rcontext);
      } catch (IOException e) {
        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e);
      }
    }
    localId = docid - rcontext.docBase;
  }

  public Object objectVal(int docid) {
    setState(docid);
    return values.objectVal(localId);
  }
}