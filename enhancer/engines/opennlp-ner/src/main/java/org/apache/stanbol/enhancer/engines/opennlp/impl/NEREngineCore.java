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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core of our EnhancementEngine, separated from the OSGi service to make it easier to test this.
 */
public class NEREngineCore implements EnhancementEngine {
    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DataFileProvider dataFileProvider;
    private final String bundleSymbolicName;
    protected final SentenceModel sentenceModel;
    protected final TokenNameFinderModel personNameModel;
    protected final TokenNameFinderModel locationNameModel;
    protected final TokenNameFinderModel organizationNameModel;
    protected Map<String,Object[]> entityTypes = new HashMap<String,Object[]>();

    /** Comments about our models */
    public static final Map<String, String> DATA_FILE_COMMENTS;
    static {
        DATA_FILE_COMMENTS = new HashMap<String, String>();
        DATA_FILE_COMMENTS.put("Default data files", "provided by the org.apache.stanbol.defaultdata bundle");
    }

    NEREngineCore(DataFileProvider dfp, String bundleSymbolicName) throws InvalidFormatException, IOException {
        dataFileProvider = dfp;
        this.bundleSymbolicName = bundleSymbolicName;
        sentenceModel = new SentenceModel(lookupModelStream("en-sent.bin"));
        personNameModel = buildNameModel("person", OntologicalClasses.DBPEDIA_PERSON);
        locationNameModel = buildNameModel("location", OntologicalClasses.DBPEDIA_PLACE);
        organizationNameModel = buildNameModel("organization", OntologicalClasses.DBPEDIA_ORGANISATION);
    }

    protected InputStream lookupModelStream(String modelRelativePath) throws IOException {
        return dataFileProvider.getInputStream(bundleSymbolicName, modelRelativePath, DATA_FILE_COMMENTS);
    }

    protected TokenNameFinderModel buildNameModel(String name, UriRef typeUri) throws IOException {
        String modelRelativePath = String.format("en-ner-%s.bin", name);
        TokenNameFinderModel model = new TokenNameFinderModel(lookupModelStream(modelRelativePath));
        // register the name finder instances for matching owl class
        entityTypes.put(name, new Object[] {typeUri, model});
        return model;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        String text;
        try {
            text = IOUtils.toString(ci.getStream(), "UTF-8");
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }
        if (text.trim().length() == 0) {
            // TODO: make the length of the data a field of the ContentItem
            // interface to be able to filter out empty items in the canEnhance
            // method
            log.warn("nothing to extract knowledge from in ContentItem {}", ci);
            return;
        }
        log.debug("computeEnhancements {} text={}", ci.getId(), StringUtils.abbreviate(text, 100));

        try {
            for (Map.Entry<String,Object[]> type : entityTypes.entrySet()) {
                String typeLabel = type.getKey();
                Object[] typeInfo = type.getValue();
                UriRef typeUri = (UriRef) typeInfo[0];
                TokenNameFinderModel nameFinderModel = (TokenNameFinderModel) typeInfo[1];
                findNamedEntities(ci, text, typeUri, typeLabel, nameFinderModel);
            }
        } catch (Exception e) {
            throw new EngineException(this, ci, e);
        }
    }

