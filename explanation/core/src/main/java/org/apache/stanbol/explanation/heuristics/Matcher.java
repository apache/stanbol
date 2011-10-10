package org.apache.stanbol.explanation.heuristics;

/**
 * Any entity that can be compared to another one using Description Logic constructs.
 * 
 * @author alessandro
 *
 */
public interface Matcher {

    /**
     * 
     * @param arg the entity to compare this one against.
     * @return true iff the two entities denote the same entity.
     */
    boolean matches(Entity arg0, Entity arg1);
    
}
