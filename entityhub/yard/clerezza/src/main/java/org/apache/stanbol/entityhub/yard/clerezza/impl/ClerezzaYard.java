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
package org.apache.stanbol.entityhub.yard.clerezza.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.query.QueryUtils;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator;
import org.apache.stanbol.entityhub.core.yard.AbstractYard;
import org.apache.stanbol.entityhub.core.yard.SimpleYardConfig;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.model.clerezza.utils.Resource2StringAdapter;
import org.apache.stanbol.entityhub.query.clerezza.RdfQueryResultList;
import org.apache.stanbol.entityhub.query.clerezza.SparqlFieldQuery;
import org.apache.stanbol.entityhub.query.clerezza.SparqlFieldQueryFactory;
import org.apache.stanbol.entityhub.query.clerezza.SparqlQueryUtils;
import org.apache.stanbol.entityhub.query.clerezza.SparqlQueryUtils.EndpointTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Yard Interface based on a RDF Triple Store. This
 * Implementation uses Clerezza as RDF Framework. The actual Triple Store used
 * to store the data depends on the configuration of Clerezza.<p>
 * This implementation uses {@link LockableGraph} interface for write locks
 * when updating the graph. SPARQL queries are not within a write lock.<p>
 *
 * @author Rupert Westenthaler
 *
 */
@Component(
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE, //the ID is required!
        specVersion="1.1",
        metatype = true
        )
@Service
@Properties(value={
    @Property(name=Yard.ID,value="changeme"),
    @Property(name=Yard.NAME,value="The human readable name"),
    @Property(name=Yard.DESCRIPTION,value="A description"),
    @Property(name=ClerezzaYard.GRAPH_URI),
    @Property(name=AbstractYard.DEFAULT_QUERY_RESULT_NUMBER,intValue=-1),
    @Property(name=AbstractYard.MAX_QUERY_RESULT_NUMBER,intValue=-1)
})
public class ClerezzaYard extends AbstractYard implements Yard {
    private static Logger log = LoggerFactory.getLogger(ClerezzaYard.class);
    /**
     * Property used to mark empty Representations managed by this ImmutableGraph. This is
     * needed to workaround the fact, that the Entityhub supports the storage of
     * empty Representations but this Yard uses the search for any outgoing
     * relation (triple with the id of the representation as Subject) for the 
     * implementation of {@link #isRepresentation(String)}. Therefore for an
     * empty Representation {@link #isRepresentation(String)} would return false
     * even if the representation was {@link #store(Representation)} previously.
     * <p>
     * Adding the Triple<br>
     * <code> ?representationId <{@value #MANAGED_REPRESENTATION}> true^^xsd:boolean </code>
     * <br> for any empty Representation avoids this unwanted behaviour.
     */
    public static final IRI MANAGED_REPRESENTATION = new IRI("urn:org.apache.stanbol:entityhub.yard:rdf.clerezza:managesRepresentation");
    /**
     * Property used to optionally configure the URI of the Clerezza ImmutableGraph.
     * This graph will be looked up by using {@link TcManager#getTriples(IRI).<p>
     * Note that if the returned RDF graph is of instance ImmutableGraph the write/delete
     * operations of this implementations will not work.
     */
    public static final String GRAPH_URI = "org.apache.stanbol.entityhub.yard.clerezza.graphuri";
    /**
     * The TRUE value used as object for the property {@link #MANAGED_REPRESENTATION}.
     */
    private static final Literal TRUE_LITERAL = LiteralFactory.getInstance().createTypedLiteral(Boolean.FALSE);
    
    //public static final String YARD_URI_PREFIX = "urn:org.apache.stanbol:entityhub.yard:rdf.clerezza:";
//    public static final IRI REPRESENTATION = new IRI(RdfResourceEnum.Representation.getUri());
//    protected ComponentContext context;
//    protected Dictionary<String,?> properties;
    @Reference
    private TcManager tcManager;
    private IRI yardGraphUri;
    private Graph graph;
    
    private ServiceRegistration graphRegistration;
    private ComponentContext context;
    private boolean immutable;
    
    //private LockableGraph graph;

