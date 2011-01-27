package org.apache.stanbol.enhancer.engines.autotagging.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.autotagging.Autotagger;
import org.apache.stanbol.autotagging.TagInfo;
import org.apache.stanbol.enhancer.engines.autotagging.AutotaggerProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * OSGi wrapper for the iks-autotagging library. Uses a lucene index of DBpedia
 * to suggest related related topics out of the text content of the
 * content item.
 *
 * @author ogrisel
 */
@Component(immediate = true, metatype = true)
@Service
public class RelatedTopicEnhancementEngine implements EnhancementEngine {

    protected static final String TEXT_PLAIN_MIMETYPE = "text/plain";

    private final Logger log = LoggerFactory.getLogger(getClass());

    // TODO: make me configurable through an OSGi property
    protected String type = "http://www.w3.org/2004/02/skos/core#Concept";

    @Reference
    AutotaggerProvider autotaggerProvider;

    public void setType(String type) {
        this.type = type;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        Autotagger autotagger = autotaggerProvider.getAutotagger();
        if (autotagger == null) {
            log.warn(getClass().getSimpleName()
                    + " is deactivated: cannot process content item: "
                    + ci.getId());
            return;
        }
        String text;
        try {
            text = IOUtils.toString(ci.getStream());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }
        if (text.trim().length() == 0) {
            // TODO: make the length of the data a field of the ContentItem
            // interface to be able to filter out empty items in the canEnhance
            // method
            log.warn("nothing to extract a topic from");
            return;
        }

        MGraph graph = ci.getMetadata();
        LiteralFactory literalFactory = LiteralFactory.getInstance();
        UriRef contentItemId = new UriRef(ci.getId());
        try {
            List<TagInfo> suggestions = autotagger.suggestForType(text, type);
            Collection<NonLiteral> noRelatedEnhancements = Collections.emptyList();
            for (TagInfo tag : suggestions) {
                EnhancementRDFUtils.writeEntityAnnotation(this, literalFactory,
                        graph, contentItemId,
                        noRelatedEnhancements, tag);
            }
        } catch (IOException e) {
            throw new EngineException(this, ci, e);
        }
    }

    public int canEnhance(ContentItem ci) {
           String mimeType = ci.getMimeType().split(";",2)[0];
        if (TEXT_PLAIN_MIMETYPE.equalsIgnoreCase(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
        }
        return CANNOT_ENHANCE;
    }

    public void bindAutotaggerProvider(AutotaggerProvider autotaggerProvider) {
        this.autotaggerProvider = autotaggerProvider;
    }
}
