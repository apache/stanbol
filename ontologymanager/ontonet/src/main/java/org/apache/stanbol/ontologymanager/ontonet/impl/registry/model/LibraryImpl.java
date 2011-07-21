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
package org.apache.stanbol.ontologymanager.ontonet.impl.registry.model;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.registry.LibraryContentNotLoadedException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryContentException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryOntologyNotLoadedException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Library;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryOntology;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Default implementation of the ontology library model.
 */
public class LibraryImpl extends AbstractRegistryItem implements Library {

    private boolean loaded = false;

    public LibraryImpl(String name) {
        super(name);
    }

    public LibraryImpl(String name, URL url) throws URISyntaxException {
        super(name, url);
    }

    @Override
    public Set<OWLOntology> getOntologies() throws RegistryContentException {
        /*
         * Note that this implementation is not synchronized. Listeners may indefinitely be notified before or
         * after the rest of this method is executed. If listeners call loadOntologies(), they could still get
         * a RegistryContentException, which however they can catch by calling loadOntologies() and
         * getOntologies() in sequence.
         */
        fireContentRequested(this);
        if (!loaded) throw new LibraryContentNotLoadedException(this);
        Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
        for (RegistryItem child : getChildren()) {
            if (child instanceof RegistryOntology) {
                OWLOntology o = ((RegistryOntology) child).asOWLOntology();
                // Should never be null if the library was loaded correctly (an error should have already been
                // thrown when loading it), but just in case.
                if (o != null) ontologies.add(o);
                else throw new RegistryOntologyNotLoadedException((RegistryOntology) child);
            }
        }
        return ontologies;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public void loadOntologies(OWLOntologyManager mgr) {
        // TODO Auto-generated method stub
        loaded = true;
    }

}
