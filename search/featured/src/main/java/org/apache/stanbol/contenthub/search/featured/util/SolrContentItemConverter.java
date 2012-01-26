package org.apache.stanbol.contenthub.search.featured.util;

import org.apache.solr.common.SolrDocument;
import org.apache.stanbol.contenthub.search.featured.ResultantDocumentImpl;
import org.apache.stanbol.contenthub.servicesapi.search.featured.ResultantDocument;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;

public class SolrContentItemConverter {
    /**
     * This method converts a {@link SolrDocument} into a {@link HTMLContentItem}. Note currently, it ignores
     * its metadata produced after enhancement process and stored. Its constraints indexed in Solr are also
     * ignored as these items are only shown as a list in HTML interface.
     */
    public static ResultantDocument solrDocument2solrContentItem(SolrDocument solrDocument) {
        return solrDocument2solrContentItem(solrDocument, null);
    }
    
    public static ResultantDocument solrDocument2solrContentItem(SolrDocument solrDocument, String baseURI) {
        String id = getStringValueFromSolrField(solrDocument, SolrFieldName.ID.toString());
        String mimeType = getStringValueFromSolrField(solrDocument, SolrFieldName.MIMETYPE.toString());
        String title = getStringValueFromSolrField(solrDocument, SolrFieldName.TITLE.toString());
        long enhancementCount = (Long) solrDocument.getFieldValue(SolrFieldName.ENHANCEMENTCOUNT.toString());
        String dereferencableURI = baseURI != null ? (baseURI + "contenthub/store/content/" + id) : null;
        title = (title == null || title.trim().equals("") ? id : title);
        ResultantDocumentImpl resultantDocument = new ResultantDocumentImpl(id, dereferencableURI, mimeType,
                enhancementCount, title);
        return resultantDocument;
    }

    private static String getStringValueFromSolrField(SolrDocument solrDocument, String field) {
        Object result = solrDocument.getFieldValue(field);
        if (result != null) {
            return result.toString();
        }
        return "";
    }
}
