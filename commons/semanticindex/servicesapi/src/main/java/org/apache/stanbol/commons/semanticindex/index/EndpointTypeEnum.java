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
package org.apache.stanbol.commons.semanticindex.index;

/**
 * Well known RESTful endpoint types offered by {@link SemanticIndex}. Use {@link EndpointTypeEnum#getUri()}
 * or {@link EndpointTypeEnum#toString()} to parse this endpoint types to the {@link SemanticIndexManager}
 * interface.
 */
public enum EndpointTypeEnum {
    /**
     * RESTful endpoint of the Solr
     */
    SOLR("http://lucene.apache.org/solr"),
    /**
     * RESTful search endpoint specific to the Contenthub
     */
    CONTENTHUB("http://stanbol.apache.org/ontology/contenthub#endpointType_CONTENTHUB"),
    /**
     * RESTful search endpoint specific to the Entityhub /query interface
     */
    ENTITYHUB_QUERY("http://stanbol.apache.org/ontology/entityhub#endpointType_FIELD_QUERY"),
    /**
     * RESTful search endpoint specific to the Entityhub /find interface
     */
    ENTITYHUB_FIND("http://stanbol.apache.org/ontology/entityhub#endpointType_find"),
    /**
     * SPARQL query interface
     */
    SPARQL("http://www.w3.org/TR/rdf-sparql-query/");

    private final String uri;

    EndpointTypeEnum(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri;
    }

}
