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
package org.apache.stanbol.entityhub.yard.solr.query;

/**
 * The position of a constraint type within the constraint for an index field.
 * <p>
 * This position consists of two parts
 * <ol>
 * <li>the {@link PositionType} defining if general position of the constraint
 * <li>an integer that defines the ordering of constraints within one position. This is e.g. needed when
 * encoding range constraints because both the lower and upper bound need to be encoded in the
 * {@link PositionType#value} category, the lower bound need to be encoded in front of the upper bound.
 * </ol>
 * 
 * @author Rupert Westenthaler
 * 
 */
public class ConstraintTypePosition implements Comparable<ConstraintTypePosition> {
    /**
     * The possible positions of constraints within a Index Constraint.
     * <p>
     * The ordinal number of the elements is used to sort the constraints in the
     * {@link EncodedConstraintParts}. So ensure, that the ordering in this enumeration corresponds with the
     * ordering in a constraint within the index
     * 
     * @author Rupert Westenthaler
     * 
     */
    public enum PositionType {
        prefix,
        field,
        suffux,
        assignment,
        value
    }

    private PositionType type;
    private int pos;

    public ConstraintTypePosition(PositionType type) {
        this(type, 0);
    }

    public ConstraintTypePosition(PositionType type, int pos) {
        if (type == null) {
            throw new IllegalArgumentException("The ConstraintPosition MUST NOT be NULL!");
        }
        this.type = type;
        this.pos = pos;
    }

    @Override
    public int compareTo(ConstraintTypePosition other) {
        return type == other.type ? pos - other.pos : type.ordinal() - other.type.ordinal();
    }

    @Override
    public int hashCode() {
        return type.hashCode() + pos;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ConstraintTypePosition && ((ConstraintTypePosition) obj).type == type
               && ((ConstraintTypePosition) obj).pos == pos;
    }
    public String getPos(){
        return type.ordinal()+"."+pos;
    }

    @Override
    public String toString() {
        return type.name()+'.'+ pos;
    }
}
