package eu.iksproject.rick.core.utils;

import java.util.Iterator;

/**
 * Implementation of an Iterator that iterates over a filtered set of elements
 * of its parent Iterator.<p>
 * Note that {@link Iterator#remove()} only works as long as 
 * {@link Iterator#hasNext()} was not called to determine if there are
 * further elements. The reason for that is, that in order to filter elements
 * of the parent iterator {@link Iterator#next()} has to be called to check
 * weather any further element is valid against the used {@link Filter}.
 * This call to {@link Iterator#next()} causes the parent Iterator to switch
 * to the next element, meaning that after that the <code>remove()</code>
 * method would delete a different element. To avoid that this Iterator
 * throws an {@link IllegalStateException} in such cases. If the parent
 * Iterator does not support <code>remove()</code> at all an
 * {@link UnsupportedOperationException} is thrown. <p>
 * This implementation is based on the {@link AdaptingIterator} to avoid
 * duplication of the filtering functionality also needed if an adapter can not
 * convert a specific value of one type to an other. 
 * 
 * @author Rupert Westenthaler
 *
 * @param <T> the type of the elements
 */
public class FilteringIterator<T> extends AdaptingIterator<T,T>{
    /**
     * Interface used by the {@link FilteringIterator} to check if an element
     * of the parent Iterator should be filtered or not. If {@link #isValid(Object)}
     * returns true the element of the parent iterator is also returned by this
     * Iterator. Otherwise the element is filtered.<p>

     * @author Rupert Westenthaler
     *
     * @param <T>
     */
    public static interface Filter<T> {
        boolean isValid(T value);
    }
    public FilteringIterator(Iterator<T> iterator,Filter<T> filter,Class<T> type) {
        super(iterator,new FilterAdapter<T>(filter),type);
    }
    
    private static class FilterAdapter<A> implements Adapter<A,A> {

        private Filter<A> filter;
        public FilterAdapter(Filter<A> filter) {
            if(filter == null){
                throw new NullPointerException("The parsed Filter MUST NOT be NULL!");
            }
            this.filter = filter;
        }
        @Override
        public A adapt(A value, Class<A> type) {
            if(filter.isValid(value)){
                return value;
            } else {
                return null;
            }
        }
        
    }
}
