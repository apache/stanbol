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
package org.apache.stanbol.enhancer.engines.celi.classification.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.createTextEnhancement;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.SKOS_CONCEPT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.xml.soap.SOAPException;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(immediate = true, metatype = true)
@Service
@Properties(value = { 
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "celiClassification"),
    @Property(name = CeliConstants.CELI_LICENSE),
    @Property(name = CeliConstants.CELI_TEST_ACCOUNT,boolValue=false),
    @Property(name = CeliConstants.CELI_CONNECTION_TIMEOUT, intValue=CeliConstants.DEFAULT_CONECTION_TIMEOUT)
})
public class CeliClassificationEnhancementEngine extends AbstractEnhancementEngine<IOException, RuntimeException> implements EnhancementEngine, ServiceProperties {
	
	/**
	 * This ensures that no connections to external services are made if Stanbol is started in offline mode 
	 * as the OnlineMode service will only be available if OfflineMode is deactivated. 
	 */
	@SuppressWarnings("unused") //it's not unused!
    @Reference
    private OnlineMode onlineMode; 
	
	private static List<String> supportedLangs = new Vector<String>();
	static {
		supportedLangs.add("en");
		supportedLangs.add("fr");
		supportedLangs.add("de");
		supportedLangs.add("it");
		supportedLangs.add("es");
		supportedLangs.add("pt");
		supportedLangs.add("pl");
		supportedLangs.add("nl");
	}
	/**
	 * The literal factory used to create types literals
	 */
    private LiteralFactory literalFactory = LiteralFactory.getInstance();

	/**
	 * The literal representing the LangIDEngine as creator.
	 */
	public static final Literal LANG_ID_ENGINE_NAME = LiteralFactory.getInstance().createTypedLiteral("org.apache.stanbol.enhancer.engines.celi.langid.impl.CeliLanguageIdentifierEnhancementEngine");

	/**
	 * The default value for the Execution of this Engine. Currently set to
	 * {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
	 */
	public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION;
	/**
	 * Currently used as fise:entity-type for TopicAnnotations
	 */
	private static final IRI OWL_CLASS = new IRI("http://www.w3.org/2002/07/owl#Class");
	
	private Logger log = LoggerFactory.getLogger(getClass());

	//NOTE: one CAN NOT store the language as member, as EnhancementEngines
	//      can be called in parallel by multiple threads!
	//private String language = null;

	/**
	 * This contains the only MIME type directly supported by this enhancement
	 * engine.
	 */
	private static final String TEXT_PLAIN_MIMETYPE = "text/plain";

