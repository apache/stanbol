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

package org.apache.stanbol.contenthub.core.search.execution;

import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.impl.OntResourceImpl;

/**
 * 
 * @author cihan
 * 
 */
public class AbstractWeighted extends OntResourceImpl {

    // final OntResource res;

    AbstractWeighted(Node n, EnhGraph g, double weight) {
        super(n, g);
        // this = new OntResourceImpl(n, g);
        // If it already have a weight then do nothing (Scored instance is being recreated and weight update
        // is done in constructor of the Scored )
        if (!this.hasProperty(SearchVocabulary.WEIGHT)) {
            this.addLiteral(SearchVocabulary.WEIGHT, weight);
        }
    }

    public final double getWeight() {
        return this.getPropertyValue(SearchVocabulary.WEIGHT).asLiteral().getDouble();
    }

    protected final void addWeight(double weight) {
        checkWeightRange(weight);
        double currentWeight = getWeight() + weight;
        this.removeAll(SearchVocabulary.WEIGHT);
        this.addLiteral(SearchVocabulary.WEIGHT, currentWeight);
    }

    private void checkWeightRange(double weight) {
        if (weight > 1 || weight < 0) {
            throw new IllegalArgumentException("Weight can not be less than 0 or greater than 1.0");
        }
    }

}
