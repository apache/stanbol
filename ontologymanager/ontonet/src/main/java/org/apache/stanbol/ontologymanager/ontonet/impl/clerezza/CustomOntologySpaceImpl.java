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
package org.apache.stanbol.ontologymanager.ontonet.impl.clerezza;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologyCollectorException;
import org.semanticweb.owlapi.model.IRI;

/**
 * Default implementation of custom ontology space.
 * 
 * @author alexdma
 * 
 */
public class CustomOntologySpaceImpl extends AbstractOntologySpaceImpl implements CustomOntologySpace {

    public static final String SUFFIX = SpaceType.CUSTOM.getIRISuffix();

    protected static String buildId(String scopeID) {
        return (scopeID != null ? scopeID : "") + "/" + SUFFIX;
    }

    public CustomOntologySpaceImpl(String scopeID, IRI namespace, TcManager tcManager) {
        super(buildId(scopeID), namespace, SpaceType.CUSTOM, tcManager);
    }

    @Override
    public void attachCoreSpace(CoreOntologySpace coreSpace, boolean skipRoot) throws UnmodifiableOntologyCollectorException {
        // TODO Auto-generated method stub

    }

    /**
     * Once it is set up, a custom space is write-locked.
     */
    @Override
    public synchronized void setUp() {
        locked = true;
    }

    @Override
    public synchronized void tearDown() {
        locked = false;
    }

}
