package org.apache.stanbol.enhancer.engine.topic;

import java.util.List;

import org.apache.stanbol.enhancer.topic.ClassifierException;
import org.apache.stanbol.enhancer.topic.TrainingSetException;

public interface BatchProcessor<T> {

    int process(List<T> batch) throws ClassifierException, TrainingSetException;

}
