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
package org.apache.stanbol.commons.web.sparql.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.DescribeQuery;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.viewable.Viewable;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;


/**
 * This is the SPARQL endpoint which is used throughout the Stanbol. It uses {@link BundleContext} to retrive
 * {@link TripleCollection} s registered to OSGi environment. To be able to execute SPARQL queries on triple
 * collections, they should be registered to the OSGi environment with the following parameters:
 * 
 * <p>
 * <ul>
 * <li>graph.uri <b>(required)</b> : The URI of the graph. This is the same as used with the TcManager</li>
 * <li>service.ranking: If this parameter is not specified, "0" will be used as default value.</li>
 * <li>graph.name: The name of the graph. Human readable name intended to be used in the UI</li>
 * <li>graph.description: human readable description providing additional information about the RDF graph</li>
 * </ul>
 * </p>
 * 
 * <p>
 * If a uri is not specified, the graph having highest service.ranking value will be chosen.
 * </p>
 * 
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/sparql")
public class SparqlEndpointResource extends BaseStanbolResource {

    private static final Comparator<ServiceReference> SERVICE_RANKING_COMPARATOR = new Comparator<ServiceReference>() {
        
        public int compare(ServiceReference ref1, ServiceReference ref2) {
            int r1,r2;
            Object tmp = ref1.getProperty(Constants.SERVICE_RANKING);
            r1 = tmp != null ? ((Integer)tmp).intValue() : 0;
            tmp = (Integer)ref2.getProperty(Constants.SERVICE_RANKING);
            r2 = tmp != null ? ((Integer)tmp).intValue() : 0;
            if(r1 == r2){
                tmp = (Long)ref1.getProperty(Constants.SERVICE_ID);
                long id1 = tmp != null ? ((Long)tmp).longValue() : Long.MAX_VALUE;
                tmp = (Long)ref2.getProperty(Constants.SERVICE_ID);
                long id2 = tmp != null ? ((Long)tmp).longValue() : Long.MAX_VALUE;
                //the lowest id must be first -> id1 < id2 -> [id1,id2] -> return -1
                return id1 < id2 ? -1 : id2 == id1 ? 0 : 1; 
            } else {
                //the highest ranking MUST BE first -> r1 < r2 -> [r2,r1] -> return 1
                return r1 < r2 ? 1:-1;
            }
        }        
        
    };
    
    @Reference
    protected TcManager tcManager;

    private static final String GRAPH_URI = "graph.uri";
    private BundleContext bundleContext;


    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    //TODO re-enable
    /*@OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }*/

    /**
     * HTTP GET service to execute SPARQL queries on {@link TripleCollection}s registered to OSGi environment.
     * If a <code>null</code>, it is assumed that the request is coming from the HTML interface of SPARQL
     * endpoint. Otherwise the query is executed on the triple collection specified by <code>graphUri</code>.
     * But, if no graph uri is passed, then the triple collection having highest service.ranking value is
     * chosen.
     * 
     * Type of the result is determined according to type of the query such that if the specified query is
     * either a <b>describe query</b> or <b>construct query</b>, results are returned in
     * <b>application/rdf+xml</b> format, otherwise in <b>pplication/sparql-results+xml</b> format.
     * 
     * @param graphUri
     *            the URI of the graph on which the SPARQL query will be executed.
     * @param sparqlQuery
     *            SPARQL query to be executed
     * @return
     */
    @GET
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces({TEXT_HTML + ";qs=2", "application/sparql-results+xml", "application/rdf+xml"})
    public Response sparql(@QueryParam(value = "graphuri") String graphUri,
                           @QueryParam(value = "query") String sparqlQuery,
                           @Context HttpHeaders headers) throws ParseException, InvalidSyntaxException {
        if (sparqlQuery == null) {
            populateTripleCollectionList(getServices(null));
            return Response.ok(new Viewable("index", this), TEXT_HTML).build();
        }

        Query query = QueryParser.getInstance().parse(sparqlQuery);
        String mediaType = "application/sparql-results+xml";
        if (query instanceof DescribeQuery || query instanceof ConstructQuery) {
            mediaType = "application/rdf+xml";
        }

        TripleCollection tripleCollection = getTripleCollection(graphUri);
        ResponseBuilder rb;
        if (tripleCollection != null) {
            Object result = tcManager.executeSparqlQuery(query, tripleCollection);
            rb = Response.ok(result, mediaType);
        } else {
            rb = Response.status(Status.NOT_FOUND).entity(
                String.format("There is no registered graph with given uri: %s", graphUri));
        }
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * HTTP GET service to execute SPARQL queries on {@link TripleCollection}s registered to OSGi environment.
     * For details, see {@link #sparql(String, String, HttpHeaders)}
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces({"application/sparql-results+xml", "application/rdf+xml"})
    public Response postSparql(@FormParam("graphuri") String graphUri,
                               @FormParam("query") String sparqlQuery,
                               @Context HttpHeaders headers) throws ParseException, InvalidSyntaxException {
        return sparql(graphUri, sparqlQuery, headers);
    }

    private TripleCollection getTripleCollection(String graphUri) throws InvalidSyntaxException {
        LinkedHashMap<ServiceReference,TripleCollection> services = getServices(graphUri);
        if (services != null && services.size() > 0) {
            return (TripleCollection) services.get(services.keySet().iterator().next());
        }
        return null;
    }

    private void populateTripleCollectionList(LinkedHashMap<ServiceReference,TripleCollection> services) {
        if (services != null) {
            for (ServiceReference service : services.keySet()) {
                Object graphUri = service.getProperty(GRAPH_URI);
                if (service.getProperty(GRAPH_URI) instanceof UriRef) {
                    graphUri = ((UriRef) graphUri).getUnicodeString();
                }
                Object graphName = service.getProperty("graph.name");
                Object graphDescription = service.getProperty("graph.description");
                if (graphUri instanceof String && graphName instanceof String
                    && graphDescription instanceof String) {
                    tripleCollections.add(new TripleCollectionInfo((String) graphUri, (String) graphName,
                            (String) graphDescription));
                }
            }
        }
    }

    private LinkedHashMap<ServiceReference,TripleCollection> getServices(String graphUri) throws InvalidSyntaxException {
        LinkedHashMap<ServiceReference,TripleCollection> registeredGraphs = new LinkedHashMap<ServiceReference,TripleCollection>();
        ServiceReference[] refs = bundleContext.getServiceReferences(TripleCollection.class.getName(),
            getFilter(graphUri));
        if (refs != null) {
            if (refs.length > 1) {
                Arrays.sort(refs, SERVICE_RANKING_COMPARATOR);
            }
            for (ServiceReference ref : refs) {
                registeredGraphs.put(ref, (TripleCollection) bundleContext.getService(ref));
            }
        }
        return registeredGraphs;
    }

    private String getFilter(String graphUri) {
        String constraint = "(%s=%s)";
        StringBuilder filterString;
        if (graphUri != null) {
            filterString = new StringBuilder("(&");
            filterString.append(String.format(constraint, GRAPH_URI, graphUri));
        } else {
            filterString = new StringBuilder();
        }
        filterString
                .append(String.format(constraint, Constants.OBJECTCLASS, TripleCollection.class.getName()));
        if (graphUri != null) {
            filterString.append(')');
        }
        return filterString.toString();
    }

    /*
     * HTML View
     */

    private List<TripleCollectionInfo> tripleCollections = new ArrayList<SparqlEndpointResource.TripleCollectionInfo>();

    public List<TripleCollectionInfo> getTripleCollectionList() {
        return this.tripleCollections;
    }

    public class TripleCollectionInfo {
        private String graphUri;
        private String graphName;
        private String graphDescription;

        public TripleCollectionInfo(String graphUri, String graphName, String graphDescription) {
            this.graphUri = graphUri;
            this.graphName = graphName != null ? graphName : "";
            this.graphDescription = graphDescription != null ? graphDescription : "";
        }

        public String getGraphUri() {
            return graphUri;
        }

        public String getGraphName() {
            return graphName;
        }

        public String getGraphDescription() {
            return graphDescription;
        }
    }
}
