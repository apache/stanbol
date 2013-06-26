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

import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig.KEY_INDEXING_CONFIG;
import static org.apache.stanbol.entityhub.indexing.source.jenatdb.Constants.DEFAULT_MODEL_DIRECTORY;
import static org.apache.stanbol.entityhub.indexing.source.jenatdb.Constants.PARAM_MODEL_DIRECTORY;

import java.io.File;
import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.IndexingComponent;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;

import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.tdb.sys.TDBMaker;

public final class Utils {

    private Utils() { /* do not create instances of utility classes */}
    
    /**
     * @param modelLocation The directory with the Jena TDB model. Will be created
     * if not existent.
     * @return
     * @throws IllegalArgumentException if <code>null</code> is parsed; 
     * if the parsed {@link File} exists but is not a directory; if the parsed 
     * File does NOT exists AND can not be created.
     */
    public static DatasetGraphTDB initTDBDataset(File modelLocation) {
        if(modelLocation == null){
            throw new IllegalArgumentException("The parsed Jena TDB directory" +
            		"MUST NOT be NULL!");
        }
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
        //TODO: change this to support transactions
        //    TDBMaker.createDatasetGraphTransaction(location);
        //  if we need transaction support!
        return TDBMaker.createDatasetGraphTDB(location);
    }

    /**
     * uses the parsed configuration to get/create the Jena TDB store
     * @param config A configuration as parsed to {@link IndexingComponent#setConfiguration(Map)}
     * @return the opened/created Jena TDB dataset
     * @throws IllegalArgumentException if the config is <code>null</code>; is
     * missing a value for the {@link IndexingConfig#KEY_INDEXING_CONFIG} or
     * {@link #initTDBDataset(File)} throws an IllegalArgumentException
     */
    public static DatasetGraphTDB getTDBDataset(Map<String,Object> config) {
        IndexingConfig indexingConfig = (IndexingConfig)config.get(KEY_INDEXING_CONFIG);
        if(indexingConfig == null){
            throw new IllegalArgumentException("No IndexingConfig object present as value of key '"
                    + KEY_INDEXING_CONFIG+"'!");
        }
        Object value = config.get(PARAM_MODEL_DIRECTORY);
        File modelLocation;
        if(value == null){
            modelLocation = new File(indexingConfig.getSourceFolder(),DEFAULT_MODEL_DIRECTORY);
        } else {
            modelLocation = new File(indexingConfig.getSourceFolder(),value.toString());
        }
        return initTDBDataset(modelLocation);

    }
}
