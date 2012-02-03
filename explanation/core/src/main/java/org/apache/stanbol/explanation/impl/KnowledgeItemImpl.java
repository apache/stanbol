package org.apache.stanbol.explanation.impl;

import org.apache.stanbol.explanation.api.KnowledgeItem;
import org.apache.stanbol.explanation.util.SimpleOWLUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;

public class KnowledgeItemImpl implements KnowledgeItem {

    private OWLIndividual entity;

    public KnowledgeItemImpl(IRI entityIri) {
        this.entity = SimpleOWLUtils.getIndividual(entityIri);
    }

    @Override
    public OWLIndividual getItem() {
        return entity;
    }

}
