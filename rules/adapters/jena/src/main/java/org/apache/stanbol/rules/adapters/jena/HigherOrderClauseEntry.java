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

import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;

/**
 * It is used for represent higher order atoms.<br/>
 * It is used to convert Stanbol atoms that accept other atoms as arguments, such as
 * <code>sum(sum(5,?x), ?y)</code>.<br/>
 * In such a situation in Jena we should use new variables as place holders, e.g., sum(?ph1, ?y, ?z) sum(5,
 * ?x, ?ph1).
 * 
 * @author anuzzolese
 * 
 */
public class HigherOrderClauseEntry implements ClauseEntry {

    private Node bindableNode;
    private List<ClauseEntry> clauseEntries;

    public HigherOrderClauseEntry(Node bindableNode, List<ClauseEntry> clauseEntries) {
        this.bindableNode = bindableNode;
        this.clauseEntries = clauseEntries;
    }

    public HigherOrderClauseEntry() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean sameAs(Object o) {
        // TODO Auto-generated method stub
        return false;
    }

    public Node getBindableNode() {
        return bindableNode;
    }

    public List<ClauseEntry> getClauseEntries() {
        return clauseEntries;
    }

}
