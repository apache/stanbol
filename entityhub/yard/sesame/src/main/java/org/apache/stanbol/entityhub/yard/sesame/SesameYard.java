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
package org.apache.stanbol.entityhub.yard.sesame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.query.QueryUtils;
import org.apache.stanbol.entityhub.core.yard.AbstractYard;
import org.apache.stanbol.entityhub.model.sesame.RdfRepresentation;
import org.apache.stanbol.entityhub.model.sesame.RdfValueFactory;
import org.apache.stanbol.entityhub.query.sparql.SparqlEndpointTypeEnum;
import org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery;
import org.apache.stanbol.entityhub.query.sparql.SparqlFieldQueryFactory;
import org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.UnsupportedQueryTypeException;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.openrdf.model.BNode;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the Yard Interface based on a Sesame {@link Repository}. 
 * <p>
 * This is NOT an OSGI component nor service. It is intended to be used by
 * Components that do allow users to configure a Repository implementation.
 * Such components will than create a SesameYard instance and register it as
 * a OSGI service.
 * <p>
 * <b>NOTE</b> This Yard does not {@link Repository#initialize() initialize} 
 * nor {@link Repository#shutDown() shutdown} the Sesame repository. Callers
 * are responsible for that. This is because this Yard implementation does
 * NOT assume exclusive access to the repository. The same repository can be
 * used by multiple Yards (e.g. configured for different 
 * {@link SesameYardConfig#setContexts(String[]) contexts}) or even other
 * components.
 *
 * @author Rupert Westenthaler
 *
 */
public class SesameYard extends AbstractYard implements Yard {
    private static Logger log = LoggerFactory.getLogger(SesameYard.class);
    /**
     * Property used to mark empty Representations managed by this Graph. This is
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
    private static final String MANAGED_REPRESENTATION_URI = "urn:org.apache.stanbol:entityhub.yard:rdf.sesame:managesRepresentation";
    /**
     * used as property for a triple to ensure existence for representations that 
     * do not define yet any triples
     */
    private final URI managedRepresentation;
    /**
     * used as value for a triple to ensure existence for representations that 
     * do not define yet any triples
     */
    private final Value managedRepresentationState;
    /**
     * If inferred Triples are included in operations on this Yard.
     */
    public static final String INCLUDE_INFERRED = "org.apache.stanbol.entityhub.yard.sesame.includeInferred";
    /**
     * By default {@link #INCLUDE_INFERRED} is enabled.
     */
    public static final boolean DEFAULT_INCLUDE_INFERRED = true;
    /**
     * Property used to enable/disable Sesame Context. If <code>false</code> the
     * {@link #CONTEXT_URI} property gets ignored. If <code>true</code> and
     * {@link #CONTEXT_URI} is missing the default context (<code>null</code>) is
     * used. Otherwise the contexts as configured for {@link #CONTEXT_URI} are
     * used.
     */
    public static final String CONTEXT_ENABLED = "org.apache.stanbol.entityhub.yard.sesame.enableContext";
    /**
     * By default the {@link #CONTEXT_ENABLED} feature is disabled.
     */
    public static final boolean DEFAULT_CONTEXT_ENABLED = false;
    
    /**
     * Property used to optionally configure one or more context URIs. empty
     * values are interpreted as <code>null</code>
     */
    public static final String CONTEXT_URI = "org.apache.stanbol.entityhub.yard.sesame.contextUri";

    /**
     * The context used by this yard. Parsed from {@link SesameYardConfig#getContexts()}
     * if <code>{@link SesameYardConfig#isContextEnabled()} == true</code>
     */
    private final URI[] contexts;
    /**
     * The {@link Dataset} similar to {@link #contexts}. Dataset is used for
     * SPARQL queries to enforce results to be restricted to the {@link #contexts}
     */
    private final Dataset dataset;
    /**
     * If inferred triples should be included or not. Configured via
     * {@link SesameYardConfig#isIncludeInferred()}
     */
    private boolean includeInferred;
    /**
     * The {@link Repository} as parsed in the constructor
     */
    private final Repository repository;
    /**
     * The Entityhub ValueFactory used to create Sesame specific Representations,
     * References and Text instances
     */
    private final RdfValueFactory valueFactory;
    /**
     * The Sesame ValueFactory. Shortcut for {@link Repository#getValueFactory()}.
     */
    private final ValueFactory sesameFactory;
    
    /**
     * The {@link URI} for {@link RdfResourceEnum#QueryResultSet}
     */
    private final URI queryRoot;
    /**
     * The {@link URI} for {@link RdfResourceEnum#queryResult}
     */
    private final URI queryResult;
    /**
     * Constructs a SesameYard for the parsed Repository and configuration.
     * @param repo The Repository used by this Yard. The parsed Repository is
     * expected to be initialised.
     * @param config the configuration for the Yard. 
     */
    public SesameYard(Repository repo, SesameYardConfig config) {
        super();
        if(repo == null){
            throw new IllegalArgumentException("The parsed repository MUST NOT be NULL!");
        }
        if(!repo.isInitialized()){
            throw new IllegalArgumentException("The parsed repository MUST BE initialised!");
        }
        this.repository = repo;
        if(config == null){
            throw new IllegalArgumentException("The parsed configuration MUST NOT be NULL!");
        }
        this.sesameFactory = repo.getValueFactory();
        this.valueFactory = new RdfValueFactory(null, sesameFactory);
        this.managedRepresentation = sesameFactory.createURI(MANAGED_REPRESENTATION_URI);
        this.managedRepresentationState = sesameFactory.createLiteral(true);
        this.includeInferred = config.isIncludeInferred();
        //init the super class
        activate(this.valueFactory, SparqlFieldQueryFactory.getInstance(), config);
        if(config.isContextEnabled()){
            //Set the contexts
            String[] contexts = config.getContexts();
            this.contexts = new URI[contexts.length];
            for(int i = 0; i < contexts.length; i++){
                this.contexts[i] = contexts[i] == null ? null : 
                    sesameFactory.createURI(contexts[i]);
            }
        } else {
            this.contexts = new URI[]{};
        }
        //also init the dataset required for SPARQL queries
        if(contexts.length > 0){
            DatasetImpl dataset = new DatasetImpl();
            for(URI context : this.contexts){
                dataset.addNamedGraph(context);
                dataset.addDefaultGraph(context);
            }
            this.dataset = dataset;
        } else {
            this.dataset = null;
        }
        queryRoot = sesameFactory.createURI(RdfResourceEnum.QueryResultSet.getUri());
        queryResult = sesameFactory.createURI(RdfResourceEnum.queryResult.getUri());
    }
    
    /**
     * Closes this Yard, but <b>does not</b> close the Sesame Repository!
     */
    public void close(){
        //init the super class
        deactivate();
    }
    
    /**
     * Getter for the context URI used by this yard.
     * @return the URI used for the RDF graph that stores all the data of this
     * yard.
     */
    public final URI[] getContexts(){
        return contexts;
    }

    @Override
    public Representation getRepresentation(String id) throws YardException{
        if(id == null){
            throw new IllegalArgumentException("The parsed representation id MUST NOT be NULL!");
        }
        if(id.isEmpty()){
            throw new IllegalArgumentException("The parsed representation id MUST NOT be EMTPY!");
        }
        RepositoryConnection con = null;
        try {
            con = repository.getConnection();
            con.begin();
            Representation rep = getRepresentation(con, sesameFactory.createURI(id), true);
            con.commit();
            return rep;
        } catch (RepositoryException e) {
            throw new YardException("Unable to get Representation "+id, e);
        } finally {
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {}
            }
        }
    }
    /**
     * Internally used to create Representations for URIs
     * @param uri the uri
     * @param check if <code>false</code> than there is no check if the URI
     *     refers to a Resource in the graph that is of type {@link #REPRESENTATION}
     * @return the Representation
     */
    protected final Representation getRepresentation(RepositoryConnection con, URI uri, boolean check) throws RepositoryException {
        if(!check || isRepresentation(con,uri)){
            return createRepresentationGraph(con, valueFactory, uri);
        } else {
            return null; //not found
        }
    }

    /**
     * Extracts the triples that belong to the {@link Representation} with the
     * parsed id from the Sesame repository.
     * @param con the repository connection
     * @param valueFactory the {@link RdfValueFactory} to use
     * @param uri the subject of the Representation to extract
     * @return the representation with the extracted data.
     * @throws RepositoryException 
     */
    protected RdfRepresentation createRepresentationGraph(RepositoryConnection con, RdfValueFactory valueFactory, URI uri) throws RepositoryException{
        RdfRepresentation rep = valueFactory.createRdfRepresentation(uri);
        Model model = rep.getModel();
        extractRepresentation(con, model, uri, new HashSet<BNode>());
        return rep;
    }
    /**
     * Recursive Method internally doing all the work for 
     * {@link #createRepresentationGraph(UriRef, TripleCollection)}
     * @param con the repository connection to read the data from
     * @param model The model to add the statements retrieved
     * @param node the current node. Changes in recursive calls as it follows
     * @param visited holding all the visited BNodes to avoid cycles. Other nodes 
     * need not be added because this implementation would not follow it anyway
     * outgoing relations if the object is a {@link BNode} instance.
     * @throws RepositoryException 
     */
    private void extractRepresentation(RepositoryConnection con,Model model, Resource node, Set<BNode> visited) throws RepositoryException{
        //we need all the outgoing relations and also want to follow bNodes until
        //the next UriRef. However we are not interested in incoming relations!
        RepositoryResult<Statement> outgoing = con.getStatements(node, null, null, includeInferred, contexts);
        Statement statement;
        Set<BNode> bnodes = new HashSet<BNode>();
        while(outgoing.hasNext()){
            statement = outgoing.next();
            model.add(statement);
            Value object = statement.getObject();
            if(object instanceof BNode && !visited.contains(object)){
                bnodes.add((BNode)object);
            }
        }
        outgoing.close();
        for(BNode bnode : bnodes){
            visited.add(bnode);
            //TODO: recursive calls could cause stackoverflows with wired graphs
            extractRepresentation(con, model, bnode, visited);
        }
    }

    @Override
    public boolean isRepresentation(String id) throws YardException {
        if(id == null) {
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL!");
        }
        if(id.isEmpty()){
            throw new IllegalArgumentException("The parsed id MUST NOT be EMPTY!");
        }
        RepositoryConnection con = null;
        try {
            con = repository.getConnection();
            con.begin();
            boolean state = isRepresentation(con, sesameFactory.createURI(id));
            con.commit();
            return state;
        } catch (RepositoryException e) {
            throw new YardException("Unable to check for Representation "+id, e);
        } finally {
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {}
            }
        }
    }
    /**
     * Internally used to check if a URI resource represents an representation
     * @param con the repository connection
     * @param subject the subject URI of the representation to check
     * @return the state
     * @throws RepositoryException 
     */
    protected final boolean isRepresentation(RepositoryConnection con , URI subject) throws RepositoryException{
        return con.hasStatement(subject, null, null, includeInferred, contexts);
    }

    @Override
    public void remove(String id) throws YardException, IllegalArgumentException {
        if(id == null) {
            throw new IllegalArgumentException("The parsed Representation id MUST NOT be NULL!");
        }
        RepositoryConnection con = null;
        try {
            con = repository.getConnection();
            con.begin();
            remove(con, sesameFactory.createURI(id));
            con.commit();
        } catch (RepositoryException e) {
            throw new YardException("Unable to remove for Representation "+id, e);
        } finally {
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {}
            }
        }
    }
    /**
     * Internally used to remove a Representation from the Repository. <p>
     * NOTE: this does not remove any {@link Statement}s for {@link BNode}s
     * beeing {@link Statement#getObject() object}s of the parsed subjects.
     * @param con the connection
     * @param subject the subject of the Representation to remove
     * @throws RepositoryException 
     */
    protected void remove(RepositoryConnection con, URI subject) throws RepositoryException{
        con.remove(subject, null, null, contexts);
    }
    
    @Override
    public final void remove(Iterable<String> ids) throws IllegalArgumentException, YardException {
        if(ids == null){
            throw new IllegalArgumentException("The parsed Iterable over the IDs to remove MUST NOT be NULL!");
        }
        RepositoryConnection con = null;
        try {
            con = repository.getConnection();
            con.begin();
            for(String id : ids){
                if(id != null){
                    remove(con, sesameFactory.createURI(id));
                }
            }
            con.commit();
        } catch (RepositoryException e) {
            throw new YardException("Unable to remove parsed Representations", e);
        } finally {
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {}
            }
        }
    }
    @Override
    public final void removeAll() throws YardException {
        RepositoryConnection con = null;
        try {
            con = repository.getConnection();
            con.begin();
            con.clear(contexts); //removes everything
            con.commit();
        } catch (RepositoryException e) {
            throw new YardException("Unable to remove parsed Representations", e);
        } finally {
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {}
            }
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
        RepositoryConnection con = null;
        try {
            con = repository.getConnection();
            con.begin();
            ArrayList<Representation> added = new ArrayList<Representation>();
            for(Representation representation : representations){
                if(representation != null){
                    Representation stored = store(con, representation,allowCreate,false); //reassign
                    //to check if the store was successful
                    if(stored != null){
                        added.add(stored);
                    } else { //can only be the case if allowCreate==false (update was called)
                        log.warn(String.format("Unable to update Representation %s in Yard %s because it is not present!",
                            representation.getId(),getId()));
                    }
                } //ignore null values in the parsed Iterable!
            }
            con.commit();
            return added;
        } catch (RepositoryException e) {
            throw new YardException("Unable to remove parsed Representations", e);
        } catch (IllegalArgumentException e) {
            try { 
                //to avoid Exception logs in case store(..) throws an Exception
                //in the case allowCreate and canNotCreateIsError do not allow
                //the store operation
                con.rollback();
            } catch (RepositoryException ignore) {}
            throw e;
        } finally {
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {}
            }
        }
    }
    /**
     * Generic store method used by store and update methods
     * @param representation the representation to store/update
     * @param allowCreate if new representation are allowed to be created
     * @param canNotCreateIsError if updates to existing one are allowed
     * @return the representation as added to the yard
     * @throws IllegalArgumentException
     * @throws YardException
     */
    protected final Representation store(Representation representation,boolean allowCreate,boolean canNotCreateIsError) throws IllegalArgumentException, YardException{
        RepositoryConnection con = null;
        try {
            con = repository.getConnection();
            con.begin();
            Representation added = store(con,representation,allowCreate,canNotCreateIsError);
            con.commit();
            return added;
        } catch (RepositoryException e) {
            throw new YardException("Unable to remove parsed Representations", e);
        } catch (IllegalArgumentException e) {
            try { 
                //to avoid Exception logs in case store(..) throws an Exception
                //in the case allowCreate and canNotCreateIsError do not allow
                //the store operation
                con.rollback();
            } catch (RepositoryException ignore) {}
            throw e;
        } finally {
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {}
            }
        }
    }        
    protected final Representation store(RepositoryConnection con, Representation representation,boolean allowCreate,boolean canNotCreateIsError) throws IllegalArgumentException, RepositoryException {
        if(representation == null) {
            return null;
        }
        log.debug("store Representation " + representation.getId());
        URI subject = sesameFactory.createURI(representation.getId());
        boolean contains = con.hasStatement(subject, null, null, includeInferred, contexts);
        con.remove(subject, null, null, contexts);
        if(!contains && !allowCreate){
            if(canNotCreateIsError) {
                throw new IllegalArgumentException("Parsed Representation "+representation.getId()+" in not managed by this Yard "+getName()+"(id="+getId()+")");
            } else {
                return null;
            }
        }
        //get the graph for the Representation and add it to the store
        RdfRepresentation toAdd = valueFactory.toRdfRepresentation(representation);
        if(toAdd.getModel().isEmpty()){
            con.add(toAdd.getURI(),managedRepresentation,managedRepresentationState, contexts);
        } else {
            con.add(toAdd.getModel(), contexts);
        }
        return toAdd;
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery parsedQuery) throws YardException, IllegalArgumentException {
        if(parsedQuery == null){
            throw new IllegalArgumentException("The parsed query MUST NOT be NULL!");
        }
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        RepositoryConnection con = null;
        TupleQueryResult results = null;
        try {
            con = repository.getConnection();
            con.begin();
            //execute the query
            int limit = QueryUtils.getLimit(query, getConfig().getDefaultQueryResultNumber(),
                getConfig().getMaxQueryResultNumber());
            results = executeSparqlFieldQuery(con, query, limit, false);
            //parse the results
            List<String> ids = limit > 0 ? new ArrayList<String>(limit) : new ArrayList<String>();
            while(results.hasNext()){
                BindingSet result = results.next();
                Value value = result.getValue(query.getRootVariableName());
                if(value instanceof Resource){
                    ids.add(value.stringValue());
                }
            }
            con.commit();
            return new QueryResultListImpl<String>(query,ids,String.class);
        } catch (RepositoryException e) {
            throw new YardException("Unable to execute findReferences query", e);
        } catch (QueryEvaluationException e) {
            throw new YardException("Unable to execute findReferences query", e);
        } finally {
            if(results != null) { //close the result if present
                try {
                    results.close();
                } catch (QueryEvaluationException ignore) {/* ignore */}
            }
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {/* ignore */}
            }
        }
    }

    /**
     * Returns the SPARQL result set for a given {@link SparqlFieldQuery} that
     * was executed on this yard
     * @param con the repository connection to use
     * @param fieldQuery the SparqlFieldQuery instance
     * @param limit the maximum number of results
     * @return the results of the SPARQL query in the {@link #contexts} of the
     * Sesame Repository 
     * @throws RepositoryException on any error while using the parsed connection
     * @throws QueryEvaluationException  on any error while executing the query
     * @throws YardException if the SPARQL query created for the parsed FieldQuery
     * was illegal formatted or if the {@link #repository} does not support 
     * SPARQL.
     */
    private TupleQueryResult executeSparqlFieldQuery(RepositoryConnection con, final SparqlFieldQuery fieldQuery, int limit, boolean select) throws RepositoryException, YardException, QueryEvaluationException {
        log.debug("> execute FieldQuery: {}", fieldQuery);
        String sparqlQueryString = SparqlQueryUtils.createSparqlSelectQuery(
            fieldQuery, select, limit, SparqlEndpointTypeEnum.Sesame);
        log.debug(" - SPARQL Query: {}", sparqlQueryString);
        TupleQuery sparqlOuery;
        try {
            sparqlOuery = con.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQueryString);
        } catch (MalformedQueryException e) {
            log.error("Unable to pparse SPARQL Query generated for a FieldQuery");
            log.error("FieldQuery: {}",fieldQuery);
            log.error("SPARQL Query: {}",sparqlQueryString);
            log.error("Exception ", e);
            throw new YardException("Unable to parse SPARQL query generated for the parse FieldQuery", e);
        } catch (UnsupportedQueryTypeException e) {
            String message = "The Sesame Repository '" + repository + "'(class: "
                    + repository.getClass().getName() + ") does not support SPARQL!";
            log.error(message, e);
            throw new YardException(message, e);
        }
        if(dataset != null){ //respect the configured contexts
            sparqlOuery.setDataset(dataset);
        }
        return sparqlOuery.evaluate();
    }
    
    @Override
    public QueryResultList<Representation> findRepresentation(FieldQuery parsedQuery) throws YardException, IllegalArgumentException {
        if(parsedQuery == null){
            throw new IllegalArgumentException("The parsed query MUST NOT be NULL!");
        }
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        RepositoryConnection con = null;
        TupleQueryResult results = null;
        try {
            con = repository.getConnection();
            con.begin();
            //execute the query
            int limit = QueryUtils.getLimit(query, getConfig().getDefaultQueryResultNumber(),
                getConfig().getMaxQueryResultNumber());
            results = executeSparqlFieldQuery(con,query, limit, false);
            //parse the results and generate the Representations
            //create an own valueFactors so that all the data of the query results
            //are added to the same Sesame Model
            Model model = new TreeModel();
            RdfValueFactory valueFactory = new RdfValueFactory(model, sesameFactory);
            List<Representation> representations = limit > 0 ? 
                    new ArrayList<Representation>(limit) : new ArrayList<Representation>();
            while(results.hasNext()){
                BindingSet result = results.next();
                Value value = result.getValue(query.getRootVariableName());
                if(value instanceof URI){
                    //copy all data to the model and create the representation
                    RdfRepresentation rep = createRepresentationGraph(con, valueFactory, (URI)value); 
                    model.add(queryRoot, queryResult, value); //link the result with the query result
                    representations.add(rep);
                } //ignore non URI results
            }
            con.commit();
            return new SesameQueryResultList(model, query, representations);
        } catch (RepositoryException e) {
            throw new YardException("Unable to execute findReferences query", e);
        } catch (QueryEvaluationException e) {
            throw new YardException("Unable to execute findReferences query", e);
        } finally {
            if(results != null) { //close the result if present
                try {
                    results.close();
                } catch (QueryEvaluationException ignore) {/* ignore */}
            }
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {/* ignore */}
            }
        }
    }
    @Override
    public final QueryResultList<Representation> find(FieldQuery parsedQuery) throws YardException, IllegalArgumentException {
        if(parsedQuery == null){
            throw new IllegalArgumentException("The parsed query MUST NOT be NULL!");
        }
        final SparqlFieldQuery query = SparqlFieldQueryFactory.getSparqlFieldQuery(parsedQuery);
        RepositoryConnection con = null;
        TupleQueryResult results = null;
        try {
            con = repository.getConnection();
            con.begin();
            //execute the query
            int limit = QueryUtils.getLimit(query, getConfig().getDefaultQueryResultNumber(),
                getConfig().getMaxQueryResultNumber());
            results = executeSparqlFieldQuery(con,query, limit, true);
            //parse the results and generate the Representations
            //create an own valueFactors so that all the data of the query results
            //are added to the same Sesame Model
            Model model = new TreeModel();
            RdfValueFactory valueFactory = new RdfValueFactory(model, sesameFactory);
            List<Representation> representations = limit > 0 ? new ArrayList<Representation>(limit)
                    : new ArrayList<Representation>();
            Map<String,URI> bindings = new HashMap<String,URI>(query.getFieldVariableMappings().size());
            for(Entry<String,String> mapping : query.getFieldVariableMappings().entrySet()){
                bindings.put(mapping.getValue(), sesameFactory.createURI(mapping.getKey()));
            }
            while(results.hasNext()){
                BindingSet result = results.next();
                Value value = result.getValue(query.getRootVariableName());
                if(value instanceof URI){
                    URI subject = (URI) value;
                    //link the result with the query result
                    model.add(queryRoot, queryResult, subject);
                    //now copy over the other selected data
                    for(String binding : result.getBindingNames()){
                        URI property = bindings.get(binding);
                        if(property != null){
                            model.add(subject, property, result.getValue(binding));
                        } //else no mapping for the query.getRootVariableName()
                    }
                    //create a representation and add it to the results
                    representations.add(valueFactory.createRdfRepresentation(subject));
                } //ignore non URI results
            }
            con.commit();
            return new SesameQueryResultList(model, query, representations);
        } catch (RepositoryException e) {
            throw new YardException("Unable to execute findReferences query", e);
        } catch (QueryEvaluationException e) {
            throw new YardException("Unable to execute findReferences query", e);
        } finally {
            if(results != null) { //close the result if present
                try {
                    results.close();
                } catch (QueryEvaluationException ignore) {/* ignore */}
            }
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException ignore) {/* ignore */}
            }
        }
    }
    /**
     * Wrapper that converts a Sesame {@link TupleQueryResult} to a {@link Iterator}.
     * <b>NOTE</b> this will not close the {@link TupleQueryResult}!
     * @author Rupert westenthaler
     *
     */
    static class TupleResultIterator implements Iterator<BindingSet> {

        private final TupleQueryResult resultList;

        public TupleResultIterator(TupleQueryResult resultList) {
            this.resultList = resultList;
        }
        @Override
        public boolean hasNext() {
            try {
                return resultList.hasNext();
            } catch (QueryEvaluationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public BindingSet next() {
            try {
                return resultList.next();
            } catch (QueryEvaluationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported by Sesame TupleQueryResult");
        }
        
    }
}
