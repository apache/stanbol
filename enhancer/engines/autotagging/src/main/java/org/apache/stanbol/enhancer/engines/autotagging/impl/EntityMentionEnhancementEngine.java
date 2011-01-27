package org.apache.stanbol.enhancer.engines.autotagging.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.autotagging.Autotagger;
import org.apache.stanbol.autotagging.TagInfo;
import org.apache.stanbol.enhancer.engines.autotagging.AutotaggerProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

/**
 * Engine that uses an AutotaggerProvider to process existing TextAnnotations of
 * an Content Item and searches for related Entities by using the Autotagger
 *
 * @author ogrisel, rwesten
 */
@Component(immediate = true, metatype = true)
@Service
public class EntityMentionEnhancementEngine implements EnhancementEngine,
        ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    AutotaggerProvider autotaggerProvider;

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link EnhancementJobManager#DEFAULT_ORDER}
     */
    public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;

    public void computeEnhancements(ContentItem ci) throws EngineException {
        Autotagger autotagger = autotaggerProvider.getAutotagger();
        if (autotagger == null) {
            log.warn(getClass().getSimpleName()
                    + " is deactivated: cannot process content item: "
                    + ci.getId());
            return;
        }
        UriRef contentItemId = new UriRef(ci.getId());

        MGraph graph = ci.getMetadata();
        LiteralFactory literalFactory = LiteralFactory.getInstance();

        // Retrieve the existing text annotations
        Map<UriRef, List<UriRef>> textAnnotations = new HashMap<UriRef, List<UriRef>>();
        for (Iterator<Triple> it = graph.filter(null, RDF_TYPE,
                ENHANCER_TEXTANNOTATION); it.hasNext();) {
            UriRef uri = (UriRef) it.next().getSubject();
            if (graph.filter(uri, DC_RELATION, null).hasNext()) {
                // this is not the most specific occurrence of this name: skip
                continue;
            }
            // This is a first occurrence, collect any subsumed annotations
            List<UriRef> subsumed = new ArrayList<UriRef>();
            for (Iterator<Triple> it2 = graph.filter(null,
                    DC_RELATION, uri); it2.hasNext();) {
                subsumed.add((UriRef) it2.next().getSubject());
            }
            textAnnotations.put(uri, subsumed);
        }

        for (Map.Entry<UriRef, List<UriRef>> entry : textAnnotations.entrySet()) {
            try {
                computeEntityRecommendations(autotagger, literalFactory, graph, contentItemId, entry.getKey(), entry.getValue());
            } catch (IOException e) {
                throw new EngineException(this, ci, e);
            }
        }
    }

    protected final Collection<TagInfo> computeEntityRecommendations(Autotagger autotagger, LiteralFactory literalFactory, MGraph graph, UriRef contentItemId, UriRef textAnnotation, List<UriRef> subsumedAnnotations) throws IOException {
        // First get the required properties for the parsed textAnnotation
        // ... and check the values
        String name = EnhancementEngineHelper.getString(graph, textAnnotation,
                ENHANCER_SELECTED_TEXT);
        if (name == null) {
            log.warn("Unable to process TextAnnotation " + textAnnotation
                    + " because property" + ENHANCER_SELECTED_TEXT
                    + " is not present");
            return Collections.emptyList();
        }
        String context = EnhancementEngineHelper.getString(graph,
                textAnnotation, ENHANCER_SELECTION_CONTEXT);
        if (context == null) {
            log.warn("Unable to process TextAnnotation " + textAnnotation
                    + " because property" + ENHANCER_SELECTION_CONTEXT
                    + " is not present");
            return Collections.emptyList();
        }

        // aggregate context from subsumed entries:
        for (NonLiteral subsumedAnnotation : subsumedAnnotations) {
            String otherContext = EnhancementEngineHelper.getString(graph,
                    subsumedAnnotation, ENHANCER_SELECTION_CONTEXT);
            if (otherContext != null) {
                context += " " + otherContext;
            }
        }

        UriRef type = EnhancementEngineHelper.getReference(graph,
                textAnnotation, DC_TYPE);
        if (type == null) {
            log.warn("Unable to process TextAnnotation " + textAnnotation
                    + " because property" + DC_TYPE
                    + " is not present");
            return Collections.emptyList();
        }

        log.debug("Process TextAnnotation " + name + " type=" + type);

        // this is a name lookup + context for disambiguation.
        List<TagInfo> matchingEntities = autotagger.suggestForType(name,
                context, type.getUnicodeString());

        List<NonLiteral> annotationsToRelate = new ArrayList<NonLiteral>();
        annotationsToRelate.add(textAnnotation);
        annotationsToRelate.addAll(subsumedAnnotations);

        for (TagInfo guess : matchingEntities) {
            EnhancementRDFUtils.writeEntityAnnotation(this, literalFactory,
                    graph, contentItemId, annotationsToRelate, guess);
        }
        return matchingEntities;
    }

    public int canEnhance(ContentItem ci) {
        /*
         * This engine consumes existing enhancements because of that it can
         * enhance any type of ci! TODO: It would also be possible to check here
         * if there is an TextAnnotation and use that as result!
         */
        return ENHANCE_SYNCHRONOUS;
    }

    public void bindAutotaggerProvider(AutotaggerProvider autotaggerProvider) {
        this.autotaggerProvider = autotaggerProvider;
    }

    public void unbindAutotaggerProvider(AutotaggerProvider autotaggerProvider) {
        this.autotaggerProvider = null;
    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING,
                (Object) defaultOrder));
    }
}