    public ClerezzaYard() {
        super();
    }
    public ClerezzaYard(ClerezzaYardConfig config) {
        super();
        activate(config);
    }
    @SuppressWarnings("unchecked")
    @Activate
    protected final void activate(ComponentContext context) throws ConfigurationException {
        log.info("in "+ClerezzaYard.class+" activate with context "+context);
        if(context == null || context.getProperties() == null){
            throw new IllegalStateException("No valid"+ComponentContext.class+" parsed in activate!");
        }
        this.context = context;
        activate(new ClerezzaYardConfig(context.getProperties()));
    }
    /**
     * Internally used to activate the Yard. In case the Yard runs within a
     * OSGI container it is called by the {@link #activate(ComponentContext)}
     * Method. In case the Yard runs outside of an OSGI Container it is called
     * by the Constructor taking the {@link YardConfig} as parameter
     * @param config The configuration for the new Yard instance
     * @throws IllegalArgumentException In case <code>null</code> is parsed as 
     * configuration or the configuration is invalid
     */
    private final void activate(ClerezzaYardConfig config) throws IllegalArgumentException {
        super.activate(RdfValueFactory.getInstance(), SparqlFieldQueryFactory.getInstance(), config);
        if(tcManager == null){ //this will be the case if we are not in an OSGI environment
          //use the getInstance() method!
            tcManager = TcManager.getInstance(); 
        }
        this.yardGraphUri = config.getGraphUri();
        if(this.yardGraphUri == null){ // use default
            String yardUri = getUriPrefix();
            //remove the "." at the last position of the prefix
            this.yardGraphUri = new IRI(yardUri.substring(0, yardUri.length()-2));
        }
        try {
             
            try {
                this.graph = tcManager.getImmutableGraph(yardGraphUri);
                immutable = true;
            } catch(NoSuchEntityException e) {
                this.graph = tcManager.getGraph(yardGraphUri);
                immutable = false;
            }
            
            log.info("  ... (re)use existing Graph {} for Yard {}",
                yardGraphUri,config.getName());
        } catch (NoSuchEntityException e) {
            log.info("   ... create new Graph {} for Yard {}",yardGraphUri,config.getName());
            this.graph =  tcManager.createGraph(yardGraphUri);
        }
        if(context != null){ //within an OSGI environment
            //Register the graph with the Stanbol SPARQL endpoint (STANBOL-677)
            Dictionary<String,Object> graphRegProp = new Hashtable<String,Object>();
            graphRegProp.put("graph.uri", yardGraphUri.getUnicodeString());
            graphRegProp.put(Constants.SERVICE_RANKING, new Integer(-100));
            graphRegProp.put("graph.name", getConfig().getName());
            if(getConfig().getDescription() != null){
                graphRegProp.put("graph.description", getConfig().getDescription());
            }
            graphRegistration = context.getBundleContext().registerService(
                Graph.class.getName(), graph, graphRegProp);
        } //else do not register when running outside OSGI
    }
    @Deactivate
    protected final void deactivate(ComponentContext context) {
        log.info("in "+ClerezzaYard.class.getSimpleName()+" deactivate with context "+context);
        if(graphRegistration != null){
            graphRegistration.unregister();
            graphRegistration = null;
        }
        this.yardGraphUri = null;
        this.graph = null;
        super.deactivate();
        this.context = null;
    }
    /**
     * Getter for the URI used for the named graph. The returned value is
     * {@link #YARD_URI_PREFIX}+{@link #getId()}.
     * @return the URI used for the RDF graph that stores all the data of this
     * yard.
     */
    public final String getYardGraphUri(){
        return yardGraphUri.getUnicodeString();
    }

