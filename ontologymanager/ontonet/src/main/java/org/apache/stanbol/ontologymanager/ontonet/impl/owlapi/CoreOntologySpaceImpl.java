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
package org.apache.stanbol.ontologymanager.ontonet.impl.owlapi;

import org.apache.stanbol.ontologymanager.ontonet.api.scope.CoreOntologySpace;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Default implementation of the core ontology space.
 * 
 * @author alexdma
 */
public class CoreOntologySpaceImpl extends AbstractOntologySpaceImpl implements CoreOntologySpace {

    public static final String SUFFIX = SpaceType.CORE.getIRISuffix();

    protected static String buildId(String scopeID) {
        return (scopeID != null ? scopeID : "") + "/" + SUFFIX;
    }

    public CoreOntologySpaceImpl(String scopeID, IRI namespace) {
        super(buildId(scopeID), namespace, SpaceType.CORE);
    }

    public CoreOntologySpaceImpl(String scopeID, IRI namespace, OWLOntologyManager ontologyManager) {
        super(buildId(scopeID), namespace, SpaceType.CORE, ontologyManager);
    }

    /**
     * When set up, a core space is write-locked.
     */
    @Override
    public synchronized void setUp() {
        locked = true;
    }

    /**
     * When torn down, a core space releases its write-lock.
     */
    @Override
    public synchronized void tearDown() {
        locked = false;
    }

}
