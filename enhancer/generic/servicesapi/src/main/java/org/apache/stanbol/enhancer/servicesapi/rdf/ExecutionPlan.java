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
package org.apache.stanbol.enhancer.servicesapi.rdf;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;

/**
 * Defines the {@link IRI}s for all classes and properties defined by the
 * Stanbol Enhancer Execution Plan ontology.
 *
 */
public final class ExecutionPlan {

    private ExecutionPlan(){/* No instances of Utility classes*/ }
    /**
     * The Class ep:ExecutionPlan
     */
    public static final IRI EXECUTION_PLAN = new IRI(NamespaceEnum.ep+"ExecutionPlan");
    /**
     * The property ep:chain linking an {@link #EXECUTION_PLAN} to the name
     * of the chain this plan is defined for
     */
    public static final IRI CHAIN = new IRI(NamespaceEnum.ep+"chain");
    /**
     * the property ep:hasExecutionNode linking an {@link #EXECUTION_PLAN} with
     * all its {@link #EXECUTION_NODE}s
     */
    public static final IRI HAS_EXECUTION_NODE = new IRI(NamespaceEnum.ep+"hasExecutionNode");
    /**
     * The Class ep:ExecutionMode
     */
    public static final IRI EXECUTION_NODE = new IRI(NamespaceEnum.ep+"ExecutionNode");
    /**
     * The property ep:engine linking an {@link #EXECUTION_NODE} with the name of 
     * the {@link EnhancementEngine} to be executed.
     */
    public static final IRI ENGINE = new IRI(NamespaceEnum.ep+"engine");
    /**
     * The property ep:dependsOn defining the list of other {@link #EXECUTION_NODE}s
     * this one depends on
     */
    public static final IRI DEPENDS_ON = new IRI(NamespaceEnum.ep+"dependsOn");
    /**
     * The property ep:optional that can be used to define that the execution of
     * an {@link #EXECUTION_NODE} is optional. The default is <code>false</code>.
     */
    public static final IRI OPTIONAL = new IRI(NamespaceEnum.ep+"optional");
 
}
