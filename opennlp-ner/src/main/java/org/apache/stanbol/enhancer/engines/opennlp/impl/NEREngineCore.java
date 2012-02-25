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
package org.apache.stanbol.enhancer.engines.opennlp.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core of our EnhancementEngine, separated from the OSGi service to make it easier to test this.
 */
public class NEREngineCore implements EnhancementEngine {
    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    /**
     * Contains the only supported mimetype {@link #TEXT_PLAIN_MIMETYPE}
     */
    protected static final Set<String> SUPPORTED_MIMETYPES = 
            Collections.singleton(TEXT_PLAIN_MIMETYPE);

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static Map<String,UriRef> entityTypes = new HashMap<String,UriRef>();
    static {
        entityTypes.put("person", OntologicalClasses.DBPEDIA_PERSON);
        entityTypes.put("location", OntologicalClasses.DBPEDIA_PLACE);
        entityTypes.put("organization", OntologicalClasses.DBPEDIA_ORGANISATION);
    }
    
    private OpenNLP openNLP;

    private final String defaultLang;

    private final Set<String> processedLangs;
    
    /** Comments about our models */
    public static final Map<String, String> DATA_FILE_COMMENTS;
    static {
        DATA_FILE_COMMENTS = new HashMap<String, String>();
        DATA_FILE_COMMENTS.put("Default data files", "provided by the org.apache.stanbol.defaultdata bundle");
    }

    public NEREngineCore(OpenNLP openNLP, String defaultLanguage, Set<String> processedLanguages) throws InvalidFormatException, IOException{
        this.openNLP = openNLP;
        this.defaultLang = defaultLanguage;
        this.processedLangs = Collections.unmodifiableSet(processedLanguages);
    }
    
