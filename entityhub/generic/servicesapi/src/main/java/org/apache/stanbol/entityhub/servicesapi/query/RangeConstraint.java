package org.apache.stanbol.entityhub.servicesapi.query;



public class RangeConstraint extends Constraint {

    private final Object lowerBound;
    private final Object upperBound;
    private final boolean inclusive;

    public RangeConstraint(Object lowerBound,Object upperBound,boolean inclusive) {
        super(ConstraintType.range);
        if(lowerBound == null && upperBound == null){
            throw new IllegalArgumentException(" At least one of \"lower bound\" and \"upper bound\" MUST BE defined");
        }
        //TODO eventually check if upper and lower bound are of the same type
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.inclusive = inclusive;
    }

    /**
     * @return the lowerBound
     */
    public Object getLowerBound() {
        return lowerBound;
    }

    /**
     * @return the upperBound
     */
    public Object getUpperBound() {
        return upperBound;
    }

    /**
     * @return the inclusive
     */
    public boolean isInclusive() {
        return inclusive;
    }
    public String toString() {
        return String.format("RangeConstraint[lower=%s|upper=%s|%sclusive]",
                lowerBound!=null?lowerBound:"*",upperBound!=null?upperBound:"*",
                        inclusive?"in":"ex");
    }

}
