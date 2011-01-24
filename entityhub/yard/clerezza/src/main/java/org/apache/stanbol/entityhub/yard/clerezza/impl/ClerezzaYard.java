package org.apache.stanbol.entityhub.yard.clerezza.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.query.QueryUtils;
import org.apache.stanbol.entityhub.core.utils.AdaptingIterator;
import org.apache.stanbol.entityhub.core.yard.AbstractYard;
import org.apache.stanbol.entityhub.core.yard.DefaultYardConfig;
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
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Yard Interface based on a RDF Triple Store. This
 * Implementation uses Clerezza as RDF Framework. The actual Triple Store used
 * to store the data depends on the configuration of Clerezza.<p>
 * This implementation uses {@link LockableMGraph} interface for write locks
 * when updating the graph. SPARQL queries are not within a write lock.<p>
 *
 * @author Rupert Westenthaler
 *
 */
@Component(
        //name="org.apache.stanbol.entityhub.yard.clerezzaYard",
        //factory="org.apache.stanbol.entityhub.yard.clerezzaYardFactory",
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE, //the ID is required!
        specVersion="1.1",
        metatype = true
        )
@Service
//@Properties(value={
//        @Property(name=Yard.ID,value="entityhubYard"),
//        @Property(name=Yard.NAME,value="Entityhub Yard"),
//        @Property(name=Yard.DESCRIPTION,value="Default values for configuring the Entityhub Yard without editing")
//})
public class ClerezzaYard extends AbstractYard implements Yard {
    Logger log = LoggerFactory.getLogger(ClerezzaYard.class);
    public static final String YARD_URI_PREFIX = "urn:org.apache.stanbol:entityhub.yard:rdf.clerezza:";
    public static final UriRef REPRESENTATION = new UriRef(RdfResourceEnum.Representation.getUri());
    /**
     * This property is used to check if a URI in the graph represents a representation by
     * calling {@link TripleCollection#filter(org.apache.clerezza.rdf.core.NonLiteral, UriRef, Resource)}
     * with the reuqested ID as subject, this {@link UriRef} as property and
     * <code>null</code> as value.<p>
     * This is the easiest way to do that, because each representation MUST HAVE
     * a entityhub:label. If this is requirements is changed in future, than the code
     * using this property MUST BE changed accordingly!
     */
    private static UriRef ENTITYHUB_LABEL_URIREF = new UriRef(RdfResourceEnum.label.getUri());
//    protected ComponentContext context;
//    protected Dictionary<String,?> properties;
    @Reference
    private TcManager tcManager;
    private UriRef yardGraphUri;
    private LockableMGraph graph;

