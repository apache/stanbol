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
package org.apache.stanbol.enhancer.engines.htmlextractor.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * HtmlExtractor.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 */
public class HtmlExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlExtractor.class);

    public static String DEFAULT_CONFIGURATION = "htmlextractors.xml";

    private HtmlParser htmlParser;

    public HtmlExtractionRegistry registry = null;

    public HtmlExtractor() {
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
    public HtmlExtractor(HtmlExtractionRegistry registry, HtmlParser parser) {
        this.registry = registry;
        this.htmlParser = parser;
    }
    
    public HtmlExtractor(String configFileName)
            throws InitializationException {
        this.htmlParser = new HtmlParser();
        this.registry = new HtmlExtractionRegistry(configFileName);
    }

    public void extract(String id,
            InputStream input, Charset charset, String mimeType,
            Graph result)
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
        Map<String, HtmlExtractionComponent> extractors =
            registry.getRegistry();
        List<String> formats = new ArrayList<String>();
        long modelSize = result.size();
        for (String s : registry.getActiveExtractors()) {
            LOG.debug("Extractor: {}", s);
            HtmlExtractionComponent extractor = extractors.get(s);
            // TODO: Handle dependencies between Microformat extractors, e.g.
            // formats used also in other formats
            if (extractor != null) {
                extractor.extract(id, doc, null, result);
                long tmpSize = result.size();
                if (modelSize < tmpSize) {
                    LOG.debug("{} Statements added: {}",(tmpSize - modelSize),s);
                    modelSize = tmpSize;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int argv = 0;
        HtmlExtractor inst = new HtmlExtractor();
        for (int i = argv; i < args.length; ++i) {
            File file = new File(args[i]);
            InputStream input = new FileInputStream(file);
            Charset charset = Charset.forName("UTF-8");
            String mimeType = "text/html";
            IRI uri = new IRI(file.toURI().toString());
            Graph container = new SimpleGraph();
            inst.extract(uri.getUnicodeString(), input, charset, mimeType, container);
            System.out.println("Model for " + args[i]);
            //TODO
//            container.writeTo(System.out);
            System.out.println();
        }
    }

}
