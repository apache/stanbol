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
package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.StringUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Default implementation of the custom ontology space.
 */
public class CustomOntologySpaceImpl extends AbstractOntologySpaceImpl implements CustomOntologySpace {

    public static final String SUFFIX = SpaceType.CUSTOM.getIRISuffix();

    public CustomOntologySpaceImpl(IRI scopeID, ClerezzaOntologyStorage storage) {
        super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/" + SpaceType.CUSTOM.getIRISuffix()),
                SpaceType.CUSTOM, storage);
    }

    public CustomOntologySpaceImpl(IRI scopeID,
                                   ClerezzaOntologyStorage storage,
                                   OWLOntologyManager ontologyManager) {
        super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/" + SpaceType.CUSTOM.getIRISuffix()),
                SpaceType.CUSTOM, storage, ontologyManager);
    }

    @Override
    public void attachCoreSpace(CoreOntologySpace coreSpace, boolean skipRoot) throws UnmodifiableOntologySpaceException {
// FIXME re-implement!
//        OWLOntology o = coreSpace.getTopOntology();
//        // This does the append thingy
//        log.debug("Attaching " + o + " TO " + getTopOntology() + " ...");
//        try {
//            // It is in fact the addition of the core space top ontology to the
//            // custom space, with import statements and all.
//            addOntology(new RootOntologySource(o, null));
//            // log.debug("ok");
//        } catch (Exception ex) {
//            log.error("FAILED", ex);
//        }

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
