package org.apache.stanbol.contenthub.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeywordSearchManager;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(SlingAnnotationsTestRunner.class)
public class RelatedSearchTest {
/*
ClosureHelper.java
IndexingHelper.java
relatedKeywordSearch.search() will be tested
OntologyResourceSearch will be tested later
ReferencedSiteSearch will not be tested
*/
	
	@TestReference
	private RelatedKeywordSearchManager relatedKeywordSearchManager;
	
//	@TestReference
//	private RelatedKeywordSearch relatedKeywordSearch;
	
//	@Test
//	public void testGetRelatedKeywordsFromAllSources(){
//		
//	}
	
//	@Test
//	public void testGetRelatedKeywordsFromOntology() throws SearchException, EnhancementException, UnsupportedEncodingException {
//        
//		TripleCollection triples = new SimpleMGraph();
//		triples.add(new TripleImpl(new UriRef("http://dbpedia.org/resource/Paris"), new UriRef(
//				"http://www.apache.org/stanbol/cms#parentRef"), new UriRef(
//				"http://dbpedia.org/resource/France")));
//		
//		triples.add(new TripleImpl(new UriRef("http://dbpedia.org/resource/Paris"), new UriRef(
//				"rdf:type"), new UriRef(
//				"http://dbpedia.org/ontology/Place")));
//		
//		triples.add(new TripleImpl(new UriRef("http://dbpedia.org/resource/Paris"), new UriRef(
//				"rdfs:subClassOf"), new UriRef(
//				"http://dbpedia.org/resource/France")));
//		
//		tcManager.createMGraph(TestVocabulary.ontologyURI);
//		tcManager.getMGraph(TestVocabulary.ontologyURI).addAll(triples);
//
//		//TODO: check whether res contains TestVocabulary.resultTerm
//		SearchResult res = relatedKeywordSearchManager.getRelatedKeywordsFromOntology(
//				TestVocabulary.queryTerm, TestVocabulary.ontologyURI.getUnicodeString());
//		
//		
//		tcManager.deleteTripleCollection(TestVocabulary.ontologyURI);
//	}
	
//	@Test
//	public void testGetRelatedKeywordsFromReferencedSites(){
//		
//	}
	
	@Test
	public void testGetRelatedKeywordsFromWordnet() throws SearchException{
		//TODO: check wordnet is setted
		boolean isFinded = false;
		Map<String, Map<String, List<RelatedKeyword>>> relatedKeywordsMap = relatedKeywordSearchManager.getRelatedKeywordsFromWordnet(TestVocabulary.queryTerm).getRelatedKeywords();
		Map<String, List<RelatedKeyword>> relatedKeyword = relatedKeywordsMap.get(TestVocabulary.queryTerm);
		List<RelatedKeyword> values = relatedKeyword.get("Wordnet");
		for(RelatedKeyword value : values){
			if(value.getKeyword().contains(TestVocabulary.resultTerm)){
				isFinded = true;
			}
		}
		assertTrue(isFinded);
	}
}
