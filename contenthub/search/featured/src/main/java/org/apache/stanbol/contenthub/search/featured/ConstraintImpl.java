/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.contenthub.search.featured;

import org.apache.stanbol.contenthub.servicesapi.search.featured.Constraint;
import org.apache.stanbol.contenthub.servicesapi.search.featured.Facet;

public class ConstraintImpl implements Constraint {

    private String value;
    private Facet facet;

    public ConstraintImpl(String value, Facet facet) {
        if (value == null || value.equals("")) {
            throw new IllegalArgumentException("A non-empty value must be specified");
        }
        if (facet == null) {
            throw new IllegalArgumentException("A non-null facet must be specified");
        }
        this.value = value;
        this.facet = facet;
        facet.getConstraints().add(this);
    }

    @Override
    public Facet getFacet() {
        return this.facet;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return value.hashCode() + facet.getLabel(null).hashCode();
    }

    /**
     * If the value of two {@link Constraint}s and default labels of their associated facets are equal,
     * constraints are treated as equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Constraint) {
            Constraint c = (Constraint) obj;
            if (c.getValue().equals(this.getValue()) && c.getFacet().equals(this.getFacet())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
