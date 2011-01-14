package eu.iksproject.rick.core.utils;

import java.util.Iterator;

/**
 * Filters elements of the base Iterator base on the generic type of this one
 * @author Rupert Westenthaler
 *
 * @param <T> the type of elements returned by this iterator
 */
public class TypeSaveIterator<T> extends AdaptingIterator<Object,T> implements Iterator<T> {

//    protected final Iterator<?> it;
//    protected final Class<T> type;
//    private T next;
//    private Boolean hasNext;
    /**
     * Constructs an iterator that selects only elements of the parsed iterator
     * that are assignable to the parse type
     * @param it the base iterator
     * @param type the type all elements of this Iterator need to be assignable to.
     */
    @SuppressWarnings("unchecked")
    public TypeSaveIterator(Iterator<?> it,Class<T> type){
        super((Iterator<Object>)it,new AssignableFormAdapter<T>(),type);
//        if(it == null){
//            throw new IllegalArgumentException("Parsed iterator MUST NOT be NULL!");
//        }
//        if(type == null){
//            throw new IllegalArgumentException("Parsed type MUST NOT be NULL!");
//        }
//        this.it = it;
//        this.type = type;
    }
//    @Override
//    public final void remove() {
//        /*
//         * TODO: Any Iterator that filters elements of the underlying Iterator
//         * needs to call Iterator#next() in the underlying Iterator to get the
//         * next element that confirms with the filter.
//         * However the Iterator#remove() is defined as removing the last element
//         * to be returned by Iterator#next(). Therefore calling hasNext would
//         * change the element to be removed by this method.
//         * Currently I do not know a way around that but I would also like to
//         * keep the remove functionality for Iterator that filter elements of an
//         * underlying Iterator. To prevent unpredictable behaviour in such cases 
//         * I throw an IllegalStateException in such cases.
//         * This decision assumes, that in most usage scenarios hasNext will not
//         * likely be called before calling remove and even in such cases
//         * it will be most likely be possible to refactor the code to confirm
//         * with this restriction.
//         * I hope this will help developers that encounter this exception to
//         * modify there code!
//         * If someone has a better Idea how to solve this please let me know!
//         * best 
//         * Rupert Westenthaler
//         */
//        if(hasNext!= null){
//            throw new IllegalStateException("Remove can not be called after calling hasNext() because this Method needs to call next() on the underlying Interator and therefore would change the element to be removed :(");
//        }
//        it.remove();
//    }
//
//    @Override
//    public final T next() {
//        hasNext(); //call hasNext (to init next Element if not already done)
//        if(!hasNext){
//            throw new NoSuchElementException();
//        } else {
//            T current = next;
//            next = null;
//            hasNext = null;
//            return current;
//        }
//    }
//
//    @Override
//    public final boolean hasNext() {
//        if(hasNext == null){ // only once even with multiple calls
//            next = prepareNext();
//            hasNext = next != null;
//        }
//        return hasNext;
//    }
//    @SuppressWarnings("unchecked")
//    protected T prepareNext(){
//        Object check;
//        while(it.hasNext()){
//            check = it.next();
//            if(type.isAssignableFrom(check.getClass())){
//                return (T)check;
//            }
//        }
//        return null;
//    }
    /**
     * Adapter implementation that uses {@link Class#isAssignableFrom(Class)}
     * to check whether a value can be casted to the requested type
     */
    private static class AssignableFormAdapter<T> implements Adapter<Object,T> {

        @SuppressWarnings("unchecked")
        @Override
        public T adapt(Object value, Class<T> type) {
            if(type.isAssignableFrom(value.getClass())){
              return (T)value;
            } else {
                return null;
            }
        }
        
    }
}

