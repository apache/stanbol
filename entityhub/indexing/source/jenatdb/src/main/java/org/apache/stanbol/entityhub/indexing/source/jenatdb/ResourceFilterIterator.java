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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * 
 * Allows to use an EntityIterator over all Resources where <ul>
 * <li> the property is equals to the configured {@link #PARAM_FIELD} value
 * <li> the value is equals to one of the configured {@link #PARAM_VALUES} values
 * (separated by ';'). This implementation will first iterate over all value1 
 * than value2, ...
 * </ul>
 * <p>
 * <b>NOTE:</b> This implementation does NOT support Wildcard value, because in
 * such cases it is much faster to use the {@link RdfIndexingSource} as
 * {@link EntityDataIterable} for indexing!
 * 
 * 
 * @author Rupert Westenthaler
 *
 */
public class ResourceFilterIterator implements EntityIterator{
    
    private final Logger log = LoggerFactory.getLogger(ResourceFilterIterator.class);
    
    public static final String PARAM_FIELD = "field";
    public static final String PARAM_VALUES = "values";
    
    public static final String DEFAULT_FIELD = "rdf:type";

    private Node field;
    private Collection<Node> values;
    
    
    /**
     * The RDF data
     */
    private DatasetGraph indexingDataset;
    /**
     * The Iterator over the current EntityFilter (or <code>null</code> if not
     * yet initialised)
     */
    private ExtendedIterator<Triple> iterator;
    /**
     * Iterator over the configured {@link #values}
     */
    private Iterator<Node> valueIterator;

    /**
     * The IndexingConfiguration
     */
    private IndexingConfig indexingConfig;

    private NamespacePrefixService nsPrefixService;

    @Override
    public void setConfiguration(Map<String,Object> config) {
        indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        this.indexingDataset = Utils.getTDBDataset(config);
        nsPrefixService = indexingConfig.getNamespacePrefixService();
        Object value = config.get(PARAM_FIELD);
        if(value == null || value.toString().isEmpty()){
            this.field = Node.createURI(NamespaceMappingUtils.getConfiguredUri(
                nsPrefixService, DEFAULT_FIELD));
            log.info("Using default Field {}",field);
        } else {
            this.field = Node.createURI(NamespaceMappingUtils.getConfiguredUri(
                nsPrefixService, value.toString()));
            log.info("configured Field: {}",field);
        }
        value = config.get(PARAM_VALUES);
        if(value == null || value.toString().isEmpty()){
            throw new IllegalArgumentException("Missing required Parameter "+PARAM_VALUES+". Set to '*' to deactivate Filtering");
        } else if(value instanceof String){
            String stringValue = value.toString().trim();
            if(stringValue.startsWith("*")){ // * -> deactivate Filtering
                throw new IllegalArgumentException("Wildcard is NOT supported as" +
                		"directoy using EntityDataIterable with the Jena TDB will" +
                		"provide much better performance (change configuration to use" +
                		"the RdfIndexingSource as EntityDataIterable)!");
            } else {
                parseFieldValues(stringValue.split(";"));
            }
        } else if (value instanceof String[]){
            parseFieldValues((String[])value);
        } else {
            throw new IllegalArgumentException("Type of parameter "+PARAM_VALUES+'='+value+
                "(type:"+value.getClass()+") is not supported MUST be String or String[]!");
        }
        valueIterator = this.values.iterator();
    }

    /**
     * @param value
     * @param stringValues
     */
    private void parseFieldValues(String...stringValues) {
        if(stringValues == null || stringValues.length < 1){
            throw new IllegalArgumentException("Parameter "+PARAM_VALUES+" does not contain a field value!");
        }
        Set<Node> values = new HashSet<Node>();
        for(String fieldValue : stringValues){
            fieldValue = fieldValue.trim();
            if(fieldValue != null){
                if(fieldValue.isEmpty()){
                    throw new IllegalArgumentException("no parsed value (seperated by ';') MUST BE an empty String");
                } else if(fieldValue.equals("*")){
                    throw new IllegalArgumentException("Wildcard is NOT supported as" +
                            "directoy using EntityDataIterable with the Jena TDB will" +
                            "provide much better performance (change configuration to use" +
                            "the RdfIndexingSource as EntityDataIterable)!");
                } else {
                    values.add(Node.createURI(NamespaceMappingUtils.getConfiguredUri(
                        nsPrefixService, fieldValue)));
                }
            }
        }
        if(values.isEmpty()){
            throw new IllegalArgumentException("Parameter "+PARAM_VALUES
                + " does not contain a valid field value (values = "
                + Arrays.toString(stringValues)+"!");
        } else {
            this.values = values;
        }
    }

    @Override
    public boolean needsInitialisation() {
        // Nope no initialisation needed
        return false;
    }

    @Override
    public void initialise() {
    }

    @Override
    public void close() {
        indexingDataset.close();
    }

    @Override
    public boolean hasNext() {
        do {
            if(iterator == null){
                //NOTE: the #setConfiguration method ensures a value to be present
                Node value = valueIterator.next(); 
                log.info("Iterator over Entities field: '{}' value '{}'",field,value);
                iterator = indexingDataset.getDefaultGraph().find(null,field,value);

            }
            if(iterator != null){
                if(iterator.hasNext()){
                    return true;
                }
                iterator.close();
                iterator = null;
            }
        } while(valueIterator.hasNext());
        return false; //iterated over all elements of all configured values
    }

    @Override
    public EntityScore next() {
        return new EntityScore(iterator.next().getSubject().toString(),null);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removal of Entities is not allowed!");
    }

}
