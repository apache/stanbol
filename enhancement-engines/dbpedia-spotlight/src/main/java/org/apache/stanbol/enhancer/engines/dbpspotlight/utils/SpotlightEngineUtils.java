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
package org.apache.stanbol.enhancer.engines.dbpspotlight.utils;

import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PARAM_URL_KEY;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PROPERTY_CONTEXTUAL_SCORE;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PROPERTY_FINAL_SCORE;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PROPERTY_PERCENTAGE_OF_SECOND_RANK;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PROPERTY_PRIOR_SCORE;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PROPERTY_SIMILARITY_SCORE;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.PROPERTY_SUPPORT;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.SUPPORTED_LANGUAGES;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.SUPPORTED_MIMTYPES;
import static org.apache.stanbol.enhancer.engines.dbpspotlight.Constants.TEXT_PLAIN_MIMETYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Dictionary;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.enhancer.engines.dbpspotlight.Constants;
import org.apache.stanbol.enhancer.engines.dbpspotlight.model.Annotation;
import org.apache.stanbol.enhancer.engines.dbpspotlight.model.CandidateResource;
import org.apache.stanbol.enhancer.engines.dbpspotlight.model.SurfaceForm;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared utilities for the Spotlight Enhancement Engines.
 */
public final class SpotlightEngineUtils {

    /**
     * Restrict instantiation
     */
    private SpotlightEngineUtils() {}

    private static final Logger log = LoggerFactory.getLogger(SpotlightEngineUtils.class);
	
	private static final LiteralFactory literalFactory = LiteralFactory.getInstance();
	
    private static final int DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE = 50;
    
