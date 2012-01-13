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
package org.apache.stanbol.enhancer.topic;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.enhancer.servicesapi.EngineException;

/**
 * Service interface for suggesting hierarchical topics from a specific scheme (a.k.a. taxonomy, thesaurus or
 * topics hierarchy) from the text content of a document or part of a document.
 */
public interface TopicClassifier {

    /**
     * @return the short id identifying this classifier / scheme: can be used as URL path component to publish
     *         the service.
     */
    public String getSchemeId();

    /**
     * @return list of language codes for text that can be automatically classified by the service.
     */
    public List<String> getAcceptedLanguages();

    /**
     * Perform automated text categorization based on statistical occurrences of words in the given text.
     * 
     * @param text
     *            the text content to analyze
     * @return the most likely topics related to the text
     * @throws EngineException
     */
    List<TopicSuggestion> suggestTopics(String text) throws ClassifierException;

    /**
     * @return the set of ids of topics directly broader than
     * @param id
     */
    Set<String> getNarrowerTopics(String broadTopicId) throws ClassifierException;

    /**
     * @return the set of ids of topics directly narrower than
     * @param id
     */
    Set<String> getBroaderTopics(String id) throws ClassifierException;

    /**
     * @return the set of ids of topics without broader topics.
     */
    Set<String> getTopicRoots() throws ClassifierException;

    /**
     * @return true if the classifier model can be updated with the {@code addTopic} / {@code removeTopic} /
     *         {@code updateModel} / methods.
     */
    boolean isUpdatable();

    /**
     * Register a topic and set it's ancestors in the taxonomy. Warning: re-adding an already existing topic
     * can delete the underlying statistical model. Calling {@code updateModel} is necessary to rebuild the
     * statistical model based on the hierarchical structure of the topics and the registered training set.
     * 
     * @param id
     *            the new topic id
     * @param broaderTopics
     *            list of directly broader topics in the thesaurus
     */
    void addTopic(String id, Collection<String> broaderTopics) throws ClassifierException;

    /**
     * Remove a topic from the thesaurus. WARNING: it is the caller responsibility to recursively remove or
     * update any narrower topic that might hold a reference on this topic. Once the tree is updated,
     * {@code updateModel} should be called to re-align the statistical model to match the new hierarchy by
     * drawing examples from the dataset.
     * 
     * @param id
     *            if of the topic to remove from the model
     */
    void removeTopic(String id) throws ClassifierException;

    /**
     * Register a training set to use to build the statistical model of the classifier.
     */
    void setTrainingSet(TrainingSet trainingSet);

    /**
     * Update (incrementally or from scratch) the statistical model of the classifier. Note: depending on the
     * size of the dataset and the number of topics to update, this process can take a long time and should
     * probably be wrapped in a dedicated thread if called by a the user interface layer.
     * 
     * @return the number of updated topics
     */
    int updateModel(boolean incremental) throws TrainingSetException, ClassifierException;

    /**
     * Perform k-fold cross validation of the model to compute estimates of the precision, recall and f1
     * score.
     */
    public void updatePerformanceEstimates(boolean incremental) throws ClassifierException,
                                                               TrainingSetException;

    /**
     * Tell the classifier which slice of data to keep aside while training for model evaluation using k-folds
     * cross validation.
     * 
     * http://en.wikipedia.org/wiki/Cross-validation_%28statistics%29#K-fold_cross-validation
     * 
     * @param foldIndex
     *            the fold id used as a training set for this classifier instance.
     * @param foldCount
     *            the number of folds used in the cross validation process (typically 3 or 5). Set to 0 to
     *            disable cross validation for this classifier.
     */
    void setCrossValidationInfo(int foldIndex, int foldCount);

    /**
     * Clone the classifier to get a new independent instance with an empty embedded model to be trained on a
     * subsample of the dataset in a cross validation setting for model evaluation.
     */
    TopicClassifier cloneWithEmdeddedModel() throws ClassifierException;

    /**
     * Free the backing resources of the model (e.g. indices persisted on the harddrive or a DB) once the
     * cross validation process is completed.
     */
    void destroyModel() throws ClassifierException;

    /**
     * Get a classification report with various accuracy metrics (precision, recall and f1-score) along with
     * the example ids of some mistakes (false positives or false negatives).
     */
    ClassificationReport getPerformanceEstimates(String topic) throws ClassifierException;
}
