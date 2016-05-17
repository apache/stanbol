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
package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import static org.apache.stanbol.entityhub.indexing.source.jenatdb.Utils.initTDBDataset;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.FilenameUtils;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityDataProvider;
import org.apache.stanbol.entityhub.indexing.core.IndexingComponent;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceLoader;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceState;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.datatypes.xsd.XSDDuration;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
/**
 * Implementation of an {@link IndexingComponent} for Entity data that provides 
 * the possibility to both:<ol>
 * <li>randomly access entity data via the {@link EntityDataProvider} interface
 * <li>iterate over all entities in this store via the {@link EntityDataIterator}
 * interface.
 * </ol>
 * 
 * @author Rupert Westenthaler
 *
 */
public class RdfIndexingSource extends AbstractTdbBackend implements EntityDataIterable,EntityDataProvider, RDFBackend<Node> {
    /**
     * The Parameter used to configure the source folder(s) relative to the
     * {@link IndexingConfig#getSourceFolder()}. The ',' (comma) is used as
     * separator to parsed multiple sources.
     */
    public static final String PARAM_SOURCE_FILE_OR_FOLDER = "source";
    /**
     * The directory where successfully imported files are copied to
     */
    public static final String PARAM_IMPORTED_FOLDER = "imported";
    /**
     * Allows to enable/disable the indexing of Bnodes (see 
     * <a href="https://issues.apache.org/jira/browse/STANBOL-765">STANBOL-765</a>
     * for details).
     */
    private static final String PARAM_BNODE_STATE = "bnode";
    /**
     * If present, this Parameter allows to convert RDF BlankNodes to dereferable
     * URIs by using {bnode-prefix}{bnode-id} (see 
     * <a href="https://issues.apache.org/jira/browse/STANBOL-765">STANBOL-765</a>
     * for details)
     */
    public static final String PARAM_BNODE_PREFIX = "bnode-prefix";
    /**
     * The Parameter that can be used to deactivate the importing of sources.
     * If this parameter is set to <code>false</code> the values configured for
     * {@link #PARAM_IMPORT_SOURCE} are ignored. The default value is
     * <code>true</code>
     */
    public static final String PARAM_IMPORT_SOURCE = "import";
    /**
     * Allows to configure a {@link RdfImportFilter} (full qualified class name).
     * If present it gets the full configuration set for this component parsed.
     * This means that the import filter can be configured by the same 
     * configuration as this component.
     */
    public static final String PARAM_IMPORT_FILTER = "import-filter";
    /**
     * The default directory name used to search for RDF files to be imported
     */
    public static final String DEFAULT_SOURCE_FOLDER_NAME = "rdfdata";
    
    public static final String DEFAULT_IMPORTED_FOLDER_NAME = "imported";    
    //protected to allow internal classes direct access (without hidden getter/
    //setter added by the compiler that decrease performance)
    protected final static Logger log = LoggerFactory.getLogger(RdfIndexingSource.class);
    
    /**
     * The RDF data
     */
    private DatasetGraphTDB indexingDataset;
    /**
     * The valueFactory used to create {@link Representation}s, {@link Reference}s
     * and {@link Text} instances.
     */
    private ValueFactory vf;
    
    private ResourceLoader loader;

    protected String bnodePrefix; //protected to allow direct access in inner classes
    /**
     * used for logging a single WARN level entry on the first ignored BlankNode
     */
    private boolean bnodeIgnored = false;
    private RdfImportFilter importFilter;
    