    protected void findNamedEntities(final ContentItem ci,
                                     final String text,
                                     final UriRef typeUri,
                                     final String typeLabel,
                                     final TokenNameFinderModel nameFinderModel) {

        if (ci == null) {
            throw new IllegalArgumentException("Parsed ContentItem MUST NOT be NULL");
        }
        if (text == null) {
            log.warn("NULL was parsed as text for content item " + ci.getId() + "! -> call ignored");
            return;
        }
        log.debug("findNamedEntities typeUri={}, type={}, text=", 
                new Object[]{ typeUri, typeLabel, StringUtils.abbreviate(text, 100) });
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        MGraph g = ci.getMetadata();
        Map<String,List<NameOccurrence>> entityNames = extractNameOccurrences(nameFinderModel, text);

        Map<String,UriRef> previousAnnotations = new LinkedHashMap<String,UriRef>();
        for (Map.Entry<String,List<NameOccurrence>> nameInContext : entityNames.entrySet()) {

            String name = nameInContext.getKey();
            List<NameOccurrence> occurrences = nameInContext.getValue();

            UriRef firstOccurrenceAnnotation = null;

            for (NameOccurrence occurrence : occurrences) {
                UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
                g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTED_TEXT, literalFactory
                        .createTypedLiteral(name)));
                g.add(new TripleImpl(textAnnotation, ENHANCER_SELECTION_CONTEXT, literalFactory
                        .createTypedLiteral(occurrence.context)));
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
    }

    public Collection<String> extractPersonNames(String text) {
        return extractNames(personNameModel, text);
    }

    public Collection<String> extractLocationNames(String text) {
        return extractNames(locationNameModel, text);
    }

    public Collection<String> extractOrganizationNames(String text) {
        return extractNames(organizationNameModel, text);
    }

    public Map<String,List<NameOccurrence>> extractPersonNameOccurrences(String text) {
        return extractNameOccurrences(personNameModel, text);
    }

    public Map<String,List<NameOccurrence>> extractLocationNameOccurrences(String text) {
        return extractNameOccurrences(locationNameModel, text);
    }

    public Map<String,List<NameOccurrence>> extractOrganizationNameOccurrences(String text) {
        return extractNameOccurrences(organizationNameModel, text);
    }

    protected Collection<String> extractNames(TokenNameFinderModel nameFinderModel, String text) {
        return extractNameOccurrences(nameFinderModel, text).keySet();
    }

    protected Map<String,List<NameOccurrence>> extractNameOccurrences(TokenNameFinderModel nameFinderModel,
                                                                      String text) {

        // version with explicit sentence endings to reflect heading / paragraph
        // structure of an HTML or PDF document converted to text
        String textWithDots = text.replaceAll("\\n\\n", ".\n");
        text = removeNonUtf8CompliantCharacters(text);

        SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentenceModel);

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
            String[] tokens = tokenizer.tokenize(sentence);
            Span[] nameSpans = finder.find(tokens);
            double[] probs = finder.probs();
            String[] names = Span.spansToStrings(nameSpans, tokens);
            int lastStartPosition = 0;
            for (int j = 0; j < names.length; j++) {
                String name = names[j];
                Double confidence = 1.0;
                for (int k = nameSpans[j].getStart(); k < nameSpans[j].getEnd(); k++) {
                    confidence *= probs[k];
                }
                int start = sentence.substring(lastStartPosition).indexOf(name);
                Integer absoluteStart = null;
                Integer absoluteEnd = null;
                if (start != -1) {
                    /*
                     * NOTE (rw, issue 19, 20100615) Here we need to set the new start position, by adding the
                     * current start to the lastStartPosion. we need also to use the lastStartPosition to
                     * calculate the start of the element. The old code had not worked if names contains more
                     * than a single element!
                     */
                    lastStartPosition += start;
                    absoluteStart = sentenceSpans[i].getStart() + lastStartPosition;
                    absoluteEnd = absoluteStart + name.length();
                }
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
        // in case text/pain;charSet=UTF8 is parsed
        String mimeType = ci.getMimeType().split(";", 2)[0];
        if (TEXT_PLAIN_MIMETYPE.equalsIgnoreCase(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
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
        byte[] bytes = text.getBytes(Charset.forName("UTF-8"));
        for (int i = 0; i < bytes.length; i++) {
            byte ch = bytes[i];
            // remove any characters outside the valid UTF-8 range as well as all control characters
            // except tabs and new lines
            if (!((ch > 31 && ch < 253) || ch == '\t' || ch == '\n' || ch == '\r')) {
                bytes[i] = ' ';
            }
        }
        return new String(bytes);
    }
}
