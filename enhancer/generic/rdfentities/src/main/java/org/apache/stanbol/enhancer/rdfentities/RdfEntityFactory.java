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
package org.apache.stanbol.enhancer.rdfentities;

import java.util.Collection;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.stanbol.enhancer.rdfentities.impl.SimpleRdfEntityFactory;


/**
 * A Factory that creates proxies over rdf nodes - so called RDF facades.
 *
 * @author Rupert Westenthaler
 */
public abstract class RdfEntityFactory {

    /**
     * Creates a new factory for the parsed {@link Graph} instance.
     *
     * @param graph the graph used by the proxies created by this factory to
     * read/write there data
     * @return the created factory
     */
    public static RdfEntityFactory createInstance(Graph graph){
        return new SimpleRdfEntityFactory(graph);
    }

    /**
     * Getter for a proxy for the parsed rdf node that implements all the parsed
     * Interfaces. The interface parsed as type must extend {@link RdfEntity}.
     * Additional interfaces must not extend this interface.
     * <p>
     * Interfaces parsed as parameter:
     * <ul>
     * <li> SHOULD have an {@link Rdf} annotation. If that is the case, than the
     * according rdf:type statements are checks/added when the proxy is created
     * <li> all methods of the parsed interfaces MUST HAVE {@link Rdf}
     * annotations. Calling methods with missing annotations causes an
     * {@link IllegalStateException} at runtime
     * <li> all methods of the parsed interface MUST HAVE a return type or a
     * single parameter (e.g. void setSomething(String value) or String
     * getSomething). Methods with a parameter do set the parsed data. Methods
     * with a return type do read the data.
     * </ul>
     *
     * Proxies returned by this Factory:
     * <ul>
     * <li> MUST NOT have an internal state. They need to represent a view over
     * the current data within the {@link Graph} instance. Direct changes to
     * the graph need to be reflected in calls to proxies.
     * <li> Implementations need to support {@link Collection} as parameter.
     * Collections need to represent a live view over the triples within the
     * {@link Graph}. However iterators may throw a
     * {@link ConcurrentModificationException} if the graph changes while using
     * the iterator.
     * </ul>
     *
     * @param <T> The interface implemented by the returned proxy
     * @param rdfNode the rdfNode represented by the proxy (created if not
     * present in the ImmutableGraph)
     * @param type The interface for the proxy. Needs to extend {@link RdfEntity}
     * @param additionalInterfaces Additional interfaces the proxy needs to
     * implement.
     *
     * @return A proxy representing the parsed rdf node and implementing all the
     * parsed interfaces
     * @throws IllegalArgumentException if the node is <code>null</code> or the
     * parsed interfaces do not fulfil the requirements as stated.
     * @throws NullPointerException if the parameter type, additionalInterfaces
     * or any entry of additionalInterfaces is <code>null</code>.
     */
    public abstract <T extends RdfEntity> T getProxy(BlankNodeOrIRI rdfNode,
            Class<T> type, Class<?>... additionalInterfaces) throws IllegalArgumentException;

}
