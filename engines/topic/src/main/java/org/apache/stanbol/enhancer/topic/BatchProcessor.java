package org.apache.stanbol.enhancer.topic;

import java.util.List;

import org.apache.stanbol.enhancer.topic.training.TrainingSetException;

public interface BatchProcessor<T> {

    int process(List<T> batch) throws ClassifierException, TrainingSetException;

}
