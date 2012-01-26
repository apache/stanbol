package org.apache.stanbol.contenthub.servicesapi.search.related;

import java.util.List;
import java.util.Map;

import org.apache.stanbol.contenthub.servicesapi.search.SearchException;

public interface RelatedKeywordSearch {

    Map<String,List<RelatedKeyword>> search(String keyword) throws SearchException;

    Map<String,List<RelatedKeyword>> search(String keyword, String ontologyURI) throws SearchException;
}
