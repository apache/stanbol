/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.stanbol.contenthub.core.search;

import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.jena.facade.JenaGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.core.search.execution.SearchContextImpl;
import org.apache.stanbol.contenthub.core.utils.IndexingUtil;
import org.apache.stanbol.contenthub.servicesapi.search.Search;
import org.apache.stanbol.contenthub.servicesapi.search.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.search.processor.SearchProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * 
 * @author anil.sinaci
 * 
 */
@Component
@Service
public class SearchImpl implements Search {

    private final Logger logger = LoggerFactory.getLogger(SearchImpl.class);

    @Reference
    private TcManager tcManager;

    @Reference
    private SearchProcessor searchProcessor;

    @Override
    public SearchResult search(String keyword) {
        return search(keyword, null, null, null);
    }

    @Override
    public SearchResult search(String keyword, String ontologyURI) {
        return search(keyword, ontologyURI, null, null);
    }

    @Override
    public SearchResult search(String keyword, List<String> allowedEngines) {
        return search(keyword, null, allowedEngines, null);
    }

    @Override
    public SearchResult search(String keyword, Map<String,List<Object>> facets) {
        return search(keyword, null, null, facets);
    }

    @Override
    public SearchResult search(String keyword, String ontologyURI, List<String> allowedEngines) {
        return search(keyword, ontologyURI, allowedEngines, null);
    }

    @Override
    public SearchResult search(String keyword, String ontologyURI, Map<String,List<Object>> facets) {
        return search(keyword, ontologyURI, null, facets);
    }

    @Override
    public SearchResult search(String keyword, List<String> allowedEngines, Map<String,List<Object>> facets) {
        return search(keyword, null, allowedEngines, facets);
    }

    @Override
    public SearchResult search(String keyword,
                               String ontologyURI,
                               List<String> allowedEngines,
                               Map<String,List<Object>> facets) {

        if (keyword == null || keyword.isEmpty()) {
            logger.debug("Keyword string is null or empty");
            throw new IllegalArgumentException("Keyword string cannot be null or empty");
        }
        String[] keywords = {keyword};
        return search(keywords, ontologyURI, allowedEngines, facets);
    }

    @Override
    public SearchResult search(String[] keywords) {
        return search(keywords, null, null, null);
    }

    @Override
    public SearchResult search(String[] keywords, String ontologyURI) {
        return search(keywords, ontologyURI, null, null);
    }

    @Override
    public SearchResult search(String[] keywords, List<String> allowedEngines) {
        return search(keywords, null, allowedEngines, null);
    }

    @Override
    public SearchResult search(String[] keywords, Map<String,List<Object>> facets) {
        return search(keywords, null, null, facets);
    }

    @Override
    public SearchResult search(String[] keywords, String ontologyURI, List<String> allowedEngines) {
        return search(keywords, ontologyURI, allowedEngines, null);
    }

    @Override
    public SearchResult search(String[] keywords, String ontologyURI, Map<String,List<Object>> facets) {
        return search(keywords, ontologyURI, null, facets);
    }

    @Override
    public SearchResult search(String[] keywords, List<String> allowedEngines, Map<String,List<Object>> facets) {
        return search(keywords, null, allowedEngines, facets);
    }

    @Override
    public SearchResult search(String[] keywords,
                               String ontologyURI,
                               List<String> allowedEngines,
                               Map<String,List<Object>> facets) {

        if (keywords == null || keywords.length == 0) {
            logger.debug("Keyword array is null or contains no keywords");
            throw new IllegalArgumentException("Keyword array is null or contains no keywords");
        }

        MGraph mgraph = null;
        if (ontologyURI != null && !ontologyURI.isEmpty()) {
            try {
                mgraph = tcManager.getMGraph(new UriRef(ontologyURI));
            } catch (Exception e) {
                logger.debug("TCManager does not contain a graph with the URI: {}", ontologyURI);
                logger.debug("Search will continue with no ontology.");
            }
        }
        OntModel ontModel = null;
        if (mgraph != null) {
            JenaGraph jenaGraph = new JenaGraph(mgraph);
            Model model = ModelFactory.createModelForGraph(jenaGraph);
            ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF);
            ontModel.add(model);
            IndexingUtil.addIndexPropertyToOntResources(ontModel);
        }

        // Create search context
        SearchContextImpl searchContext = new SearchContextImpl(ontModel, keywords, allowedEngines, facets);
        searchProcessor.processQuery(searchContext);

        return searchContext;
    }
}
