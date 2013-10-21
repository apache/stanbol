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
package org.apache.stanbol.entityhub.model.sesame;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.openrdf.model.BNode;
import org.openrdf.model.Value;

/**
 * Internally used to handle BNodes. Externally mapped to {@link Reference}.
 * <p>
 * <b>NOTE:</b> this does not aim to fully support BNodes
 * @author Rupert Westenthaler
 *
 */
public class RdfBNode implements Reference, RdfWrapper {

    private BNode node;

    protected RdfBNode(BNode node) {
        this.node = node;
    }
    
    @Override
    public Value getValue() {
        return node;
    }

    @Override
    public String getReference() {
        return node.getID();
    }
    
    @Override
    public int hashCode() {
        return node.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Reference && 
                getReference().equals(((Reference)obj).getReference());
    }
    
    @Override
    public String toString() {
        return node.toString();
    }
    
}