    NEREngineCore(DataFileProvider dfp,String defaultLanguage, Set<String> processedLanguages) throws InvalidFormatException, IOException {
        this(new OpenNLP(dfp),defaultLanguage,processedLanguages);
    }

//    protected TokenNameFinderModel buildNameModel(String name, UriRef typeUri) throws IOException {
//        //String modelRelativePath = String.format("en-ner-%s.bin", name);
//        TokenNameFinderModel model = openNLP.getNameModel(name, "en");
//        // register the name finder instances for matching owl class
////        entityTypes.put(name, new Object[] {typeUri, model});
//        return model;
//    }
    @Override
    public String getName() {
        return getClass().getName();
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        //first check the langauge before processing the content (text)
        String language = extractLanguage(ci);
        if(language == null){
            throw new IllegalStateException("Unable to extract Language for "
                + "ContentItem "+ci.getUri()+": This is also checked in the canEnhance "
                + "method! -> This indicated an Bug in the implementation of the "
                + "EnhancementJobManager!");
        }
        if(!isProcessedLangage(language)){
            throw new IllegalStateException("The language '"+language+"' of ContentItem "+ci.getUri() 
                + " is not configured to be processed by this NER engine instance "
                + "(processed "+processedLangs+"): This is also checked in the canEnhance "
                + "method! -> This indicated an Bug in the implementation of the "
                + "EnhancementJobManager!");
        }
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);
        if(contentPart == null){
            throw new IllegalStateException("No ContentPart with Mimetype '"
                + TEXT_PLAIN_MIMETYPE+"' found for ContentItem "+ci.getUri()
                + ": This is also checked in the canEnhance method! -> This "
                + "indicated an Bug in the implementation of the "
                + "EnhancementJobManager!");
        }
        String text;
        try {
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }
        if (text.trim().length() == 0) {
            // TODO: make the length of the data a field of the ContentItem
            // interface to be able to filter out empty items in the canEnhance
            // method
            log.warn("ContentPart {} of ContentItem {} does not contain any text" +
            		"to extract knowledge from in ContentItem {}", 
            		contentPart.getKey(),ci);
            return;
        }
        log.debug("computeEnhancements from ContentPart {} of ContentItem {}: text={}",
            new Object[]{contentPart.getKey(),ci.getUri().getUnicodeString(), 
                         StringUtils.abbreviate(text, 100)});
        try {
            for (Map.Entry<String,UriRef> type : entityTypes.entrySet()) {
                String typeLabel = type.getKey();
                UriRef typeUri = type.getValue();
                TokenNameFinderModel nameFinderModel = openNLP.getNameModel(typeLabel, language);
                if(nameFinderModel == null){
                    log.info("No NER Model for {} and language {} available!",typeLabel,language);
                } else {
                    findNamedEntities(ci, text, language, typeUri, typeLabel, nameFinderModel);
                }
            }
        } catch (Exception e) {
            throw new EngineException(this, ci, e);
        }
    }

    protected void findNamedEntities(final ContentItem ci,
                                     final String text,
                                     final String lang,
                                     final UriRef typeUri,
                                     final String typeLabel,
                                     final TokenNameFinderModel nameFinderModel) {

        if (ci == null) {
            throw new IllegalArgumentException("Parsed ContentItem MUST NOT be NULL");
        }
        if (text == null) {
            log.warn("NULL was parsed as text for content item " + ci.getUri().getUnicodeString() + "! -> call ignored");
            return;
        }
        final Language language;
        if(lang != null && !lang.isEmpty()){
            language = new Language(lang);
        } else {
            language = null;
        }
        log.debug("findNamedEntities typeUri={}, type={}, text=", 
                new Object[]{ typeUri, typeLabel, StringUtils.abbreviate(text, 100) });
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        MGraph g = ci.getMetadata();
        Map<String,List<NameOccurrence>> entityNames = extractNameOccurrences(nameFinderModel, text);
        //lock the ContentItem while writing the RDF data for found Named Entities
        ci.getLock().writeLock().lock();
        try {
            Map<String,UriRef> previousAnnotations = new LinkedHashMap<String,UriRef>();
            for (Map.Entry<String,List<NameOccurrence>> nameInContext : entityNames.entrySet()) {
    
                String name = nameInContext.getKey();
                List<NameOccurrence> occurrences = nameInContext.getValue();
    
                UriRef firstOccurrenceAnnotation = null;
    
                for (NameOccurrence occurrence : occurrences) {
                    UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
                    g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, 
                        new PlainLiteralImpl(name, language)));
                    g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_CONTEXT, 
                        new PlainLiteralImpl(occurrence.context, language)));
                    g.add(new TripleImpl(textAnnotation, DC_TYPE, typeUri));
                    g.add(new TripleImpl(textAnnotation, ENHANCER_CONFIDENCE, literalFactory
                            .createTypedLiteral(occurrence.confidence)));
                    if (occurrence.start != null && occurrence.end != null) {
                        g.add(new TripleImpl(textAnnotation, ENHANCER_START, literalFactory
                                .createTypedLiteral(occurrence.start)));
                        g.add(new TripleImpl(textAnnotation, ENHANCER_END, literalFactory
                                .createTypedLiteral(occurrence.end)));
                    }
    
                    // add the subsumption relationship among occurrences of the same
                    // name
                    if (firstOccurrenceAnnotation == null) {
                        // check already extracted annotations to find a first most
                        // specific occurrence
                        for (Map.Entry<String,UriRef> entry : previousAnnotations.entrySet()) {
                            if (entry.getKey().contains(name)) {
                                // we have found a most specific previous
                                // occurrence, use it as subsumption target
                                firstOccurrenceAnnotation = entry.getValue();
                                g.add(new TripleImpl(textAnnotation, DC_RELATION, firstOccurrenceAnnotation));
                                break;
                            }
                        }
                        if (firstOccurrenceAnnotation == null) {
                            // no most specific previous occurrence, I am the first,
                            // most specific occurrence to be later used as a target
                            firstOccurrenceAnnotation = textAnnotation;
                            previousAnnotations.put(name, textAnnotation);
                        }
                    } else {
                        // I am referring to a most specific first occurrence of the
                        // same name
                        g.add(new TripleImpl(textAnnotation, DC_RELATION, firstOccurrenceAnnotation));
                    }
                }
            }
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    public Collection<String> extractPersonNames(String text) {
        return extractNames(getNameModel("person","en"),text);
    }

    public Collection<String> extractLocationNames(String text) {
        return extractNames(getNameModel("location","en"), text);
    }

    public Collection<String> extractOrganizationNames(String text) {
        return extractNames(getNameModel("organization","en"), text);
    }

    public Map<String,List<NameOccurrence>> extractPersonNameOccurrences(String text) {
        return extractNameOccurrences(getNameModel("person","en"), text);
    }

    public Map<String,List<NameOccurrence>> extractLocationNameOccurrences(String text) {
        return extractNameOccurrences(getNameModel("location","en"), text);
    }

    public Map<String,List<NameOccurrence>> extractOrganizationNameOccurrences(String text) {
        return extractNameOccurrences(getNameModel("organization","en"), text);
    }

    protected Collection<String> extractNames(TokenNameFinderModel nameFinderModel, String text) {
        return extractNameOccurrences(nameFinderModel, text).keySet();
    }

    /**
     * Gets/builds a TokenNameFinderModel by using {@link #openNLP} and throws
     * {@link IllegalStateException}s in case the model could not be built or
     * the data for the model where not found.
     * @param the type of the named finder model
     * @param language the language for the model
     * @return the model or an {@link IllegalStateException} if not available
     */
    private TokenNameFinderModel getNameModel(String type,String language) {
        try {
            TokenNameFinderModel model = openNLP.getNameModel(type, language);
            if(model != null){
                return model;
            } else {
                throw new IllegalStateException(String.format(
                    "Unable to built Model for extracting %s from '%s' language " +
                    "texts because the model data could not be loaded.",
                    type,language));
            }
        } catch (InvalidFormatException e) {
            throw new IllegalStateException(String.format(
                "Unable to built Model for extracting %s from '%s' language texts.",
                type,language),e);
        } catch (IOException e) {
            throw new IllegalStateException(String.format(
                "Unable to built Model for extracting %s from '%s' language texts.",
                type,language),e);
        }
    }
    private SentenceModel getSentenceModel(String language) {
        try {
            SentenceModel model = openNLP.getSentenceModel(language);
            if(model != null){
                return model;
            } else {
                throw new IllegalStateException(String.format(
                    "Unable to built Model for extracting sentences from '%s' " +
                    "language texts because the model data could not be loaded.",
                    language));
            }
        } catch (InvalidFormatException e) {
            throw new IllegalStateException(String.format(
                "Unable to built Model for extracting sentences from '%s' language texts.",
                language),e);
        } catch (IOException e) {
            throw new IllegalStateException(String.format(
                "Unable to built Model for extracting sentences from '%s' language texts.",
                language),e);
        }
    }
    
    protected Map<String,List<NameOccurrence>> extractNameOccurrences(TokenNameFinderModel nameFinderModel,
                                                                      String text) {

        // version with explicit sentence endings to reflect heading / paragraph
        // structure of an HTML or PDF document converted to text
        String textWithDots = text.replaceAll("\\n\\n", ".\n");
        text = removeNonUtf8CompliantCharacters(text);

        SentenceDetectorME sentenceDetector = new SentenceDetectorME(getSentenceModel("en"));

        Span[] sentenceSpans = sentenceDetector.sentPosDetect(textWithDots);

        NameFinderME finder = new NameFinderME(nameFinderModel);
        Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
        Map<String,List<NameOccurrence>> nameOccurrences = new LinkedHashMap<String,List<NameOccurrence>>();
        for (int i = 0; i < sentenceSpans.length; i++) {
            String sentence = sentenceSpans[i].getCoveredText(text).toString().trim();

            // build a context by concatenating three sentences to be used for
            // similarity ranking / disambiguation + contextual snippet in the
            // extraction structure
            List<String> contextElements = new ArrayList<String>();
            if (i > 0) {
                CharSequence previousSentence = sentenceSpans[i - 1].getCoveredText(text);
                contextElements.add(previousSentence.toString().trim());
            }
            contextElements.add(sentence.toString().trim());
            if (i + 1 < sentenceSpans.length) {
                CharSequence nextSentence = sentenceSpans[i + 1].getCoveredText(text);
                contextElements.add(nextSentence.toString().trim());
            }
            String context = StringUtils.join(contextElements, " ");

            // extract the names in the current sentence and
            // keep them store them with the current context
            Span[] tokenSpans = tokenizer.tokenizePos(sentence);
            String[] tokens = Span.spansToStrings(tokenSpans, sentence);
            Span[] nameSpans = finder.find(tokens);
            double[] probs = finder.probs();
            String[] names = Span.spansToStrings(nameSpans, tokens);
            //int lastStartPosition = 0;
            for (int j = 0; j < names.length; j++) {
                String name = names[j];
                Double confidence = 1.0;
                for (int k = nameSpans[j].getStart(); k < nameSpans[j].getEnd(); k++) {
                    confidence *= probs[k];
                }
                int start = tokenSpans[nameSpans[j].getStart()].getStart();
                int absoluteStart = sentenceSpans[i].getStart() + start;
                int absoluteEnd = absoluteStart + name.length();
                NameOccurrence occurrence = new NameOccurrence(name, absoluteStart, absoluteEnd, context,
                        confidence);

                List<NameOccurrence> occurrences = nameOccurrences.get(name);
                if (occurrences == null) {
                    occurrences = new ArrayList<NameOccurrence>();
                }
                occurrences.add(occurrence);
                nameOccurrences.put(name, occurrences);
            }
        }
        finder.clearAdaptiveData();
        log.debug("{} name occurrences found: {}", nameOccurrences.size(), nameOccurrences);
        return nameOccurrences;
    }

    public int canEnhance(ContentItem ci) {
        if(ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES) != null 
                && isProcessedLangage(extractLanguage(ci))){
                return ENHANCE_ASYNC; //The NER engine now supports Async processing!
        }
        return CANNOT_ENHANCE;
    }

    /**
     * Remove non UTF-8 compliant characters (typically control characters) so has to avoid polluting the
     * annotation graph with snippets that are not serializable as XML.
     */
    protected static String removeNonUtf8CompliantCharacters(final String text) {
        if (null == text) {
            return null;
        }
        Charset UTF8 = Charset.forName("UTF-8");
        byte[] bytes = text.getBytes(UTF8);
        for (int i = 0; i < bytes.length; i++) {
            byte ch = bytes[i];
            // remove any characters outside the valid UTF-8 range as well as all control characters
            // except tabs and new lines
            if (!((ch > 31 && ch < 253) || ch == '\t' || ch == '\n' || ch == '\r')) {
                bytes[i] = ' ';
            }
        }
        return new String(bytes, UTF8);
    }

    /**
     * The default language
     * @return the defaultLang
     */
    public String getDefaultLanguage() {
        return defaultLang;
    }
    /**
     * Checks if the parsed language is enabled for processing.
     * If <code>null</code> is parsed as language this returns <code>false</code>
     * even if processing of all languages is enabled. <p>
     * NOTE: If this Method returns <code>true</code> this does
     * not mean that text with this language can be actually processed because this
     * also requires that the NER model for this language are available via the
     * parsed {@link OpenNLP} instance.
     * @param lang the language
     * @return the state
     */
    public boolean isProcessedLangage(String lang){
        return lang != null && (processedLangs.isEmpty() || processedLangs.contains(lang));
    }
    /*
     * The following Utility extracts the language from the metadata of the
     * parsed Content Item.
     * This Utility is actually a copy of the same form the KeywordExtractionEngine.
     * TODO: change this to a global Utility as soon as STANBOL Enhancement
     * Structure is defined
     */
    /**
     * The literal representing the LangIDEngine as creator.
     */
    public static final Literal LANG_ID_ENGINE_NAME = LiteralFactory.getInstance().createTypedLiteral("org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine");
    /**
     * Extracts the language of the parsed ContentItem from the metadata
     * @param ci the content item
     * @return the language
     */
    private String extractLanguage(ContentItem ci) {
        MGraph metadata = ci.getMetadata();
        Iterator<Triple> langaugeEnhancementCreatorTriples = 
            metadata.filter(null, Properties.DC_CREATOR, LANG_ID_ENGINE_NAME);
        if(langaugeEnhancementCreatorTriples.hasNext()){
            String lang = EnhancementEngineHelper.getString(metadata, 
                langaugeEnhancementCreatorTriples.next().getSubject(), 
                Properties.DC_LANGUAGE);
            if(lang != null){
                return lang;
            } else {
                log.info("Unable to extract language for ContentItem %s! The Enhancement of the %s is missing the %s property",
                    new Object[]{ci.getUri().getUnicodeString(),LANG_ID_ENGINE_NAME.getLexicalForm(),Properties.DC_LANGUAGE});
                log.info(" ... return '{}' as default",defaultLang);
                return defaultLang;
            }
        } else {
            log.warn("Unable to extract language for ContentItem %s! Is the %s active?",
                ci.getUri().getUnicodeString(),LANG_ID_ENGINE_NAME.getLexicalForm());
            log.warn(" ... return '{}' as default",defaultLang);
            return defaultLang;
        }
    }
}
