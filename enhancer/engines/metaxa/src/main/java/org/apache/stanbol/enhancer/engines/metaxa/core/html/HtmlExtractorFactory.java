package org.apache.stanbol.enhancer.engines.metaxa.core.html;

import org.semanticdesktop.aperture.extractor.Extractor;

/**
 * HtmlExtractorFactory.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 *
 */
public class HtmlExtractorFactory extends
        org.semanticdesktop.aperture.extractor.html.HtmlExtractorFactory {

    @Override
    public Extractor get() {
        return new IksHtmlExtractor();
    }

}
