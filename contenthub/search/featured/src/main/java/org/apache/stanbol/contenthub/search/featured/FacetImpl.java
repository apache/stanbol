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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.stanbol.contenthub.servicesapi.index.search.featured.Constraint;
import org.apache.stanbol.contenthub.servicesapi.index.search.featured.Facet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetImpl implements Facet {
    private static final Logger log = LoggerFactory.getLogger(FacetImpl.class);

    private Set<Constraint> constraints;

    private List<PlainLiteral> labels;

    /**
     * Creates a {@link Facet} with given <code>constraints</code> and <code>labels</code>. The first item of
     * label list is considered as the default label of the this facet. The default label of a facet is
     * obtained by <code>getLabel(null)</code>.It is important, because any facets are considered as equal if
     * their default labels are the same.
     * 
     * @param constraints
     *            all possible {@link Constraint} corresponding to the facet values
     * @param labels
     *            a list of labels representing this facet. First of the labels passed in this list is
     *            considered as the default value of this facet
     */
    public FacetImpl(List<PlainLiteral> labels) {
        if (labels == null || labels.isEmpty()) {
            throw new IllegalArgumentException("Label list must include at least one item");
        }
        this.labels = labels;
        this.constraints = new HashSet<Constraint>();
    }

    @Override
    public Set<Constraint> getConstraints() {
        return this.constraints;
    }

    @Override
    public String getLabel(Locale locale) {
        if (locale == null) {
            return labels.get(0).getLexicalForm();
        } else {
            for (PlainLiteral pl : labels) {
                if (pl.getLanguage().toString().equals(locale.getLanguage())) {
                    return pl.getLexicalForm();
                }
            }
            log.warn("There is no label for specified language: {}. Returnin default label ",
                locale.getLanguage());
            return labels.get(0).getLexicalForm();
        }
    }

    @Override
    public int hashCode() {
        return labels.get(0).getLexicalForm().hashCode();
    }

    /**
     * If the default labels of two {@link Facet}s are equal, they are considered as equal. Default label of a
     * facet is obtained by passing a <code>null</code> parameter to {@link #getLabel(Locale)} method.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Facet) {
            return this.getLabel(null) == ((Facet) obj).getLabel(null);
        } else {
            return false;
        }
    }
}
