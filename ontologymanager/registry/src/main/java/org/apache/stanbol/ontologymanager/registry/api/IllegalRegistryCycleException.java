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
package org.apache.stanbol.ontologymanager.registry.api;

import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;

/**
 * Thrown when an attempt to create an illegal cycle in the registry item model is detected. Examples of
 * illegal cycles include being both a parent and a child of the same registry item, or a parent or child of
 * itself, or a library being a child of an ontology.
 * 
 * @author alexdma
 */
public class IllegalRegistryCycleException extends RegistryContentException {

    /**
     * 
     */
    private static final long serialVersionUID = -2929796860026423332L;

    private RegistryOperation operationType;

    private RegistryItem sourceNode, targetNode;

    /**
     * Creates a new instance of {@link IllegalRegistryCycleException}.
     * 
     * @param sourceNode
     *            the source node of the cycle.
     * @param targetNode
     *            the target node of the cycle.
     * @param operationType
     *            the type of operation attempted, i.e. the disallowed arc of the cycle.
     */
    public IllegalRegistryCycleException(RegistryItem sourceNode,
                                         RegistryItem targetNode,
                                         RegistryOperation operationType) {
        super("Cycles of type " + operationType + " between registry items " + sourceNode + " and "
              + targetNode + " are not allowed.");
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
    }

    /**
     * Returns the type of operation attempted, i.e. the disallowed arc of this cycle.
     * 
     * @return the type of operation attempted.
     */
    public RegistryOperation getOperationType() {
        return operationType;
    }

    /**
     * Returns the source node of this cycle.
     * 
     * @return the source node of the cycle.
     */
    public RegistryItem getSourceNode() {
        return sourceNode;
    }

    /**
     * Returns the target node of this cycle.
     * 
     * @return the target node of the cycle.
     */
    public RegistryItem getTargetNode() {
        return targetNode;
    }

}
