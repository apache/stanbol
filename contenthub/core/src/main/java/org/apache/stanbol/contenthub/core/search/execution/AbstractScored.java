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

import java.text.DecimalFormat;

import org.apache.stanbol.contenthub.servicesapi.search.execution.Scored;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;

/**
 * 
 * @author cihan
 * 
 */
public abstract class AbstractScored extends AbstractWeighted implements Scored {

    private static final DecimalFormat df = new DecimalFormat("#.###");

    AbstractScored(Node n, EnhGraph g, Double weight, Double score) {
        super(n, g, weight);
        checkScoreRange(score);
        if (this.hasProperty(SearchVocabulary.SCORE)) {
            updateScore(score, weight);
        } else {
            // setScoreLiteral(weight * score);
            setScoreLiteral(score);
        }
    }

    @Override
    public final double getScore() {
        return getScoreLiteral() / getWeight();
    }

    @Override
    public String getScoreString() {
        return df.format(getScore());
    }

    @Override
    public final void updateScore(Double score, Double weight) {
        checkScoreRange(score);
        double currentScore = getScoreLiteral();
        currentScore += score * weight;
        addWeight(weight);
        setScoreLiteral(currentScore);

    }

    private void checkScoreRange(Double score) {
        if (score > 1 || score < 0) {
            throw new IllegalArgumentException("Score can not be less than 0 or greater than 1.0");
        }
    }

    private double getScoreLiteral() {
        return this.getPropertyValue(SearchVocabulary.SCORE).asLiteral().getDouble();
    }

    private void setScoreLiteral(double score) {
        // First remove any assertions
        this.removeAll(SearchVocabulary.SCORE);
        // Then add current value
        this.addLiteral(SearchVocabulary.SCORE, score);
    }

}
