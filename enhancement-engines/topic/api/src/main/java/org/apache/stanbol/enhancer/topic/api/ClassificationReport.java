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
package org.apache.stanbol.enhancer.topic.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data transfer object to report estimated classification performance of a classifier.
 * 
 * <p>
 * Report scores to evaluate the quality of a model on a labeled evaluation dataset (that should not have been
 * used when training the model).
 * </p>
 * 
 * <p>
 * See: http://en.wikipedia.org/wiki/Precision_and_recall
 * </p>
 * 
 * <p>
 * Precision, Recall are F1-score and preferred over a simple rate of good classification so as to account for
 * potentially imbalanced evaluation set (e.g. when the number of negative examples is much larger than the
 * number of positive examples).
 * </p>
 */
public class ClassificationReport {

    /**
     * Number of true positives divided by the sum of true positives and false positives.
     */
    public final float precision;

    /**
     * Number of true positives divided by the sum of true positives and false negatives.
     */
    public final float recall;

    /**
     * Harmonic mean of the precision and recall that balance the importance of false positive and false
     * negatives equally.
     */
    public final float f1;

    /**
     * Total number of positive examples used by the evaluation procedure.
     */
    public final int positiveSupport;

    /**
     * Total number of negative examples used by the evaluation procedure.
     */
    public final int negativeSupport;

    public final boolean uptodate;

    public final Date evaluationDate;

    public final List<String> falsePositiveExampleIds = new ArrayList<String>();

    public final List<String> falseNegativeExampleIds = new ArrayList<String>();

    public ClassificationReport(float precision,
                                float recall,
                                int positiveSupport,
                                int negativeSupport,
                                boolean uptodate,
                                Date evaluationDate) {
        this.precision = precision;
        this.recall = recall;
        if (precision != 0 || recall != 0) {
            this.f1 = 2 * precision * recall / (precision + recall);
        } else {
            this.f1 = 0;
        }
        this.positiveSupport = positiveSupport;
        this.negativeSupport = negativeSupport;
        this.uptodate = uptodate;
        this.evaluationDate = evaluationDate;
    }

    @Override
    public String toString() {
        return String.format("ClassificationReport: precision=%f, recall=%f, f1=%f", precision, recall, f1);
    }

}
