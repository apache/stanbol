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
package org.apache.stanbol.enhancer.engines.celi.ner.impl;

import static org.apache.stanbol.enhancer.engines.celi.utils.Utils.getSelectionContext;
import static org.apache.stanbol.enhancer.nlp.NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.soap.SOAPException;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.NoConvertorException;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OnlineMode;
import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.apache.stanbol.enhancer.engines.celi.utils.Utils;
import org.apache.stanbol.enhancer.nlp.NlpProcessingRole;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true)
@Service
@Properties(value = { 
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "celiNer"),
    @Property(name = CeliConstants.CELI_LICENSE),
    @Property(name = CeliConstants.CELI_TEST_ACCOUNT,boolValue=false),
    @Property(name = CeliConstants.CELI_CONNECTION_TIMEOUT, intValue=CeliConstants.DEFAULT_CONECTION_TIMEOUT)
})
public class CeliNamedEntityExtractionEnhancementEngine extends AbstractEnhancementEngine<IOException, RuntimeException> implements EnhancementEngine, ServiceProperties {
	
	/**
	 * This ensures that no connections to external services are made if Stanbol is started in offline mode 
	 * as the OnlineMode service will only be available if OfflineMode is deactivated. 
	 */
	@SuppressWarnings("unused")
    @Reference
    private OnlineMode onlineMode; 
	
	/**
	 * The literal representing the LangIDEngine as creator.
	 */
	public static final Literal LANG_ID_ENGINE_NAME = LiteralFactory.getInstance().createTypedLiteral("org.apache.stanbol.enhancer.engines.celi.langid.impl.CeliLanguageIdentifierEnhancementEngine");

	private static Map<String, IRI> entityTypes = new HashMap<String, IRI>();
	static {
		entityTypes.put("pers", OntologicalClasses.DBPEDIA_PERSON);
		entityTypes.put("PER", OntologicalClasses.DBPEDIA_PERSON);
		entityTypes.put("loc", OntologicalClasses.DBPEDIA_PLACE);
		entityTypes.put("GPE", OntologicalClasses.DBPEDIA_PLACE);
		entityTypes.put("org", OntologicalClasses.DBPEDIA_ORGANISATION);

		entityTypes.put("time", OntologicalClasses.SKOS_CONCEPT);
	}
	/**
	 * The supported languages (configured via the {@link #SUPPORTED_LANGUAGES}
	 * configuration.
	 */
	private Collection<String> supportedLangs;

	/**
	 * The default value for the Execution of this Engine. Currently set to
	 * {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
	 */
	public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION;

