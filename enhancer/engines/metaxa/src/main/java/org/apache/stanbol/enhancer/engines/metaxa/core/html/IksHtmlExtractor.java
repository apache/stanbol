package org.apache.stanbol.enhancer.engines.metaxa.core.html;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.semanticdesktop.aperture.extractor.Extractor;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.RDFContainerFactory;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * IksHtmlExtractor.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 */
public class IksHtmlExtractor implements Extractor {

    private static final Logger LOG = LoggerFactory.getLogger(IksHtmlExtractor.class);

    public static String DEFAULT_CONFIGURATION = "htmlextractors.xml";

    private static HtmlParser htmlParser = new HtmlParser();

    private static HtmlExtractionRegistry registry =
        new HtmlExtractionRegistry();
    static {
        try {
            registry.initialize(DEFAULT_CONFIGURATION);
        } catch (InitializationException e) {
            LOG.error("Registration Initialization Error: " + e.getMessage());
        }
    }

    public IksHtmlExtractor() {
    }

    public IksHtmlExtractor(String configFileName)
            throws InitializationException {
        this();
        registry = new HtmlExtractionRegistry(configFileName);
    }

    public void extract(URI id,
            InputStream input, Charset charset, String mimeType,
            RDFContainer result)
            throws ExtractorException {

        String encoding;
        if (charset == null) {
            if (!input.markSupported()) {
                input = new BufferedInputStream(input);
            }
            try {
                encoding = CharsetRecognizer.detect(input, "html", "UTF-8");
            } catch (IOException e) {
                LOG.error("Charset detection problem: " + e.getMessage());
                throw new ExtractorException("Charset detection problem: "
                    + e.getMessage());
            }
        }
        else {
            encoding = charset.name();
        }
        Document doc = htmlParser.getDOM(input, encoding);
        /*
         * This solves namespace problem but makes it difficult to handle normal
         * HTML and namespaced XHTML documents on a par. Rather avoid namespaces
         * in transformers for HTML elements! Problem remains that scripts then
         * cannot be tested offline Way out might be to use disjunctions in
         * scripts or ignore namespace by checking local-name() only
         * (match=*[local-name() = 'xxx']) Are Microformats, RDFa, ... only used
         * in XHTML? That would make the decision easier! Also have to solve the
         * problem how to connect/map SemanticDesktop ontologies with those from
         * the extractors String docText = DOMUtils.getStringFromDoc(doc,
         * "UTF-8", null); logger.info(docText); doc = DOMUtils.parse(docText,
         * "UTF-8");
         */
        HashMap<String, HtmlExtractionComponent> extractors =
            registry.getRegistry();
        List<String> formats = new ArrayList<String>();
        long modelSize = result.getModel().size();
        for (String s : registry.getActiveExtractors()) {
            LOG.info("Extractor: " + s);
            HtmlExtractionComponent extractor = extractors.get(s);
            // TODO: Handle dependencies between Microformat extractors, e.g.
            // formats used also in other formats
            if (extractor != null) {
                extractor.extract(id.toString(), doc, null, result);
                long tmpSize = result.getModel().size();
                if (modelSize < tmpSize) {
                    LOG.info((tmpSize - modelSize) + " Statements added: "
                        + s);
                    modelSize = tmpSize;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int argv = 0;
        IksHtmlExtractor inst = new IksHtmlExtractor();
        RDFContainerFactory rdfFactory = new RDFContainerFactoryImpl();
        for (int i = argv; i < args.length; ++i) {
            File file = new File(args[i]);
            InputStream input = new FileInputStream(file);
            Charset charset = Charset.forName("UTF-8");
            String mimeType = "text/html";
            URI uri = new URIImpl(file.toURI().toString());
            RDFContainer container = rdfFactory.getRDFContainer(uri);
            inst.extract(uri, input, charset, mimeType, container);
            System.out.println("Model for " + args[i]);
            container.getModel().writeTo(System.out);
            System.out.println();
            container.dispose();
        }
    }

}
