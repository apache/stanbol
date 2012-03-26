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
import java.util.Collection;
import java.util.Collections;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.OntologyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ontology input source that returns a Clerezza {@link TripleCollection} ({@link Graph} or {@link MGraph})
 * after parsing its serialized content from an input stream.
 * 
 * @author alexdma
 * 
 */
public class GraphContentInputSource extends AbstractClerezzaGraphInputSource {

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Creates a new graph input source by parsing <code>content</code>. Every supported format will be tried
     * until one is parsed successfully. The resulting graph is created in-memory, and its triples will have
     * to be manually added to a stored graph if necessary.
     * 
     * @param content
     *            the serialized graph content.
     */
    public GraphContentInputSource(InputStream content) {
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
    public GraphContentInputSource(InputStream content, String formatIdentifier) {
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
    public GraphContentInputSource(InputStream content, String formatIdentifier, TcProvider tcProvider) {
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
    public GraphContentInputSource(InputStream content,
                                   String formatIdentifier,
                                   TcProvider tcProvider,
                                   Parser parser) {
        long before = System.currentTimeMillis();

        if (content == null) throw new IllegalArgumentException("No content supplied");
        if (parser == null) parser = Parser.getInstance();
        // No physical IRI
        bindPhysicalIri(null);
        bindTriplesProvider(tcProvider);
        boolean loaded = false;

        Collection<String> formats;
        if (formatIdentifier == null || "".equals(formatIdentifier.trim())) formats = OntologyUtils
                .getPreferredSupportedFormats(parser.getSupportedFormats());
        else formats = Collections.singleton(formatIdentifier);
        TripleCollection graph = null;
        if (tcProvider != null) {
            UriRef name = new UriRef(getClass().getCanonicalName() + "-" + System.currentTimeMillis());
            graph = tcProvider.createMGraph(name);
        } else graph = new IndexedMGraph();
        for (String format : formats) {
            try {
                parser.parse((MGraph) graph, content, format);
                loaded = true;
                break;
            } catch (UnsupportedFormatException e) {
                log.debug("Parsing format {} failed.", format);
                continue;
            } catch (Exception e) {
                log.error("Error parsing " + format, e);
                continue;
            }
        }
        if (loaded) {
            bindRootOntology(graph);
            log.debug("Root ontology is a {}.", getRootOntology().getClass().getCanonicalName());
        }
        log.debug("Input source initialization completed in {} ms.", (System.currentTimeMillis() - before));
    }

    /**
     * Creates a new graph input source by parsing <code>content</code> into a graph created using the
     * supplied {@link TcProvider}. Every supported format will be tried until one is parsed successfully.
     * 
     * @param content
     *            the serialized graph content.
     * @param tcProvider
     *            the provider that will create the graph where the triples will be stored.
     */
    public GraphContentInputSource(InputStream content, TcProvider tcProvider) {
        this(content, null, tcProvider);
    }

    @Override
    public String toString() {
        return "<GRAPH_CONTENT>" + OWLUtils.guessOntologyIdentifier(getRootOntology());
    }

}