    @Override
    public Representation getRepresentation(String id) {
        if(id == null){
            throw new IllegalArgumentException("The parsed representation id MUST NOT be NULL!");
        }
        if(id.isEmpty()){
            throw new IllegalArgumentException("The parsed representation id MUST NOT be EMTPY!");
        }
        return getRepresentation(new IRI(id),true);
    }
    /**
     * Internally used to create Representations for URIs
     * @param uri the uri
     * @param check if <code>false</code> than there is no check if the URI
     *     refers to a RDFTerm in the graph that is of type {@link #REPRESENTATION}
     * @return the Representation
     */
    protected final Representation getRepresentation(IRI uri, boolean check) {
        final Lock readLock = readLockGraph();
        try {
            if(!check || isRepresentation(uri)){
                Graph nodeGraph = createRepresentationGraph(uri, graph);
                //Remove the triple internally used to represent an empty Representation
                // ... this will only remove the triple if the Representation is empty
                //     but a check would take longer than the this call
                nodeGraph.remove(new TripleImpl(uri,MANAGED_REPRESENTATION,TRUE_LITERAL));
                return ((RdfValueFactory)getValueFactory()).createRdfRepresentation(uri, nodeGraph);
            } else {
                return null; //not found
            }
        } finally {
            if(readLock != null){
                readLock.unlock();
            }
        }
    }
    /**
     * @return the readLock or <code>null</code>if no read lock is needed
     */
    private Lock readLockGraph() {
        final Lock readLock;
        readLock = graph.getLock().readLock();
        readLock.lock();
        return readLock;
    }
    /**
     * Extracts the triples that belong to the {@link Representation} with the
     * parsed id from the parsed graph. The graph is not modified and changes
     * in the returned graph will not affect the parsed graph.
     * @param id the {@link IRI} node representing the id of the Representation.
     * @param graph the ImmutableGraph to extract the representation from
     * @return the extracted graph.
     */
    protected Graph createRepresentationGraph(IRI id, Graph graph){
        return extractRepresentation(graph, new IndexedGraph(), id, new HashSet<BlankNode>());
    }
    /**
     * Recursive Method internally doing all the work for 
     * {@link #createRepresentationGraph(IRI, Graph)}
     * @param source The graph to extract the Representation (source)
     * @param target The graph to store the extracted triples (target)
     * @param node the current node. Changes in recursive calls as it follows
     * @param visited holding all the visited BlankNodes to avoid cycles. Other nodes 
     * need not be added because this implementation would not follow it anyway
     * outgoing relations if the object is a {@link BlankNode} instance.
     * @return the target graph (for convenience)
     */
    private Graph extractRepresentation(Graph source,Graph target, BlankNodeOrIRI node, Set<BlankNode> visited){
        //we need all the outgoing relations and also want to follow bNodes until
        //the next IRI. However we are not interested in incoming relations!
        Iterator<Triple> outgoing = source.filter(node, null, null);
        while (outgoing.hasNext()) {
            Triple triple = outgoing.next();
            target.add(triple);
            RDFTerm object = triple.getObject();
            if(object instanceof BlankNode){
                //add first and than follow because there might be a triple such as
                // bnode1 <urn:someProperty> bnode1
                visited.add((BlankNode)object);
                extractRepresentation(source, target, (BlankNodeOrIRI)object, visited);
            }
        }
        return target;
    }

    @Override
    public boolean isRepresentation(String id) {
        if(id == null) {
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL!");
        }
        if(id.isEmpty()){
            throw new IllegalArgumentException("The parsed id MUST NOT be EMPTY!");
        }
        //search for any outgoing triple
        return isRepresentation(new IRI(id));
    }
    /**
     * Internally used to check if a URI resource represents an representation
     * @param resource the resource to check
     * @return the state
     */
    protected final boolean isRepresentation(IRI resource){
        return graph.filter(resource, null, null).hasNext();
    }

