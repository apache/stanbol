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
