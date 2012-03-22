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

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.ImportManagementPolicy;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OWLAPI-based (in-memory) ontology cache implementation.
 * 
 * TODO re-introduce SCR annotations, just avoid loading the component
 * 
 * @author alexdma
 * 
 */
// @Component(immediate = true, metatype = false)
// @Service(OntologyProvider.class)
public class OWLAPIOntologyProvider implements OntologyProvider<OWLOntologyManager> {

    private Logger log = LoggerFactory.getLogger(getClass());

    private OWLOntologyManager store = null;

    public OWLAPIOntologyProvider() {
        this(OWLManager.createOWLOntologyManager());
    }

    /**
     * Creates a new instance of ClerezzaCache with an embedded {@link OWLOntologyManager}.
     */
    public OWLAPIOntologyProvider(OWLOntologyManager store) {
        if (store == null) throw new IllegalArgumentException("Cache requires a non-null OWLOntologyManager.");
        this.store = store;
    }

    @Override
    public ImportManagementPolicy getImportManagementPolicy() {
        return ImportManagementPolicy.PRESERVE;
    }

    @Override
    public String getKey(IRI ontologyIRI) {
        return ontologyIRI.toString();
    }

    @Override
    public Set<String> getKeys() {
        Set<String> result = new HashSet<String>();
        for (OWLOntology o : store.getOntologies())
            result.add(OWLUtils.guessOntologyIdentifier(o).toString());
        return result;
    }

    @Override
    public OWLOntologyManager getStore() {
        return store;
    }

    @Override
    public <O> O getStoredOntology(IRI reference, Class<O> returnType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <O> O getStoredOntology(IRI reference, Class<O> returnType, boolean merge) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <O> O getStoredOntology(String identifier, Class<O> returnType) {
        if (returnType == null) {
            returnType = (Class<O>) OWLOntology.class;
            log.warn("No return type given for ontologies. Will return a {}", returnType);
        }
        boolean canDo = false;
        for (Class<?> clazz : getSupportedReturnTypes())
            if (clazz.isAssignableFrom(returnType)) {
                canDo = true;
                break;
            }
        if (!canDo) throw new UnsupportedOperationException(
                "Return type " + returnType
                        + " is not allowed in this implementation. Only allowed return types are "
                        + getSupportedReturnTypes());
        return null;
    }

    @Override
    public <O> O getStoredOntology(String key, Class<O> returnType, boolean merge) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?>[] getSupportedReturnTypes() {
        return new Class<?>[] {OWLOntology.class};
    }

    @Override
    public boolean hasOntology(OWLOntologyID id) {
        return store.contains(id);
    }

    @Override
    public boolean hasOntology(String key) {
        return store.contains(IRI.create(key));
    }

    @Override
    public String loadInStore(InputStream data, String formatIdentifier, String preferredKey, boolean force) {
        try {
            OWLOntology o = store.loadOntologyFromOntologyDocument(data);
            return OWLUtils.guessOntologyIdentifier(o).toString();
        } catch (OWLOntologyCreationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String loadInStore(IRI location, String formatIdentifier, String preferredKey, boolean force) {
        OWLOntology o = null;
        try {
            o = store.loadOntologyFromOntologyDocument(location);
        } catch (OWLOntologyAlreadyExistsException e) {
            if (!force) o = store.getOntology(e.getOntologyID());
        } catch (OWLOntologyDocumentAlreadyExistsException e) {
            if (!force) o = store.getOntology(e.getOntologyDocumentIRI());
        } catch (OWLOntologyCreationException e) {
            throw new IllegalArgumentException(e);
        }
        return OWLUtils.guessOntologyIdentifier(o).toString();
    }

    @Override
    public String loadInStore(Object ontology, String preferredKey, boolean force) {
        throw new UnsupportedOperationException("Not implemented for OWL API version.");
    }

    @Override
    public void setImportManagementPolicy(ImportManagementPolicy policy) {
        if (!ImportManagementPolicy.PRESERVE.equals(policy)) throw new IllegalArgumentException(
                "The OWL API implementation does not support import policies other than PRESERVE.");
    }

    @Override
    public boolean hasOntology(IRI ontologyIri) {
        return hasOntology(new OWLOntologyID(ontologyIri));
    }

}
