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
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;

public class FiledMapperProcessor implements EntityProcessor{
    public static final String PARAM_MAPPINGS = "mappings";
    public static final String PARAM_VALUE_FACTORY = "valueFactory";
    public static final String DEFAULT_MAPPINGS_FILE_NAME = "fieldMappings.txt";
    private FieldMapper mapper;
    private ValueFactory vf;
    private NamespacePrefixService nsPrefixService;
    /**
     * This Constructor relays on that {@link #setConfiguration(Map)} is called
     * afterwards!
     */
    public FiledMapperProcessor(){
        this(null,null);
    }
    /**
     * Internally used to initialise the {@link ValueFactory}
     * @param vf the value factory or <code>null</code> to use the {@link InMemoryValueFactory}.
     */
    private FiledMapperProcessor(NamespacePrefixService nps, ValueFactory vf){
        setValueFactory(vf);
        this.nsPrefixService = nps;
    }
    public FiledMapperProcessor(FieldMapper mapper, NamespacePrefixService nps, ValueFactory vf){
        this(nps,vf);
        if(mapper == null){
            throw new IllegalArgumentException("The parsed FieldMapper MUST NOT be NULL!");
        } else if(mapper.getMappings().isEmpty()){
            throw new IllegalStateException("The parsed field mappings MUST contain at least a single valid mapping!");
        }
        this.mapper = mapper;
    }
    public FiledMapperProcessor(Iterator<String> mappings, NamespacePrefixService nps, ValueFactory vf){
        this(nps, vf);
        if(mappings == null){
            throw new IllegalArgumentException("The parsed field mappings MUST NOT be NULL!");
        }
        mapper = FieldMappingUtils.createDefaultFieldMapper(mappings,nsPrefixService);
        if(mapper.getMappings().isEmpty()){
            throw new IllegalStateException("The parsed field mappings MUST contain at least a single valid mapping!");
        }
    }
    public FiledMapperProcessor(InputStream mappings, NamespacePrefixService nps, ValueFactory vf) throws IOException{
        this(nps, vf);
        if(mappings == null){
            throw new IllegalArgumentException("The parsed field mappings MUST NOT be NULL!");
        }
        this.mapper = createMapperFormStream(mappings,nsPrefixService);
    }
    @Override
    public Representation process(Representation source) {
        if(mapper == null){
            throw new IllegalStateException("The mapper is not initialised. One must call setConfiguration to configure the FieldMapper!");
        }
        if(source == null){
            return null;
        } else {
            return mapper.applyMappings(source,
                vf.createRepresentation(source.getId()),
                vf);
        }
    }
    /**
     * used by the different constructors to init the {@link ValueFactory}
     * @param vf the value factory or <code>null</code> to use the default
     */
    private void setValueFactory(ValueFactory vf) {
        if(vf == null){
            this.vf = InMemoryValueFactory.getInstance();
        } else {
            this.vf = vf;
        }
    }
    @Override
    public void close() {
        //nothing todo
        
    }
    @Override
    public void initialise() {
        //nothing todo
    }
    @Override
    public boolean needsInitialisation() {
        return false;
    }
    @Override
    public void setConfiguration(Map<String,Object> config) {
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        nsPrefixService = indexingConfig.getNamespacePrefixService();
        Object value = config.get(PARAM_MAPPINGS);
        if(value == null || value.toString().isEmpty()){
            //use the mappings configured for the Index
            this.mapper = FieldMappingUtils.createDefaultFieldMapper(
                indexingConfig.getIndexFieldConfiguration());
        } else {
            //load (other) mappings based on the provided mappings parameter
            //final File file = new File(indexingConfig.getConfigFolder(),value.toString());
            File mappings = indexingConfig.getConfigFile(value.toString());
            if(mappings != null){
                try {
                    InputStream in = new FileInputStream(mappings);
                    this.mapper = createMapperFormStream(in,nsPrefixService);
                    IOUtils.closeQuietly(in);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to access FieldMapping file "+
                        value+" not found in configuration directory "+
                        indexingConfig.getConfigFolder());
                }
            } else {
                throw new IllegalArgumentException("FieldMapping file "+
                    value+" not found in configuration directory "+
                    indexingConfig.getConfigFolder());
            }
        }
        //TODO: get the valueFactory form the config (currently an InMemory is
        //create by the default constructor!
    }
    /**
     * Utility that allows to create a FieldMapper form an inputStream.
     * It uses {@link IOUtils#lineIterator(InputStream, String)} and parses it
     * to {@link FieldMappingUtils#createDefaultFieldMapper(Iterator)}
     * @param in the stream to read the mappings from
     * @throws IOException on any error while reading the data from the stream
     */
    private static FieldMapper createMapperFormStream(final InputStream in, NamespacePrefixService nps) throws IOException {
        return FieldMappingUtils.createDefaultFieldMapper(new Iterator<String>() {
            LineIterator it = IOUtils.lineIterator(in, "UTF-8");
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }
            @Override
            public String next() {
                return it.nextLine();
            }
            @Override
            public void remove() {
                it.remove();
            }
        },nps);
    }

}
