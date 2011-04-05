package org.apache.stanbol.ontologymanager.store.api;

import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;

public interface JenaPersistenceProvider {

    boolean clear();

    List<String> listModels();

    Model createModel(String ontologyURI);

    boolean hasModel(String ontologyURI);

    Model getModel(String ontologyURI);

    void removeModel(String ontologyURI);

    boolean commit(Model model);

}
