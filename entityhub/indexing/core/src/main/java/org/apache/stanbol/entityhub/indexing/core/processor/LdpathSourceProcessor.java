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
package org.apache.stanbol.entityhub.indexing.core.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.transformers.NodeTransformer;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.marmotta.ldpath.model.transformers.IdentityTransformer;
import org.apache.marmotta.ldpath.parser.Configuration;

/**
 * LDpath based processor that tries to cast the 
 * @author westei
 *
 */
public class LdpathSourceProcessor implements EntityProcessor {

    private final Logger log = LoggerFactory.getLogger(LdpathProcessor.class);
    /**
     * @see LdpathProcessor#PARAMETER_LD_PATH
     */
    public static final String PARAMETER_LD_PATH = LdpathProcessor.PARAMETER_LD_PATH;
    /**
     * @see LdpathProcessor#PARAMETER_APPEND
     */
    public static final String PARAMETER_APPEND = LdpathProcessor.PARAMETER_APPEND;
    /**
     * @see LdpathProcessor#DEFAULT_APPEND_MODE
     */
    public static final boolean DEFAULT_APPEND_MODE = LdpathProcessor.DEFAULT_APPEND_MODE;

    /**
     * ValueFactory used to create Representation
     */
    private final ValueFactory vf = InMemoryValueFactory.getInstance();
    /**
     * {@link LDPath} instance of an unknown generic type (depends on the 
     * used Indexing source
     */
    @SuppressWarnings("rawtypes")
    protected LDPath ldPath;
    /**
     * The RDF backend
     */
    @SuppressWarnings("rawtypes")
    protected RDFBackend backend;
    @SuppressWarnings("rawtypes")
    protected Configuration configuration;
    @SuppressWarnings("rawtypes")
    private Map<String,NodeTransformer> transformer;
    @SuppressWarnings("rawtypes")
    private Program program;
    /**
     * If results are appended to the parsed Representation
     */
    private boolean appendMode;