    public static boolean canProcess(ContentItem ci){
		if (ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES) != null) {
			String language = EnhancementEngineHelper.getLanguage(ci);
			if(!SUPPORTED_LANGUAGES.contains(language)) {
				log.info("DBpedia Spotlight can not process ContentItem {} "
						+ "because language {} is not supported (supported: {})",
						new Object[] { ci.getUri(), language, SUPPORTED_LANGUAGES });
				return false;
			} else {
				return true;
			}
		} else {
			log.info("DBpedia Spotlight can not process ContentItem {} "
					+ "because it does not have 'plain/text' content",
					ci.getUri());
			return false;
		}
    }
	public static Language getContentLanguage(ContentItem ci) {
		String lang = EnhancementEngineHelper.getLanguage(ci);
		if(!SUPPORTED_LANGUAGES.contains(lang)){
			throw new IllegalStateException("Langage '"+lang
					+ "' as annotated for ContentItem "
				    + ci.getUri() + " is not supported by this Engine: "
				    + "This is also checked in the canEnhance method! -> This "
					+ "indicated an Bug in the implementation of the "
					+ "EnhancementJobManager!");
		} else {
			return lang == null || lang.isEmpty() ? null : new Language(lang);
		}
	}
	public static String getPlainContent(ContentItem ci) 
			throws EngineException {
		Entry<IRI, Blob> contentPart = ContentItemHelper.getBlob(ci,
				SUPPORTED_MIMTYPES);
		if (contentPart == null) {
			throw new IllegalStateException(
					"No ContentPart with Mimetype '"
							+ TEXT_PLAIN_MIMETYPE
							+ "' found for ContentItem "
							+ ci.getUri()
							+ ": This is also checked in the canEnhance method! -> This "
							+ "indicated an Bug in the implementation of the "
							+ "EnhancementJobManager!");
		}
		try {
			return ContentItemHelper.getText(contentPart.getValue());
		} catch (IOException e) {
			throw new EngineException("Unable to read plain text content form" +
					"contentpart "+contentPart.getKey()+" of ContentItem " +
					ci.getUri());
		}
	}
	/**
	 * Parses the URL from the {@link Constants#PARAM_URL_KEY}
	 * @param properties the configuration of the engine
	 * @return the URL of the service
	 * @throws ConfigurationException if the configuration is missing,
	 * empty or not a valid URL
	 */
	public static URL parseSpotlightServiceURL(
			Dictionary<String, Object> properties)
			throws ConfigurationException {
		Object value = properties.get(PARAM_URL_KEY);
		if(value == null || value.toString().isEmpty()){
			throw new ConfigurationException(PARAM_URL_KEY, "The URL with the DBpedia "
					+ "Spotlight Annotate RESTful Service MUST NOT be NULL nor empty!");
		} else {
			try {
				return new URL(value.toString());
			} catch (MalformedURLException e) {
				throw new ConfigurationException(PARAM_URL_KEY, "The parsed URL for the "
						+ "DBpedia Spotlight Annotate RESTful Service is illegal formatted!",
						e);
			}
		}
	}
	/**
     * Extracts the selection context based on the content, selection and
     * the start char offset of the selection
     * @param content the content
     * @param selection the selected text
     * @param selectionStartPos the start char position of the selection
     * @return the context
     */
    public static String getSelectionContext(String content, String selection,int selectionStartPos){
        //extract the selection context
        int beginPos;
        if(selectionStartPos <= DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE){
            beginPos = 0;
        } else {
            int start = selectionStartPos-DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            beginPos = content.indexOf(' ',start);
            if(beginPos < 0 || beginPos >= selectionStartPos){ //no words
                beginPos = start; //begin within a word
            }
        }
        int endPos;
        if(selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE >= content.length()){
            endPos = content.length();
        } else {
            int start = selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            endPos = content.lastIndexOf(' ', start);
            if(endPos <= selectionStartPos+selection.length()){
                endPos = start; //end within a word;
            }
        }
        return content.substring(beginPos, endPos);
    }
    /**
     * Creates a fise:TextAnnotation for the parsed parameters and
     * adds it the the {@link ContentItem#getMetadata()}. <p>
     * This method assumes a write lock on the parsed content item.
     * @param occ the SurfaceForm
     * @param engine the Engine
     * @param ci the ContentITem
     * @param content the content 
     * @param lang the language of the content or <code>null</code>
     * @return the URI of the created fise:TextAnnotation
     */
	public static IRI createTextEnhancement(SurfaceForm occ,
			EnhancementEngine engine, ContentItem ci, String content,
			Language lang) {
		Graph model = ci.getMetadata();
		IRI textAnnotation = EnhancementEngineHelper
				.createTextEnhancement(ci, engine);
		model.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT,
				new PlainLiteralImpl(occ.name, lang)));
		model.add(new TripleImpl(textAnnotation, ENHANCER_START,
				literalFactory.createTypedLiteral(occ.offset)));
		model.add(new TripleImpl(textAnnotation, ENHANCER_END,
				literalFactory.createTypedLiteral(occ.offset
						+ occ.name.length())));
		if(occ.type != null && !occ.type.isEmpty()){
			model.add(new TripleImpl(textAnnotation, DC_TYPE, new IRI(
					occ.type)));
		}
		model.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_CONTEXT, 
				new PlainLiteralImpl(
						getSelectionContext(content, occ.name, occ.offset),
						lang)));
		return textAnnotation;
	}
	/**
	 * Creates a fise:EntityAnnotation for the parsed parameters and
     * adds it the the {@link ContentItem#getMetadata()}. <p>
     * This method assumes a write lock on the parsed content item.
	 * @param resource the candidate resource
	 * @param engine the engine
	 * @param ci the content item
	 * @param textAnnotation the fise:TextAnnotation to dc:relate the
	 * created fise:EntityAnnotation
	 * @return the URI of the created fise:TextAnnotation
	 */
	public static IRI createEntityAnnotation(CandidateResource resource,
			EnhancementEngine engine, ContentItem ci, IRI textAnnotation) {
		IRI entityAnnotation = EnhancementEngineHelper
				.createEntityEnhancement(ci, engine);
		Graph model = ci.getMetadata();
		Literal label = new PlainLiteralImpl(resource.label,
				new Language("en"));
		model.add(new TripleImpl(entityAnnotation, DC_RELATION,
				textAnnotation));
		model.add(new TripleImpl(entityAnnotation,
				ENHANCER_ENTITY_LABEL, label));
		model.add(new TripleImpl(entityAnnotation,
				ENHANCER_ENTITY_REFERENCE, resource.getUri()));
		model.add(new TripleImpl(entityAnnotation, PROPERTY_CONTEXTUAL_SCORE,
				literalFactory.createTypedLiteral(resource.contextualScore)));
		model.add(new TripleImpl(entityAnnotation,PROPERTY_PERCENTAGE_OF_SECOND_RANK,
				literalFactory.createTypedLiteral(resource.percentageOfSecondRank)));
		model.add(new TripleImpl(entityAnnotation, PROPERTY_SUPPORT, literalFactory
				.createTypedLiteral(resource.support)));
		model.add(new TripleImpl(entityAnnotation, PROPERTY_PRIOR_SCORE, literalFactory
				.createTypedLiteral(resource.priorScore)));
		model.add(new TripleImpl(entityAnnotation, PROPERTY_FINAL_SCORE, literalFactory
				.createTypedLiteral(resource.finalScore)));
		return entityAnnotation;
	}
	/**
	 * Creates a fise:EntityAnnotation for the parsed parameter and
     * adds it the the {@link ContentItem#getMetadata()}. <p>
     * This method assumes a write lock on the parsed content item.
	 * @param annotation the Annotation
	 * @param engine the engine
	 * @param ci the language
	 * @param textAnnotation the TextAnnotation the created
	 * EntityAnnotation links by using dc:relation
	 * @param language the language of the label of the referenced
	 * Entity (or <code>null</code> if none).
	 */
	public static void createEntityAnnotation(Annotation annotation, 
			EnhancementEngine engine, ContentItem ci,
			IRI textAnnotation, Language language) {
		Graph model = ci.getMetadata();
		IRI entityAnnotation = EnhancementEngineHelper
				.createEntityEnhancement(ci, engine);
		Literal label = new PlainLiteralImpl(annotation.surfaceForm.name,
				language);
		model.add(new TripleImpl(entityAnnotation, DC_RELATION,
				textAnnotation));
		model.add(new TripleImpl(entityAnnotation,
				ENHANCER_ENTITY_LABEL, label));
		model.add(new TripleImpl(entityAnnotation,
				ENHANCER_ENTITY_REFERENCE, annotation.uri));
		//set the fise:entity-type
		for(String type : annotation.getTypeNames()){
			IRI annotationType = new IRI(type);
			model.add(new TripleImpl(entityAnnotation,
					ENHANCER_ENTITY_TYPE, annotationType));
		}
		//TODO (rwesten): Pleas check: I use the similarityScore as fise:confidence value
		model.add(new TripleImpl(entityAnnotation, ENHANCER_CONFIDENCE, literalFactory
				.createTypedLiteral(annotation.similarityScore)));
		//add spotlight specific information
		model.add(new TripleImpl(entityAnnotation,PROPERTY_PERCENTAGE_OF_SECOND_RANK,
				literalFactory.createTypedLiteral(annotation.percentageOfSecondRank)));
		model.add(new TripleImpl(entityAnnotation, PROPERTY_SUPPORT, literalFactory
				.createTypedLiteral(annotation.support)));
		model.add(new TripleImpl(entityAnnotation, PROPERTY_SIMILARITY_SCORE, literalFactory
				.createTypedLiteral(annotation.similarityScore)));
	}
	
	public static int getConnectionTimeout(Dictionary<String,Object> engineConfig) throws ConfigurationException {
	    Object value = engineConfig.get(Constants.PARAM_CONNECTION_TIMEOUT);
	    if(value instanceof Number){
	        return ((Number) value).intValue();
	    } else if(value != null){
	        try {
	            return Integer.parseInt(value.toString());
	        } catch (NumberFormatException e) {
                throw new ConfigurationException(Constants.PARAM_CONNECTION_TIMEOUT, 
                    "Parsed value MUST be a valid Integer (Seconds)");
            }
	    } else {
	        return -1;
	    }
	}

}
