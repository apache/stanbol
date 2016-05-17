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
package org.apache.stanbol.entityhub.site.linkeddata.impl;

import static org.apache.stanbol.entityhub.site.linkeddata.impl.SparqlEndpointUtils.sendSparqlRequest;
import static org.apache.stanbol.entityhub.site.linkeddata.impl.SparqlSearcher.extractEntitiesFromJsonResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.site.AbstractEntitySearcher;
import org.apache.stanbol.entityhub.query.clerezza.RdfQueryResultList;
import org.apache.stanbol.entityhub.query.sparql.SparqlEndpointTypeEnum;
import org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery;
import org.apache.stanbol.entityhub.query.sparql.SparqlFieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.slf4j.LoggerFactory;

@Component(name = "org.apache.stanbol.entityhub.searcher.VirtuosoSearcher", 
    factory = "org.apache.stanbol.entityhub.searcher.VirtuosoSearcherFactory", 
    specVersion = "1.1")
public class VirtuosoSearcher extends AbstractEntitySearcher implements EntitySearcher {
    @Reference
    private Parser parser;

    public VirtuosoSearcher() {
        super(LoggerFactory.getLogger(VirtuosoSearcher.class));
    }

    @Override
    public final QueryResultList<Representation> find(FieldQuery parsedQuery) throws IOException {
        long start = System.currentTimeMillis();
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        query.setSparqlEndpointType(SparqlEndpointTypeEnum.Virtuoso);
        String sparqlQuery = query.toSparqlConstruct();
        long initEnd = System.currentTimeMillis();
        log.info("  > InitTime: " + (initEnd - start));
        log.info("  > SPARQL query:\n" + sparqlQuery);
        InputStream in = SparqlEndpointUtils.sendSparqlRequest(getQueryUri(), sparqlQuery,
            SparqlSearcher.DEFAULT_RDF_CONTENT_TYPE);
        long queryEnd = System.currentTimeMillis();
        log.info("  > QueryTime: " + (queryEnd - initEnd));
        if (in != null) {
            Graph graph;
            Graph rdfData = parser.parse(in, SparqlSearcher.DEFAULT_RDF_CONTENT_TYPE, new IRI(
                    getBaseUri()));
            if (rdfData instanceof Graph) {
                graph = (Graph) rdfData;
            } else {
                graph = new IndexedGraph(rdfData);
            }
            long parseEnd = System.currentTimeMillis();
            log.info("  > ParseTime: " + (parseEnd - queryEnd));
            return new RdfQueryResultList(query, graph);
        } else {
            return null;
        }
    }

    @Override
    public final QueryResultList<String> findEntities(FieldQuery parsedQuery) throws IOException {
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        query.setSparqlEndpointType(SparqlEndpointTypeEnum.Virtuoso);
        String sparqlQuery = query.toSparqlSelect(false);
        log.trace("Sending Sparql request [{}].", sparqlQuery);
        InputStream in = sendSparqlRequest(getQueryUri(), sparqlQuery,
            SparqlSearcher.DEFAULT_SPARQL_RESULT_CONTENT_TYPE);
        // Move to util class!
        final List<String> entities = extractEntitiesFromJsonResult(in, query.getRootVariableName());
        return new QueryResultListImpl<String>(query, entities.iterator(), String.class);
    }

}
