package eu.iksproject.rick.core.utils;

import java.util.Iterator;

/**
 * Filters elements of the base Iterator base on the generic type of this one
 * @author Rupert Westenthaler
 *
 * @param <T> the type of elements returned by this iterator
 */
public class TypeSaveIterator<T> implements Iterator<T> {

    protected final Iterator<?> it;
    protected final Class<T> type;
    private T next;
    /**
     * Constructs an iterator that selects only elements of the parsed iterator
     * that are assignable to the parse type
     * @param it the base iterator
     * @param type the type all elements of this Iterator need to be assignable to.
     */
    public TypeSaveIterator(Iterator<?> it,Class<T> type){
        if(it == null){
            throw new IllegalArgumentException("Parsed iterator MUST NOT be NULL!");
        }
        if(type == null){
            throw new IllegalArgumentException("Parsed type MUST NOT be NULL!");
        }
        this.it = it;
        this.type = type;
        //init next ...
        next = prepareNext();
    }
    @Override
    public final void remove() {
        it.remove();

    }

    @Override
    public final T next() {
        T current = next;
        next = prepareNext();
        return current;
    }

    @Override
    public final boolean hasNext() {
        return next != null;
    }
    @SuppressWarnings("unchecked")
    protected T prepareNext(){
        Object check;
        while(it.hasNext()){
            check = it.next();
            if(type.isAssignableFrom(check.getClass())){
                return (T)check;
            }
        }
        return null;
    }
}
