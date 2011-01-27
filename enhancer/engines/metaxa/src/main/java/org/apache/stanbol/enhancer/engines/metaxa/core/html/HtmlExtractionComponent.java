package org.apache.stanbol.enhancer.engines.metaxa.core.html;

import java.util.Map;

import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.w3c.dom.Document;

/**
 * HtmlExtractionComponent.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 *
 */
public interface HtmlExtractionComponent {

    void extract(String id, Document doc, Map<String, Object> params, RDFContainer result)
            throws ExtractorException;

}
