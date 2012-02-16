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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.FilenameUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.datatypes.xsd.XSDDuration;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;
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
public class RdfIndexingSource implements EntityDataIterable,EntityDataProvider {
    /**
     * The Parameter used to configure the source folder(s) relative to the
     * {@link IndexingConfig#getSourceFolder()}. The ',' (comma) is used as
     * separator to parsed multiple sources.
     */
    public static final String PARAM_SOURCE_FILE_OR_FOLDER = "source";
    /**
     * Parameter used to configure the name of the directory used to store the
     * RDF model (a Jena TDB dataset). The default name is
     * {@link #DEFAULT_MODEL_DIRECTORY}
     */
    public static final String PARAM_MODEL_DIRECTORY = "model";
    /**
     * The Parameter that can be used to deactivate the importing of sources.
     * If this parameter is set to <code>false</code> the values configured for
     * {@link #PARAM_IMPORT_SOURCE} are ignored. The default value is
     * <code>true</code>
     */
    public static final String PARAM_IMPORT_SOURCE = "import";
    /**
     * The default directory name used to search for RDF files to be imported
     */
    public static final String DEFAULT_SOURCE_FOLDER_NAME = "rdfdata";
    /**
     * The default name of the folder used to initialise the 
     * {@link DatasetGraphTDB Jena TDB dataset}.
     */
    public static final String DEFAULT_MODEL_DIRECTORY = "tdb";
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
     */
    public RdfIndexingSource(File modelLocation, 
                               File sourceFileOrDirectory,
                               ValueFactory valueFactory){
        if(modelLocation == null){
            throw new IllegalArgumentException("The parsed model location MUST NOT be NULL!");
        }
        //init the store
        this.indexingDataset = createRdfModel(modelLocation);
        //use a ResourceLoader that fails on the first invalid RDF file (STANBOL-328)
        this.loader =  new ResourceLoader(new RdfResourceImporter(indexingDataset), true,true);
        loader.addResource(sourceFileOrDirectory);
    }
    @Override
    public void setConfiguration(Map<String,Object> config) {
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        //first init the RDF Model
        Object value = config.get(PARAM_MODEL_DIRECTORY);
        File modelLocation;
        if(value == null){
            modelLocation = new File(indexingConfig.getSourceFolder(),DEFAULT_MODEL_DIRECTORY);
        } else {
            modelLocation = new File(indexingConfig.getSourceFolder(),value.toString());
        }
        this.indexingDataset = createRdfModel(modelLocation);
        //second we need to check if we need to import RDF files to the RDF model
        //create the ResourceLoader
        this.loader =  new ResourceLoader(new RdfResourceImporter(indexingDataset), true); 
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
    }
    /**
     * @param modelLocation
     */
    private DatasetGraphTDB createRdfModel(File modelLocation) {
        if(modelLocation.exists() && !modelLocation.isDirectory()){
            throw new IllegalArgumentException("The configured RDF model directory "+
                modelLocation+"exists but is not a Directory");
        } else if(!modelLocation.exists()){
            if(!modelLocation.mkdirs()){
                throw new IllegalArgumentException("Unable to create the configured RDF model directory "+
                    modelLocation+"!");
            }
        }
        Location location = new Location(modelLocation.getAbsolutePath());
        return TDBFactory.createDatasetGraph(location);
    }
    @Override
    public boolean needsInitialisation() {
        //if there are resources with the state REGISTERED we need an initialisation
        return !loader.getResources(ResourceState.REGISTERED).isEmpty();
    }
    @Override
    public void initialise(){
        loader.loadResources();
    }
    @Override
    public void close() {
        loader = null;
        indexingDataset.close();
    }
    @Override
    public EntityDataIterator entityDataIterator() {
        String enityVar = "s";
        String fieldVar = "p";
        String valueVar = "o";
        StringBuilder qb = new StringBuilder();
        qb.append(String.format("SELECT ?%s ?%s ?%s \n",
            enityVar,fieldVar,valueVar)); //for the select
        qb.append("{ \n");
        qb.append(String.format("    ?%s ?%s ?%s . \n",
            enityVar,fieldVar,valueVar)); //for the where
        qb.append("} \n");
        log.debug("EntityDataIterator Query: \n"+qb.toString());
        Query q = QueryFactory.create(qb.toString(), Syntax.syntaxARQ);
        return new RdfEntityIterator(
            QueryExecutionFactory.create(q, indexingDataset.toDataset()).execSelect(),
            enityVar,fieldVar,valueVar);
    }

    @Override
    public Representation getEntityData(String id) {
        Node resource = Node.createURI(id);
        Representation source = vf.createRepresentation(id);
        ExtendedIterator<Triple> outgoing = indexingDataset.getDefaultGraph().find(resource, null, null);
        boolean found = outgoing.hasNext();
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
        if(found) {
            return source;
            //log.info("Resource: \n"+ModelUtils.getRepresentationInfo(source));
        } else {
            log.debug("No Statements found for Entity {}!",id);
            return null;
        }
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
                        // -> in such cases yust use the lecial type
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
                    } else {
                        source.add(field, literalValue);
                    }
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
        } else {
            if(value.isBlank()){
                log.info("ignoreing blank node value {} for field {} and Resource {}!",
                        new Object[]{value,field,source.getId()});
            } else {
                log.warn("ignoreing value {} for field {} and Resource {} because it is of an unsupported type!",
                        new Object[]{value,field,source.getId()});
            }
        } //end different value node type
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
                    if(entityNode.isURI()){ //only uri nodes are valid
                      //store it temporarily in nextBinding
                        nextBinding = firstValid; 
                        //store it as next (first) entity
                        nextEntity = entityNode;
                    } else {
                        log.warn(String.format("Current Entity %s is not a URI Node -> ignored",entityNode));
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
                if(entityNode.isURI() && 
                        // it's unbelievable, but Jena URIs might be empty!
                        !entityNode.toString().isEmpty()){
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
                    log.warn(String.format("Current Entity '%s' is not a valid URI Node -> skiped",entityNode));
                }
            }
            if(!next){ // exit the loop but still no new entity ... that means
                nextEntity = null; //there are no more entities
                nextBinding = null; // and there are also no more solutions
            }
            return currentEntity.toString();
        }
        /**
         * Processes a {@link Binding} by storing the {@link Node}s for the 
         * variables {@link #fieldVar} and {@link #valueVar} to {@link #data}.
         * This method ensures that both values are not <code>null</code> and
         * that the {@link Node} representing the field is an URI (
         * returns <code>true</code> for {@link Node#isURI()})
         * @param binding the binding to process
         */
        private void processSolution(Binding binding) {
            Node field = binding.get(fieldVar);
            if(field != null && field.isURI()){
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
            Representation representation = vf.createRepresentation(currentEntity.toString());
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
    
}
