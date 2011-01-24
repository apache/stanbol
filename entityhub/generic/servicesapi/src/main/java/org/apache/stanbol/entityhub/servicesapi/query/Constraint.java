package org.apache.stanbol.entityhub.servicesapi.query;


/**
 * Abstract base class for all types of Constraints.
 * @author Rupert Westenthaler
 *
 */
public abstract class Constraint {
    /**
     * Defines the enumeration of available Constraints.
     * TODO Maybe we need a more "extensible" way to define different constraints
     * in future
     * @author Rupert Westenthaler
     *
     */
    public static enum ConstraintType{
// NOTE (2010-Nov-09,rw) Because a reference constraint is now a special kind of
//                       a value constraint.
//        /**
//         * Constraints a field to have a specific reference
//         */
//        reference,
        /**
         * Constraints the value and possible the dataType
         */
        value,
        /**
         * Constraints a field to have a value within a range
         */
        range,
        /**
         * Constraints a field to have a lexical value
         */
        text
        //TODO: value Type for checking non lexical values
    }
    private final ConstraintType type;
    protected Constraint(ConstraintType type){
        if(type == null){
            throw new IllegalArgumentException("The ConstraintType MUST NOT be NULL");
        }
        this.type = type;
    }
    /**
     * Getter for the type of the constraint.
     * @return The type of the constraint
     */
    public final ConstraintType getType(){
        return type;
    }

}
