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
package org.apache.stanbol.enhancer.engines.dbpspotlight.disambiguate;

import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_CONFIDENCE;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_DISAMBIGUATOR;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_RESTRICTION;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_SPARQL;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_SUPPORT;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_URL_KEY;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.UTF8;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.utils.XMLParser.loadXMLFromInputStream;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.apache.stanbol.enhancer.engines.dbpspotlight.model.Annotation;
import org.apache.stanbol.enhancer.engines.dbpspotlight.utils.SpotlightEngineUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * {@link DBPSpotlightDisambiguateEnhancementEngine} provides functionality to
 * enhance document with their language.
 * 
 * @author Iavor Jelev, Babelmonkeys (GzEvD)
 */
@Component(metatype = true, immediate = true, 
	label = "%stanbol.DBPSpotlightDisambiguateEnhancementEngine.name", 
	description = "%stanbol.DBPSpotlightDisambiguateEnhancementEngine.description")
@Service
@Properties(value = { 
		@Property(name = EnhancementEngine.PROPERTY_NAME, value = "dbpspotlightdisambiguate"),
		@Property(name = PARAM_URL_KEY, value = "http://spotlight.dbpedia.org/rest/annotate"),
		@Property(name = PARAM_DISAMBIGUATOR, value = "Document"),
		@Property(name = PARAM_RESTRICTION),
		@Property(name = PARAM_SPARQL),
		@Property(name = PARAM_SUPPORT),
		@Property(name = PARAM_CONFIDENCE)
})
public class DBPSpotlightDisambiguateEnhancementEngine extends
		AbstractEnhancementEngine<IOException, RuntimeException> implements
		EnhancementEngine, ServiceProperties {

	/**
	 * Ensures this engine is deactivated in {@link OfflineMode}
	 */
	@SuppressWarnings("unused")
	@Reference
	private OnlineMode onlineMode;

	/**
	 * The default value for the Execution of this Engine. Currently set to
	 * {@link ServiceProperties#ORDERING_PRE_PROCESSING}
	 */
	public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION - 31;

	/** This contains the logger. */
	private static final Logger log = LoggerFactory
			.getLogger(DBPSpotlightDisambiguateEnhancementEngine.class);
	/** holds the url of the Spotlight REST endpoint */
	private URL spotlightUrl;
	/** holds the chosen of disambiguator to be used */
	private String spotlightDisambiguator;
	/** holds the type restriction for the results, if the user wishes one */
	private String spotlightTypesRestriction;
	/** holds the chosen minimal support value */
	private String spotlightSupport;
	/** holds the chosen minimal confidence value */
	private String spotlightConfidence;
	/** holds the sparql restriction for the results, if the user wishes one */
	private String spotlightSparql;
	/**
	 * holds the existing TextAnnotations, which are used as input for DBpedia
	 * Spotlight, and later for linking of the results
	 */
	private Hashtable<String, IRI> textAnnotationsMap;

    private int connectionTimeout;
	/**
	 * Default constructor used by OSGI. It is expected that
	 * {@link #activate(ComponentContext)} is called before
	 * using the instance.
	 */
	public DBPSpotlightDisambiguateEnhancementEngine(){}
	
	/**
	 * Constructor intended to be used for unit tests
	 * @param serviceURL
	 */
	protected DBPSpotlightDisambiguateEnhancementEngine(URL serviceURL,int connectionTimeout){
		this.spotlightUrl = serviceURL;
		this.connectionTimeout = connectionTimeout;
	}
	/**
	 * Initialize all parameters from the configuration panel, or with their
	 * default values
	 * 
	 * @param ce
	 *            the {@link ComponentContext}
	 */
	@SuppressWarnings("unchecked")
	protected void activate(ComponentContext ce) throws ConfigurationException,
			IOException {

		super.activate(ce);

		Dictionary<String, Object> properties = ce.getProperties();
		spotlightUrl = SpotlightEngineUtils.parseSpotlightServiceURL(properties);
        connectionTimeout = SpotlightEngineUtils.getConnectionTimeout(properties);
		spotlightDisambiguator = properties.get(PARAM_DISAMBIGUATOR) == null ? null
				: (String) properties.get(PARAM_DISAMBIGUATOR);
		spotlightTypesRestriction = properties.get(PARAM_RESTRICTION) == null ? null
				: (String) properties.get(PARAM_RESTRICTION);
		spotlightSparql = properties.get(PARAM_SPARQL) == null ? null
				: (String) properties.get(PARAM_SPARQL);
		spotlightSupport = properties.get(PARAM_SUPPORT) == null ? "-1"
				: (String) properties.get(PARAM_SUPPORT);
		spotlightConfidence = properties.get(PARAM_CONFIDENCE) == null ? "-1"
				: (String) properties.get(PARAM_CONFIDENCE);
	}

	/**
	 * Check if the content can be enhanced
	 * 
	 * @param ci
	 *            the {@link ContentItem}
	 */
	public int canEnhance(ContentItem ci) throws EngineException {
		return SpotlightEngineUtils.canProcess(ci) ?
				ENHANCE_ASYNC : CANNOT_ENHANCE;
	}

	/**
	 * Calculate the enhancements by doing a POST request to the DBpedia
	 * Spotlight endpoint and processing the results
	 * 
	 * @param ci
	 *            the {@link ContentItem}
	 */
	public void computeEnhancements(ContentItem ci) throws EngineException {
		Language language = SpotlightEngineUtils.getContentLanguage(ci);
		String text = SpotlightEngineUtils.getPlainContent(ci);


		// Retrieve the existing text annotations (requires read lock)
		Graph graph = ci.getMetadata();
		String xmlTextAnnotations = this.getSpottedXml(text, graph);
		Collection<Annotation> dbpslGraph = doPostRequest(text,
				xmlTextAnnotations, ci.getUri());
		if (dbpslGraph != null) {
			// Acquire a write lock on the ContentItem when adding the
			// enhancements
			ci.getLock().writeLock().lock();
			try {
				createEnhancements(dbpslGraph, ci, language);
				if (log.isDebugEnabled()) {
					Serializer serializer = Serializer.getInstance();
					ByteArrayOutputStream debugStream = new ByteArrayOutputStream();
					serializer.serialize(debugStream, ci.getMetadata(),
							"application/rdf+xml");
					try {
						log.debug("DBpedia Enhancements:\n{}",
								debugStream.toString("UTF-8"));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			} finally {
				ci.getLock().writeLock().unlock();
			}
		}
	}

	/**
	 * The method adds the returned DBpedia Spotlight annotations to the content
	 * item's metadata. For each DBpedia resource an EntityAnnotation is created
	 * and linked to the according TextAnnotation.
	 * 
	 * @param occs
	 *            a Collection of entity information
	 * @param ci
	 *            the content item
	 */
	public void createEnhancements(Collection<Annotation> occs,
			ContentItem ci, Language language) {
		HashMap<RDFTerm, IRI> entityAnnotationMap = new HashMap<RDFTerm, IRI>();

		for (Annotation occ : occs) {

			if (textAnnotationsMap.get(occ.surfaceForm) != null) {
				IRI textAnnotation = textAnnotationsMap.get(occ.surfaceForm);
				Graph model = ci.getMetadata();
				IRI entityAnnotation = EnhancementEngineHelper
						.createEntityEnhancement(ci, this);
				entityAnnotationMap.put(occ.uri, entityAnnotation);
				Literal label = new PlainLiteralImpl(occ.surfaceForm.name, language);
				model.add(new TripleImpl(entityAnnotation, DC_RELATION,
						textAnnotation));
				model.add(new TripleImpl(entityAnnotation,
						ENHANCER_ENTITY_LABEL, label));

				Collection<String> t = occ.getTypeNames();
				if (t != null) {
					Iterator<String> it = t.iterator();
					while (it.hasNext())
						model.add(new TripleImpl(entityAnnotation,
								ENHANCER_ENTITY_TYPE, new IRI(it.next())));
				}
				model.add(new TripleImpl(entityAnnotation,
						ENHANCER_ENTITY_REFERENCE, occ.uri));
			}
		}
	}

	/**
	 * Sends a POST request to the DBpediaSpotlight url.
	 * 
	 * @param text
	 *            a <code>String</code> with the text to be analyzed
	 * @param xmlTextAnnotations
	 * @param textAnnotations
	 * @param contentItemUri the URI of the {@link ContentItem} (only
	 * used for logging in case of an error)
	 * @return a <code>String</code> with the server response
	 * @throws EngineException
	 *             if the request cannot be sent
	 */
	protected Collection<Annotation> doPostRequest(String text,
			String xmlTextAnnotations, IRI contentItemUri) throws EngineException {
		HttpURLConnection connection = null;
		BufferedWriter wr = null;
		try {
			connection = (HttpURLConnection) spotlightUrl.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			connection.setRequestProperty("Accept", "text/xml");

            //set ConnectionTimeout (if configured)
            if(connectionTimeout > 0){
                connection.setConnectTimeout(connectionTimeout*1000);
                connection.setReadTimeout(connectionTimeout*1000);
            }

            connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			wr = new BufferedWriter(new OutputStreamWriter(
					connection.getOutputStream(),UTF8));
		} catch (IOException e) {
			IOUtils.closeQuietly(wr);
			throw new EngineException("Unable to open connection to "+
					spotlightUrl,e);
		}
		try {

			wr.write("spotter=SpotXmlParser&");
			if (spotlightDisambiguator != null
					&& !spotlightDisambiguator.isEmpty()){
				wr.write("disambiguator=");
				wr.write(URLEncoder.encode(spotlightDisambiguator, "UTF-8"));
				wr.write('&');
			}
			if (spotlightTypesRestriction != null
					&& !spotlightTypesRestriction.isEmpty()){
				wr.write("types=");
				wr.write(URLEncoder.encode(spotlightTypesRestriction, "UTF-8"));
				wr.write('&');
			}
			if (spotlightSupport != null && !spotlightSupport.isEmpty()) {
				wr.write("support=");
				wr.write(URLEncoder.encode(spotlightSupport, "UTF-8"));
				wr.write('&');
			}
			if (spotlightConfidence != null && !spotlightConfidence.isEmpty()){
				wr.write("confidence=");
				wr.write(URLEncoder.encode(spotlightConfidence, "UTF-8"));
				wr.write('&');
			}
			if (spotlightSparql != null && !spotlightSparql.isEmpty()
					&& spotlightTypesRestriction == null) {
				wr.write("sparql=");
				wr.write(URLEncoder.encode(spotlightSparql, "UTF-8"));
				wr.write('&');
			}
			wr.write("text=");
			wr.write(URLEncoder.encode(xmlTextAnnotations, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(
					"The platform does not support encoding " + UTF8.name(),e);
		} catch (IOException e) {
			throw new EngineException("Unable to write 'plain/text' content "
					+ "for ContentItem "+contentItemUri+" to "
					+ spotlightUrl,e);
		} finally {
			IOUtils.closeQuietly(wr);
		}
		InputStream is = null;
		Document xmlDoc;
		try {
			// Get Response
			 is = connection.getInputStream();
			xmlDoc = loadXMLFromInputStream(is);
		} catch (IOException e) {
			throw new EngineException("Unable to spot Entities with"
					+ "Dbpedia Spotlight Annotate RESTful Serice running at "
					+ spotlightUrl,e);
		} catch(SAXException e) {
			throw new EngineException("Unable to parse Response from "
					+ "Dbpedia Spotlight Annotate RESTful Serice running at "
					+ spotlightUrl,e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		return Annotation.parseAnnotations(xmlDoc);
	}

	private String getSpottedXml(String text, Graph graph) {
		StringBuilder xml = new StringBuilder();
		textAnnotationsMap = new Hashtable<String, IRI>();

		xml.append(String.format("<annotation text=\"%s\">", text));
		try {
			for (Iterator<Triple> it = graph.filter(null, RDF_TYPE,
					TechnicalClasses.ENHANCER_TEXTANNOTATION); it.hasNext();) {
				// Triple tAnnotation = it.next();
				IRI uri = (IRI) it.next().getSubject();
				String surfaceForm = EnhancementEngineHelper.getString(graph,
						uri, ENHANCER_SELECTED_TEXT);
				if (surfaceForm != null) {
					String offset = EnhancementEngineHelper.getString(graph,
							uri, ENHANCER_START);
					textAnnotationsMap.put(surfaceForm, uri);
					xml.append(String.format(
							"<surfaceForm name=\"%s\" offset=\"%s\"/>",
							surfaceForm, offset));
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return xml.append("</annotation>").toString();
	}

	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(
				ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}


}