	private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> sp = new HashMap<String,Object>();
        sp.put(ENHANCEMENT_ENGINE_ORDERING,defaultOrder);
        sp.put(ENHANCEMENT_ENGINE_NLP_ROLE, NlpProcessingRole.NamedEntityRecognition);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(sp);
        
    }

	private static final Logger log = LoggerFactory.getLogger(CeliNamedEntityExtractionEnhancementEngine.class);

	/**
	 * This contains the only MIME type directly supported by this enhancement
	 * engine.
	 */
	private static final String TEXT_PLAIN_MIMETYPE = "text/plain";

	/**
	 * Set containing the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
	 */
	private static final Set<String> SUPPORTED_MIMTYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);

	
	@Property(value = "http://linguagrid.org/LSGrid/ws/com.celi-france.linguagrid.namedentityrecognition.v0u0.demo")
	public static final String SERVICE_URL = "org.apache.stanbol.enhancer.engines.celi.ner.url";

    @Property(value = {"fr","it"},cardinality=1000)
    public static final String SUPPORTED_LANGUAGES = "org.apache.stanbol.enhancer.engines.celi.ner.languages";
		
	private String licenseKey;
	private URL serviceURL;

	private NERserviceClientHTTP client;

	@Override
	@Activate
	protected void activate(ComponentContext ctx) throws IOException, ConfigurationException {
		super.activate(ctx);
		@SuppressWarnings("unchecked")
        Dictionary<String, Object> properties = ctx.getProperties();
        log.info("Activate CELI NER engine:");
        log.info(" > name: {}",getName());
        this.licenseKey = Utils.getLicenseKey(properties,ctx.getBundleContext());
		String url = (String) properties.get(SERVICE_URL);
		if (url == null || url.isEmpty()) {
			throw new ConfigurationException(SERVICE_URL, String.format("%s : please configure the URL of the CELI Web Service (e.g. by" + "using the 'Configuration' tab of the Apache Felix Web Console).", getClass().getSimpleName()));
		}
		this.serviceURL = new URL(url);
		int conTimeout = Utils.getConnectionTimeout(properties, ctx.getBundleContext());
		this.client = new NERserviceClientHTTP(this.serviceURL, this.licenseKey,conTimeout);
        log.info(" > CELI service: {}",serviceURL);
		
		//init the supported languages (now configurable)
		Object languageObject = properties.get(SUPPORTED_LANGUAGES);
		HashSet<String> languages;
		if(languageObject instanceof String){
		    //support splitting multiple languages with ';'
		    languages = new HashSet<String>(Arrays.asList(languageObject.toString().split(";")));
		    if(languages.remove("")){
		        log.warn("Languages configuration '{}' contained empty language -> removed",languageObject);
		    }//empty not allowed
		} else if(languageObject instanceof Iterable<?>){ //does not work for arrays :(
		    languages = new HashSet<String>();
		    for(Object o : (Iterable<Object>)languageObject){
		        if(o != null && !o.toString().isEmpty()){
		            languages.add(o.toString());
		        } else {
		            log.warn("Language configuration '{}' contained illegal value '{}' -> removed",
		                languageObject,o);
		        }
		    }
		} else if(languageObject.getClass().isArray()){
            languages = new HashSet<String>();
		    for(Object langObj : (Object[])languageObject){
		        if(langObj != null){
		            languages.add(langObj.toString());
		        } else {
                    log.warn("Language configuration '{}' contained illegal value '{}' -> removed",
                            Arrays.toString((Object[])languageObject),langObj);
		        }
		    }
		} else {
		    languages = null;
		}
		if(languages == null || languages.isEmpty()){
		    throw new ConfigurationException(SUPPORTED_LANGUAGES, String.format(
		        "Missing or invalid configuration of the supported languages (config :'%s'",
		        languageObject != null && languageObject.getClass().isArray() ?
		                Arrays.toString((Object[])languageObject): //nicer logging for arrays
		                    languageObject));
		}
		this.supportedLangs = Collections.unmodifiableSet(languages);
        log.info(" > supported languages: {}",supportedLangs);
	}

	@Override
	@Deactivate
	protected void deactivate(ComponentContext ce) {
		super.deactivate(ce);
        this.supportedLangs = null;
        this.client = null;
        this.serviceURL = null;
	}

	@Override
	public int canEnhance(ContentItem ci) throws EngineException {
		String language = EnhancementEngineHelper.getLanguage(ci);
		if (language == null) {
		    log.info("Unable to extract language annotation for ContentItem  -> will not enhance",
		        ci.getUri());
		    return CANNOT_ENHANCE;
		} else if(!isLangSupported(language)){
		    log.debug("Language '{}' of contentItem {} is not supported (supported: {}) -> will not enhance",
		        new Object[]{language,ci.getUri(),supportedLangs});
		    return CANNOT_ENHANCE;
		}
		
		if (ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES) != null)
			return ENHANCE_ASYNC;
		else
		    log.debug("No Content of type {} found in ConentItem {} -> will not enhance",
		        SUPPORTED_MIMTYPES,ci.getUri());
			return CANNOT_ENHANCE;
	}


	@Override
	public void computeEnhancements(ContentItem ci) throws EngineException {
		Entry<IRI, Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES);
		if (contentPart == null) {
			throw new IllegalStateException("No ContentPart with Mimetype '" + TEXT_PLAIN_MIMETYPE + "' found for ContentItem " + ci.getUri() + ": This is also checked in the canEnhance method! -> This "
					+ "indicated an Bug in the implementation of the " + "EnhancementJobManager!");
		}
		String text = "";
		try {
			text = ContentItemHelper.getText(contentPart.getValue());
		} catch (IOException e) {
			throw new InvalidContentException(this, ci, e);
		}
		if (text.trim().length() == 0) {
			log.info("No text contained in ContentPart {" + contentPart.getKey() + "} of ContentItem {" + ci.getUri() + "}");
			return;
		}
        String language = EnhancementEngineHelper.getLanguage(ci);
        if (language == null) {
            throw new IllegalStateException("Unable to extract Language for " + "ContentItem " + ci.getUri() + ": This is also checked in the canEnhance " + "method! -> This indicated an Bug in the implementation of the " + "EnhancementJobManager!");
        }
        Language lang = new Language(language); //used for the palin literals in TextAnnotations
		try {
			List<NamedEntity> lista = this.client.extractEntities(text, language);
			LiteralFactory literalFactory = LiteralFactory.getInstance();

			Graph g = ci.getMetadata();

			for (NamedEntity ne : lista) {
				try {
					IRI textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
					//add selected text as PlainLiteral in the language extracted from the text
					g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, 
					    new PlainLiteralImpl(ne.getFormKind(),lang)));
					g.add(new TripleImpl(textAnnotation, DC_TYPE, getEntityRefForType(ne.type)));
					if (ne.getFrom() != null && ne.getTo() != null) {
						g.add(new TripleImpl(textAnnotation, ENHANCER_START, literalFactory.createTypedLiteral(
						    ne.getFrom().intValue())));
						g.add(new TripleImpl(textAnnotation, ENHANCER_END, literalFactory.createTypedLiteral(
						    ne.getTo().intValue())));
						g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_CONTEXT, 
						    new PlainLiteralImpl(getSelectionContext(text, ne.getFormKind(), ne.getFrom().intValue()), lang)));
					}
				} catch (NoConvertorException e) {
					log.error(e.getMessage(), e);
				}
			}
        } catch (IOException e) {
            throw new EngineException("Error while calling the CELI NER (Named Entity Recognition)"
                +" service (configured URL: " +serviceURL+")!",e);
        } catch (SOAPException e) {
            throw new EngineException("Error wile encoding/decoding the request/"
                +"response to the CELI NER (Named Entity Recognition) service!",e);
        } 

	}

	private boolean isLangSupported(String language) {
		return supportedLangs.contains(language);
	}

	private RDFTerm getEntityRefForType(String type) {
		if (!entityTypes.containsKey(type))
			return OntologicalClasses.SKOS_CONCEPT;
		else
			return entityTypes.get(type);
	}

	@Override
	public Map<String, Object> getServiceProperties() {
		return SERVICE_PROPERTIES;
	}

}
