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
package org.apache.stanbol.contenthub.test.store.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.store.file.serializer.ContentPartDeserializer;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class ContentPartDeserializerTest {
    private static final Logger log = LoggerFactory.getLogger(FileStoreDBManagerTest.class);

    @TestReference
    ContentPartDeserializer contentPartDeserializer;

    @TestReference
    ContentItemFactory contentItemFactory;

    @TestReference
    Serializer serializer;

    @Test
    public void testBlobDeserializerProvider() throws StoreException {
        Blob blobExpected = null;
        try {
            blobExpected = contentItemFactory.createBlob(new StringSource("I live in Paris."));
        } catch (IOException e) {
            log.error("Blob cannot be created.");
            throw new StoreException("Blob cannot be created.", e);
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            IOUtils.copy(blobExpected.getStream(), os);
        } catch (IOException e) {
            log.error("Failed to serialize Blob into OutputStream");
            throw new StoreException("Failed to serialize Blob into OutputStream", e);
        }

        InputStream is = new ByteArrayInputStream(os.toByteArray());
        Blob blobActual = contentPartDeserializer.deserializeContentPart(is, Blob.class,
            blobExpected.getMimeType());

        assertEquals(blobExpected.getMimeType(), blobActual.getMimeType());
        assertEquals(blobExpected.getContentLength(), blobActual.getContentLength());
        try {
            assertEquals(org.apache.commons.io.IOUtils.toString(blobExpected.getStream()),
                org.apache.commons.io.IOUtils.toString(blobActual.getStream()));
        } catch (IOException e) {
            log.error("Failed to convert InputStream to String.");
            throw new StoreException("Failed to convert InputStream to String.", e);
        }
    }

    @Test
    public void testGraphDeserializerProvider() throws StoreException {
        TripleCollection gExpected = new SimpleMGraph();
        gExpected.add(new TripleImpl(new UriRef("http://dbpedia.org/resource/Paris"), new UriRef(
                "http://dbpedia.org/ontology/label"), new UriRef(
                "http://www.w3.org/2000/01/rdf-schema#label/Paris")));
        gExpected.add(new TripleImpl(new UriRef("http://dbpedia.org/resource/Paris"), new UriRef(
                "http://dbpedia.org/ontology/populationTotal"), new UriRef(
                "http://www.w3.org/2001/XMLSchema#long/2193031")));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        serializer.serialize(os, gExpected, SupportedFormat.RDF_XML);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

        Graph gActual = contentPartDeserializer.deserializeContentPart(is, Graph.class);
        assertTrue(gExpected.containsAll(gActual));
        assertTrue(gActual.containsAll(gExpected));
    }

    @Test
    public void testIndexedMGraphDeserializerProvider() throws StoreException {
        TripleCollection tc = new SimpleMGraph();
        tc.add(new TripleImpl(new UriRef("http://dbpedia.org/resource/Paris"), new UriRef(
                "http://dbpedia.org/ontology/label"), new UriRef(
                "http://www.w3.org/2000/01/rdf-schema#label/Paris")));
        tc.add(new TripleImpl(new UriRef("http://dbpedia.org/resource/Paris"), new UriRef(
                "http://dbpedia.org/ontology/populationTotal"), new UriRef(
                "http://www.w3.org/2001/XMLSchema#long/2193031")));

        IndexedMGraph gExpected = new IndexedMGraph(tc);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        serializer.serialize(os, tc, SupportedFormat.RDF_XML);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

        MGraph gActual = contentPartDeserializer.deserializeContentPart(is, MGraph.class);
        assertTrue(gExpected.containsAll(gActual));
        assertTrue(gActual.containsAll(gExpected));
    }
}
