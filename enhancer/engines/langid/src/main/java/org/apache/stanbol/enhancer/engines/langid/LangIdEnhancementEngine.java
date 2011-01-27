package org.apache.stanbol.enhancer.engines.langid;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.knallgrau.utils.textcat.TextCategorizer;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.NIE_PLAINTEXTCONTENT;

/**
 * {@link LangIdEnhancementEngine} provides functionality to enhance document
 * with their language.
 *
 * @author Joerg Steffen, DFKI
 * @version $Id$
 */
@Component(immediate = true, metatype = true)
@Service
public class LangIdEnhancementEngine implements EnhancementEngine, ServiceProperties {

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING}
     */
    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING - 2;

    /**
     * This contains the only supported MIME type of this enhancement engine.
     */
    private static final String TEXT_PLAIN_MIMETYPE = "text/plain";

    /**
     * This contains the logger.
     */
    private static final Logger log = LoggerFactory.getLogger(LangIdEnhancementEngine.class);

    /**
     * This contains the language identifier.
     */
    private TextCategorizer languageIdentifier;

    private static final int PROBE_LENGTH_DEFAULT = 400;

    @Property
    public static final String PROBE_LENGTH_PROP = "org.apache.stanbol.enhancer.engines.langid.probe-length";

    /**
     * How much text should be used for testing: If the value is 0 or smaller, the complete text will be used. Otherwise a text probe of the given length is taken from the middle of the text. The default length is 400 characters.
     */
    private int probeLength = PROBE_LENGTH_DEFAULT;

    /**
     * The activate method.
     *
     * @param ce the {@link ComponentContext}
     */
    protected void activate(@SuppressWarnings("unused") ComponentContext ce) {
        if (ce != null) {
            Dictionary<String, String> properties = ce.getProperties();
            String lengthVal = properties.get(PROBE_LENGTH_PROP);
            probeLength = lengthVal == null ? PROBE_LENGTH_DEFAULT : Integer.parseInt(lengthVal);
        }
        languageIdentifier = new TextCategorizer();
    }

    /**
     * The deactivate method.
     *
     * @param ce the {@link ComponentContext}
     */
    protected void deactivate(@SuppressWarnings("unused") ComponentContext ce) {
        languageIdentifier = null;
    }

    public int canEnhance(ContentItem ci) throws EngineException {
        String mimeType = ci.getMimeType().split(";", 2)[0];
        if (TEXT_PLAIN_MIMETYPE.equalsIgnoreCase(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
        }

        // TODO: check whether there is the graph contains the text
        UriRef subj = new UriRef(ci.getId());
        Iterator<Triple> it = ci.getMetadata().filter(subj, NIE_PLAINTEXTCONTENT, null);
        if (it.hasNext()) {
            return ENHANCE_SYNCHRONOUS;
        }
        return CANNOT_ENHANCE;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        String text = "";
        if (TEXT_PLAIN_MIMETYPE.equals(ci.getMimeType())) {
            try {
                text = IOUtils.toString(ci.getStream());
            } catch (IOException e) {
                throw new InvalidContentException(this, ci, e);
            }
        } else {
            Iterator<Triple> it = ci.getMetadata().filter(new UriRef(ci.getId()), NIE_PLAINTEXTCONTENT, null);
            while (it.hasNext()) {
                text += it.next().getObject();
            }
        }
        if (text.trim().length() == 0) {
            log.warn("no text found");
            return;
        }

        // truncate text to some piece from the middle if probeLength > 0
        int checkLength = probeLength;
        if (checkLength > 0 && text.length() > checkLength) {
            text = text.substring(text.length() / 2 - checkLength / 2, text.length() / 2 + checkLength / 2);
        }
        String language = languageIdentifier.categorize(text);
        log.info("language identified as " + language);

        // add language to metadata
        MGraph g = ci.getMetadata();
        UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
        g.add(new TripleImpl(textEnhancement, DC_LANGUAGE, new PlainLiteralImpl(language)));
    }

    public int getProbeLength() {
        return probeLength;
    }

    public void setProbeLength(int probeLength) {
        this.probeLength = probeLength;
    }

    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    }

}
