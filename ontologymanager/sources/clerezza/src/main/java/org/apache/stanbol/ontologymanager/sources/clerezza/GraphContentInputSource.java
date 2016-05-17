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
package org.apache.stanbol.ontologymanager.sources.clerezza;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.ontologymanager.servicesapi.io.Origin;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyLoadingException;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ontology input source that returns a Clerezza {@link Graph} ({@link ImmutableGraph} or {@link Graph})
 * after parsing its serialized content from an input stream.
 * 
 * @author alexdma
 * 
 */
public class GraphContentInputSource extends AbstractClerezzaGraphInputSource {

    protected Logger log = LoggerFactory.getLogger(getClass());

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
     * into a graph created using the supplied {@link TcProvider}, assuming it has the given format. An
     * {@link OntologyLoadingException} will be thrown if the parser fails.
     * 
     * @param content
     *            the serialized graph content.
     * @param formatIdentifier
     *            the format to parse the content as. Blank or null values are allowed, but could cause
     *            exceptions to be thrown if the supplied input stream cannot be reset.
     * @param tcProvider
     *            the provider that will create the graph where the triples will be stored. If null, an
     *            in-memory graph will be created, in which case any ontology collectors using this input
     *            source will most likely have to copy it to persistent storage.
     * @param parser
     *            the parser to use for creating the graph. If null, the default one will be used. * @deprecated
     */
    public GraphContentInputSource(InputStream content,
                                   String formatIdentifier,
                                   TcProvider tcProvider,
                                   Parser parser) {
        long before = System.currentTimeMillis();

        if (content == null) throw new IllegalArgumentException("No content supplied");
        if (parser == null) parser = Parser.getInstance();
        boolean loaded = false;

        // Check if we can make multiple attempts at parsing the data stream.
        if (content.markSupported()) {
            log.debug("Stream mark/reset supported. Can try multiple formats if necessary.");
            content.mark(Integer.MAX_VALUE);
        }

        // Check for supported formats to try.
        Collection<String> formats;
        if (formatIdentifier == null || "".equals(formatIdentifier.trim())) formats = OntologyUtils
                .getPreferredSupportedFormats(parser.getSupportedFormats());
        else formats = Collections.singleton(formatIdentifier);

        // TODO guess/lookahead the ontology ID and use it in the graph name.
        IRI name = new IRI( /* "ontonet" + "::" + */
        getClass().getCanonicalName() + "-time:" + System.currentTimeMillis());

        Graph graph = null;
        if (tcProvider != null && tcProvider != null) {
            // ImmutableGraph directly stored in the TcProvider prior to using the source
            graph = tcProvider.createGraph(name);
            bindPhysicalOrigin(Origin.create(name));
            // XXX if addition fails, should rollback procedures also delete the graph?
        } else {
            // In memory graph, will most likely have to be copied afterwards.
            graph = new IndexedGraph();
            bindPhysicalOrigin(null);
        }

        Iterator<String> itf = formats.iterator();
        if (!itf.hasNext()) throw new OntologyLoadingException("No suitable format found or defined.");
        do {
            String f = itf.next();
            log.debug("Parsing with format {}", f);
            try {
                parser.parse((Graph) graph, content, f);
                loaded = true;
                log.info("ImmutableGraph parsed, has {} triples", graph.size());
            } catch (UnsupportedFormatException e) {
                log.debug("Parsing format {} failed.", f);
            } catch (Exception e) {
                log.debug("Error parsing format " + f, e);
            } finally {
                if (!loaded && content.markSupported()) try {
                    content.reset();
                } catch (IOException e) {
                    log.debug("Failed to reset data stream while parsing format {}.", f);
                    // No way to retry if the stream cannot be reset. Must recreate it.
                    break;
                }
            }
        } while (!loaded && itf.hasNext());

        if (loaded) {
            bindRootOntology(graph);
            log.debug("Root ontology is a {}.", getRootOntology().getClass().getCanonicalName());
        } else {
            // Rollback graph creation, if any
            if (tcProvider != null && tcProvider != null) {
                tcProvider.deleteGraph(name);
                log.error("Parsing failed. Deleting triple collection {}", name);
            }
            throw new OntologyLoadingException("Parsing failed. Giving up.");
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
     * 
     */
    public GraphContentInputSource(InputStream content, TcProvider tcProvider) {
        this(content, null, tcProvider);
    }

    @Override
    public String toString() {
        return "<GRAPH_CONTENT>" + getOrigin();
    }

}
