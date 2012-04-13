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

package org.apache.stanbol.contenthub.search.related.ontologyresource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.jena.facade.JenaGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.search.related.RelatedKeywordImpl;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeywordSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.larq.IndexBuilderString;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.query.larq.LARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * 
 * @author cihan
 * 
 */
@Component
@Service
public class OntologyResourceSearch implements RelatedKeywordSearch {
    
    private static final Logger logger = LoggerFactory.getLogger(OntologyResourceSearch.class);

    private IndexBuilderString userGraphIndexBuilder;

    private OntModel userOntology;

    @Reference
    private TcManager tcManager;

    /**
     * Search for the given keyword in the external ontology as OWL individual, OWL class or CMS object by
     * appending a "*" character at the end of the keyword if there is not any
     */
    @Override
    public Map<String,List<RelatedKeyword>> search(String keyword, String ontologyURI) throws SearchException {
        Map<String,List<RelatedKeyword>> relatedKeywordsMap = new HashMap<String,List<RelatedKeyword>>();
        if (ontologyURI != null && !ontologyURI.trim().isEmpty()) {
            List<RelatedKeyword> relatedKeywords = new ArrayList<RelatedKeyword>();
            // index of the search model is obtained and checked
            indexOntology(ontologyURI);
            IndexLARQ index = userGraphIndexBuilder.getIndex();

            // classes
            logger.debug("Processing classes for related keywords");
            Query query = QueryFactory.getClassQuery(keyword);
            QueryExecution classQExec = QueryExecutionFactory.create(query, userOntology);
            LARQ.setDefaultIndex(classQExec.getContext(), index);
            ResultSet result = classQExec.execSelect();
            processClassResultSet(result, keyword, relatedKeywords);

            // individuals
            logger.debug("Processing individuals for related keywords");
            query = QueryFactory.getIndividualQuery(keyword);
            QueryExecution individualQExec = QueryExecutionFactory.create(query, userOntology);
            LARQ.setDefaultIndex(individualQExec.getContext(), index);
            result = individualQExec.execSelect();
            processIndividualResultSet(result, keyword, relatedKeywords);

            // CMS Objects
            logger.debug("Processing CMS objects for related keywords");
            query = QueryFactory.getCMSObjectQuery(keyword);
            QueryExecution cmsObjectQueryExec = QueryExecutionFactory.create(query, userOntology);
            LARQ.setDefaultIndex(cmsObjectQueryExec.getContext(), index);
            result = cmsObjectQueryExec.execSelect();
            processCMSObjectResultSet(result, keyword, relatedKeywords);
            relatedKeywordsMap.put(RelatedKeyword.Source.ONTOLOGY.toString(), relatedKeywords);
        }
        return relatedKeywordsMap;
    }

    private void indexOntology(String userOntologyURI) throws SearchException {
        OntModel userGraph = null;
        MGraph mgraph = tcManager.getMGraph(new UriRef(userOntologyURI));
        if (mgraph != null) {
            JenaGraph jenaGraph = new JenaGraph(mgraph);
            Model model = ModelFactory.createModelForGraph(jenaGraph);
            userGraph = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF);
            userGraph.add(model);
            IndexingHelper.addIndexPropertyToOntResources(userGraph);
        } else {
            throw new SearchException(String.format("MGraph having URI %s could obtained through TcManager",
                userOntologyURI));
        }
        this.userOntology = userGraph;
        this.userGraphIndexBuilder = new IndexBuilderString(IndexingHelper.HAS_LOCAL_NAME);
        this.userGraphIndexBuilder.indexStatements(userGraph.listStatements());
        // Do not forget to set the default index just before the query execution according to its context
        // LARQ.setDefaultIndex(index);
    }

    private void processClassResultSet(ResultSet result, String keyword, List<RelatedKeyword> relatedKeywords) {
        Map<String,Double> results = new HashMap<String,Double>();
        while (result.hasNext()) {
            QuerySolution resultBinding = result.nextSolution();
            RDFNode rdfNode = resultBinding.get("class");
            double score = resultBinding.getLiteral("score").getDouble();
            if (rdfNode.isURIResource()) {
                String uri = rdfNode.asResource().getURI();
                results.put(uri, score);
            }
        }
        for (String uri : results.keySet()) {
            double score = results.get(uri);
            ClosureHelper.getInstance(userOntology).computeClassClosure(uri, 6, score, 1.5, keyword,
                relatedKeywords);
        }
    }

    private void processIndividualResultSet(ResultSet result,
                                            String keyword,
                                            List<RelatedKeyword> relatedKeywords) {
        Map<String,Double> results = new HashMap<String,Double>();
        while (result.hasNext()) {
            QuerySolution resultBinding = result.nextSolution();
            RDFNode rdfNode = resultBinding.get("individual");
            double score = resultBinding.getLiteral("score").getDouble();
            if (rdfNode.isURIResource()) {
                String uri = rdfNode.asResource().getURI();
                results.put(uri, score);
            }
        }
        for (String uri : results.keySet()) {
            double score = results.get(uri);
            ClosureHelper.getInstance(userOntology).computeIndividualClosure(uri, 6, score, 1.5, keyword,
                relatedKeywords);
        }
    }

    /**
     * Process the SPARQL result passed in <code>result</code> parameter and computes closures for them.
     */
    private void processCMSObjectResultSet(ResultSet result,
                                           String keyword,
                                           List<RelatedKeyword> relatedKeywords) {
        Map<String,Double> results = new HashMap<String,Double>();
        while (result.hasNext()) {
            QuerySolution resultBinding = result.nextSolution();
            RDFNode rdfNode = resultBinding.get("cmsobject");
            double score = resultBinding.getLiteral("score").getDouble();
            if (rdfNode.isURIResource()) {
                String uri = rdfNode.asResource().getURI();
                results.put(uri, score);
            }
        }

        /*
         * Look up for each URI returned in SPARQL results for a name which is kept in
         * "http://www.apache.org/stanbol/cms#name" property. If there is a corresponding name, create a new
         * keyword with this name and compute closures for the corresponding context resources.
         */
        Property subsumptionProp = ResourceFactory
                .createProperty("http://www.apache.org/stanbol/cms#parentRef");
        Property nameProp = ResourceFactory.createProperty("http://www.apache.org/stanbol/cms#name");
        for (String uri : results.keySet()) {
            Resource keywordResource = ResourceFactory.createResource(uri);
            List<Statement> nameStatements = userOntology.listStatements(keywordResource, nameProp,
                (RDFNode) null).toList();
            if (nameStatements.size() > 0) {
                String matchedResourceName = nameStatements.get(0).getString();
                double initialScore = results.get(uri);
                relatedKeywords.add(new RelatedKeywordImpl(matchedResourceName, initialScore,
                        RelatedKeyword.Source.ONTOLOGY));

                ClosureHelper.getInstance(userOntology).computeClosureWithProperty(keywordResource,
                    subsumptionProp, 2, initialScore, 1.5, keyword, relatedKeywords);

            }
        }
    }

    @Override
    public Map<String,List<RelatedKeyword>> search(String keyword) throws SearchException {
        // TODO Cannot search without an ontology URI. A default one maybe?
        return new HashMap<String,List<RelatedKeyword>>();
    }
}