    /**
     * Default Constructor relaying on that {@link #setConfiguration(Map)} is
     * called afterwards to provide the configuration!
     */
    public RdfIndexingSource(){
        this(null);
    }
    /**
     * Internally used to initialise a {@link ValueFactory}
     * @param valueFactory
     */
    private RdfIndexingSource(ValueFactory valueFactory){
        if(valueFactory == null){
            this.vf = InMemoryValueFactory.getInstance();
        } else {
            this.vf = valueFactory;
        }
    }
    /**
     * Constructs an instance based on the provided parameter
     * @param modelLocation the directory for the RDF model. MUST NOT be NULL
     * however the parsed {@link File} needs not to exist.
     * @param sourceFileOrDirectory the source file or directory containing the
     * file(s) to import. Parse <code>null</code> if no RDF files need to be 
     * imported
     * @param valueFactory The {@link ValueFactory} used to create instances
     * or <code>null</code> to use the default implementation.
     * @param importFilter Optionally an importFilter used for filtering some
     * triples read from the RDF source files.
     */
    public RdfIndexingSource(File modelLocation, 
                               File sourceFileOrDirectory,
                               ValueFactory valueFactory,
                               RdfImportFilter importFilter){
        if(modelLocation == null){
            throw new IllegalArgumentException("The parsed model location MUST NOT be NULL!");
        }
        //init the store
        this.indexingDataset = initTDBDataset(modelLocation);
        //use a ResourceLoader that fails on the first invalid RDF file (STANBOL-328)
        this.loader =  new ResourceLoader(new RdfResourceImporter(indexingDataset,importFilter), true,true);
        loader.addResource(sourceFileOrDirectory);
    }
    @Override
    public void setConfiguration(Map<String,Object> config) {
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        //first init the RDF Model
        this.indexingDataset = Utils.getTDBDataset(config);
        //second we need to check if we need to import RDF files to the RDF model
        //look if we need want to use an import filter
        Object value = config.get(PARAM_IMPORT_FILTER);
        if(value == null){
            log.info("No RDF Import Filter configured");
            importFilter = null;
        } else {
            String[] filterNames = value.toString().split(",");
            List<RdfImportFilter> filters = new ArrayList<RdfImportFilter>();
            ClassLoader cl = indexingConfig.getClass().getClassLoader();
            for(String filterName : filterNames){
                filterName = filterName.trim();
                try {
                    Class<? extends RdfImportFilter> importFilterClass = cl.loadClass(
                        filterName).asSubclass(RdfImportFilter.class);
                    RdfImportFilter filter = importFilterClass.newInstance();
                    filter.setConfiguration(config);
                    filters.add(filter);
                    log.info("Use RDF ImportFilter {} (type: {})",importFilter,importFilterClass.getSimpleName());
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Configured RdfImportFilter '"
                        +filterName+"' not found", e);
                } catch (InstantiationException e) {
                    throw new IllegalArgumentException("Configured RdfImportFilter '"
                            +filterName+"' can not be instantiated", e);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Configured RdfImportFilter '"
                            +filterName+"' can not be created", e);
                }
            }
            if(filters.isEmpty()){
                this.importFilter = null;
            } else if(filters.size() == 1){
                this.importFilter = filters.get(0);
            } else {
                this.importFilter = new UnionImportFilter(filters.toArray(
                    new RdfImportFilter[filters.size()]));
            }
        }
        
        boolean failOnError = indexingConfig.isFailOnError();
        //create the ResourceLoader
        this.loader =  new ResourceLoader(new RdfResourceImporter(indexingDataset, importFilter), failOnError);
        
        value = config.get(PARAM_IMPORTED_FOLDER);
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
                            //this would not be necessary because the directory will
                            //be empty - however I like to be consistent and have
                            //all configured and existent files & dirs added the the
                            //resource loader
                            this.loader.addResource(sourceFileOrDirectory);
                        }
                    } else {
                        log.warn("Unable to find RDF source {} within the indexing Source folder ",source,indexingConfig.getSourceFolder());
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
        //STANBOL-765: parsed bnode-prefix from parsed configuration.
        value = config.get(PARAM_BNODE_STATE);
        final Boolean bnodeState;
        if(value != null){
            bnodeState = value instanceof Boolean ? (Boolean) value :
                Boolean.parseBoolean(value.toString());
        } else if(config.containsKey(PARAM_BNODE_STATE)){ //support key without value
            bnodeState = true;
        } else {
            bnodeState = null; //undefined
        }
        if(bnodeState == null || bnodeState){ //null or enabled -> consider prefix
            value = config.get(PARAM_BNODE_PREFIX);
            if(value != null){
                try {
                    new URI(value.toString());
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("The configured "+PARAM_BNODE_PREFIX+"='"
                        + value.toString() + "' MUST BE a valid URI!");
                }
                bnodePrefix = value.toString();
            } else if(bnodeState != null) { //use default prefix if bnodeState is true
                bnodePrefix = String.format("urn:bnode:%s:",indexingConfig.getName());
            } // else bnodeState == null and no custom prefix -> disable by default
        }
        if(bnodePrefix != null){
            log.info("Indexing of Bnodes enabled (prefix: {}",bnodePrefix);
        } else {
            log.info("Indexing of Bnodes disabled");
            
        }
    }
    @Override
    public boolean needsInitialisation() {
        return (importFilter != null && importFilter.needsInitialisation()) ||
                !loader.getResources(ResourceState.REGISTERED).isEmpty();
    }
    @Override
    public void initialise(){
        if(importFilter != null && importFilter.needsInitialisation()){
            importFilter.initialise();
        }
        if(!loader.getResources(ResourceState.REGISTERED).isEmpty()){
            loader.loadResources();
        }
    }
    @Override
    public void close() {
        loader = null;
        indexingDataset.close();
        if(importFilter != null){
            importFilter.close();
        }
    }
    public void debug(){
        String entityVar = "s";
        String fieldVar = "p";
        String valueVar = "o";
        StringBuilder qb = new StringBuilder();
        qb.append(String.format("SELECT ?%s ?%s ?%s \n",
            entityVar,fieldVar,valueVar)); //for the select
        qb.append("{ \n");
        qb.append(String.format("    ?%s ?%s ?%s . \n",
            entityVar,fieldVar,valueVar)); //for the where
        qb.append("} \n");
        log.debug("EntityDataIterator Query: \n"+qb.toString());
        Query q = QueryFactory.create(qb.toString(), Syntax.syntaxARQ);
        ResultSet rs = QueryExecutionFactory.create(q, indexingDataset.toDataset()).execSelect();
        Var s = Var.alloc(entityVar);
        Var p = Var.alloc(fieldVar);
        Var o = Var.alloc(valueVar);
        while (rs.hasNext()){
            Binding b = rs.nextBinding();
            log.debug("{} {} {}",new Object[]{b.get(s),b.get(p),b.get(o)});
        }
    }
    
    @Override
    public EntityDataIterator entityDataIterator() {
        String entityVar = "s";
        String fieldVar = "p";
        String valueVar = "o";
        StringBuilder qb = new StringBuilder();
        qb.append(String.format("SELECT ?%s ?%s ?%s \n",
            entityVar,fieldVar,valueVar)); //for the select
        qb.append("{ \n");
        qb.append(String.format("    ?%s ?%s ?%s . \n",
            entityVar,fieldVar,valueVar)); //for the where
        qb.append("} \n");
        log.debug("EntityDataIterator Query: \n"+qb.toString());
        Query q = QueryFactory.create(qb.toString(), Syntax.syntaxARQ);
        return new RdfEntityIterator(
            QueryExecutionFactory.create(q, indexingDataset.toDataset()).execSelect(),
            entityVar,fieldVar,valueVar);
    }

    @Override
    public Representation getEntityData(String id) {
        final Node resource;
        //STANBOL-765: check if the parsed id represents an bnode
        if(bnodePrefix != null && id.startsWith(bnodePrefix)){
            resource = NodeFactory.createAnon(AnonId.create(id.substring(bnodePrefix.length())));
        } else {
            resource = NodeFactory.createURI(id);
        }
        Representation source = vf.createRepresentation(id);
        boolean found;
        ExtendedIterator<Triple> outgoing = null;
        try { // There may still be exceptions while reading triples
            outgoing = indexingDataset.getDefaultGraph().find(resource, null, null);
            found = outgoing.hasNext();
            while(outgoing.hasNext()){ //iterate over the statements for that resource
                Triple statement = outgoing.next();
                Node predicate = statement.getPredicate();
                if(predicate == null || !predicate.isURI()){
                    log.warn("Ignore field {} for resource {} because it is null or not an URI!",
                        predicate,resource);
                } else {
                    String field = predicate.getURI();
                    Node value = statement.getObject();
                    processValue(value, source, field);
                } //end else predicate != null
            } //end iteration over resource triple
        } catch (Exception e) {
            log.warn("Unable to retrieve entity data for Entity '"+id+"'",e);
            found = false;
            try {
                if(outgoing != null){
                    outgoing.close();
                }
            } catch (Exception e1) { /* ignore */}
        }
        if(found) {
            if(log.isTraceEnabled()){
                log.info("RDFTerm: \n{}", ModelUtils.getRepresentationInfo(source));
            }
            return source;
        } else {
            log.debug("No Statements found for id {} (Node: {})!",id,resource);
            return null;
        }
    }
    /**
     * Getter for the Jena TDB {@link DatasetGraph} used as source
     * @return the indexingDataset
     */
    public final DatasetGraphTDB getIndexingDataset() {
        return indexingDataset;
    }

    /**
     * Processes a {@link Node} and adds the according value to the parsed
     * Representation.
     * @param value The node to convert to an value for the Representation
     * @param source the representation (MUST NOT be <code>null</code>
     * @param field the field (MUST NOT be <code>null</code>)
     */
    private void processValue(Node value, Representation source, String field) {
        if(value == null){
            log.warn("Encountered NULL value for field {} and entity {}",
                    field,source.getId());
        } else if(value.isURI()){ //add a reference
            source.addReference(field, value.getURI());
        } else if(value.isLiteral()){ //add a value or a text depending on the dataType
            LiteralLabel ll = value.getLiteral();
//            log.debug("LL: lexical {} | value {} | dataType {} | language {}",
//                new Object[]{ll.getLexicalForm(),ll.getValue(),ll.getDatatype(),ll.language()});
            //if the dataType == null , than we can expect a plain literal
            RDFDatatype dataType = ll.getDatatype();
            if(dataType != null){ //add a value
                Object literalValue;
                try {
                    literalValue = ll.getValue();
                    if(literalValue instanceof BaseDatatype.TypedValue){
                        //used for unknown data types
                        // -> in such cases just use the lexical type
                        String lexicalValue = ((BaseDatatype.TypedValue)literalValue).lexicalValue;
                        if(lexicalValue != null && !lexicalValue.isEmpty()){
                            source.add(field,lexicalValue);
                        }
                    } else if(literalValue instanceof XSDDateTime) {
                        source.add(field, ((XSDDateTime)literalValue).asCalendar().getTime()); //Entityhub uses the time
                    } else if(literalValue instanceof XSDDuration) {
                        String duration = literalValue.toString();
                        if(duration != null && !duration.isEmpty()) {
                            source.add(field, literalValue.toString());
                        }
                    } else if(!ll.getLexicalForm().isEmpty()){
                        source.add(field, literalValue);
                    } //else ignore literals that are empty
                } catch (DatatypeFormatException e) {
                    log.warn(" Unable to convert {} to {} -> use lecicalForm",
                        ll.getLexicalForm(),ll.getDatatype());
                    literalValue = ll.getLexicalForm();
                }
            } else { //add a text
                String lexicalForm = ll.getLexicalForm();
                if(lexicalForm != null && !lexicalForm.isEmpty()){
                    String language = ll.language();
                    if(language!=null && language.length()<1){
                        language = null;
                    }
                    source.addNaturalText(field, lexicalForm, language);
                } //else ignore empty literals
            }
            // "" is parsed if there is no language
        } else if(value.isBlank()) { 
            if(bnodePrefix != null) { //STANBOL-765: convert Bnodes to URIs
                StringBuilder sb = new StringBuilder(bnodePrefix);
                sb.append(value.getBlankNodeId().getLabelString());
                source.addReference(field, sb.toString());
            } else {
                logIgnoredBnode(log, source, field, value);
            }
        }  else {
            log.warn("ignoreing value {} for field {} and RDFTerm {} because it is of an unsupported type!",
                    new Object[]{value,field,source.getId()});
        } //end different value node type
    }
    /**
     * Logs that a BlankNode was ignored (only the first time). Also debugs the
     * ignored triple.
     * @param log the logger to use
     * @param s subject
     * @param p predicate
     * @param o object
     */
    protected void logIgnoredBnode(Logger log, Object s, Object p, Object o) {
        if(!bnodeIgnored){
            bnodeIgnored = true;
            log.warn("The Indexed RDF Data do contain Blank Nodes. Those are "
                + "ignored unless the '{}' parameter is set to valid URI. "
                + "If this parameter is set Bnodes are converted to URIs by "
                + "using {bnode-prefix}{bnodeId} (see STANBOL-765)",
                PARAM_BNODE_PREFIX);
        }
        log.debug("ignoreing blank node value(s) for Triple {},{},{}!",
            new Object[]{s,p,o});
    }
    /**
     * Implementation of the iterator over the entities stored in a
     * {@link RdfIndexingSource}. This Iterator is based on query
     * {@link ResultSet}. It uses the low level SPARQL API because this allows
     * to use the same code to create values for Representations
     * @author Rupert Westenthaler
     *
     */
    public final class RdfEntityIterator implements EntityDataIterator {
        /**
         * Variable used to
         */
        final Var entityVar;
        final Var fieldVar;
        final Var valueVar;
        /**
         * The result set containing all triples in the form of <code>
         * "entity -&gt; field -&gt; value"</code>
         */
        private final ResultSet resultSet;
        /**
         * The {@link Node} representing the current entity or <code>null</code>
         * if the iterator is newly created.<p>
         * {@link Node#isURI()} is guaranteed to return <code>true</code> and
         * {@link Node#getURI()} is guaranteed to return the id for the entity
         */
        private Node currentEntity = null;
        /**
         * The {@link Node} for the next Entity in the iteration or <code>null</code>
         * in case there are no further or the iterator is newly created (in that
         * case {@link #currentEntity} will be also <code>null</code>)<p>
         * {@link Node#isURI()} is guaranteed to return <code>true</code> and
         * {@link Node#getURI()} is guaranteed to return the id for the entity
         */
        private Node nextEntity = null;
        /**
         * The Representation of the current Element. Only available after a
         * call to {@link #getRepresentation()}
         */
        private Representation currentRepresentation = null;
        /**
         * Holds all <code>field,value"</code> pairs of the current Entity.
         * Elements at even positions represent<code>fields</code> and elements 
         * at uneven positions represent <code>values</code>.
         */
        private List<Node> data = new ArrayList<Node>();
        /**
         * The next (not consumed) solution of the query. 
         */
        private Binding nextBinding = null;
        
        protected RdfEntityIterator(ResultSet resultSet, String entityVar,String fieldVar, String valueVar){
            if(resultSet == null){
                throw new IllegalArgumentException("The parsed ResultSet MUST NOT be NULL!");
            }
            //check if the ResultSet provides the required variables to perform the query
            List<String> vars = resultSet.getResultVars();
            if(!vars.contains(entityVar)){
                throw new IllegalArgumentException("The parsed ResultSet is missing the required" +
                		"Variable \""+entityVar+"\" representing the Entity!");
            } else {
                this.entityVar = Var.alloc(entityVar);
            }
            if(!vars.contains(fieldVar)){
                throw new IllegalArgumentException("The parsed ResultSet is missing the required" +
                        "Variable \""+fieldVar+"\" representing the Field of an Entity!");
            } else {
                this.fieldVar = Var.alloc(fieldVar);
            }
            if(!vars.contains(valueVar)){
                throw new IllegalArgumentException("The parsed ResultSet is missing the required" +
                        "Variable \""+valueVar+"\" representing the Value of a Field of an Entity!");
            } else {
                this.valueVar = Var.alloc(valueVar);
            }
            this.resultSet = resultSet;
            //this will read until the first binding of the first Entity is found
            initFirst(); 
        }
        private void initFirst(){
            if(currentEntity == null && nextEntity == null){ //only for the first call
                //consume binding until the first valid entity starts
                while(nextEntity == null && resultSet.hasNext()){
                    Binding firstValid = resultSet.nextBinding();
                    Node entityNode = firstValid.get(entityVar);
                    if((entityNode.isURI() && !entityNode.toString().isEmpty()) ||
                            entityNode.isBlank() && bnodePrefix != null){
                      //store it temporarily in nextBinding
                        nextBinding = firstValid; 
                        //store it as next (first) entity
                        nextEntity = entityNode;
                    } else {
                        logIgnoredBnode(log,entityNode,firstValid.get(fieldVar),firstValid.get(valueVar));
                    }
                }
            } else {
                throw new IllegalStateException("This Mehtod MUST be only used for Initialisation!");
            }
        }
        @Override
        public void close() {
            data.clear();
            data = null;
            currentEntity = null;
            currentRepresentation = null;
            //Looks like it is not possible to close a resultSet
        }

        @Override
        public Representation getRepresentation() {
            //current Entity will be null if
            //  - next() was never called
            //  - the end of the iteration was reached
            if(currentEntity == null){ 
                return null;
            } else if(currentRepresentation == null){
                currentRepresentation = createRepresentation();
            }
            return currentRepresentation;
        }

        @Override
        public boolean hasNext() {
            return resultSet.hasNext();
        }

        @Override
        public String next() {
            return getNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                "Removal of Entities is not supported by this Implementation!");
        }
        /**
         * Iterates over all {@link QuerySolution} of the {@link #resultSet}
         * that do have {@link #currentEntity} as 
         * {@link RdfIndexingSource#VARIABLE_NAME_ENTITY VARIABLE_NAME_ENTITY}.
         * NOTES: <ul>
         * <li>This method also initialises the {@link #data} and sets the 
         * {@link #nextBinding} to the first solution of the next entity.<br>
         * <li>That means also, that it would iterate over additional 
         * {@link RdfIndexingSource#VARIABLE_NAME_ENTITY VARIABLE_NAME_ENTITY}
         * values that are not URIResources ( in cases
         * {@link RDFNode#isURIResource()} returns <code>false</code>)
         * <li>This method is also used to initialise the first Entity
         * @return the URI of the current entity
         */
        private String getNext(){
            //check for more elements
            if(!resultSet.hasNext()){
                throw new NoSuchElementException("No more Entities available");
            }
            //clean up data of the previous entity
            this.data.clear(); //remove data of the previous entity
            this.currentRepresentation = null; //and the representation
            this.currentEntity = nextEntity; //set the nextEntity to the current

            //and process the first binding already consumed from the resultSet
            //by calling this method for the previous Entity
            if(nextBinding != null){ //will be null for the first Entity
                processSolution(nextBinding);
            }
            //now get all the other Solutions for the current entity
            boolean next = false;
            while(!next && resultSet.hasNext()){
                Binding binding = resultSet.nextBinding();
                Node entityNode = binding.get(entityVar);
                //NOTES:
                // * for URIs we need to check for empty URIs!
                // * STANBOL-765: added support for BlankNodes
                if((entityNode.isURI() && !entityNode.toString().isEmpty()) ||
                        entityNode.isBlank() && bnodePrefix != null){
                    if(!entityNode.equals(currentEntity)){
                        //start of next Entity
                        this.nextEntity = entityNode; //store the node for the next entity
                        this.nextBinding = binding; //store the first binding of the next entity
                        //we are done for this entity -> exit the loop
                        next = true;
                    } else {
                        processSolution(binding);
                    }
                } else {
                    logIgnoredBnode(log,entityNode,binding.get(fieldVar),binding.get(valueVar));
                }
            }
            if(!next){ // exit the loop but still no new entity ... that means
                nextEntity = null; //there are no more entities
                nextBinding = null; // and there are also no more solutions
            }
            //STANBOL-765: if current is a Bnode add the bnode-prefix
            return currentEntity.isBlank() ?
                new StringBuilder(bnodePrefix).append(currentEntity.getBlankNodeId().getLabelString()).toString() :
                    currentEntity.getURI();
        }
        /**
         * Processes a {@link Binding} by storing the {@link Node}s for the 
         * variables {@link #fieldVar} and {@link #valueVar} to {@link #data}.
         * This method ensures that both values are not <code>null</code> and
         * that the {@link Node} representing the field is an URI (
         * returns <code>true</code> for {@link Node#isURI()}).
         * @param binding the binding to process
         */
        private void processSolution(Binding binding) {
            Node field = binding.get(fieldVar);
            if(field != null && field.isURI()){ //property MUST BE an URI
                Node value = binding.get(valueVar);
                if(value != null){
                    //add the pair
                    data.add(field);
                    data.add(value);
                }
            } else {
                //This may only happen if the Query used to create the ResultSet
                //containing this Solution does not link the variable
                //VARIABLE_NAME_FIELD to properties.
                log.error("Found Field {} for Entity {} that is not an URIResource",field,currentEntity);
            }
        }
        /**
         * Used to create the Representation the first time 
         * {@link #getRepresentation()} is called for the current entity. The
         * information for the Representation are already stored in {@link #data}
         */
        private Representation createRepresentation() {
            final String uri;
            if(currentEntity.isBlank()){ //STANBOL-765: support bNodes
                StringBuilder sb = new StringBuilder(bnodePrefix);
                sb.append(currentEntity.getBlankNodeId().getLabelString());
                uri = sb.toString();
            } else {
                uri = currentEntity.getURI();
            }
            Representation representation = vf.createRepresentation(uri);
            Iterator<Node> it = data.iterator();
            while(it.hasNext()){ 
                //data contains field,value pairs
                //because of that we call two times next for
                String field = it.next().getURI(); //the field
                Node value = it.next();//and the value
                processValue(value, representation, field);
            }
            return representation;
        }
    }
    
    /* ----------------------------------------------------------------------
     *     RDF Backend implementation
     * ----------------------------------------------------------------------
     */
    @Override
    public Collection<Node> listObjects(Node subject, Node property) {
        Collection<Node> nodes = new ArrayList<Node>();
        if(bnodePrefix != null && subject.isURI() && subject.getURI().startsWith(bnodePrefix)){
            subject = NodeFactory.createAnon(new AnonId(subject.getURI().substring(bnodePrefix.length())));
        }
        ExtendedIterator<Triple> it = indexingDataset.getDefaultGraph().find(subject, property, null);
        while(it.hasNext()){
            //STANBOL-765: we need also to transform bnodes to URIs for the
            //RDFBackend implementation
            Node object = it.next().getObject();
            if(bnodePrefix != null && object.isBlank()){
                StringBuilder sb = new StringBuilder(bnodePrefix);
                sb.append(object.getBlankNodeId().getLabelString());
                object = NodeFactory.createURI(sb.toString());
            }
            nodes.add(object);
        }
        it.close();
        return nodes;
    }
    @Override
    public Collection<Node> listSubjects(Node property, Node object) {
        Collection<Node> nodes = new ArrayList<Node>();
        if(bnodePrefix != null && object.isURI() && object.getURI().startsWith(bnodePrefix)){
            object = NodeFactory.createAnon(new AnonId(object.getURI().substring(bnodePrefix.length())));
        }
        ExtendedIterator<Triple> it = indexingDataset.getDefaultGraph().find(null, property, object);
        while(it.hasNext()){
            Node subject = it.next().getSubject();
            //STANBOL-765: we need also to transform bnodes to URIs for the
            //RDFBackend implementation
            if(bnodePrefix != null && subject.isBlank()){
                StringBuilder sb = new StringBuilder(bnodePrefix);
                sb.append(subject.getBlankNodeId().getLabelString());
                subject = NodeFactory.createURI(sb.toString());
            }
            nodes.add(subject);
        }
        it.close();
        return nodes;
    }
    /**
     * Since STANBOL-765 BlankNodes are converted to URIs if a {@link #bnodePrefix}
     * is configured. This also means that one needs to expect calls to the
     * {@link RDFBackend} interface with transformed Nodes. <p>
     * This method ensures that if someone requests an uri {@link Node} for a
     * URI that represents a transformed Bnode (when the URI starts with 
     * {@link #bnodePrefix}) that the according bnode {@link Node} is created
     * @param node the node
     * @return
     */
    @Override
    public Node createURI(String uri) {
        if(bnodePrefix != null && uri.startsWith(bnodePrefix)){
            return NodeFactory.createAnon(AnonId.create(uri.substring(bnodePrefix.length())));
        } else {
            return super.createURI(uri);
        }
    }
    /**
     * used in case multiple {@link RdfImportFilter}s are configured.
     * @author Rupert Westenthaler
     *
     */
    private class UnionImportFilter implements RdfImportFilter {

        RdfImportFilter[] filters;
        
        UnionImportFilter(RdfImportFilter[] filters){
            this.filters = filters;
        }
        
        @Override
        public void setConfiguration(Map<String,Object> config) {}

        @Override
        public boolean needsInitialisation() { return false;}

        @Override
        public void initialise() {}

        @Override
        public void close() {}

        @Override
        public boolean accept(Node s, Node p, Node o) {
            boolean state = true;
            for(int i=0;state && i < filters.length;i++){
                state = filters[i].accept(s, p, o);
            }
            return state;
        }
        
    }
    
}
