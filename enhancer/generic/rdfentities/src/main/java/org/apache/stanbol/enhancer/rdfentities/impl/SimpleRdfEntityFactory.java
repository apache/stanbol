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
package org.apache.stanbol.enhancer.rdfentities.impl;

import java.lang.reflect.Proxy;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.stanbol.enhancer.rdfentities.RdfEntity;
import org.apache.stanbol.enhancer.rdfentities.RdfEntityFactory;


public class SimpleRdfEntityFactory extends RdfEntityFactory {

    private final Graph graph;
    private final LiteralFactory literalFactory;

    public SimpleRdfEntityFactory(Graph graph) {
        if (graph == null){
            throw new IllegalArgumentException("The Graph parsed as parameter MUST NOT be NULL!");
        }
        this.graph = graph;
        literalFactory = LiteralFactory.getInstance();
    }

    @SuppressWarnings("unchecked")
    public <T extends RdfEntity> T getProxy(BlankNodeOrIRI rdfNode, Class<T> type,Class<?>...additionalInterfaces) {
        Class<?>[] interfaces = new Class<?>[additionalInterfaces.length+1];
        interfaces[0] = type;
        System.arraycopy(additionalInterfaces, 0, interfaces, 1, additionalInterfaces.length);
        //Class<?> proxy = Proxy.getProxyClass(WrapperFactory.class.getClassLoader(), interfaces);
        Object instance = Proxy.newProxyInstance(
                SimpleRdfEntityFactory.class.getClassLoader(),
                interfaces,
                new RdfProxyInvocationHandler(this, rdfNode, interfaces, literalFactory));
        return (T)instance;
    }

    protected Graph getGraph() {
        return graph;
    }

}
