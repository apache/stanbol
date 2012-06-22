package org.apache.stanbol.contenthub.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.store.file.serializer.ContentPartSerializer;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class ContentPartSerializerTest {
	private static final Logger log = LoggerFactory
			.getLogger(FileStoreDBManagerTest.class);

	@TestReference
	ContentPartSerializer contentPartSerializer;

	@TestReference
	ContentItemFactory contentItemFactory;

	@TestReference
	private Parser parser;

	@Test
	public void testBlobSerializerProvider() throws StoreException {
		String strExpected = "I live in Paris.";
		Blob blob = null;
		try {
			blob = contentItemFactory.createBlob(new StringSource(strExpected));
		} catch (IOException e) {
			log.error("Blob cannot be created.");
		}

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		contentPartSerializer.serializeContentPart(os, blob);

		String strActual = new String(os.toByteArray());
		assertEquals(strExpected, strActual);
	}

	@Test
	public void testTripleCollectionSerializerProvider() throws StoreException {
		TripleCollection tcExpected = new SimpleMGraph();
		tcExpected.add(new TripleImpl(new UriRef(
				"http://dbpedia.org/resource/Paris"), new UriRef(
				"http://dbpedia.org/ontology/label"), new UriRef(
				"http://www.w3.org/2000/01/rdf-schema#label/Paris")));
		tcExpected.add(new TripleImpl(new UriRef(
				"http://dbpedia.org/resource/Paris"), new UriRef(
				"http://dbpedia.org/ontology/populationTotal"), new UriRef(
				"http://www.w3.org/2001/XMLSchema#long/2193031")));

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		contentPartSerializer.serializeContentPart(os, tcExpected);

		InputStream is = new ByteArrayInputStream(os.toByteArray());
		MGraph tcActual = new SimpleMGraph();
		parser.parse(tcActual, is, SupportedFormat.RDF_XML);

		assertTrue(tcExpected.containsAll(tcActual));
		assertTrue(tcActual.containsAll(tcExpected));
	}
}
