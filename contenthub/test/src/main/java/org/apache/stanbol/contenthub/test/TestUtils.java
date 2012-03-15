package org.apache.stanbol.contenthub.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;

public class TestUtils {
	
	public static void submitDocumentToSolr(SolrServer solrServer, int documentCount,boolean addCons) throws StoreException, SolrServerException, IOException {

		SolrInputDocument doc = new SolrInputDocument();
        doc.addField(SolrFieldName.ID.toString(), TestVocabulary.attachedId);
        doc.addField(SolrFieldName.CONTENT.toString(), TestVocabulary.content);
        doc.addField(SolrFieldName.MIMETYPE.toString(), TestVocabulary.contentType);
        doc.addField(SolrFieldName.CREATIONDATE.toString(), TestVocabulary.creationDate);
        doc.addField(SolrFieldName.ENHANCEMENTCOUNT.toString(), 0);
        doc.addField(SolrFieldName.TITLE.toString(), TestVocabulary.title);
        
        if(addCons){
        	doc.addField("author"+SolrVocabulary.SOLR_DYNAMIC_FIELD_TEXT, TestVocabulary.consValues);
        }
		solrServer.add(doc);
		
		if(documentCount == 2){
	        doc.setField(SolrFieldName.ID.toString(), TestVocabulary.attachedId+"2");
	        doc.setField(SolrFieldName.TITLE.toString(), TestVocabulary.title+"2");
	        
	        solrServer.add(doc);
		}
		solrServer.commit();
	}
	
	public static SolrDocument getSolrDocument(SolrServer solrServer)
			throws StoreException, SolrServerException {

		SolrQuery query = new SolrQuery();
		String queryString = SolrFieldName.ID.toString() + ":\""
				+ TestVocabulary.attachedId
				+ "\"";
		query.setQuery(queryString);
		SolrDocumentList results = solrServer.query(query).getResults();
		if(results.size() > 0){
			return results.get(0);
		} else {
			return null;
		}
	}
	
	public static void deleteDocument(SolrServer solrServer, MGraph enhancementGraph) throws SolrServerException, IOException, StoreException{
        Iterator<Triple> it = enhancementGraph.filter(new UriRef(TestVocabulary.attachedId), null, null);
        List<Triple> willBeRemoved = new ArrayList<Triple>();
        while (it.hasNext()) {
            willBeRemoved.add(it.next());
        }
        enhancementGraph.removeAll(willBeRemoved);
        
        solrServer.deleteById(TestVocabulary.attachedId);
        solrServer.commit();
	}
}
