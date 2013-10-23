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
package org.apache.stanbol.entityhub.indexing.core.config;


/**
 * Constants defines/used for Indexing.
 * @author Rupert Westenthaler
 *
 */
public interface IndexingConstants {

    String KEY_NAME                  = "name";
    String KEY_DESCRIPTION           = "description";
    String KEY_ENTITY_DATA_ITERABLE  = "entityDataIterable";
    String KEY_ENTITY_DATA_PROVIDER  = "entityDataProvider";
    String KEY_ENTITY_ID_ITERATOR    = "entityIdIterator";
    String KEY_ENTITY_SCORE_PROVIDER = "entityScoreProvider";
    String KEY_INDEXING_DESTINATION = "indexingDestination";
    String KEY_INDEX_FIELD_CONFIG = "fieldConfiguration";
    /**
     * Name of the file relative to the destination folder used to store
     * the IDs of indexed Entities.
     */
    String KEX_INDEXED_ENTITIES_FILE = "indexedEntitiesFile";
    /**
     * usage:<br>
     * <pre>
     * {class1},name:{name1};{class2},name:{name2};...
     * </pre>
     * The class implementing the normaliser and the name of the configuration
     * file stored within /config/normaliser/{name}.properties
     */
    String KEY_SCORE_NORMALIZER      = "scoreNormalizer";
    String KEY_ENTITY_PROCESSOR      = "entityProcessor";
    String KEY_ENTITY_POST_PROCESSOR = "entityPostProcessor";
    String KEY_FAIL_ON_ERROR_LOADING_RESOURCE = "failOnErrorLoadingResource";

}
