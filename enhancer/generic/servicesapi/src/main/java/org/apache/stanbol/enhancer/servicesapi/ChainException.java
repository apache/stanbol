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
package org.apache.stanbol.enhancer.servicesapi;

//import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getDependend;
//import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.getEngine;
//import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.isOptional;
//
//import org.apache.clerezza.commons.rdf.ImmutableGraph;
//import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;

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
    
//Removed - unused
//    /**
//     * Creates a chain exception for the parsed node within the parsed executionPlan
//     * @param executionPlan
//     * @param node
//     * @param message
//     * @param cause
//     */
//    public ChainException(ImmutableGraph executionPlan, BlankNodeOrIRI node, String message, Throwable cause){
//        super(String.format("Unable to execute node {} (engine: {} | optional : {}" +
//        		" | dependsOn : {}) because of: {}",
//            node,getEngine(executionPlan, node),
//            isOptional(executionPlan, node), getDependend(executionPlan, node),
//            message == null || message.isEmpty() ? "<unknown>": message),cause);
//    }
}
