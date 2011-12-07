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

package org.apache.stanbol.contenthub.search.engines.ontologyresource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.core.utils.ClosureHelper;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngineException;
import org.apache.stanbol.contenthub.servicesapi.search.execution.ClassResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.IndividualResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword.RelatedKeywordSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.query.larq.LARQ;
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
public class OntologyResourceSearchEngine implements SearchEngine {
    private static final Logger logger = LoggerFactory.getLogger(OntologyResourceSearchEngine.class);

    @Override
    public void search(SearchContext searchContext) throws SearchEngineException {
        for (QueryKeyword qk : searchContext.getQueryKeyWords()) {
            searchForKeyword(qk, searchContext);
        }
    }

    private void searchForKeyword(Keyword kw, SearchContext searchContext) {
        // index of the search model is obtained and checked
        IndexLARQ index = searchContext.getIndex();
        if (index != null) {
            // classes
            Query query = QueryFactory.getClassQuery(kw.getKeyword());
            QueryExecution classQExec = QueryExecutionFactory.create(query, searchContext.getSearchModel());
            LARQ.setDefaultIndex(classQExec.getContext(), index);
            ResultSet result = classQExec.execSelect();
            processClassResultSet(result, kw, searchContext);

            // individuals
            query = QueryFactory.getIndividualQuery(kw.getKeyword());
            QueryExecution individualQExec = QueryExecutionFactory.create(query,
                searchContext.getSearchModel());
            LARQ.setDefaultIndex(individualQExec.getContext(), index);
            result = individualQExec.execSelect();
            processIndividualResultSet(result, kw, searchContext);

            // CMS Objects
            query = QueryFactory.getCMSObjectQuery(kw.getKeyword());
            QueryExecution cmsObjectQueryExec = QueryExecutionFactory.create(query,
                searchContext.getSearchModel());
            LARQ.setDefaultIndex(cmsObjectQueryExec.getContext(), index);
            result = cmsObjectQueryExec.execSelect();
            processCMSObjectResultSet(result, kw, searchContext);
        } else {
            logger.warn("Keyword Engine skipped since no index for search model");
        }

    }

    private void processClassResultSet(ResultSet result, Keyword kw, SearchContext context) {
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
            ClassResource cr = context.getFactory().createClassResource(uri, 1.0, results.get(uri), kw);
            ClosureHelper.getInstance(context).computeClassClosure(cr, 6, 1.5, kw);
        }
    }

    private void processIndividualResultSet(ResultSet result, Keyword kw, SearchContext context) {
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
            IndividualResource ir = context.getFactory().createIndividualResource(uri, 1.0, results.get(uri),
                kw);
            ClosureHelper.getInstance(context).computeIndividualClosure(ir, 6, 1.5, kw);
        }
    }

    private void processCMSObjectResultSet(ResultSet result, Keyword kw, SearchContext context) {
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
        Property subsumptionProp = ResourceFactory
                .createProperty("http://www.apache.org/stanbol/cms#parentRef");
        Property nameProp = ResourceFactory.createProperty("http://www.apache.org/stanbol/cms#name");
        for (String uri : results.keySet()) {
            Resource keywordResource = ResourceFactory.createResource(uri);
            List<Statement> nameStatements = context.getSearchModel()
                    .listStatements(keywordResource, nameProp, (RDFNode) null).toList();
            if (nameStatements.size() > 0) {
                String keyword = nameStatements.get(0).getString();
                context.getFactory().createKeyword(keyword, results.get(uri), kw.getRelatedQueryKeyword(),RelatedKeywordSource.ONTOLOGYRESOURCE.getName());
                double initialScore = results.get(uri);
                ClosureHelper.getInstance(context).computeClosureWithProperty(keywordResource,
                    subsumptionProp, 2, initialScore, 1.5, kw);
            }
        }
    }
}
