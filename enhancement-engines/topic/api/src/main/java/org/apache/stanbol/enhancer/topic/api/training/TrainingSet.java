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
package org.apache.stanbol.enhancer.topic.api.training;

import org.apache.stanbol.enhancer.topic.api.Batch;

import java.util.Date;
import java.util.List;

/**
 * Source of categorized text documents that can be used to build a the statistical model of a
 * TopicClassifier.
 */
public interface TrainingSet {

    /**
     * The short name of the training set. Can be used as the URI component to identify the training set in
     * the Web management interface or in RDF descriptions of the service.
     */
    String getName();

    /**
     * @return true if the training set can be updated using the {@code registerExample} API. If false that
     *         means that the component is a view on a remote datasource that has its own API for updates
     *         (e.g. the document repository of a CMS).
     */
    boolean isUpdatable() throws TrainingSetException;

    /**
     * Register some text content to be used as an example of document that should be positively classified as
     * topics by the model.
     * 
     * @param id
     *            Unique identifier of the example to create or override. If null, a new example with a
     *            generated id will be created.
     * @param text
     *            Text content of the example. If null the example with the matching id will be deleted.
     * 
     * @param topics
     *            The list of all the topics the example should be classified as.
     * @return the id of the registered example (can be automatically generated)
     */
    String registerExample(String exampleId, String text, List<String> topics) throws TrainingSetException;

    /**
     * Fetch examples representative of the set of topics passed as argument so as to be able to build a
     * statistical model.
     * 
     * @param topics
     *            list of admissible topics to search examples for: each example in the batch will be
     *            classified in at list one of the requested topics. This list would typically comprise a
     *            topic along with it's direct narrower descendants (and maybe level 2 descendants too).
     * @param offset
     *            marker value to fetch the next batch. Pass null to fetch the first batch.
     * @return a batch of example suitable for training a classifier model for the requested topics.
     */
    Batch<Example> getPositiveExamples(List<String> topics, Object offset) throws TrainingSetException;

    /**
     * Fetch examples representative of any document not specifically classified in one of the passed topics.
     * This can be useful to train a statistical model for a classifier of those topics to negatively weight
     * generic features (term occurrences) and limit the number of false positives in the classification. It
     * is up to the classifier model to decide to use such negative examples or not at training time.
     * 
     * @param topics
     *            list of non-admissible topics to search example for: each example in the batch must no be
     *            classified in any of the passed topics.
     * @param offset
     *            marker value to fetch the next batch. Pass null to fetch the first batch.
     * @return a batch of examples suitable for training (negative-refinement) a classifier model for the
     *         requested topics.
     */
    Batch<Example> getNegativeExamples(List<String> topics, Object offset) throws TrainingSetException;

    /**
     * Number of examples to fetch at once.
     */
    void setBatchSize(int batchSize);

    /**
     * Method to tell the classifier if topic model should be updated if there exists examples classified in
     * one of those topics that has changed.
     * 
     * @param topics
     *            topics to check
     * @param referenceDate
     *            look for changes after that date
     * @return true if one of the passed topics has changed since the last date
     */
    boolean hasChangedSince(List<String> topics, Date referenceDate) throws TrainingSetException;

    /**
     * Trigger optimization of the underlying index. 
     */
    void optimize() throws TrainingSetException;

}
