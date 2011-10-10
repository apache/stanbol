package org.apache.stanbol.explanation.heuristics;

public interface Comparator {

    /**
     * 
     * @param arg0
     * @param arg1
     * @return a positive integer if <tt>arg1</tt> compares greater than <tt>arg2</tt>, a negative integer if
     *         <tt>arg1</tt> compares greater than <tt>arg2</tt>, zero if they are equal.
     * 
     * @throws IncomparableException
     *             if the two entities are not comparable by this criterion.
     */
    int compare(Entity arg0, Entity arg1) throws IncomparableException;

}
