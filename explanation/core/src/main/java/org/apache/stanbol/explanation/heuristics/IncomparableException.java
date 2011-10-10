package org.apache.stanbol.explanation.heuristics;

public class IncomparableException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 7252445752553251938L;
    
    public IncomparableException(Entity first, Entity last) {
        this.first = first;
        this.last = last;
    }
    
    private Entity first, last;

    public Entity getFirst() {
        return first;
    }

    public Entity getLast() {
        return last;
    }

}
