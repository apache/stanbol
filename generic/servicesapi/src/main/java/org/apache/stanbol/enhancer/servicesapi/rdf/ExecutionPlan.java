package org.apache.stanbol.enhancer.servicesapi.rdf;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * Defines the {@link UriRef}s for all classes and properties defined by the
 * Stanbol Enhancer Execution Plan ontology.
 *
 */
public final class ExecutionPlan {

    private ExecutionPlan(){/* No instances of Utility classes*/ }
    /**
     * The Class ep:ExecutionPlan
     */
    public static final UriRef EXECUTION_PLAN = new UriRef(NamespaceEnum.ep+"ExecutionPlan");
    /**
     * The property ep:chain linking an {@link #EXECUTION_PLAN} to the name
     * of the chain this plan is defined for
     */
    public static final UriRef CHAIN = new UriRef(NamespaceEnum.ep+"chain");
    /**
     * the property ep:hasExecutionNode linking an {@link #EXECUTION_PLAN} with
     * all its {@link #EXECUTION_NODE}s
     */
    public static final UriRef HAS_EXECUTION_NODE = new UriRef(NamespaceEnum.ep+"hasExecutionNode");
    /**
     * The Class ep:ExecutionMode
     */
    public static final UriRef EXECUTION_NODE = new UriRef(NamespaceEnum.ep+"ExecutionNode");
    /**
     * The property ep:engine linking an {@link #EXECUTION_NODE} with the name of 
     * the {@link EnhancementEngine} to be executed.
     */
    public static final UriRef ENGINE = new UriRef(NamespaceEnum.ep+"engine");
    /**
     * The property ep:dependsOn defining the list of other {@link #EXECUTION_NODE}s
     * this one depends on
     */
    public static final UriRef DEPENDS_ON = new UriRef(NamespaceEnum.ep+"dependsOn");
    /**
     * The property ep:optional that can be used to define that the execution of
     * an {@link #EXECUTION_NODE} is optional. The default is <code>false</code>.
     */
    public static final UriRef OPTIONAL = new UriRef(NamespaceEnum.ep+"optional");
 
}
