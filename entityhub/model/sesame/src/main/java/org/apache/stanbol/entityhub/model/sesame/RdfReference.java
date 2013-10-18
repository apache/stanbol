package org.apache.stanbol.entityhub.model.sesame;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/**
 * A {@link Reference} implementation backed by a Sesame {@link URI}
 * @author Rupert Westenthaler
 *
 */
public class RdfReference implements Reference, RdfWrapper {

    private final URI uri;


    protected RdfReference(URI uri){
        this.uri = uri;
    }
    
    @Override
    public String getReference() {
        return uri.stringValue();
    }
    /**
     * The wrapped Sesame {@link URI}
     * @return the URI
     */
    public URI getURI() {
        return uri;
    }
    @Override
    public Value getValue() {
        return uri;
    }
    
    @Override
    public int hashCode() {
        return uri.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Reference && 
                getReference().equals(((Reference)obj).getReference());
    }
    
    @Override
    public String toString() {
        return uri.toString();
    }
}