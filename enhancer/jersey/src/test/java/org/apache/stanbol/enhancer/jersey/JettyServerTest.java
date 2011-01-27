package org.apache.stanbol.enhancer.jersey;

/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.clerezza.rdf.rdfjson.serializer.RdfJsonSerializingProvider;
import org.apache.stanbol.enhancer.engines.autotagging.impl.ConfiguredAutotaggerProvider;
import org.apache.stanbol.enhancer.engines.autotagging.impl.RelatedTopicEnhancementEngine;
import org.apache.stanbol.enhancer.jersey.JettyServer;
import org.apache.stanbol.enhancer.jersey.processors.FreemarkerViewProcessor;
import org.apache.stanbol.enhancer.jobmanager.impl.InMemoryJobManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;


import static javax.ws.rs.core.MediaType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Starts a server, then run a few HTTP requests against it using the Jersey
 * client.
 */
public class JettyServerTest {

    protected static final String TEXT_CONTENT = "Let the autotagger guess who was a Jamaican"
            + " musician, a lead singer and guitarist"
            + " for a well known reggae band.";

    public static final int PORT = 9999;

    public static final String TEST_URI = "http://localhost:" + PORT + "/";

    public static final String ENGINES_URI = TEST_URI + "engines";

    private static JettyServer server;

    private static InMemoryJobManager jobManager;

    private static Store store;

    private static RelatedTopicEnhancementEngine engine;

    private Client client;

    private WebResource enginesResource;

    private static String testContentItemID = "urn:test:contentItem:ID";

    @BeforeClass
    public static void startServer() throws Exception {
        server = new JettyServer();
        server.start(TEST_URI);
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stop();
    }

    @Before
    public void setUp() throws Exception {

        // simulate OSGi runtime by registering the components to test manually

        jobManager = new InMemoryJobManager();
        server.setAttribute(EnhancementJobManager.class.getName(), jobManager);
        store = new Store() {
            Map<String, ContentItem> map = new HashMap<String, ContentItem>();

            @Override
            public String put(ContentItem ci) {
                map.put(ci.getId(), ci);
                return ci.getId();
            }

            @Override
            public ContentItem get(String id) {
                return map.get(id);
            }

            @Override
            public ContentItem create(String id, final byte[] content,
                    final String contentType) {
                final String final_id;
                if (id == null) {
                    final_id = testContentItemID;
                } else {
                    final_id = id;
                }
                return new ContentItem() {
                    final MGraph mGraph = new SimpleMGraph();

                    @Override
                    public InputStream getStream() {
                        return new ByteArrayInputStream(content);
                    }

                    @Override
                    public String getMimeType() {
                        return contentType;
                    }

                    @Override
                    public MGraph getMetadata() {
                        return mGraph;
                    }

                    @Override
                    public String getId() {
                        return final_id;
                    }
                };
            }

            @Override
            public MGraph getEnhancementGraph() {
                return new SimpleMGraph();
            }
        };
        // store = new InMemoryStore();
        server.setAttribute(Store.class.getName(), store);

        engine = new RelatedTopicEnhancementEngine();
        // the default index does not contain any topic yet, hence test with a
        // Person context match
        engine.setType("http://dbpedia.org/ontology/Person");

        ConfiguredAutotaggerProvider provider = new ConfiguredAutotaggerProvider();
        provider.activate(new MockComponentContext());
        engine.bindAutotaggerProvider(provider);
        jobManager.bindEnhancementEngine(engine);

        Serializer serializer = new Serializer();
        serializer.bindSerializingProvider(new JenaSerializerProvider());
        serializer.bindSerializingProvider(new RdfJsonSerializingProvider());
        server.setAttribute(Serializer.class.getName(), serializer);

        TcManager tcManager = new TcManager();
        server.setAttribute(TcManager.class.getName(), tcManager);

        server.setAttribute(
                FreemarkerViewProcessor.FREEMARKER_TEMPLATE_PATH_INIT_PARAM,
                "/META-INF/templates");

        client = Client.create();
        enginesResource = client.resource(ENGINES_URI);

        // reproducible tests
        EnhancementEngineHelper.setSeed(42);
    }

    @Test
    public void testWebView() throws Exception {
        // the job manager is registered along with an engine (see the
        // #startServer method)
        String r = enginesResource.accept(TEXT_HTML).get(String.class);
        assertTrue(r, r.contains("<strong>1</strong> active engines"));
        assertTrue(r, r.contains(engine.getClass().getName()));

        // unbind the engine from the job manager
        jobManager.unbindEnhancementEngine(engine);
        r = enginesResource.get(String.class);
        assertTrue(r, r.contains("There is no active engines."));

        // remove the job manager
        server.removeAttribute(EnhancementJobManager.class.getName());
        r = enginesResource.get(String.class);
        assertTrue(r, r.contains("There is no active engines."));
    }

    @Test
    public void testGetEnginesAsJSON() throws Exception {
        // the job manager is registered along with an engine (see the
        // #startServer method)
        String r = enginesResource.accept(APPLICATION_JSON).get(
                String.class);
        assertEquals("[\"http:\\/\\/localhost:9999\\/engines\\/relatedtopic\"]\n", r);

        // unbind the engine from the job manager
        jobManager.unbindEnhancementEngine(engine);
        r = enginesResource.accept(APPLICATION_JSON).get(String.class);
        assertEquals("[]\n", r);

        // remove the job manager
        server.removeAttribute(EnhancementJobManager.class.getName());
        r = enginesResource.accept(APPLICATION_JSON).get(String.class);
        assertEquals("[]\n", r);
    }

