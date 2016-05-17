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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.site.AbstractEntitySearcher;
import org.apache.stanbol.entityhub.query.clerezza.RdfQueryResultList;
import org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery;
import org.apache.stanbol.entityhub.query.sparql.SparqlFieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.LoggerFactory;


@Component(
        name="org.apache.stanbol.entityhub.searcher.SparqlSearcher",
        factory="org.apache.stanbol.entityhub.searcher.SparqlSearcherFactory",
        specVersion="1.1"
        )
public class SparqlSearcher extends AbstractEntitySearcher implements EntitySearcher {

    public SparqlSearcher() {
        super(LoggerFactory.getLogger(SparqlSearcher.class));
    }

    @Reference
    private Parser parser;

    protected static final String DEFAULT_RDF_CONTENT_TYPE = SupportedFormat.RDF_XML;
    protected static final String DEFAULT_SPARQL_RESULT_CONTENT_TYPE = SparqlEndpointUtils.SPARQL_RESULT_JSON;
    @Override
    public final QueryResultList<String> findEntities(FieldQuery parsedQuery)  throws IOException {
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        String sparqlQuery = query.toSparqlSelect(false);
        InputStream in = SparqlEndpointUtils.sendSparqlRequest(getQueryUri(), sparqlQuery, DEFAULT_SPARQL_RESULT_CONTENT_TYPE);
        //Move to util class!
        final List<String> entities = extractEntitiesFromJsonResult(in,query.getRootVariableName());
        return new QueryResultListImpl<String>(query, entities.iterator(),String.class);
    }

    /**
     * Extracts the values of the Query. Also used by {@link VirtuosoSearcher}
     * and {@link LarqSearcher}
     * @param rootVariable the name of the variable to extract
     * @param in the input stream with the data
     * @return the extracted results
     * @throws IOException if the input streams decides to explode
     */
    protected static List<String> extractEntitiesFromJsonResult(InputStream in, final String rootVariable) throws IOException {
        final List<String> entities;
        try {
            JSONObject result = new JSONObject(IOUtils.toString(in));
            JSONObject results = result.getJSONObject("results");
            if(results != null){
                JSONArray bindings = results.getJSONArray("bindings");
                if(bindings != null && bindings.length()>0){
                    entities = new ArrayList<String>(bindings.length());
                    for(int i=0;i<bindings.length();i++){
                        JSONObject solution = bindings.getJSONObject(i);
                        if(solution != null){
                            JSONObject rootVar = solution.getJSONObject(rootVariable);
                            if(rootVar != null){
                                String entityId = rootVar.getString("value");
                                if(entityId != null){
                                    entities.add(entityId);
                                } //else missing value (very unlikely)
                            } //else missing binding for rootVar (very unlikely)
                        } //else solution in array is null (very unlikely)
                    } //end for all solutions
                } else {
                    entities = Collections.emptyList();
                }
            } else {
                entities = Collections.emptyList();
            }
        } catch (JSONException e) {
            //TODO: convert in better exception
            throw new IOException("Unable to parse JSON Result Set for parsed query",e);
        }
        return entities;
    }

    @Override
    public final QueryResultList<Representation> find(FieldQuery parsedQuery) throws IOException{
        long start = System.currentTimeMillis();
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        String sparqlQuery = query.toSparqlConstruct();
        long initEnd = System.currentTimeMillis();
        log.debug("  > InitTime: "+(initEnd-start));
        log.debug("  > SPARQL query:\n"+sparqlQuery);
        InputStream in = SparqlEndpointUtils.sendSparqlRequest(getQueryUri(), sparqlQuery, DEFAULT_RDF_CONTENT_TYPE);
        long queryEnd = System.currentTimeMillis();
        log.debug("  > QueryTime: "+(queryEnd-initEnd));
        if(in != null){
            Graph graph;
            Graph rdfData = parser.parse(in, DEFAULT_RDF_CONTENT_TYPE,
                new IRI(getBaseUri()));
            if(rdfData instanceof Graph){
                graph = (Graph) rdfData;
            } else {
                graph = new IndexedGraph(rdfData);
            }
            long parseEnd = System.currentTimeMillis();
            log.debug("  > ParseTime: "+(parseEnd-queryEnd));
            return new RdfQueryResultList(query, graph);
        } else {
            return null;
        }
    }

}
