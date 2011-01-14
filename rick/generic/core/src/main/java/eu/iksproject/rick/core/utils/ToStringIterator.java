package eu.iksproject.rick.core.utils;

import java.util.Iterator;

/**
 * Implementation of an Iterator over {@link String} values, that uses the 
 * {@link Object#toString()} on elements of the parent Iterator for the 
 * conversion.<p>
 * This Implementation does not use {@link AdaptingIterator}s implementation, 
 * because the {@link Object#toString()} method can be used to create a string
 * representation for every object and therefore there is no need for the
 * filtering functionality provided by the {@link AdaptingIterator}.
 * 
 * @author Rupert Westenthaler
 */
public class ToStringIterator implements Iterator<String> {

    protected final Iterator<?> it;
    /**
     * Creates an string iterator over parsed parent
     * @param it the parent iterator
     * @throws NullPointerException if <code>null</code> is parsed as parent
     * iterator
     */
    public ToStringIterator(Iterator<?> it) throws NullPointerException{
        if(it == null){
            throw new NullPointerException("Parsed iterator MUST NOT be NULL!");
        }
        this.it = it;
    }
    @Override
    public final void remove() {
        it.remove();
    }

    @Override
    public final String next() {
        Object next =  it.next();
        return next != null?next.toString():null;
    }

    @Override
    public final boolean hasNext() {
        return it.hasNext();
    }
}
