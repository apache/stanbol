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
package org.apache.stanbol.contentorganizer.servicesapi;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

public interface KnowledgeRetriever {

    /**
     * 
     * TODO I would prefer to return a {@link Graph}, but since the Clerezza implementation of
     * {@link MGraph#getGraph()} seems to create a new in-memory mirror image, i have to remain open to
     * returning {@link MGraph} too.
     * 
     * @param ci
     * @return
     */
    TripleCollection aggregateKnowledge(ContentItem ci);

}
