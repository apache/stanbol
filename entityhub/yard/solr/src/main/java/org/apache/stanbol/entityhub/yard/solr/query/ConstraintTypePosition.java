/**
 *
 */
package org.apache.stanbol.entityhub.yard.solr.query;

/**
 * The position of a constraint type within the constraint for an index field.<p>
 * This position consists of two parts<ol>
 * <li> the {@link PositionType} defining if general position of the constraint
 * <li> an integer that defines the ordering of constraints within one position.
 *      This is e.g. needed when encoding range constraints because both the
 *      lower and upper bound need to be encoded in the {@link PositionType#value}
 *      category, the lower bound need to be encoded in front of the upper
 *      bound.
 * </ol>
 * @author Rupert Westenthaler
 *
 */
public class ConstraintTypePosition implements Comparable<ConstraintTypePosition>{
    /**
     * The possible positions of constraints within a Index Constraint.<p>
     * The ordinal number of the elements is used to sort the constraints in the
     * {@link EncodedConstraintParts}. So ensure, that the ordering in this
     * enumeration corresponds with the ordering in a constraint within the
     * index
     * @author Rupert Westenthaler
     *
     */
    public static enum PositionType {
        prefix,
        field,
        suffux,
        assignment,
        value;
    }
    private PositionType type;
    private int pos;

    public ConstraintTypePosition(PositionType type) {
        this(type,0);
    }

    public ConstraintTypePosition(PositionType type,int pos) {
        if(type == null){
            throw new IllegalArgumentException("The ConstraintPosition MUST NOT be NULL!");
        }
        this.type = type;
        this.pos = pos;
    }

    @Override
    public int compareTo(ConstraintTypePosition other) {
        return type == other.type?pos-other.pos:type.ordinal()-other.type.ordinal();
    }
    @Override
    public int hashCode() {
        return type.hashCode()+pos;
    }
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof ConstraintTypePosition
            && ((ConstraintTypePosition)obj).type == type && ((ConstraintTypePosition)obj).pos == pos;
    }
    @Override
    public String toString() {
        return String.format("constraintPosition %s,%d", type,pos);
    }
}