    @Override
    public void remove(String id) throws YardException, IllegalArgumentException {
        if(id == null) {
            throw new IllegalArgumentException("The parsed Representation id MUST NOT be NULL!");
        }
        IRI resource = new IRI(id);
        final Lock writeLock = writeLockGraph();
        try {
            Iterator<Triple> it = graph.filter(resource, null, null);
            while(it.hasNext()){
                it.next();
                it.remove();
            }
//            if(isRepresentation(resource)){
//                graph.removeAll(createRepresentationGraph(resource, graph));
//            } //else not found  -> nothing to do
        }finally {
            writeLock.unlock();
        }
    }
    /**
     * @return
     * @throws YardException
     */
    private Lock writeLockGraph() throws YardException {
        if (immutable) {
             throw new YardException("Unable modify data in ClerezzaYard '"+getId()
                + "' because the backing RDF graph '"+yardGraphUri
                + "' is read-only!");
        }
        final Lock writeLock;
        writeLock = graph.getLock().writeLock();
        writeLock.lock();
        return writeLock;
    }
    @Override
    public final void remove(Iterable<String> ids) throws IllegalArgumentException, YardException {
        if(ids == null){
            throw new IllegalArgumentException("The parsed Iterable over the IDs to remove MUST NOT be NULL!");
        }
        for(String id : ids){
            if(id != null){
                remove(id);
            } //else ignore null values within the parsed Iterable
        }
    }
    @Override
    public final void removeAll() throws YardException {
        final Lock writeLock = writeLockGraph();
        try {
            graph.clear();
        } finally {
            writeLock.unlock();
        }
    }
    @Override
    public final Representation store(Representation representation) throws IllegalArgumentException, YardException {
        if(representation == null){
            throw new IllegalArgumentException("The parsed Representation MUST NOT be NULL!");
        }
        return store(representation,true,true);
    }
    @Override
    public final Iterable<Representation> store(Iterable<Representation> representations) throws IllegalArgumentException, YardException {
        if(representations == null){
            throw new IllegalArgumentException("The parsed Iterable over the Representations to store MUST NOT be NULL!");
        }
        return store(representations, true);
    }
    @Override
    public final Representation update(Representation representation) throws IllegalArgumentException, YardException {
        if(representation == null){
            throw new IllegalArgumentException("The parsed Representation MUST NOT be NULL!");
        }
        return store(representation,false,true);
    }
    @Override
    public final Iterable<Representation> update(Iterable<Representation> representations) throws YardException, IllegalArgumentException {
        if(representations == null){
            throw new IllegalArgumentException("The parsed Iterable over the Representations to update MUST NOT be NULL!");
        }
        return store(representations,false);
    }
    protected final Iterable<Representation> store(Iterable<Representation> representations,boolean allowCreate) throws IllegalArgumentException, YardException{
        ArrayList<Representation> added = new ArrayList<Representation>();
        for(Representation representation : representations){
            if(representation != null){
                Representation stored = store(representation,allowCreate,false); //reassign
                //to check if the store was successful
                if(stored != null){
                    added.add(stored);
                } else { //can only be the case if allowCreate==false (update was called)
                    log.warn(String.format("Unable to update Representation %s in Yard %s because it is not present!",
                        representation.getId(),getId()));
                }
            } //ignore null values in the parsed Iterable!
        }
        return added;
    }
    protected final Representation store(Representation representation,boolean allowCreate,boolean canNotCreateIsError) throws IllegalArgumentException, YardException{
        if(representation == null) {
            return null;
        }
        log.debug("store Representation " + representation.getId());
        IRI id = new IRI(representation.getId());
        final Lock writeLock = writeLockGraph();
        try {
            Iterator<Triple> current = graph.filter(id, null, null);
            boolean contains = current.hasNext();
            while(current.hasNext()){ //delete current
                current.next();
                current.remove();
            }
            if(!contains && !allowCreate){
                if(canNotCreateIsError) {
                    throw new IllegalArgumentException("Parsed Representation "+representation.getId()+" in not managed by this Yard "+getName()+"(id="+getId()+")");
                } else {
                    return null;
                }
            }
            //get the graph for the Representation and add it to the store
            RdfRepresentation toAdd = ((RdfValueFactory)getValueFactory()).toRdfRepresentation(representation);
            //log.info("  > add "+toAdd.size()+" triples to Yard "+getId());
            Iterator<Triple> it = toAdd.getRdfGraph().filter(toAdd.getNode(), null, null);
            if(!it.hasNext()){
                //TODO: Note somewhere that this Triple is reserved and MUST NOT
                //      be used by externally.
                graph.add(new TripleImpl(toAdd.getNode(), MANAGED_REPRESENTATION, TRUE_LITERAL));
            } else {
                while(it.hasNext()){
                    graph.add(it.next());
                }
            }
            return toAdd;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery parsedQuery) throws YardException, IllegalArgumentException {
        if(parsedQuery == null){
            throw new IllegalArgumentException("The parsed query MUST NOT be NULL!");
        }
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        final ResultSet result = executeSparqlFieldQuery(query);
        //A little bit complex construct ...
        // first we use the adaptingIterator to convert reseource to string
        // to get the resources we have to retrieve the root-variable of the
        // Iterator<SolutionMapping> provided by the ResultSet of the SPARQL query
        Iterator<String> representationIdIterator = new AdaptingIterator<RDFTerm, String>(
                new Iterator<RDFTerm>() {
                    @Override public void remove() { result.remove(); }
                    @Override public RDFTerm next() {
                        return result.next().get(query.getRootVariableName()); }
                    @Override public boolean hasNext() { return result.hasNext(); }
                },
                new Resource2StringAdapter<RDFTerm>(), String.class);
        return new QueryResultListImpl<String>(query,representationIdIterator,String.class);
    }
    /**
     * Returns the SPARQL result set for a given {@link SparqlFieldQuery} that
     * was executed on this yard
     * @param query the SparqlFieldQuery instance
     * @return the results of the SPARQL query in the yard
     * @throws YardException in case the generated SPARQL query could not be parsed
     * or the generated Query is not an SPARQL SELECT query.
     */
    private ResultSet executeSparqlFieldQuery(final SparqlFieldQuery query) throws YardException {
        int limit = QueryUtils.getLimit(query, getConfig().getDefaultQueryResultNumber(), getConfig().getMaxQueryResultNumber());
        SelectQuery sparqlQuery;
        String sparqlQueryString = SparqlQueryUtils.createSparqlSelectQuery(query, false,limit,EndpointTypeEnum.Standard);
        try {
            sparqlQuery = (SelectQuery)QueryParser.getInstance().parse(sparqlQueryString);
        } catch (ParseException e) {
            log.error("ParseException for SPARQL Query in findRepresentation");
            log.error("FieldQuery: "+query);
            log.error("SPARQL Query: "+sparqlQueryString);
            throw new YardException("Unable to parse SPARQL query generated for the parse FieldQuery",e);
        } catch (ClassCastException e){
            log.error("ClassCastExeption because parsed SPARQL Query is not of Type "+SelectQuery.class);
            log.error("FieldQuery: "+query);
            log.error("SPARQL Query: "+sparqlQueryString);
            throw new YardException("Unable to parse SPARQL SELECT query generated for the parse FieldQuery",e);
        }
        return tcManager.executeSparqlQuery(sparqlQuery, graph);
    }
    @Override
    public QueryResultList<Representation> findRepresentation(FieldQuery parsedQuery) throws YardException, IllegalArgumentException {
        if(parsedQuery == null){
            throw new IllegalArgumentException("The parsed query MUST NOT be NULL!");
        }
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        final ResultSet result = executeSparqlFieldQuery(query);
        //Note: An other possibility would be to first iterate over all results and add it to
        //      a list and create this Iterator than based on the List. This would
        //      be the preferenced way if changes in the graph could affect the
        //     Iteration over the SPARQL query results.
        Iterator<Representation> representationIterator = new AdaptingIterator<SolutionMapping, Representation>(
                result, new AdaptingIterator.Adapter<SolutionMapping, Representation>() {
                    /**
                     * Adapter that gets the rootVariable of the Query (selecting the ID)
                     * and creates a Representation for it.
                     * @param solution a solution of the query
                     * @param type the type (no generics here)
                     * @return the representation or <code>null</code> if result is
                     * not an IRI or there is no Representation for the result.
                     */
                    @Override
                    public Representation adapt(SolutionMapping solution, Class<Representation> type) {
                        RDFTerm resource = solution.get(query.getRootVariableName());
                        if(resource instanceof IRI){
                            try {
                                return getRepresentation((IRI)resource,false);
                            } catch (IllegalArgumentException e) {
                                log.warn("Unable to create Representation for ID "+resource+"! -> ignore query result");
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }
                }, Representation.class);
        //NOTE: currently this list iterates in the constructor over all elements
        //      of the Iterator. This means, that all the representations are
        //      created before the method returns.
        return new QueryResultListImpl<Representation>(query,representationIterator,Representation.class);
    }
    @Override
    public final QueryResultList<Representation> find(FieldQuery parsedQuery) throws YardException, IllegalArgumentException {
        if(parsedQuery == null){
            throw new IllegalArgumentException("The parsed query MUST NOT be NULL!");
        }
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        int limit = QueryUtils.getLimit(query, getConfig().getDefaultQueryResultNumber(), getConfig().getMaxQueryResultNumber());
        Query sparqlQuery;
        //NOTE:
        // - use the endpoint type standard, because we do not know what type of
        //   SPARQL implementation is configured for Clerezza via OSGI
        String sparqlQueryString = SparqlQueryUtils.createSparqlConstructQuery(
            query, limit,EndpointTypeEnum.Standard);
        try {
            sparqlQuery = QueryParser.getInstance().parse(sparqlQueryString);
        } catch (ParseException e) {
            log.error("ParseException for SPARQL Query in findRepresentation");
            log.error("FieldQuery: "+query);
            log.error("SPARQL Query: "+sparqlQueryString);
            throw new YardException("Unable to parse SPARQL query generated for the parse FieldQuery",e);
        }
        Object resultObject = tcManager.executeSparqlQuery(sparqlQuery, graph);
        final Graph resultGraph;
        if(resultObject instanceof Graph){
            resultGraph = (Graph)resultObject;
        } else if(resultObject instanceof ImmutableGraph){
            resultGraph = new IndexedGraph();
            resultGraph.addAll((ImmutableGraph)resultObject);
        } else {
            log.error("Unable to create "+Graph.class+" instance for query reults of type "+resultObject.getClass()+" (this indicates that the used SPARQL Query was not of type CONSTRUCT)");
            log.error("FieldQuery: "+query);
            log.error("SPARQL Query: "+sparqlQueryString);
            throw new YardException("Unable to process results of Query");
        }
        return new RdfQueryResultList(query, resultGraph);
    }
}
