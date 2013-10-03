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
package org.apache.stanbol.enhancer.ldpath.function;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.api.functions.SelectorFunction;
import at.newmedialab.ldpath.api.selectors.NodeSelector;

/**
 * Maps a {@link NodeSelector} to a function name. This is useful to provide
 * function shortcuts for longer ld-path statements.
 * @author Rupert Westenthaler
 *
 * @param <Node>
 */
public class PathFunction<Node> implements SelectorFunction<Node> {
    
    private final String name;
    private final NodeSelector<Node> selector;

    /**
     * create a function available under fn:{name} for the parsed selector
     * @param name the name of the function MUST NOT be <code>null</code> nor
     * empty
     * @param selector the selector MUST NOT be <code>null</code>
     * @throws IllegalArgumentException if the parsed name is <code>null</code>
     * or empty; if the parsed {@link NodeSelector} is <code>null</code>.
     */
    public PathFunction(String name, NodeSelector<Node> selector){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed function name MUST NOT be NULL nor empty!");
        }
        this.name = name;
        if(selector == null){
            throw new IllegalArgumentException("The parsed NodeSelector MUST NOT be NULL!");
        }
        this.selector = selector;
    }

    @Override
    public Collection<Node> apply(RDFBackend<Node> backend, Collection<Node>... args) throws IllegalArgumentException {
        if(args == null || args.length < 1 || args[0] == null || args[0].isEmpty()){
            throw new IllegalArgumentException("The 'fn:"+name+"' function " +
                    "requires at least a single none empty parameter (the context). Use 'fn:" +
                    name+"(.)' to execute it on the path context!");
        }
        Set<Node> selected = new HashSet<Node>();
        for(Node context : args[0]){
            selected.addAll(selector.select(backend, context));
        }
        return selected;
    }

    @Override
    public String getPathExpression(RDFBackend<Node> backend) {
        return name;
    }

}
