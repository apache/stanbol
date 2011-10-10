package org.apache.stanbol.explanation.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.explanation.heuristics.Entity;
import org.apache.stanbol.explanation.heuristics.Identifier;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;

public class BasicEntity implements Entity {

    private Set<Identifier> ids;

    private OWLEntity reference;

    public BasicEntity(OWLEntity reference) {
        ids = new HashSet<Identifier>();
        this.reference = reference;
    }

    @Override
    public Set<Identifier> getIDs() {
        return ids;
    }

    @Override
    public OWLEntity getOWLEntity() {
        return reference;
    }

    @Override
    public Set<Entity> getRelatedEntities(IRI propertyID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addID(Identifier id) {
ids.add(id);
    }

}
