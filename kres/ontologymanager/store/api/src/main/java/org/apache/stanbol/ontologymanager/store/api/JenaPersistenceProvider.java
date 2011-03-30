package org.apache.stanbol.ontologymanager.store.api;

import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;

public interface JenaPersistenceProvider {

    public abstract boolean clear();

    public abstract List<String> listModels();

    public abstract Model createModel(String ontologyURI);

    public abstract boolean hasModel(String ontologyURI);

    public abstract Model getModel(String ontologyURI);

    public abstract void removeModel(String ontologyURI);

    public abstract boolean commit(Model model);

}
