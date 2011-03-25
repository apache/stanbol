package org.apache.stanbol.ontologymanager.ontonet.impl.io;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.NoSuchStoreException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

public class InMemoryOntologyStorage extends ClerezzaOntologyStorage {

    private Map<IRI,OWLOntology> store;

    public InMemoryOntologyStorage() {
        store = new HashMap<IRI,OWLOntology>();
    }

    public void clear() {
        store.clear();
    }

    public void delete(IRI arg0) {
        store.remove(arg0);
    }

    public void deleteAll(Set<IRI> arg0) {
        for (IRI iri : arg0)
            delete(iri);
    }

    public OWLOntology getGraph(IRI arg0) throws NoSuchStoreException {
        return store.get(arg0);
    }

    public Set<IRI> listGraphs() {
        return store.keySet();
    }

    public OWLOntology load(IRI arg0) {
        return store.get(arg0);
    }

    public OWLOntology sparqlConstruct(String arg0, String arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    public void store(OWLOntology arg0) {
        try {
            store.put(arg0.getOntologyID().getOntologyIRI(), arg0);
        } catch (Exception ex) {
            store.put(arg0.getOWLOntologyManager().getOntologyDocumentIRI(arg0), arg0);
        }
    }

    public void store(OWLOntology arg0, IRI arg1) {
        store.put(arg1, arg0);
    }

}