	/**
	 * Set containing the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
	 */
	private static final Set<String> SUPPORTED_MIMTYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);


	@Property(value = "http://linguagrid.org/LSGrid/ws/dbpedia-classification")
	public static final String SERVICE_URL = "org.apache.stanbol.enhancer.engines.celi.classification.url";

	private String licenseKey;
	private URL serviceURL;

	private ClassificationClientHTTP client;

	@Override
	@Activate
	protected void activate(ComponentContext ctx) throws IOException, ConfigurationException {
		super.activate(ctx);
		@SuppressWarnings("unchecked")
        Dictionary<String, Object> properties = ctx.getProperties();
        this.licenseKey = Utils.getLicenseKey(properties,ctx.getBundleContext());
		String url = (String) properties.get(SERVICE_URL);
		if (url == null || url.isEmpty()) {
			throw new ConfigurationException(SERVICE_URL, String.format("%s : please configure the URL of the CELI Web Service (e.g. by" + "using the 'Configuration' tab of the Apache Felix Web Console).", getClass().getSimpleName()));
		}
		this.serviceURL = new URL(url);
		int conTimeout = Utils.getConnectionTimeout(properties, ctx.getBundleContext());
		this.client = new ClassificationClientHTTP(this.serviceURL, this.licenseKey, conTimeout);
	}
	
	@Override
	@Deactivate
	protected void deactivate(ComponentContext ce) {
		super.deactivate(ce);
	}

	@Override
	public int canEnhance(ContentItem ci) throws EngineException {
		String language = EnhancementEngineHelper.getLanguage(ci);
		//canEnhance should inform if it can not enhance a ContentItem because
		//of an potential error in the EnhancementChain configuration, but not
		//throw runtime exceptions.
//		if (language == null) {
//			throw new IllegalStateException("Unable to extract Language for " + "ContentItem " + ci.getUri() + ": This is also checked in the canEnhance " + "method! -> This indicated an Bug in the implementation of the " + "EnhancementJobManager!");
//		}
        if(language==null) {
            log.warn("Unable to enhance ContentItem {} because language of the Content is unknown." +
                    " Please check that a language identification engine is active in this EnhancementChain.",
                    ci.getUri());
        }

		if (ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES) != null && this.isLangSupported(language)) {
		    //NOTE: ENHANCE_ASYNC indicates that the computeEnhancements Method
		    //      correctly applies read/write locks to the contentItem
			return ENHANCE_ASYNC;
		} else {
			return CANNOT_ENHANCE;
		}
	}


	@Override
	public void computeEnhancements(ContentItem ci) throws EngineException {
	    //NOTE: in the computeEnhancements Method on can check metadata already
	    //      checked within the canEnhance method. THis is not required, but it
	    //      may help to identify potential bugs in the EnhancementJobManager
	    //      implementation
        String language = EnhancementEngineHelper.getLanguage(ci);
        if (!isLangSupported(language)){
            throw new IllegalStateException("Call to computeEnhancement with unsupported language '"
                    +language+" for ContentItem "+ ci.getUri() +": This is also checked "
                    + "in the canEnhance method! -> This indicated an Bug in the "
                    + "implementation of the " + "EnhancementJobManager!");
        }
		Entry<IRI, Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES);
		if (contentPart == null) {
			throw new IllegalStateException("No ContentPart with Mimetype '" 
			        + TEXT_PLAIN_MIMETYPE + "' found for ContentItem " 
			        + ci.getUri() + ": This is also checked in the canEnhance "
			        + "method! -> This indicates an Bug in the implementation of "
			        + "the EnhancementJobManager!");
		}
		String text;
		try {
			text = ContentItemHelper.getText(contentPart.getValue());
		} catch (IOException e) {
			throw new InvalidContentException(this, ci, e);
		}
		if (text.trim().length() == 0) {
			log.info("No text contained in ContentPart {} of ContentItem {}",
			    contentPart.getKey(),ci.getUri());
			return;
		}
		//NOTE: EnhancementEngine implementations should pass all Exceptions 
		//      (RuntimeExceptions as is and others wrapped as EngineExceptions). 
		//      The EnhancementJobManager implementation has to catch and
		//      process all those. Handling depends on the configuration of the
		//      EnhancementChain (e.g. if this engine is optional enhancement of
		//      the ContentItem will continue).
		//      This is important as otherwise Users would get "200 ok" replies
		//      for failed enhancement requests that have failed!
		//
		//      This means that:
		//      * Http clients should pass on IOExceptions and SOAPExceptions
		//      * No try/catch that also includes RuntimeExceptions
		List<Concept> lista;
		try {
			lista = this.client.extractConcepts(text, language);
        } catch (IOException e) { //re-throw exceptions as EngineException
            throw new EngineException("Error while calling the CELI classification"
                +" service (configured URL: " +serviceURL+")!",e);
        } catch (SOAPException e) {
            throw new EngineException("Error wile encoding/decoding the request/"
                +"response to the CELI classification service!",e);
        } 
		if(lista.isEmpty()){ //not topics found
		    return; //nothing to do
		}
		Graph g = ci.getMetadata();
		//NOTE: EnhancementEngines that use "ENHANCE_ASYNC" need to acquire a
		//      writeLock before modifications to the enhancement metadata
		ci.getLock().writeLock().lock();
		try {
    		//see STANBOL-617 for rules how to encode extracted topics
    		//we need a single TextAnnotation to link all TopicAnnotations
    		IRI textAnnotation = createTextEnhancement(ci, this);
    		// add the dc:type skos:Concept
    		g.add(new TripleImpl(textAnnotation, DC_TYPE, SKOS_CONCEPT));
    		
    		//not create the fise:TopicAnnotations
    		for (Concept ne : lista) {
    		    IRI topicAnnotation = EnhancementEngineHelper.createTopicEnhancement(ci, this);
    	        g.add(new TripleImpl(topicAnnotation, ENHANCER_ENTITY_REFERENCE, ne.getUri()));
                g.add(new TripleImpl(topicAnnotation, ENHANCER_ENTITY_LABEL, 
                    new PlainLiteralImpl(ne.getLabel())));
                //TODO: currently I use owl:class as entity-type, because that is
                //      what the linked dbpedia ontology resources are.
                g.add(new TripleImpl(topicAnnotation, ENHANCER_ENTITY_TYPE, OWL_CLASS));
                g.add(new TripleImpl(topicAnnotation, ENHANCER_CONFIDENCE, 
                    literalFactory.createTypedLiteral(ne.getConfidence())));
                //link to the TextAnnotation
                g.add(new TripleImpl(topicAnnotation, DC_RELATION, textAnnotation));
    		}
		} finally {
		    ci.getLock().writeLock().unlock();
		}
	}

	private boolean isLangSupported(String language) {
		if (supportedLangs.contains(language))
			return true;
		else
			return false;
	}


	@Override
	public Map<String, Object> getServiceProperties() {
		return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
	}

}
