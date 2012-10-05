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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.impl;

import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.semanticweb.owlapi.model.IRI;

/**
 * Default implementation of core ontology space.
 * 
 * @author alexdma
 * 
 */
public class CoreSpaceImpl extends AbstractOntologySpaceImpl {

    public static final String SUFFIX = SpaceType.CORE.getIRISuffix();

    protected static String buildId(String scopeID) {
        return (scopeID != null ? scopeID : "") + "/" + SUFFIX;
    }

    public CoreSpaceImpl(String scopeID, IRI namespace, OntologyProvider<?> ontologyProvider) {
        super(buildId(scopeID), namespace, SpaceType.CORE, ontologyProvider);
        setConnectivityPolicy(ConnectivityPolicy.LOOSE);
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null) return false;
        if (!(arg0 instanceof OntologySpace)) return false;
        if (this == arg0) return true;
        log.warn(
            "{} only implements weak equality, i.e. managed ontologies are only checked by public key, not by content.",
            getClass());
        return super.equals(arg0);
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
