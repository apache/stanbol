package org.apache.stanbol.entityhub.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Utils {
    /**
     * Converts an Iterator to a Collection by iterating over all elements and
     * adding them to a List.
     * @param <T>
     * @param it the iterator
     * @return the collection containing all elements of the iterator
     */
    public static <T> Collection<T> asCollection(Iterator<T> it){
        Collection<T> c = new ArrayList<T>();
        while(it.hasNext()){
            c.add(it.next());
        }
        return c;
    };
}
