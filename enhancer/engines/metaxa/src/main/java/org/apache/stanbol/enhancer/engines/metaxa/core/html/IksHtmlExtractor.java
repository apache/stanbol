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

    private HtmlParser htmlParser;

    public HtmlExtractionRegistry registry = null;

    public IksHtmlExtractor() {
      // lazy initialization when used first
      if (registry == null) {
        try {
            this.htmlParser = new HtmlParser();
            this.registry = new HtmlExtractionRegistry(DEFAULT_CONFIGURATION);
        } catch (InitializationException e) {
          LOG.error("Registry Initialization Error: " + e.getMessage());
        }
      }
    }
    public IksHtmlExtractor(HtmlExtractionRegistry registry, HtmlParser parser) {
        this.registry = registry;
        this.htmlParser = parser;
    }
    
    public IksHtmlExtractor(String configFileName)
            throws InitializationException {
        this.htmlParser = new HtmlParser();
        this.registry = new HtmlExtractionRegistry(configFileName);
    }

    public void extract(URI id,
            InputStream input, Charset charset, String mimeType,
            RDFContainer result)
            throws ExtractorException {
        if (registry == null)
            return;
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
            LOG.debug("Extractor: {}", s);
            HtmlExtractionComponent extractor = extractors.get(s);
            // TODO: Handle dependencies between Microformat extractors, e.g.
            // formats used also in other formats
            if (extractor != null) {
                extractor.extract(id.toString(), doc, null, result);
                long tmpSize = result.getModel().size();
                if (modelSize < tmpSize) {
                    LOG.debug("{} Statements added: {}",(tmpSize - modelSize),s);
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
