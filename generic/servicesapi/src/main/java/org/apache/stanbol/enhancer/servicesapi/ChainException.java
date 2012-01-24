package org.apache.stanbol.enhancer.servicesapi;

import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getDependend;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getEngine;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.isOptional;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.NonLiteral;

/**
 * BaseException thrown by {@link Chain} implementations or
 * {@link EnhancementJobManager} implementations when encountering problems 
 * while executing e Chain
 * @author Rupert Westenthaler
 *
 */
public class ChainException extends EnhancementException {

    private static final long serialVersionUID = 1L;

    public ChainException(String message) {
        super(message);
    }
    public ChainException(String message, Throwable cause) {
        super(message,cause);
    }
    /**
     * Creates a chain exception for the parsed node within the parsed executionPlan
     * @param executionPlan
     * @param node
     * @param message
     * @param cause
     */
    public ChainException(Graph executionPlan, NonLiteral node, String message, Throwable cause){
        super(String.format("Unable to execute node {} (engine: {} | optional : {}" +
        		" | dependsOn : {}) because of: {}",
            node,getEngine(executionPlan, node),
            isOptional(executionPlan, node), getDependend(executionPlan, node),
            message == null || message.isEmpty() ? "<unknown>": message),cause);
    }
}
