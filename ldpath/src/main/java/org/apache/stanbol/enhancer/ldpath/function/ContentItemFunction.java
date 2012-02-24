package org.apache.stanbol.enhancer.ldpath.function;

import java.util.Collection;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.stanbol.enhancer.ldpath.backend.ContentItemBackend;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.api.functions.SelectorFunction;

/**
 * This class checks if the {@link RDFBackend} parsed to 
 * {@link #apply(ContentItemBackend, Collection...) apply} is an instance of
 * {@link ContentItemBackend}. It also implements the 
 * {@link #getPathExpression(RDFBackend)} method by returning the name parsed
 * in the constructor.
 * 
 * @author Rupert Westenthaler
 *
 */
public abstract class ContentItemFunction implements SelectorFunction<Resource> {
    
    private final String name;

    protected ContentItemFunction(String name){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL nor empty!");
        }
        this.name = name;
    }
    
    public final Collection<Resource> apply(RDFBackend<Resource> backend, Collection<Resource>... args) throws IllegalArgumentException {
        if(backend instanceof ContentItemBackend){
            return apply((ContentItemBackend)backend, args);
        } else {
            throw new IllegalArgumentException("This ContentFunction can only be " +
                    "used in combination with an RDFBackend of type '"+
                    ContentItemBackend.class.getSimpleName()+"' (parsed Backend: "+
                    backend.getClass()+")!");
        }
    };

    public abstract Collection<Resource> apply(ContentItemBackend backend,Collection<Resource>... args);
    
    @Override
    public String getPathExpression(RDFBackend<Resource> backend) {
        return name;
    }
}