    public ClerezzaYard() {
        super();
    }
    public ClerezzaYard(String yardId) {
        super();
    }
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        log.info("in "+ClerezzaYard.class+" activate with context "+context);
        if(context == null || context.getProperties() == null){
            throw new IllegalStateException("No valid"+ComponentContext.class+" parsed in activate!");
        }
        activate(new DefaultYardConfig(context.getProperties()));
    }
    protected final void activate(YardConfig config) throws ConfigurationException,IllegalArgumentException {
        super.activate(RdfValueFactory.getInstance(), SparqlFieldQueryFactory.getInstance(), config);
        this.yardGraphUri = new UriRef(YARD_URI_PREFIX+config.getId());
        try {
            this.graph = tcManager.getMGraph(yardGraphUri);
            log.info("  ... (re)use existing Graph "+yardGraphUri+" for Yard "+config.getName());
        } catch (NoSuchEntityException e) {
            log.info("   ... create new Graph "+yardGraphUri+" for Yard "+config.getName()+"!");
            this.graph =  tcManager.createMGraph(yardGraphUri);
        }

    }
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in "+ClerezzaYard.class.getSimpleName()+" deactivate with context "+context);
        this.yardGraphUri = null;
        this.graph = null;
        super.deactivate();
    }
    /**
     * Getter for the URI used for the named graph. The returned value is
     * {@link #YARD_URI_PREFIX}+{@link #getId()}.
     * @return the URI used for the RDF graph that stores all the data of this
     * yard.
     */
    public String getYardGraphUri(){
        return yardGraphUri.getUnicodeString();
    }

    @Override
    public Representation getRepresentation(String id) {
        if(id == null){
            return null;
        }
        return getRepresentation(new UriRef(id),true);
    }
    /**
     * Internally used to create Representations for URIs
     * @param uri the uri
     * @param check if <code>false</code> than there is no check if the URI
     *     refers to a Resource in the graph that is of type {@link #REPRESENTATION}
     * @return the Representation
     */
    protected Representation getRepresentation(UriRef uri, boolean check) {
        if(!check || graph.filter(uri, ENTITYHUB_LABEL_URIREF, null).hasNext()){
            /*
             * We need to use an own graph for the Representation, because
             * changes to the Representation should not be reflected in the
             * Yard until a store() or update().
             * Currently the GraphNode.getNodeContext() functionality is used
             * to calculate the graph included for the Representation.
             */
            GraphNode node = new GraphNode(uri, graph);
            //create a changeable graph for the representation, because
            //node.getNodeContext returns an immutable Graph!
            MGraph nodeGraph = new SimpleMGraph(node.getNodeContext());
            return ((RdfValueFactory)valueFactory).createRdfRepresentation(uri, nodeGraph);
        } else {
            return null; //not found
        }
    }

    @Override
    public boolean isRepresentation(String id) {
        return id!=null?graph.filter(new UriRef(id), ENTITYHUB_LABEL_URIREF , null).hasNext():null;
    }

    @Override
    public void remove(String id) throws IllegalArgumentException {
        if(id == null) return;
        UriRef resource = new UriRef(id);
        Lock writeLock = graph.getLock().writeLock();
        writeLock.lock();
        try {
            if(graph.filter(resource, RDF.type, REPRESENTATION).hasNext()){
                GraphNode node = new GraphNode(resource, graph);
                /*
                 * Currently the "context" of the Clerezza GraphNode implementation
                 * is used for CRUD operations on Representations.
                 * This includes incoming and outgoing relations the resource and
                 * recursively bNodes.
                 */
                node.deleteNodeContext();
            } //else not found  -> nothing to do
        }finally {
            writeLock.unlock();
        }
    }
    @Override
    public void remove(Iterable<String> ids) throws IllegalArgumentException, YardException {
        if(ids == null){
            throw new IllegalArgumentException("The parsed Iterable over the IDs to remove MUST NOT be NULL!");
        }
        for(String id : ids){
            remove(id);
        }
    }
    @Override
    public Representation store(Representation representation) throws IllegalArgumentException, YardException {
        return store(representation,true,true);
    }
    @Override
    public Iterable<Representation> store(Iterable<Representation> representations) throws IllegalArgumentException, YardException {
        if(representations == null){
            throw new IllegalArgumentException("The parsed Iterable over the Representations to store MUST NOT be NULL!");
        }
        return store(representations, true);
    }
    @Override
    public Representation update(Representation representation) throws IllegalArgumentException, YardException {
        return store(representation,false,true);
    }
    @Override
    public Iterable<Representation> update(Iterable<Representation> representations) throws YardException, IllegalArgumentException {
        if(representations == null){
            throw new IllegalArgumentException("The parsed Iterable over the Representations to update MUST NOT be NULL!");
        }
        return store(representations,false);
    }
    protected final Iterable<Representation> store(Iterable<Representation> representations,boolean allowCreate) throws IllegalArgumentException, YardException{
        ArrayList<Representation> added = new ArrayList<Representation>();
        for(Representation representation : representations){
            added.add(store(representation,allowCreate,false));
        }
        return added;
    }
    protected final Representation store(Representation representation,boolean allowCreate,boolean canNotCreateIsError) throws IllegalArgumentException, YardException{
        log.info("store Representation "+representation.getId());
//        log.info("  > entityhub size: "+graph.size());
        if(representation == null) return null;
        if(isRepresentation(representation.getId())){
//            log.info("  > remove previous version");
            remove(representation.getId());
//            log.info("  > entityhub size: "+graph.size());
        } else if(!allowCreate){
            if(canNotCreateIsError) {
                throw new YardException("Parsed Representation "+representation.getId()+" in not managed by this Yard "+getName()+"(id="+getId()+")");
            } else {
                return null;
            }
        }
        //get the graph for the Representation and add it to the store
        RdfRepresentation toAdd = ((RdfValueFactory)valueFactory).toRdfRepresentation(representation);
//        log.info("  > add "+toAdd.size()+" triples to Yard "+getId());
        Lock writeLock = graph.getLock().writeLock();
        writeLock.lock();
        try {
            graph.addAll(toAdd.getRdfGraph());
        } finally {
            writeLock.unlock();
        }
//        log.info("  > currently "+graph.size()+" triples in Yard "+getId());
        return toAdd;
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery parsedQuery) throws YardException, IllegalArgumentException {
        if(parsedQuery == null){
            throw new IllegalArgumentException("The parsed query MUST NOT be NULL!");
        }
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        int limit = QueryUtils.getLimit(query, config.getDefaultQueryResultNumber(), config.getMaxQueryResultNumber());
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
        final ResultSet result = tcManager.executeSparqlQuery((SelectQuery)sparqlQuery, graph);
        //A little bit complex construct ...
        // first we use the adaptingIterator to convert reseource to string
        // to get the resources we have to retrieve the root-variable of the
        // Iterator<SolutionMapping> provided by the ResultSet of the SPARQL query
        Iterator<String> representationIdIterator = new AdaptingIterator<Resource, String>(
                new Iterator<Resource>() {
                    @Override public void remove() { result.remove(); }
                    @Override public Resource next() {
                        return result.next().get(query.getRootVariableName()); }
                    @Override public boolean hasNext() { return result.hasNext(); }
                },
                new Resource2StringAdapter<Resource>(), String.class);
        return new QueryResultListImpl<String>(query,representationIdIterator,String.class);
    }
    @Override
    public QueryResultList<Representation> findRepresentation(FieldQuery parsedQuery) throws YardException, IllegalArgumentException {
        if(parsedQuery == null){
            throw new IllegalArgumentException("The parsed query MUST NOT be NULL!");
        }
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        int limit = QueryUtils.getLimit(query, config.getDefaultQueryResultNumber(), config.getMaxQueryResultNumber());
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
        final ResultSet result = tcManager.executeSparqlQuery((SelectQuery)sparqlQuery, graph);
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
                     * not an UriRef or there is no Representation for the result.
                     */
                    @Override
                    public Representation adapt(SolutionMapping solution, Class<Representation> type) {
                        Resource resource = solution.get(query.getRootVariableName());
                        if(resource != null && resource instanceof UriRef){
                            try {
                                return getRepresentation((UriRef)resource,false);
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
    public QueryResultList<Representation> find(FieldQuery parsedQuery) throws YardException, IllegalArgumentException {
        if(parsedQuery == null){
            throw new IllegalArgumentException("The parsed query MUST NOT be NULL!");
        }
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        int limit = QueryUtils.getLimit(query, config.getDefaultQueryResultNumber(), config.getMaxQueryResultNumber());
        Query sparqlQuery;
        //NOTE(s):
        // - parse RdfResourceEnum.representationType as additional field, because
        //   this info is needed to correctly init the Representations
        // - use the endpoint type standard, because we do not know what type of
        //   SPARQL implementation is configured for Clerezza via OSGI
        String sparqlQueryString = SparqlQueryUtils.createSparqlConstructQuery(query, limit,EndpointTypeEnum.Standard,RdfResourceEnum.signType.getUri());
        try {
            sparqlQuery = QueryParser.getInstance().parse(sparqlQueryString);
        } catch (ParseException e) {
            log.error("ParseException for SPARQL Query in findRepresentation");
            log.error("FieldQuery: "+query);
            log.error("SPARQL Query: "+sparqlQueryString);
            throw new YardException("Unable to parse SPARQL query generated for the parse FieldQuery",e);
        }
        Object resultObject = tcManager.executeSparqlQuery(sparqlQuery, graph);
        final MGraph resultGraph;
        if(resultObject instanceof MGraph){
            resultGraph = (MGraph)resultObject;
        } else if(resultObject instanceof Graph){
            resultGraph = new SimpleMGraph();
            resultGraph.addAll((Graph)resultObject);
        } else {
            log.error("Unable to create "+MGraph.class+" instance for query reults of type "+resultObject.getClass()+" (this indicates that the used SPARQL Query was not of type CONSTRUCT)");
            log.error("FieldQuery: "+query);
            log.error("SPARQL Query: "+sparqlQueryString);
            throw new YardException("Unable to process results of Query");
        }
        return new RdfQueryResultList(query, resultGraph);
    }
}
