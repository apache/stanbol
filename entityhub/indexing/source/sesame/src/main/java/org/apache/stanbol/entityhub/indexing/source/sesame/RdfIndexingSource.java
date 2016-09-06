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

package org.apache.stanbol.entityhub.indexing.source.sesame;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.ConvertingIteration;
import info.aduna.iteration.DistinctIteration;
import info.aduna.iteration.FilterIteration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

import org.apache.commons.io.FilenameUtils;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityDataProvider;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceLoader;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceState;
import org.apache.stanbol.entityhub.model.sesame.RdfRepresentation;
import org.apache.stanbol.entityhub.model.sesame.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
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
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfIndexingSource extends AbstractSesameBackend implements EntityDataIterable, EntityDataProvider, RDFBackend<Value> {

    private final Logger log = LoggerFactory.getLogger(RdfIndexingSource.class);
    
    public static final String PARAM_REPOSITORY_CONFIG = "repo";
    
    public static final String DEFAULT_REPOSITORY_CONFIG = "repository.ttl";
    
    /**
     * The Parameter used to configure the source folder(s) relative to the
     * {@link IndexingConfig#getSourceFolder()}. The ',' (comma) is used as
     * separator to parsed multiple sources.
     */
    public static final String PARAM_SOURCE_FILE_OR_FOLDER = "source";
    /**
     * The default directory name used to search for RDF files to be imported
     */
    public static final String DEFAULT_SOURCE_FOLDER_NAME = "rdfdata";

    /**
     * The Parameter that can be used to deactivate the importing of sources.
     * If this parameter is set to <code>false</code> the values configured for
     * {@link #PARAM_IMPORT_SOURCE} are ignored. The default value is
     * <code>true</code>
     */
    public static final String PARAM_IMPORT_SOURCE = "import";
    
    /**
     * The directory where successfully imported files are copied to
     */
    public static final String PARAM_IMPORTED_FOLDER = "imported";
    /**
     * The default directory bane where successfully imported files are copied to
     */
    public static final String DEFAULT_IMPORTED_FOLDER_NAME = "imported";

    public static final Object PARAM_BASE_URI = "baseUri";
    
    public static final String DEFAULT_BASE_URI = "http://www.fake-base-uri.org/base-uri/";

    protected ValueFactory sesameFactory;
    
    protected RdfValueFactory vf = RdfValueFactory.getInstance();
    
    Repository repository;
    boolean shutdownRepository = false; //if we need to shutdown the repo
    //protected RepositoryConnection connection;

    /**
     * Default Constructor. Expects that the config is parsed by calling
     * {@link #setConfiguration(Map)}
     */
    public RdfIndexingSource() {}
    
    /**
     * Constructs a {@link RdfIndexingSource} for the parsed parameters. This
     * expects that {@link #setConfiguration(Map)} is not called
     * @param repository
     * @param contexts
     */
    public RdfIndexingSource(Repository repository, Resource...contexts){
        if(repository == null){
            throw new IllegalArgumentException("The parsed Repository MUST NOT be NULL!");
        }
        if(!repository.isInitialized()){
            throw new IllegalStateException("Parsed Repository is not initialized");
        }
        this.repository = repository;
        this.sesameFactory = repository.getValueFactory();
        this.contexts = contexts;
    }
    
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

    private ResourceLoader loader;

    private String baseUri;
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        //(0) parse the baseUri
        Object value = config.get(PARAM_BASE_URI);
        baseUri = value == null ? DEFAULT_BASE_URI : value.toString();
        //(1) init the Sesame Repository from the RDF config
        value = config.get(PARAM_REPOSITORY_CONFIG);
        File repoConfigFile = indexingConfig.getConfigFile(
            value != null ? value.toString() : DEFAULT_REPOSITORY_CONFIG);
        if(repoConfigFile.isFile()){ //read the config (an RDF file)
            this.repoConfig = loadRepositoryConfig(repoConfigFile);
        } else {
            throw new IllegalArgumentException("The configured Sesame Repository configuration file "
                + repoConfigFile +" is missing. Please use the '"+PARAM_REPOSITORY_CONFIG 
                + "' paramteter to configure the actual configuration file (relative "
                + "to the config '"+indexingConfig.getConfigFolder()+"'folder)");
        }
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
            sesameFactory = repository.getValueFactory();
            repository.initialize();
            shutdownRepository = true; //we created it, so we do shut it down
        } catch (RepositoryConfigException e) {
            throw new IllegalStateException("Unable to initialise Repository (id: "
                + repoConfig.getID()+ ", title: "+repoConfig.getTitle() + ", impl: "
                + repoConfig.getRepositoryImplConfig().getType()+")!", e);
        } catch (RepositoryException e) {
            throw new IllegalStateException("Unable to initialise Repository (id: "
                    + repoConfig.getID()+ ", title: "+repoConfig.getTitle() + ", impl: "
                    + repoConfig.getRepositoryImplConfig().getType()+")!", e);
        }
        //(2) init the resourceLoader
        loader = new ResourceLoader(new RdfResourceImporter(repository, baseUri), 
            indexingConfig.isFailOnError());
        value = config.get(PARAM_IMPORTED_FOLDER);
        //set the folder for imported files
        String importedFolderName;
        if(value != null && !value.toString().isEmpty()){
            importedFolderName = value.toString();
        } else {
            importedFolderName = DEFAULT_IMPORTED_FOLDER_NAME;
        }
        File importedFolder = new File(indexingConfig.getSourceFolder(),importedFolderName);
        log.info("Imported RDF File Folder: {}",importedFolder);
        this.loader.setImportedDir(importedFolder);
        //check if importing is deactivated
        boolean importSource = true; //default is true
        value = config.get(PARAM_IMPORT_SOURCE);
        if(value != null){
            importSource = Boolean.parseBoolean(value.toString());
        }
        if(importSource){ // if we need to import ... check the source config
            log.info("Importing RDF data from:");
            value = config.get(PARAM_SOURCE_FILE_OR_FOLDER);
            if(value == null){ //if not set use the default
                value = DEFAULT_SOURCE_FOLDER_NAME;
            }
            for(String source : value.toString().split(",")){
                File sourceFileOrDirectory = indexingConfig.getSourceFile(source);
                if(sourceFileOrDirectory.exists()){
                    //register the configured source with the ResourceLoader
                    this.loader.addResource(sourceFileOrDirectory);
                } else {
                    if(FilenameUtils.getExtension(source).isEmpty()){
                        //non existent directory -> create
                        //This is typically the case if this method is called to
                        //initialise the default configuration. So we will try
                        //to create the directory users need to copy the source
                        //RDF files.
                        if(!sourceFileOrDirectory.mkdirs()){
                            log.warn("Unable to create directory {} configured to improt RDF data from. " +
                                    "You will need to create this directory manually before copying the" +
                                    "RDF files into it.",sourceFileOrDirectory);
                            this.loader.addResource(sourceFileOrDirectory);
                        }
                    } else {
                        log.warn("Unable to find RDF source {} within the indexing Source folder {}",
                            source, indexingConfig.getSourceFolder());
                    }
                }
            }
            if(log.isInfoEnabled()){
                for(String registeredSource : loader.getResources(ResourceState.REGISTERED)){
                    log.info(" > "+registeredSource);
                }
            }
        } else {
            log.info("Importing RDF data deactivated by parameer {}={}"+PARAM_IMPORT_SOURCE,value);
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
            configRepo.initialize();
            con = configRepo.getConnection();
            //We need to load the configuration into a context
            org.openrdf.model.URI configContext = con.getValueFactory().createURI(
                "urn:stanbol.entityhub:indexing.source.sesame:config.context");
            RDFFormat format = Rio.getParserFormatForFileName(repoConfigFile.getName());
            try {
                con.add(new InputStreamReader(
                    new FileInputStream(repoConfigFile),Charset.forName("UTF-8")), 
                    baseUri, format,configContext);
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
        //check if we need to load resources
        return loader != null && !loader.getResources(ResourceState.REGISTERED).isEmpty();
    }

    @Override
    public void initialise() {
        if(loader != null){
            loader.loadResources();
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
        if(shutdownRepository){
            try {
                repository.shutDown();
            } catch (RepositoryException e) {
                log.warn("Error while closing Sesame Connection", e);
            }
        }
    }

    public final boolean isFollowBNodeState() {
        return followBNodeState;
    }

    public final void setFollowBNodeState(boolean followBNodeState) {
        this.followBNodeState = followBNodeState;
    }

    public final boolean isIncludeInferred() {
        return includeInferred;
    }

    public final void setIncludeInferred(boolean includeInferred) {
        this.includeInferred = includeInferred;
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
        protected final CloseableIteration<URI,RepositoryException> subjectItr;
        protected final boolean followBNodes;

        /**
         * The current Representation as created by {@link #next()}
         */
        protected RdfRepresentation currentRep;
        
        protected RdfEntityDataIterator(boolean followBNodes,
                boolean includeInferred, Resource...contexts) throws RepositoryException{
            this.connection = repository.getConnection();
            CloseableIteration<URI, RepositoryException> converter = 
                    new ConvertingIteration<Statement, URI, RepositoryException>(
                            connection.getStatements(null, null, null, includeInferred, contexts)) {
                @Override
                protected URI convert(Statement sourceObject) throws RepositoryException {
                    Resource r = sourceObject.getSubject();
                    return r instanceof URI ? (URI)r : null;
                }
            };
            CloseableIteration<URI,RepositoryException> filter = 
                    new FilterIteration<URI,RepositoryException>(converter){
                @Override
                protected boolean accept(URI object) throws RepositoryException {
                    return object != null;
                }    
            };
            this.subjectItr = new DistinctIteration<URI, RepositoryException>(filter);
            this.followBNodes = followBNodes;
            entityDataIterators.add(this);
        }
        
        @Override
        public boolean hasNext() {
            try {
                return subjectItr.hasNext();
            } catch (RepositoryException e) {
                throw new IllegalStateException("Exceptions while checking "
                        + "for next subject" ,e);
            }
        }

        @Override
        public String next() {
            URI subject = null;
            try {
                subject = subjectItr.next();
                currentRep = vf.createRdfRepresentation(subject);
                    createRepresentation(subject, currentRep.getModel());
                return subject.toString();
            } catch (RepositoryException e) {
                currentRep = null;
                throw new IllegalStateException("Unable to read statements "
                    + "for Entity " + (subject == null ? "unknown" : subject) +"!",e);
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
            RepositoryResult<Statement> stmts = connection.getStatements(
                subject,null,null,includeInferred,contexts);
            final Set<BNode> bnodes;
            final Set<BNode> visited;
            if(followBNodeState){
                bnodes = new HashSet<BNode>();
                visited = new HashSet<BNode>();
            } else {
                bnodes = null;
                visited = null;
            }
            try {
                while(stmts.hasNext()){
                    Statement currentStd = stmts.next();
                    model.add(currentStd);
                    if(followBNodeState){ //keep referenced BNodes
                        Value object = currentStd.getObject();
                        if(object instanceof BNode){
                            bnodes.add((BNode)object);
                        }
                    } //else do not follow BNode values
                }
            } finally {
                stmts.close();
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
                subjectItr.close();
            } catch (RepositoryException e) {/* ignore */}
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
    public Literal createLiteral(String content, Locale language, java.net.URI type) {
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
        if(ldpathConnection != null){
            ldpathConnectionLock.lock();
            try {
                if(ldpathConnection != null){
                    ldpathConnection.close();
                    ldpathConnection = null;
                }
            } catch (RepositoryException e1) { 
                /* ignore */
            } finally {
                ldpathConnectionLock.unlock();
            }
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
        if(entityDataProviderConnection != null){
            entityDataProviderConnectionLock.lock();
            try {
                if(entityDataProviderConnection != null){
                    entityDataProviderConnection.close();
                    entityDataProviderConnection = null;
                }
            } catch (RepositoryException e1) { /* ignore */
                
            } finally {
                entityDataProviderConnectionLock.unlock();
            }
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
