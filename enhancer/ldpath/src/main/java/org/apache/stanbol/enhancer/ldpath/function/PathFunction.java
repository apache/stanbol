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

import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;
import org.apache.marmotta.ldpath.api.selectors.NodeSelector;

/**
 * Maps a {@link NodeSelector} to a function name. This is useful to provide
 * function shortcuts for longer ld-path statements.
 * @author Rupert Westenthaler
 *
 * @param <Node>
 */
public class PathFunction<Node> extends SelectorFunction<Node> {
    
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
    public Collection<Node> apply(RDFBackend<Node> backend, Node context, Collection<Node>... args) throws IllegalArgumentException {
        Set<Node> selected = new HashSet<Node>();
        if(args != null && args.length > 0 && args[0] != null && !args[0].isEmpty()){
            for(Node contextParam : args[0]){
                selected.addAll(selector.select(backend, contextParam,
                    null,null)); //no path tracking support possible within functions
            }
        } else {
            selected.addAll(selector.select(backend, context,
                null,null)); //no path tracking support possible within functions
        }
        return selected;
    }
    
    @Override
    protected String getLocalName() {
        return name;
    }

    @Override
    public String getSignature() {
        return "fn:"+name+"([context])";
    }

    @Override
    public String getDescription() {
        return "Wraps a Selector as a function. The first parameter can be used "
                + "to parse context(s) to execute the wraped selector on. If no arguments "
                + "are parsed the current context is used.";
    }
    
}
