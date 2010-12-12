package eu.iksproject.rick.core.utils;

import java.util.Iterator;
/**
 * Uses the parsed Adapter to convert values of type T to values of type
 * A. If an instance of T can not be converted to A, than such values are
 * filtered in the Iteration.
 * @author Rupert Westenthaler
 *
 * @param <T> The type of the incoming elements
 * @param <A> The type of the elements returned by this iterator
 */
public class AdaptingIterator<T,A> implements Iterator<A> {

    /**
     * Adapts values of type T to values of type A.
     * @author westei
     *
     * @param <T>
     * @param <A>
     */
    public static interface Adapter<T,A> {
        /**
         * Converts the value of type T to a value of type A. If an instance of
         * T can not be converted to A, than <code>null</code> is returned
         * @param value the incoming value
         * @param type the target type
         * @return the converted value or <code>null</code> if the parsed value
         * is <code>null</code> or the parsed value can not be converted
         */
        A adapt(T value, Class<A> type);
    }
    protected final Adapter<T, A> adapter;
    protected final Iterator<T> it;
    protected final Class<A> type;
    private A next;
    /**
     * Constructs an instance based on an iterator of type T, an adapter and the
     * target type
     * @param it the base iterator
     * @param adapter the adapter
     * @param type the target type
     */
    public AdaptingIterator(Iterator<T> it,Adapter<T,A> adapter,Class<A> type){
        if(it == null){
            throw new IllegalArgumentException("Parsed iterator MUST NOT be NULL!");
        }
        if(adapter == null){
            throw new IllegalArgumentException("Parsed adapter MUST NOT be NULL!");
        }
        if(type == null){
            throw new IllegalArgumentException("Parsed type MUST NOT be NULL!");
        }
        this.it = it;
        this.adapter = adapter;
        this.type = type;
        //init next
        next = prepareNext();
    }
    @Override
    public final boolean hasNext() {
        return next != null;
    }

    @Override
    public final A next() {
        A current = next;
        next = prepareNext();
        return current;
    }

    @Override
    public final void remove() {
        it.remove();
    }
    protected A prepareNext(){
        T check;
        A converted;
        while(it.hasNext()){
            check = it.next();
            converted = adapter.adapt(check,type);
            if(converted != null){
                return converted;
            }
        }
        return null;
    }

}
