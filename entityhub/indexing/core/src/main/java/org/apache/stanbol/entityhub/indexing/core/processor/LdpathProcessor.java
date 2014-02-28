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
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.SingleRepresentationBackend;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LdpathProcessor implements EntityProcessor{

    private final Logger log = LoggerFactory.getLogger(LdpathProcessor.class);
    /**
     * The reference to the file containing the LDPath statement used for
     * processing. The path is evaluated relative to the config directory
     * of the indexing
     */
    public static final String PARAMETER_LD_PATH = "ldpath";
    /**
     * If results of the LDPath transformation are appended to the incoming
     * representation, or if the incoming Representation is replaced by the
     * results of the LDPath program (default is append).
     */
    public static final String PARAMETER_APPEND = "append";
    /**
     * By default appending of LDPath results to the parsed Representation is
     * activeted
     */
    public static final boolean DEFAULT_APPEND_MODE = true;

    private final ValueFactory vf;
    protected EntityhubLDPath ldPath;
    private final SingleRepresentationBackend backend;
    private Program<Object> program;
    private boolean appendMode;
    protected IndexingConfig indexingConfig;
    
    public LdpathProcessor(){
        vf = InMemoryValueFactory.getInstance();
        this.backend = new SingleRepresentationBackend(vf);
        this.ldPath = new EntityhubLDPath(backend);
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

    @Override
    public Representation process(Representation source) {
        if(source == null){
            return null;
        }
        backend.setRepresentation(source);
        Representation result = ldPath.execute(vf.createReference(source.getId()), program);
        if(appendMode){
            Iterator<String> fields = result.getFieldNames();
            while(fields.hasNext()){
                String field = fields.next();
                source.add(field, result.get(field));
            }
            return source;
        } else {
            return result;
        }
    }

    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        //parse the ldpath
        final File ldpathFile;
        Object value = config.get(PARAMETER_LD_PATH);
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
        value = config.get(PARAMETER_APPEND);
        if(value instanceof Boolean){
            this.appendMode = ((Boolean) value).booleanValue();
        } else if(value != null && !value.toString().isEmpty()){
            this.appendMode = Boolean.parseBoolean(value.toString());
        } else {
            this.appendMode = DEFAULT_APPEND_MODE;
        }
    }

    
}
