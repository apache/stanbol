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

package org.apache.stanbol.rules.adapters.jena;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;

/**
 * A wrapper in order to treat a node as a {@link ClauseEntry}
 * 
 * @author anuzzolese
 * 
 */
public class NodeClauseEntry implements ClauseEntry {

    private Node node;

    public NodeClauseEntry(Node node) {
        this.node = node;
    }

    @Override
    public boolean sameAs(Object o) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Get the node that is an instance of the class {@link Node}.
     * 
     * @return the node
     */
    public Node getNode() {
        return node;
    }

}