    @Test
    public void testEnginesFormPostNt() throws Exception {
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("content", TEXT_CONTENT);

        String r = enginesResource.type(APPLICATION_FORM_URLENCODED).accept(
                "text/rdf+nt").post(String.class, formData);
        assertTrue(r.contains("<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://fise.iks-project.eu/ontology/EntityAnnotation> ."));
        assertTrue(r.contains("<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028> <http://fise.iks-project.eu/ontology/extracted-from> <urn:content-item-sha1-9a674f3a975dbb1fc50c50d5686bbdb00595ba94> ."));
        assertTrue(r.contains("<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028> <http://purl.org/dc/terms/creator> \"org.apache.stanbol.enhancer.engines.autotagging.impl.RelatedTopicEnhancementEngine\"^^<http://www.w3.org/2001/XMLSchema#string> ."));
        assertTrue(r.contains("<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028> <http://fise.iks-project.eu/ontology/entity-reference> <http://dbpedia.org/resource/Bob_Marley> ."));
    }

    // @Test
    // TODO: rewrite with a high level parsing helper that handle the low level
    // RDF payload
    public void testFormPostTurtle() throws Exception {

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("content", TEXT_CONTENT);

        // TODO: find a way to pass a namespace prefix configuration to the
        // serializers
        String r = enginesResource.type(APPLICATION_FORM_URLENCODED).accept(
                "text/turtle").post(String.class, formData);

        assertTrue(r.contains(""
                + "<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028>\n"
                + "      a       <http://fise.iks-project.eu/ontology/EntityAnnotation> ;\n"
                + "      <http://fise.iks-project.eu/ontology/entity-reference>\n"
                + "              <http://dbpedia.org/resource/Bob_Marley> ;"));
    }

    @Test
    public void testEnginesFormPostJson() throws Exception {
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("content", TEXT_CONTENT);

        EnhancementEngineHelper.setSeed(42);
        String r = enginesResource.type(APPLICATION_FORM_URLENCODED).accept(
                APPLICATION_JSON).post(String.class, formData);
        assertTrue(r.contains("{\"value\":\"http:\\/\\/dbpedia.org\\/resource\\/Bob_Marley\""));
    }

    @Test
    public void testEnginesFormPostXml() throws Exception {
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("content", TEXT_CONTENT);

        // test force mimetype from HTML form parameters
        formData.putSingle("format", "application/rdf+xml");
        String r = enginesResource.type(APPLICATION_FORM_URLENCODED).accept(
                TEXT_HTML).post(String.class, formData);
        assertTrue(r.contains("<rdf:Description rdf:about=\"urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028\">"));
    }

    @Test
    public void testEnginesRawPostNt() throws Exception {
        String r = enginesResource.type(TEXT_PLAIN).accept(
                "text/rdf+nt").post(String.class, TEXT_CONTENT);
        assertTrue(r.contains("<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://fise.iks-project.eu/ontology/EntityAnnotation> ."));
        assertTrue(r.contains("<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028> <http://fise.iks-project.eu/ontology/extracted-from> <urn:content-item-sha1-9a674f3a975dbb1fc50c50d5686bbdb00595ba94> ."));
        assertTrue(r.contains("<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028> <http://purl.org/dc/terms/creator> \"org.apache.stanbol.enhancer.engines.autotagging.impl.RelatedTopicEnhancementEngine\"^^<http://www.w3.org/2001/XMLSchema#string> ."));
        assertTrue(r.contains("<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028> <http://fise.iks-project.eu/ontology/entity-reference> <http://dbpedia.org/resource/Bob_Marley> ."));
    }

    // @Test
    public void testEnginesRawPosTurtle() throws Exception {
        String r = enginesResource.type(TEXT_PLAIN).accept(
                "text/turtle").post(String.class, TEXT_CONTENT);
        assertTrue(r.contains(""
                + "<urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028>\n"
                + "      a       <http://iks-project.eu/ns/enhancer/extraction/Extraction> ;\n"
                + "      <http://iksproject.eu/ns/extraction/related-topic>\n"
                + "              <http://dbpedia.org/resource/Bob_Marley> ;"));
    }

    @Test
    public void testEnginesRawPosXml() throws Exception {
        String r = enginesResource.type(TEXT_PLAIN).accept(
                "application/rdf+xml").post(String.class, TEXT_CONTENT);
        assertTrue(r.contains("<rdf:Description rdf:about=\"urn:enhancement-ba419d35-0dfe-8af7-aee7-bbe10c45c028\">"));
    }

    @Test
    public void testEnginesRawPosJson() throws Exception {
        String r = enginesResource.type(TEXT_PLAIN).accept(
                APPLICATION_JSON).post(String.class, TEXT_CONTENT);
        assertTrue(r.contains("{\"value\":\"http:\\/\\/dbpedia.org\\/resource\\/Bob_Marley\""));
    }

}
