package org.apache.stanbol.entityhub.model.sesame;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.openrdf.model.BNode;
import org.openrdf.model.Value;

/**
 * Internally used to handle BNodes. Externally mapped to {@link Reference}.
 * <p>
 * <b>NOTE:</b> this does not aim to fully support BNodes
 * @author Rupert Westenthaler
 *
 */
public class RdfBNode implements Reference, RdfWrapper {

    private BNode node;

    protected RdfBNode(BNode node) {
        this.node = node;
    }
    
    @Override
    public Value getValue() {
        return node;
    }

    @Override
    public String getReference() {
        return node.getID();
    }
    
    @Override
    public int hashCode() {
        return node.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Reference && 
                getReference().equals(((Reference)obj).getReference());
    }
    
    @Override
    public String toString() {
        return node.toString();
    }
    
}