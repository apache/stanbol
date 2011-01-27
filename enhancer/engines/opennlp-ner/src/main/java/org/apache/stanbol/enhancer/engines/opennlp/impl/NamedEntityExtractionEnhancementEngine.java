package org.apache.stanbol.enhancer.engines.opennlp.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import opennlp.maxent.GISModel;
import opennlp.maxent.io.BinaryGISModelReader;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.autotagging.AutotaggerProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;


import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;

/**
 * Apache Stanbol Enhancer Named Entity Recognition enhancement engine based on 
 * opennlp's Maximum Entropy models and a DBpedia index for optionally matching 
 * them to well know DBpedia entities.
 */
@Component(immediate = true, metatype = true)
@Service
public class NamedEntityExtractionEnhancementEngine implements
        EnhancementEngine, ServiceProperties {

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     */
    public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION;

    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";

    @Property
    public static final String MODELS_PATH = "org.apache.stanbol.enhancer.engines.opennlp.models.path";

    public static final Log log = LogFactory.getLog(NamedEntityExtractionEnhancementEngine.class);

    protected GISModel sentenceModel;

    protected GISModel personNameModel;

    protected GISModel locationNameModel;

    protected GISModel organizationNameModel;

    protected Map<String, Object[]> entityTypes = new HashMap<String, Object[]>();

    protected BundleContext bundleContext;

    @Reference
    protected AutotaggerProvider autotaggerProvider;

    // @Activate
    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext ce) throws IOException {
        bundleContext = ce.getBundleContext();

        String directoryPath = null;
        if (ce != null) {
            Dictionary<String, String> properties = ce.getProperties();
            directoryPath = properties.get(MODELS_PATH);
        }
        sentenceModel = loadModel(directoryPath,
                "english/sentdetect/EnglishSD.bin.gz");

        personNameModel = buildNameModel(directoryPath, "person",
                OntologicalClasses.DBPEDIA_PERSON);

        locationNameModel = buildNameModel(directoryPath, "location",
                OntologicalClasses.DBPEDIA_PLACE);

        organizationNameModel = buildNameModel(directoryPath, "organization",
                OntologicalClasses.DBPEDIA_ORGANISATION);
    }

    // @Deactivate
    protected void deactivate(ComponentContext ce) {
        sentenceModel = null;
        personNameModel = null;
        locationNameModel = null;
        organizationNameModel = null;
    }

    protected GISModel loadModel(String directoryPath, String modelRelativePath)
            throws IOException {

        ClassLoader loader = this.getClass().getClassLoader();
        if (directoryPath != null && directoryPath.length() > 0) {
            // load custom models from the provided FS directory
            File modelData = new File(new File(directoryPath),
                    modelRelativePath);
            return new BinaryGISModelReader(modelData).getModel();
        } else {
            // load default OpenNLP models from jars
            String resourcePath = "opennlp/" + modelRelativePath;
            InputStream in = null;
            if (autotaggerProvider != null) {
                // Lookup the OSGI bundle of the autotagger that embeds the
                // default opennlp models data: this is hackish, the
                // iks-autotagging project should be refactored to do all of
                // this by it-self
                URL entry = autotaggerProvider.getBundleContext().getBundle().getEntry(
                        resourcePath);
                in = entry != null ? entry.openStream() : null;
            } else {
                // regular classloading for the tests
                in = loader.getResourceAsStream(resourcePath);
            }
            if (in == null) {
                throw new IOException("coult not find resource: "
                        + resourcePath);
            }
            return new BinaryGISModelReader(new DataInputStream(
                    new GZIPInputStream(in))).getModel();
        }
    }

    protected GISModel buildNameModel(String directoryPath, String name,
            UriRef typeUri) throws IOException {
        String modelRelativePath = String.format("english/namefind/%s.bin.gz",
                name);
        GISModel model = loadModel(directoryPath, modelRelativePath);
        // register the name finder instances for matching owl class
        entityTypes.put(name, new Object[] { typeUri, model });
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
            log.warn("nothing to extract knowledge from");
            return;
        }

        try {
            for (Map.Entry<String, Object[]> type : entityTypes.entrySet()) {
                String typeLabel = type.getKey();
                Object[] typeInfo = type.getValue();
                UriRef typeUri = (UriRef) typeInfo[0];
                GISModel nameFinderModel = (GISModel) typeInfo[1];
                findNamedEntities(ci, text, typeUri, typeLabel, nameFinderModel);
            }
        } catch (Exception e) { // TODO: makes it sense to catch Exception here?
            throw new EngineException(this, ci, e);
        }
    }

    protected void findNamedEntities(final ContentItem ci, final String text,
            final UriRef typeUri, final String typeLabel,
            final GISModel nameFinderModel) {

        if (ci == null) {
            throw new IllegalArgumentException(
                    "Parsed ContentItem MUST NOT be NULL");
        }
        if (text == null) {
            log.warn("NULL was parsed as text for content item " + ci.getId()
                    + "! -> call ignored");
            return;
        }
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        MGraph g = ci.getMetadata();
        Map<String, List<NameOccurrence>> entityNames = extractNameOccurrences(
                nameFinderModel, text);

        Map<String, UriRef> previousAnnotations = new LinkedHashMap<String, UriRef>();
        for (Map.Entry<String, List<NameOccurrence>> nameInContext : entityNames.entrySet()) {

            String name = nameInContext.getKey();
            List<NameOccurrence> occurrences = nameInContext.getValue();

            UriRef firstOccurrenceAnnotation = null;

            for (NameOccurrence occurrence : occurrences) {
                UriRef textAnnotation = EnhancementEngineHelper.createTextEnhancement(
                        ci, this);
                g.add(new TripleImpl(textAnnotation,
                        ENHANCER_SELECTED_TEXT,
                        literalFactory.createTypedLiteral(name)));
                g.add(new TripleImpl(textAnnotation,
                        ENHANCER_SELECTION_CONTEXT,
                        literalFactory.createTypedLiteral(occurrence.context)));
                g.add(new TripleImpl(textAnnotation, DC_TYPE,
                        typeUri));
                g.add(new TripleImpl(
                        textAnnotation,
                        ENHANCER_CONFIDENCE,
                        literalFactory.createTypedLiteral(occurrence.confidence)));
                if (occurrence.start != null && occurrence.end != null) {
                    g.add(new TripleImpl(textAnnotation, ENHANCER_START,
                            literalFactory.createTypedLiteral(occurrence.start)));
                    g.add(new TripleImpl(textAnnotation, ENHANCER_END,
                            literalFactory.createTypedLiteral(occurrence.end)));
                }

                // add the subsumption relationship among occurrences of the same
                // name
                if (firstOccurrenceAnnotation == null) {
                    // check already extracted annotations to find a first most
                    // specific occurrence
                    for (Map.Entry<String, UriRef> entry : previousAnnotations.entrySet()) {
                        if (entry.getKey().contains(name)) {
                            // we have found a most specific previous
                            // occurrence, use it as subsumption target
                            firstOccurrenceAnnotation = entry.getValue();
                            g.add(new TripleImpl(textAnnotation,
                                    DC_RELATION,
                                    firstOccurrenceAnnotation));
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
                    g.add(new TripleImpl(textAnnotation,
                            DC_RELATION, firstOccurrenceAnnotation));
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

    public Map<String, List<NameOccurrence>> extractPersonNameOccurrences(
            String text) {
        return extractNameOccurrences(personNameModel, text);
    }

    public Map<String, List<NameOccurrence>> extractLocationNameOccurrences(
            String text) {
        return extractNameOccurrences(locationNameModel, text);
    }

    public Map<String, List<NameOccurrence>> extractOrganizationNameOccurrences(
            String text) {
        return extractNameOccurrences(organizationNameModel, text);
    }

    protected Collection<String> extractNames(GISModel nameFinderModel,
            String text) {
        return extractNameOccurrences(nameFinderModel, text).keySet();
    }

    protected Map<String, List<NameOccurrence>> extractNameOccurrences(
            GISModel nameFinderModel, String text) {

        // version with explicit sentence endings to reflect heading / paragraph
        // structure of an HTML or PDF document converted to text
        String textWithDots = text.replaceAll("\\n\\n", ".\n");

        SentenceDetectorME sentenceDetector = new SentenceDetectorME(
                sentenceModel);

        int[] sentenceEndings = sentenceDetector.sentPosDetect(textWithDots);
        int[] sentencePositions = new int[sentenceEndings.length + 1];
        sentencePositions[0] = 0;
        System.arraycopy(sentenceEndings, 0, sentencePositions, 1,
                sentenceEndings.length);
        String[] sentences = new String[sentenceEndings.length];
        for (int i = 0; i < sentences.length; i++) {
            log.debug(String.format("Sentence %d from char %d to %d", i,
                    sentencePositions[i], sentencePositions[i + 1]));
            sentences[i] = text.substring(sentencePositions[i],
                    sentencePositions[i + 1]);
            log.debug("Sentence: " + sentences[i]);
        }

        NameFinderME finder = new NameFinderME(nameFinderModel);

        Map<String, List<NameOccurrence>> nameOccurrences = new LinkedHashMap<String, List<NameOccurrence>>();
        Tokenizer tokenizer = new SimpleTokenizer();
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];

            // build a context by concatenating three sentences to be used for
            // similarity ranking / disambiguation + contextual snippet in the
            // extraction structure
            List<String> contextElements = new ArrayList<String>();
            if (i - 1 > 0) {
                String previousSentence = sentences[i - 1];
                contextElements.add(previousSentence.trim());
            }
            contextElements.add(sentence.trim());
            if (i + 1 < sentences.length) {
                String nextSentence = sentences[i + 1];
                contextElements.add(nextSentence.trim());
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
                     * NOTE (rw, issue 19, 20100615) Here we need to set the new
                     * start position, by adding the current start to the
                     * lastStartPosion. we need also to use the
                     * lastStartPosition to calculate the start of the element.
                     * The old code had not worked if names contains more than a
                     * single element!
                     */
                    lastStartPosition += start;
                    absoluteStart = sentencePositions[i] + lastStartPosition;
                    absoluteEnd = absoluteStart + name.length();
                }
                NameOccurrence occurrence = new NameOccurrence(name,
                        absoluteStart, absoluteEnd, context, confidence);

                List<NameOccurrence> occurrences = nameOccurrences.get(name);
                if (occurrences == null) {
                    occurrences = new ArrayList<NameOccurrence>();
                }
                occurrences.add(occurrence);
                nameOccurrences.put(name, occurrences);
            }
        }
        finder.clearAdaptiveData();

        if (log.isDebugEnabled()) {
            for (List<NameOccurrence> occurrences : nameOccurrences.values()) {
                log.debug("Occurrences found: "
                        + StringUtils.join(occurrences, ", "));
            }
        }
        return nameOccurrences;
    }

    public int canEnhance(ContentItem ci) {
        //in case text/pain;charSet=UTF8 is parsed
        String mimeType = ci.getMimeType().split(";",2)[0];
        if (TEXT_PLAIN_MIMETYPE.equalsIgnoreCase(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
        }
        return CANNOT_ENHANCE;
    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING,
                (Object) defaultOrder));
    }

}