    /**
     * Default constructor. Expects that {@link #setConfiguration(Map)} is
     * called to initialise the instance
     */
    public LdpathSourceProcessor() {
    }
    /**
     * Initializes the {@link LdpathSourceProcessor} with the parsed parameters and
     * appendMode enabled
     * @param backend the {@link RDFBackend} to use for executing the program
     * @param program the {@link Program} to be executed by this processor for
     * entities to process
     */
    @SuppressWarnings({ "rawtypes"})
    public LdpathSourceProcessor(RDFBackend backend, Program program) {
        this(backend,program,true);
    }
    /**
     * Initializes the {@link LdpathSourceProcessor} with the parsed parameters
     * @param backend the {@link RDFBackend} to use for executing the program
     * @param program the {@link Program} to be executed by this processor for
     * entities to process
     * @param appendMode if <code>true</code> results of the {@link LDPath} execution
     * will be appended to parsed {@link Representation}. If <code>false</code> the
     * results of this processor will be added to a new {@link Representation} -
     * meaning that fields of the input will not be present in the processing
     * results
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public LdpathSourceProcessor(RDFBackend backend, Program program, boolean appendMode) {
        assert backend != null;
        assert program != null;
        this.backend = backend;
        this.program = program;
        this.appendMode = appendMode;
        this.configuration = new EntityhubConfiguration(vf);
        this.transformer = configuration.getTransformers();
        this.ldPath = new LDPath(backend,configuration);
    }
    
    /**
     * The indexing configuration
     */
    protected IndexingConfig indexingConfig;
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setConfiguration(Map<String,Object> config) {
        indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        Object indexingSource;
        //we need to check for both EntityDataProvider and EntityDataIterator
        indexingSource = indexingConfig.getEntityDataProvider();
        if(indexingSource == null){
            indexingSource = indexingConfig.getDataIterable();
        }
        if(indexingSource == null){
            throw new IllegalStateException("Indexing Configuration does not contain" +
            		"neither an EntityDataProvider nor an EntityIdIterator!");
        }
        if(indexingSource instanceof RDFBackend<?>){
            //NOTE we use the EntityhubConfiguration to have the same pre-registered
            //     namespaces as the other components.
            this.backend = (RDFBackend)indexingSource;
            this.configuration = new EntityhubConfiguration(vf);
            this.transformer = configuration.getTransformers();
            this.ldPath = new LDPath(backend,configuration);
        } else {
            throw new IllegalArgumentException("The configured IndexingSource '"
                    + indexingSource.getClass().getSimpleName()+"' does not support "
                    + "LDPath (does not implement RDFBackend)! This Processor "
                    + "can only be used with IndexingSources that support LDPath!");
        }
        Object value = config.get(PARAMETER_LD_PATH);
        final File ldpathFile;
        if(value != null && !value.toString().isEmpty()){
            ldpathFile = indexingConfig.getConfigFile(value.toString());
            if(ldpathFile == null || !ldpathFile.exists()){
                throw new IllegalArgumentException("Configured '"
                        + PARAMETER_LD_PATH +"' file was not found!");
            }
            if(!ldpathFile.isFile()){
                throw new IllegalArgumentException("Configured '"
                        + PARAMETER_LD_PATH +"' file exists but is not a File!");
            }
        } else {
            throw new IllegalArgumentException("Missing required configuration '"
                + PARAMETER_LD_PATH +"' - the file containing the LDPath program used by this "
                + LdpathProcessor.class.getSimpleName()+"!");
        }
        //The backend needs not to be initialised to parse a program as
        //parsing only requires the "value converter" methods that need also to
        //work without initialising
        //if this is a Problem one can also move parsing to the init method
        parseLdPathProgram(ldpathFile);
        value = config.get(PARAMETER_APPEND);
        if(value instanceof Boolean){
            this.appendMode = ((Boolean) value).booleanValue();
        } else if(value != null && !value.toString().isEmpty()){
            this.appendMode = Boolean.parseBoolean(value.toString());
        } else {
            this.appendMode = DEFAULT_APPEND_MODE;
        }
    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    private void parseLdPathProgram(File ldpathFile) {
        Reader in = null;
        try {
            in = new InputStreamReader(new FileInputStream(ldpathFile), Charset.forName("UTF-8"));
            this.program = ldPath.parseProgram(in);
            log.info("ldpath program: \n{}\n",program.getPathExpression(backend));
        } catch (IOException e) {
            throw new IllegalStateException("Unabwle to read LDPath program from configured file '"
                + ldpathFile +"'!",e);
        } catch (LDPathParseException e) {
            throw new IllegalStateException("Unable to parse LDPath program from configured file '"
                    + ldpathFile +"'!",e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void initialise() {

    }

    @Override
    public void close() {

    }

    @SuppressWarnings({"unchecked","rawtypes"})
    @Override
    public Representation process(Representation source) {
        if(log.isTraceEnabled()){
            log.trace(" - process {} (backend: {}, program: {}, append: {})",
                    source.getId(), backend, program, appendMode);
        }
        Object context = backend.createURI(source.getId());
        Representation result  = appendMode ? source : vf.createRepresentation(source.getId());
        /*
         * NOTE: LDPath will return Node instances of the RDFRepositroy if no
         * transformation is defined for a statement (line) in the configured
         * LDpath program (the ":: xsd:int" at the end). this Nodes need to be
         * converted to valid Entityhub Representation values.
         * As we can not know the generic type used by the RDFRepository
         * implementation of the indexing source this is a little bit tricky.
         * What this does is:
         *   - for URIs it creates References
         *   - for plain literal it adds natural texts
         *   - for typed literals it uses the NodeTransformer registered with 
         *     the LDPath (or more precise the Configuration object parsed to 
         *     the LDPath in the constructor) to transform the values to
         *     Java objects. If no transformer is found or an Exeption occurs
         *     than the lexical form is used and added as String to the 
         *     Entityhub.
         */
        Map<String,Collection<Object>> resultMap = (Map<String,Collection<Object>>)program.execute(backend, context);
        for(Entry<String,Collection<Object>> entry : resultMap.entrySet()){
            NodeTransformer fieldTransformer = program.getField(entry.getKey()).getTransformer();
            if(fieldTransformer == null || fieldTransformer instanceof IdentityTransformer<?>){
                //we need to convert the RDFBackend Node to an Representation object
                for(Object value : entry.getValue()){
                    if(backend.isURI(value)){
                        result.addReference(entry.getKey(), backend.stringValue(value));
                    } else if(backend.isLiteral(value)){ //literal
                        Locale locale = backend.getLiteralLanguage(value);
                        if(locale != null){ //text with language
                            String lang = locale.getLanguage();
                            result.addNaturalText(entry.getKey(), backend.stringValue(value), 
                                lang.isEmpty() ? null : lang);
                        } else { // no language
                            URI type = backend.getLiteralType(value);
                            if(type != null){ //typed literal -> need to transform
                                NodeTransformer nt = transformer.get(type.toString());
                                if(nt != null){ //add typed literal
                                    try {
                                        result.add(entry.getKey(), nt.transform(backend, value, 
                                            Collections.<String,String>emptyMap()));
                                    } catch (RuntimeException e) {
                                       log.info("Unable to transform {} to dataType {} -> will use lexical form",value,type);
                                       result.add(entry.getKey(),backend.stringValue(value));
                                    }
                                } else { //no transformer
                                    log.info("No transformer for type {} -> will use lexical form",type);
                                    result.add(entry.getKey(),backend.stringValue(value));
                                    
                                }
                            } else { //no langauge and no type -> literal with no language
                                result.addNaturalText(entry.getKey(), backend.stringValue(value));
                            }
                        }
                    } else { //bNode
                        log.info("Ignore bNode {} (class: {})",value,value.getClass());
                    }
                } //end for all values
            } else { //already a transformed values
                result.add(entry.getKey(), entry.getValue()); //just add all values
            }
        }
        return result;
    }

}
