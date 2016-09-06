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

package org.apache.stanbol.reasoners.web.input.provider.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputProvider;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportEvent;
import org.semanticweb.owlapi.model.MissingImportListener;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class ByteArrayInputProvider implements ReasoningServiceInputProvider {
    private final Logger log = LoggerFactory.getLogger(ByteArrayInputProvider.class);
    private byte[] bytes;
    
    public ByteArrayInputProvider(byte[] bytes) {
        this.bytes = bytes;
    }
    
    @Override
    public <T> Iterator<T> getInput(Class<T> type) throws IOException {

        if (type.isAssignableFrom(OWLAxiom.class)) {
            // We add additional axioms
               OWLOntology fromUrl;
               try {
                   fromUrl = createOWLOntologyManager().loadOntologyFromOntologyDocument(new ByteArrayInputStream(bytes));
               } catch (OWLOntologyCreationException e) {
                   throw new IOException(e);
               }
               Set<OWLOntology> all = fromUrl.getImportsClosure();
               List<OWLAxiom> axiomList = new ArrayList<OWLAxiom>();
               for(OWLOntology o : all){
                  axiomList.addAll(o.getAxioms());
               }
               final Iterator<OWLAxiom> iterator = axiomList.iterator();
               return new Iterator<T>(){

                   @Override
                   public boolean hasNext() {
                       return iterator.hasNext();
                   }

                   @SuppressWarnings("unchecked")
                   @Override
                   public T next() { 
                       return (T) iterator.next();
                   }

                   @Override
                   public void remove() {
                       // This iterator is read-only
                       throw new UnsupportedOperationException("Cannot remove statements from the iterator");
                   }
                   
               };
           } else if (type.isAssignableFrom(Statement.class)) {
               final OntModel input = ModelFactory.createOntologyModel();
               synchronized (bytes) {
                     // XXX 
                     // Not sure this would always work. What if we have an RDF/XML relying on an implicit base?
                     input.read(new ByteArrayInputStream(bytes), "");
               }
               final StmtIterator iterator = input.listStatements();
               return new Iterator<T>(){
                   
                   @Override
                   public boolean hasNext() {
                       return iterator.hasNext();
                   }

                   @SuppressWarnings("unchecked")
                   @Override
                   public T next() {
                       return (T) iterator.next();
                   }

                   @Override
                   public void remove() {
                       // This iterator is read-only
                       throw new UnsupportedOperationException("Cannot remove statements from the iterator");
                   }
               };
           } else {
               throw new UnsupportedOperationException("This provider does not adapt to the given type");
           }
    }

    @Override
    public <T> boolean adaptTo(Class<T> type) {
        if (type.isAssignableFrom(OWLAxiom.class) || type.isAssignableFrom(Statement.class)) return true;
        return false;
    }
    
    @SuppressWarnings("deprecation")
    private OWLOntologyManager createOWLOntologyManager() {
        // We isolate here the creation of the temporary manager
        // TODO How to behave when resolving owl:imports?
        // We should set the manager to use a service to lookup for ontologies,
        // instead of trying on the web
        // directly/only
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
       
        // FIXME Which is the other way of doing this?
        // Maybe -> OWLOntologyManagerProperties();
        manager.setSilentMissingImportsHandling(true);
        // Listening for missing imports
        manager.addMissingImportListener(new MissingImportListener() {
            @Override
            public void importMissing(MissingImportEvent arg0) {
                log.warn("Missing import {} ", arg0.getImportedOntologyURI());
            }
        });
        manager.addOntologyLoaderListener(new OWLOntologyLoaderListener(){

            @Override
            public void finishedLoadingOntology(LoadingFinishedEvent arg0) {
                log.info("Finished loading {} (imported: {})",arg0.getOntologyID(),arg0.isImported());
            }

            @Override
            public void startedLoadingOntology(LoadingStartedEvent arg0) {
                log.info("Started loading {} (imported: {}) ...",arg0.getOntologyID(),arg0.isImported());
                log.info(" ... from {}",arg0.getDocumentIRI().toString());
            }});
        return manager;
    }

}
