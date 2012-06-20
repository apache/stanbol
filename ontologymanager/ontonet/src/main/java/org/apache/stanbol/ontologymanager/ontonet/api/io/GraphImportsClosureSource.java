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
package org.apache.stanbol.ontologymanager.ontonet.api.io;

import java.io.InputStream;
import java.util.Set;

import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.semanticweb.owlapi.model.OWLOntology;

public class GraphImportsClosureSource extends GraphContentInputSource implements SetInputSource {

    /**
     * Creates a new graph input source by parsing <code>content</code>. Every supported format will be tried
     * until one is parsed successfully. The resulting graph is created in-memory, and its triples will have
     * to be manually added to a stored graph if necessary.
     * 
     * @param content
     *            the serialized graph content.
     */
    public GraphImportsClosureSource(InputStream content) {
        this(content, (String) null);
    }

    /**
     * Creates a new graph input source by parsing <code>content</code> assuming it has the given format. The
     * resulting graph is created in-memory, and its triples will have to be manually added to a stored graph
     * if necessary.
     * 
     * @param content
     *            the serialized graph content.
     * @param formatIdentifier
     *            the format to parse the content as.
     */
    public GraphImportsClosureSource(InputStream content, String formatIdentifier) {
        this(content, formatIdentifier, null);
    }

    /**
     * Creates a new graph input source by parsing <code>content</code> into a graph created using the
     * supplied {@link TcProvider}, assuming it has the given format.
     * 
     * @param content
     *            the serialized graph content.
     * @param formatIdentifier
     *            the format to parse the content as.
     * @param tcProvider
     *            the provider that will create the graph where the triples will be stored.
     */
    public GraphImportsClosureSource(InputStream content, String formatIdentifier, TcProvider tcProvider) {
        this(content, formatIdentifier, tcProvider, Parser.getInstance());
    }

    /**
     * Creates a new graph input source by parsing <code>content</code> (using the supplied {@link Parser})
     * into a graph created using the supplied {@link TcProvider}, assuming it has the given format.
     * 
     * @param content
     *            the serialized graph content.
     * @param formatIdentifier
     *            the format to parse the content as.
     * @param tcProvider
     *            the provider that will create the graph where the triples will be stored.
     * @param parser
     *            the parser to use for creating the graph. If null, the default one will be used.
     */
    public GraphImportsClosureSource(InputStream content,
                                     String formatIdentifier,
                                     TcProvider tcProvider,
                                     Parser parser) {
        super(content, formatIdentifier, tcProvider, parser);
    }

    @Override
    public Set<OWLOntology> getOntologies() {
        // TODO Auto-generated method stub
        return null;
    }

}
