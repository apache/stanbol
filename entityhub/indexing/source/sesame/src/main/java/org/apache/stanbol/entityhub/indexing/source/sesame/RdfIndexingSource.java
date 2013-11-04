package org.apache.stanbol.entityhub.indexing.source.sesame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityDataProvider;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.model.sesame.RdfRepresentation;
import org.apache.stanbol.entityhub.model.sesame.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.openrdf.model.BNode;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigUtil;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryRegistry;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.api.backend.RDFBackend;

public class RdfIndexingSource extends AbstractSesameBackend implements EntityDataIterable, EntityDataProvider, RDFBackend<Value> {

    private final Logger log = LoggerFactory.getLogger(RdfIndexingSource.class);
    
    public static final String PARAM_REPOSITORY_CONFIG = "config";
    
    public static final String DEFAULT_REPOSITORY_CONFIG = "repository.ttl";
    
    protected ValueFactory sesameFactory;
    
    protected RdfValueFactory vf;
    
    Repository repository;
    //protected RepositoryConnection connection;

    
    
    /**
     * If {@link BNode} being values of outgoing triples should be followed.
     */
    protected boolean followBNodeState = true; //TODO: make configurable

    private Resource[] contexts = new Resource[]{}; //TODO: make configurable

    private boolean includeInferred = true; //TODO: make configurable
    
    protected RepositoryConfig repoConfig;
    private RepositoryConnection ldpathConnection;
    private Lock ldpathConnectionLock = new ReentrantLock();
    
