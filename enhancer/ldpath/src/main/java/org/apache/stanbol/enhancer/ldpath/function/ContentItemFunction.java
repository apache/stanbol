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

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.stanbol.enhancer.ldpath.backend.ContentItemBackend;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.api.functions.SelectorFunction;

/**
 * This class checks if the {@link RDFBackend} parsed to 
 * {@link #apply(ContentItemBackend, Collection...) apply} is an instance of
 * {@link ContentItemBackend}. It also implements the 
 * {@link #getPathExpression(RDFBackend)} method by returning the name parsed
 * in the constructor.
 * 
 * @author Rupert Westenthaler
 *
 */
public abstract class ContentItemFunction extends SelectorFunction<RDFTerm> {
    
    private final String name;

    protected ContentItemFunction(String name){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL nor empty!");
        }
        this.name = name;
    }
    
    public final Collection<RDFTerm> apply(RDFBackend<RDFTerm> backend, RDFTerm context, Collection<RDFTerm>... args) throws IllegalArgumentException {
        if(backend instanceof ContentItemBackend){
            return apply((ContentItemBackend)backend, context, args);
        } else {
            throw new IllegalArgumentException("This ContentFunction can only be " +
                    "used in combination with an RDFBackend of type '"+
                    ContentItemBackend.class.getSimpleName()+"' (parsed Backend: "+
                    backend.getClass()+")!");
        }
    };

    public abstract Collection<RDFTerm> apply(ContentItemBackend backend,RDFTerm context, Collection<RDFTerm>... args);
    
    @Override
    protected String getLocalName() {
        return name;
    }
}
