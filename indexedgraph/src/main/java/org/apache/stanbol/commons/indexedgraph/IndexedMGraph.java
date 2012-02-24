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
package org.apache.stanbol.commons.indexedgraph;

import java.util.Collection;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;

public class IndexedMGraph extends IndexedTripleCollection implements MGraph {

    public IndexedMGraph() {
        super();
    }

    public IndexedMGraph(Collection<Triple> baseCollection) {
        super(baseCollection);
    }

    public IndexedMGraph(Iterator<Triple> iterator) {
        super(iterator);
    }

    @Override
    public Graph getGraph() {
        return new IndexedGraph(this);
    }

}
