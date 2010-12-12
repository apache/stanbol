package eu.iksproject.rick.model.clerezza.impl;

import java.util.Iterator;

import org.apache.clerezza.rdf.core.UriRef;

import eu.iksproject.rick.core.utils.AdaptingIterator.Adapter;
import eu.iksproject.rick.model.clerezza.RdfValueFactory;
import eu.iksproject.rick.servicesapi.model.Reference;
/**
 * TODO: Change implementation to {@link Adapter}!
 * @author Rupert Westenthaler
 *
 */
public class ReferenceIterator implements Iterator<Reference> {

    private final RdfValueFactory valueFactory = RdfValueFactory.getInstance();
    private final Iterator<UriRef> it;

    public ReferenceIterator(Iterator<UriRef> it){
        if(it == null){
            throw new IllegalArgumentException("The parent Iterator<UriRef> MUST NOT be NULL");
        }
        this.it = it;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public Reference next() {
        UriRef next = it.next();
        return next!=null?valueFactory.createReference(next):null;
    }

    @Override
    public void remove() {
        it.remove();
    }

}
