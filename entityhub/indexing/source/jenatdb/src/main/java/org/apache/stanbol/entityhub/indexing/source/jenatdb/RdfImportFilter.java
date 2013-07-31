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

import org.apache.stanbol.entityhub.indexing.core.IndexingComponent;

import com.hp.hpl.jena.graph.Node;

/**
 * Allows to filter Triples parsed from RDF files. Useful to NOT import some
 * RDF triples from RDF dumps that are not relevant for the indexing process.
 * @author Rupert Westenthaler
 *
 */
public interface RdfImportFilter extends IndexingComponent{

    
    public boolean accept(Node s, Node p, Node o);
    
}
