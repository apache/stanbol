package org.apache.stanbol.entityhub.site.linkedData.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.site.AbstractEntitySearcher;
import org.apache.stanbol.entityhub.query.clerezza.RdfQueryResultList;
import org.apache.stanbol.entityhub.query.clerezza.SparqlFieldQuery;
import org.apache.stanbol.entityhub.query.clerezza.SparqlFieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;


@Component(
        name="org.apache.stanbol.entityhub.site.SparqlSearcher",
        factory="org.apache.stanbol.entityhub.site.SparqlSearcherFactory",
        policy=ConfigurationPolicy.REQUIRE, //the queryUri and the SPARQL Endpoint are required
        specVersion="1.1"
        )
public class SparqlSearcher extends AbstractEntitySearcher implements EntitySearcher {

    public SparqlSearcher() {
        super(LoggerFactory.getLogger(SparqlSearcher.class));
    }

    @Reference
    protected Parser parser;

    protected static final String DEFAULT_RDF_CONTENT_TYPE = SupportedFormat.N3;
    protected static final String DEFAULT_SPARQL_RESULT_CONTENT_TYPE = SparqlEndpointUtils.SPARQL_RESULT_JSON;
    @Override
    public QueryResultList<String> findEntities(FieldQuery parsedQuery)  throws IOException {
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
    public QueryResultList<Representation> find(FieldQuery parsedQuery) throws IOException{
        long start = System.currentTimeMillis();
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        String sparqlQuery = query.toSparqlConstruct();
        long initEnd = System.currentTimeMillis();
        log.info("  > InitTime: "+(initEnd-start));
        log.info("  > SPARQL query:\n"+sparqlQuery);
        InputStream in = SparqlEndpointUtils.sendSparqlRequest(getQueryUri(), sparqlQuery, DEFAULT_RDF_CONTENT_TYPE);
        long queryEnd = System.currentTimeMillis();
        log.info("  > QueryTime: "+(queryEnd-initEnd));
        if(in != null){
            MGraph graph;
            TripleCollection rdfData = parser.parse(in, DEFAULT_RDF_CONTENT_TYPE);
            if(rdfData instanceof MGraph){
                graph = (MGraph) rdfData;
            } else {
                graph = new SimpleMGraph(rdfData);
            }
            long parseEnd = System.currentTimeMillis();
            log.info("  > ParseTime: "+(parseEnd-queryEnd));
            return new RdfQueryResultList(query, graph);
        } else {
            return null;
        }
    }



}
