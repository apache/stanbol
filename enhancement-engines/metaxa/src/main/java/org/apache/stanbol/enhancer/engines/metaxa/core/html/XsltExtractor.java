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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.ontoware.rdf2go.exception.ModelRuntimeException;
import org.ontoware.rdf2go.model.Syntax;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * XsltExtractor.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 */
public class XsltExtractor implements HtmlExtractionComponent {

    /**
     * This contains the logger.
     */
    private static final Logger LOG =
        LoggerFactory.getLogger(XsltExtractor.class);
    public static Syntax N3 =
        new Syntax("N3", "application/rdf+n3", ".n3", true);
    private String uriParameter = "uri";
    private Transformer transformer;
    private String id;
    private URI source;
    private Syntax syntax = Syntax.RdfXml;


    public XsltExtractor() {
    }

    public XsltExtractor(String id, String fileName, TransformerFactory factory)
            throws InitializationException {

        this.id = id;
        try {
            URI location =
                getClass().getClassLoader().getResource(fileName).toURI();
            source = location;
        } catch (URISyntaxException e) {
            throw new InitializationException(e.getMessage(), e);
        }
        initialize(factory);
    }

    public String getUriParameter() {
        return uriParameter;
    }

    public void setUriParameter(String uriParameter) {
        this.uriParameter = uriParameter;
    }

    public Syntax getSyntax() {
        return syntax;
    }

    public void setSyntax(Syntax syntax) {
        this.syntax = syntax;
    }

    public Transformer getTransformer() {
        return transformer;
    }

    public void setTransformer(Transformer transformer) {
        this.transformer = transformer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URI getSource() {
        return source;
    }

    public void setSource(URI source) {
        this.source = source;
    }

    public synchronized void extract(String id, Document doc, Map<String, Object> params,
            RDFContainer result)
            throws ExtractorException {

        if (params == null) {
            params = new HashMap<String, Object>();
        }
        params.put(this.uriParameter, id);
        initTransformerParameters(params);
        Source source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult output = new StreamResult(writer);
        try {
            this.transformer.transform(source, output);
            String rdf = writer.toString();
            LOG.debug(rdf);
            StringReader reader = new StringReader(rdf);
            result.getModel().readFrom(reader, this.syntax);
            reader.close();
        } catch (TransformerException e) {
            throw new ExtractorException(e.getMessage(), e);
        } catch (ModelRuntimeException e) {
            throw new ExtractorException(e.getMessage(), e);
        } catch (IOException e) {
            throw new ExtractorException(e.getMessage(), e);
        }
    }

    public void initialize(TransformerFactory factory)
            throws InitializationException {

        if (source == null || id == null) {
            throw new InitializationException("Missing source or id");
        }
        if (factory == null) {
          factory = TransformerFactory.newInstance();
          factory.setURIResolver(new BundleURIResolver());
        }
        StreamSource xsltSource = new StreamSource(source.toString());
        xsltSource.setSystemId(source.toString());
        try {
            transformer = factory.newTransformer(xsltSource);
        } catch (TransformerConfigurationException e) {
            throw new InitializationException(e.getMessage(), e);
        }
    }

    public void initTransformerParameters(Map<String, Object> params) {
        transformer.clearParameters();
        if (params != null) {
            Set<String> parms = params.keySet();
            for (String piter : parms) {
                transformer.setParameter(piter, params.get(piter));
            }
        }
    }

}