    private RepositoryConnection entityDataProviderConnection;
    private Lock entityDataProviderConnectionLock = new ReentrantLock();
    /**
     * {@link EntityDataIterator}s created by {@link #entityDataIterator()}
     * do add themselves to this list while active. calling {@link #close()}
     * to this indexing source will also call close to all iterators in this list
     */
    protected final List<EntityDataIterator> entityDataIterators = new CopyOnWriteArrayList<EntityDataIterator>();
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        File repoConfigFile;
        Object value = config.get(PARAM_REPOSITORY_CONFIG);
        if(value != null){
            repoConfigFile = new File(indexingConfig.getConfigFolder(),value.toString());
        } else {
            repoConfigFile = new File(indexingConfig.getConfigFolder(),DEFAULT_REPOSITORY_CONFIG);
        }
        if(repoConfigFile.isFile()){ //read the config (an RDF file)
            
            this.repoConfig = loadRepositoryConfig(repoConfigFile);
        } else {
            throw new IllegalArgumentException("The configured Sesame Repository configuration fiel "
                + repoConfigFile +" is missing. Please use the '"+PARAM_REPOSITORY_CONFIG 
                + "' paramteter to configure the actual configuration file (relative "
                + "to the config '"+indexingConfig.getConfigFolder()+"'folder)");
        }
    }

    /**
     * @param repoConfigFile
     * @return
     */
    private RepositoryConfig loadRepositoryConfig(File repoConfigFile) {
        Repository configRepo = new SailRepository(new MemoryStore());
        RepositoryConnection con = null;
        try {
            con = configRepo.getConnection();
            RDFFormat format = Rio.getParserFormatForFileName(repoConfigFile.getName());
            try {
                con.add(new InputStreamReader(
                    new FileInputStream(repoConfigFile),Charset.forName("UTF-8")), 
                    null, format);
            } catch (RDFParseException e) {
                throw new IllegalArgumentException("Unable to parsed '"
                    + repoConfigFile+ "' using RDF format '"+ format +"'!", e);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to access '"
                        + repoConfigFile+ "'!", e);
            }
            con.commit();
        } catch (RepositoryException e) {
            throw new IllegalStateException("Unable to load '"
                    + repoConfigFile+ "' to inmemory Sail!", e);
        } finally {
            if(con != null){
                try {
                    con.close();
                } catch (RepositoryException e) {/* ignore */}
            }
        }
        Set<String> repoNames;
        RepositoryConfig repoConfig;
        try {
            repoNames = RepositoryConfigUtil.getRepositoryIDs(configRepo);
            if(repoNames.size() == 1){
                repoConfig = RepositoryConfigUtil.getRepositoryConfig(configRepo, repoNames.iterator().next());
                repoConfig.validate();
            } else if(repoNames.size() > 1){
                throw new IllegalArgumentException("Repository configuration file '"
                    +repoConfigFile+"' MUST only contain a single repository configuration!");
            } else {
                throw new IllegalArgumentException("Repository configuration file '"
                        +repoConfigFile+"' DOES NOT contain a repository configuration!");
            }
        } catch (RepositoryException e) {
            throw new IllegalStateException("Unable to read RepositoryConfiguration form the "
                + "in-memory Sail!",e);
        } catch (RepositoryConfigException e) {
            throw new IllegalArgumentException("Repository Configuration in '"
                + repoConfigFile + "is not valid!",e);
        } finally {
            try {
                configRepo.shutDown();
            } catch (RepositoryException e) { /* ignore */ }
        }
        if(repoConfig.getRepositoryImplConfig() == null){
            throw new IllegalArgumentException("Missing RepositoryImpl config for "
                + "config "+repoConfig.getID()+" of file "+repoConfigFile+"!");
        }
        return repoConfig;
    }

    @Override
    public boolean needsInitialisation() {
        return true;
    }

    @Override
    public void initialise() {
        // TODO create the Sesame Connection
        RepositoryFactory factory = RepositoryRegistry.getInstance().get(
            repoConfig.getRepositoryImplConfig().getType());
        if(factory == null){
            throw new IllegalStateException("Unable to initialise Repository (id: "
                + repoConfig.getID()+ ", title: "+repoConfig.getTitle() + ", impl: "
                + repoConfig.getRepositoryImplConfig().getType()+") because no "
                + "RepositoryFactory is present for the specified implementation!");
        }
        try {
            repository = factory.getRepository(repoConfig.getRepositoryImplConfig());
        } catch (RepositoryConfigException e) {
            throw new IllegalStateException("Unable to initialise Repository (id: "
                + repoConfig.getID()+ ", title: "+repoConfig.getTitle() + ", impl: "
                + repoConfig.getRepositoryImplConfig().getType()+")!", e);
        }
    }

    @Override
    public void close() {
        //first close still active RdfEntityDataIterator instances
        for(EntityDataIterator edi : entityDataIterators){
            edi.close();
        }
        //close connections used for LDPath and EntityDataProvider
        ungetLdPathConnection();
        ungetEntityDataProviderConnection();
        //finally shutdown the repository
        try {
            repository.shutDown();
        } catch (RepositoryException e) {
            log.warn("Error while closing Sesame Connection", e);
        }
    }

    @Override
    public Representation getEntityData(String id) {
        try {
            return createRepresentationGraph(getEntityDataProviderConnection(),
                sesameFactory.createURI(id));
        } catch (RepositoryException e) {
            ungetEntityDataProviderConnection();
            throw new IllegalStateException("Unable to create Representation '"
                    + id + "'!", e);
        }
    }

    @Override
    public EntityDataIterator entityDataIterator() {
        try {
            return new RdfEntityDataIterator(followBNodeState, includeInferred, contexts);
        } catch (RepositoryException e) {
            throw new IllegalStateException("Unable to create EntityDataIterator for"
                    + "Sesame Repository "+ repoConfig.getID() + "'!", e);
        }
    }

    protected class RdfEntityDataIterator implements EntityDataIterator {

        protected final RepositoryConnection connection;
        protected final RepositoryResult<Statement> stdItr;
        protected final boolean followBNodes;

        private org.openrdf.model.URI currentEntity = null;
        /**
         * The last {@link Statement} read from {@link #stdItr}
         */
        private Statement currentStd = null;
        /**
         * The current Representation as created by {@link #next()}
         */
        protected RdfRepresentation currentRep;
        /**
         * If the {@link #stdItr} is positioned on the 2nd {@link Statement} 
         * of the next Entity and {@link #currentStd} holds the first one.
         */
        private boolean nextInitialised = false;
        
        protected RdfEntityDataIterator(boolean followBNodes,
                boolean includeInferred, Resource...contexts) throws RepositoryException{
            this.connection = repository.getConnection();
            stdItr = connection.getStatements(null, null, null, includeInferred, contexts);
            this.followBNodes = followBNodes;
            entityDataIterators.add(this);
        }
        
        @Override
        public boolean hasNext() {
            if(nextInitialised){
                return true;
            }
            try {
                while(stdItr.hasNext() && !(currentStd.getSubject() instanceof org.openrdf.model.URI)){
                    currentStd = stdItr.next();
                }
                if(stdItr.hasNext()){
                    nextInitialised = true;
                }
                return nextInitialised;
            } catch (RepositoryException e) {
                throw new IllegalArgumentException("Exceptions while reading "
                        + "Statements after " + currentStd ,e);
            }
        }

        @Override
        public String next() {
            if(nextInitialised || hasNext()){
                final org.openrdf.model.URI subject = 
                        (org.openrdf.model.URI)currentStd.getSubject();
                currentRep = vf.createRdfRepresentation(subject);
                try {
                    createRepresentation(subject, currentRep.getModel());
                } catch (RepositoryException e) {
                    currentRep = null;
                    throw new IllegalStateException("Unable to read statements "
                        + "for Entity " + (currentStd != null ? currentStd.getSubject() :
                            "") +"!",e);
                }
                nextInitialised = false;
                return subject.toString();
            } else {
                currentRep = null;
                throw new NoSuchElementException();
            }
        }

        /**
         * Creates a representation by consuming Statements from the
         * {@link #stdItr} until the subject changes. If {@link #followBNodes}
         * is enabled it also recursively includes statements where the object
         * is an {@link BNode}.
         * @param subject the subject of the Representation to create
         * @param model the model to add the Statements
         * @throws RepositoryException
         */
        protected void createRepresentation(org.openrdf.model.URI subject, final Model model)
                throws RepositoryException {
            final Set<BNode> bnodes;
            final Set<BNode> visited;
            if(followBNodeState){
                bnodes = new HashSet<BNode>();
                visited = new HashSet<BNode>();
            } else {
                bnodes = null;
                visited = null;
            }
            boolean next = false;
            while(!next && stdItr.hasNext()){
                currentStd = stdItr.next();
                next = !subject.equals(currentStd.getSubject());
                if(!next){
                    model.add(currentStd);
                    if(followBNodeState){ //keep referenced BNodes
                        Value object = currentStd.getObject();
                        if(object instanceof BNode){
                            bnodes.add((BNode)object);
                        }
                    } //else do not follow BNode values
                } //else the subject has changed ... stop here
            }
            if(followBNodeState){ //process BNodes
                for(BNode bnode : bnodes){
                    visited.add(bnode);
                    extractRepresentation(connection, model, bnode, visited);
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("read-only iterator!");
        }

        @Override
        public Representation getRepresentation() {
            if(currentRep == null){
                throw new NoSuchElementException();
            } else {
                return currentRep;
            }
        }

        @Override
        public void close() {
            entityDataIterators.remove(this);
            try {
                connection.close();
            } catch (RepositoryException e) { /* ignore */ }
        }
        
    }
    
    /**
     * Extracts the triples that belong to the {@link Representation} with the
     * parsed id from the Sesame repository.
     * @param con the repository connection
     * @param uri the subject of the Representation to extract
     * @return the representation with the extracted data.
     * @throws RepositoryException 
     */
    protected RdfRepresentation createRepresentationGraph(RepositoryConnection con, 
            org.openrdf.model.URI uri) throws RepositoryException{
        RdfRepresentation rep = vf.createRdfRepresentation(uri);
        Model model = rep.getModel();
        extractRepresentation(con, model, uri, 
            followBNodeState ? new HashSet<BNode>() : null);
        return rep;
    }
    
    /**
     * Extracts all {@link Statement}s part of the Representation. If
     * {@link #followBNodeState} this is called recursively for {@link Statement}s
     * where the value is an {@link BNode}.
     */
    protected void extractRepresentation(RepositoryConnection con,Model model, Resource node, Set<BNode> visited) throws RepositoryException{
        //we need all the outgoing relations and also want to follow bNodes until
        //the next UriRef. However we are not interested in incoming relations!
        RepositoryResult<Statement> outgoing = con.getStatements(node, null, null, includeInferred, contexts);
        Statement statement;
        Set<BNode> bnodes = followBNodeState ? new HashSet<BNode>() : null;
        while(outgoing.hasNext()){
            statement = outgoing.next();
            model.add(statement);
            if(followBNodeState){
                Value object = statement.getObject();
                if(object instanceof BNode && !visited.contains(object)){
                    bnodes.add((BNode)object);
                }
            } //else do not follow values beeing BNodes
        }
        outgoing.close();
        if(followBNodeState){
            for(BNode bnode : bnodes){
                visited.add(bnode);
                //TODO: recursive calls could cause stackoverflows with wired graphs
                extractRepresentation(con, model, bnode, visited);
            }
        }
    }
    
    /* -------------------------------------------------------------------------
     * LDPath Backend methods
     * -------------------------------------------------------------------------
     */
    
    @Override
    public Literal createLiteral(String content) {
        return createLiteralInternal(sesameFactory, content);
    }

    @Override
    public Literal createLiteral(String content, Locale language, URI type) {
        return createLiteralInternal(sesameFactory, content, language, type);
    }

    @Override
    public org.openrdf.model.URI createURI(String uri) {
        return createURIInternal(sesameFactory, uri);
    }

    @Override
    public Collection<Value> listObjects(Value subject, Value property) {
        try {
            return listObjectsInternal(getLdPathConnection(), (Resource)subject, 
                asUri(property), includeInferred, contexts);
        } catch (RepositoryException e) {
            ungetLdPathConnection();
            throw new IllegalStateException("Exception while accessing values for "
                    + "TriplePattern: "+subject+", "+property+", null!",e);
        } catch (ClassCastException e){
            throw new IllegalStateException("Subject of triple pattern MUST NOT be "
            		+ "a Literal (TriplePattern: "+subject+", "+property+", null)!",e);
        }
    }

    @Override
    public Collection<Value> listSubjects(Value property, Value object) {
        try {
            return listSubjectsInternal(getLdPathConnection(), asUri(property), object,
                includeInferred, contexts);
        } catch (RepositoryException e) {
            ungetLdPathConnection();
            throw new IllegalStateException("Exception while accessing values for "
                + "TriplePattern: null, "+property+", "+object+"!",e);
        }
    }

    protected RepositoryConnection getLdPathConnection() throws RepositoryException {
        if(ldpathConnection == null){
            ldpathConnectionLock.lock();
            try {
                if(ldpathConnection == null){
                    ldpathConnection = repository.getConnection();
                }
            } finally {
                ldpathConnectionLock.unlock();
            }
        }
        return ldpathConnection;
    }

    protected void ungetLdPathConnection() {
        ldpathConnectionLock.lock();
        try {
            ldpathConnection.close();
            ldpathConnection = null;
        } catch (RepositoryException e1) { /* ignore */
            
        } finally {
            ldpathConnectionLock.unlock();
        }
    }
    
    protected RepositoryConnection getEntityDataProviderConnection() throws RepositoryException {
        if(entityDataProviderConnection == null){
            entityDataProviderConnectionLock.lock();
            try {
                if(entityDataProviderConnection == null){
                    entityDataProviderConnection = repository.getConnection();
                }
            } finally {
                entityDataProviderConnectionLock.unlock();
            }
        }
        return entityDataProviderConnection;
    }

    protected void ungetEntityDataProviderConnection() {
        entityDataProviderConnectionLock.lock();
        try {
            entityDataProviderConnection.close();
            entityDataProviderConnection = null;
        } catch (RepositoryException e1) { /* ignore */
            
        } finally {
            entityDataProviderConnectionLock.unlock();
        }
    }

    
    private org.openrdf.model.URI asUri(Value property){
        if(property instanceof org.openrdf.model.URI){
            return (org.openrdf.model.URI)property;
        } else {
            return createURI(property.stringValue());
        }
    }
    
    
}
