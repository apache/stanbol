/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.entityhub.servicesapi.query;

/**
 * Abstract base class for all types of Constraints.
 * 
 * @author Rupert Westenthaler
 * 
 */
public abstract class Constraint {
    
    private static Double ONE = Double.valueOf(1.0);
    
    /**
     * The boost allows to define the relative importance of a constraint for
     * ranking of the query results. Values MUST BE <code> &gt;= 0</code>
     */
    private Double boost = ONE;
    
    /**
     * Defines the enumeration of available Constraints. TODO Maybe we need a more "extensible" way to define
     * different constraints in future
     * 
     * @author Rupert Westenthaler
     * 
     */
    public enum ConstraintType {
        // NOTE (2010-Nov-09,rw) Because a reference constraint is now a special
        // kind of
        // a value constraint.
        // /**
        // * Constraints a field to have a specific reference
        // */
        // reference,
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
        text,
        /**
         * Constraints a field to have a lexical value along with statistics to be able to compute a
         * similarity metric (e.g. using the MoreLikeThis Solr handler)
         */
        similarity
    }

    private final ConstraintType type;

    protected Constraint(ConstraintType type) {
        if (type == null) {
            throw new IllegalArgumentException("The ConstraintType MUST NOT be NULL");
        }
        this.type = type;
    }

    /**
     * Getter for the type of the constraint.
     * 
     * @return The type of the constraint
     */
    public final ConstraintType getType() {
        return type;
    }

    /**
     * The boost
     * @return the boost or <code>null</code> if not set
     */
    public final Double getBoost() {
        return boost;
    }

    /**
     * Setter for the boost.
     * @param boost the boost or <code>null</code> to unset the boost
     * @throws IllegalArgumentException it the boost is lower than zero
     */
    public final void setBoost(Double boost) {
        if(boost == null){
            this.boost = null;
        } else if(boost <= 0f){
            throw new IllegalArgumentException("The parsed Boost '{}' MUST NOT be <= zero!");
        } else {
            this.boost = boost;
        }
    }

    @Override
    public String toString() {
        return String.format("Constraint [type :: %s][class :: %s]", type, getClass().toString());
    }

}
