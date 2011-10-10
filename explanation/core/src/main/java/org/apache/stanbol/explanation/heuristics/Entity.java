package org.apache.stanbol.explanation.heuristics;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;

public interface Entity {

    void addID(Identifier id);
    
    Set<Identifier> getIDs();

    OWLEntity getOWLEntity();

    Set<Entity> getRelatedEntities(IRI propertyID);

}
