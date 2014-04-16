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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * HtmlExtractionRegistry.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 */
public class HtmlExtractionRegistry {

    /**
     * This contains the logger.
     */
    private static final Logger LOG =
        LoggerFactory.getLogger(HtmlExtractionRegistry.class);

    private Map<String, HtmlExtractionComponent> registry;
    private Set<String> activeExtractors;


    public HtmlExtractionRegistry() {
        registry = new HashMap<String, HtmlExtractionComponent>();
        activeExtractors = new HashSet<String>();
    }

    public HtmlExtractionRegistry(String configFileName)
            throws InitializationException {
        this();
        InputStream config = getClass().getClassLoader().getResourceAsStream(configFileName);
        if (config == null) {
            throw new InitializationException("File not found: "+configFileName);
        }
        initialize(config);
    }

    public HtmlExtractionRegistry(InputStream config) throws InitializationException {
        this();
        initialize(config);
    }
    
    
    public void initialize(InputStream configFileStream)
            throws InitializationException {

        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            DocumentBuilder parser =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = parser.parse(new InputSource(configFileStream));
            Node node;
            NodeList nodes = (NodeList) xPath.evaluate("/htmlextractors/extractor", document, XPathConstants.NODESET);
            if (nodes != null) {
                TransformerFactory transFac = TransformerFactory.newInstance();
                transFac.setURIResolver(new BundleURIResolver());
                for (int j = 0, iCnt = nodes.getLength(); j < iCnt; j++) {
                    Node nd = nodes.item(j);
                    node = (Node)xPath.evaluate("@id", nd, XPathConstants.NODE);
                    String id = node.getNodeValue();
                    Node srcNode =
                        (Node)xPath.evaluate("source", nd, XPathConstants.NODE);
                    if (srcNode != null) {
                        node = (Node) xPath.evaluate("@type", srcNode, XPathConstants.NODE);
                        String srcType = node.getNodeValue();
                        if (srcType.equals("xslt")) {
                            String rdfFormat = "rdfxml";
                            String rdfSyntax = SupportedFormat.RDF_XML;
                            node =
                                (Node)xPath.evaluate("@syntax", srcNode,
                                XPathConstants.NODE);
                            if (node != null) {
                                //TODO check syntax types
                                rdfFormat = node.getNodeValue();
                                if (rdfFormat.equalsIgnoreCase("turtle")) {
                                    rdfSyntax = SupportedFormat.TURTLE;
                                }
                                else if (rdfFormat.equalsIgnoreCase("xturtle")) {
                                    rdfSyntax = SupportedFormat.X_TURTLE;
                                }
                                else if (rdfFormat.equalsIgnoreCase("ntriple")) {
                                    rdfSyntax = SupportedFormat.N_TRIPLE;
                                }
                                else if (rdfFormat.equalsIgnoreCase("n3")) {
                                    rdfSyntax = SupportedFormat.N3;
                                }
                                else if (!rdfFormat.equalsIgnoreCase("rdfxml")) {
                                    throw new InitializationException(
                                        "Unknown RDF Syntax: " + rdfFormat
                                        + " for " + id + " extractor");
                                }
                            }
                            String fileName = DOMUtils.getText(srcNode);
                            XsltExtractor xsltExtractor =
                                new XsltExtractor(id, fileName,transFac);
                            xsltExtractor.setSyntax(rdfSyntax);
                            // name of URI/URL parameter of the script (default
                            // "uri")
                            node =
                                (Node)xPath.evaluate("@uri", srcNode,
                                XPathConstants.NODE);
                            if (node != null) {
                                xsltExtractor.setUriParameter(node
                                    .getNodeValue());
                            }
                            registry.put(id, xsltExtractor);
                            activeExtractors.add(id);
                        }
                        else if (srcType.equals("java")) {
                            String clsName = srcNode.getNodeValue();
                            Object extractor =
                                Class.forName(clsName).newInstance();
                            if (extractor instanceof HtmlExtractionComponent) {
                                registry.put(id,
                                    (HtmlExtractionComponent)extractor);
                                activeExtractors.add(id);
                            }
                            else {
                                throw new InitializationException(
                                    "clsName is not an HtmlExtractionComponent");
                            }
                        }
                        else {
                            LOG.warn("No valid type for extractor found: "
                                + id);
                        }
                        LOG.info("Extractor for: " + id);
                    }

                }
            }
        } catch (FileNotFoundException e) {
            throw new InitializationException(e.getMessage(), e);
        } catch (XPathExpressionException e) {
            throw new InitializationException(e.getMessage(), e);
        } catch (DOMException e) {
            throw new InitializationException(e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw new InitializationException(e.getMessage(), e);
        } catch (SAXException e) {
            throw new InitializationException(e.getMessage(), e);
        } catch (IOException e) {
            throw new InitializationException(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            throw new InitializationException(e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new InitializationException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new InitializationException(e.getMessage(), e);
        }
    }

    public Map<String, HtmlExtractionComponent> getRegistry() {
        return registry;
    }

    public void setRegistry(Map<String, HtmlExtractionComponent> registry) {
        this.registry = registry;
    }

    public Set<String> getActiveExtractors() {
        return activeExtractors;
    }

    public void setActiveExtractors(Set<String> activeExtractors) {
        this.activeExtractors = activeExtractors;
    }

    public static void main(String[] args) throws Exception {
        int argv = 0;
        HtmlExtractionRegistry inst = new HtmlExtractionRegistry(args[0]);
        System.err.println("Active Components: " + inst.activeExtractors.size());
        for (String s : inst.activeExtractors) {
            System.err.println(s);
        }
    }

    public void add(String id, String resourceName, String type)
            throws InitializationException {
    }

    public void remove(String id) {
    }

    public void activate(String id) {
    }

    public void deactivate(String id) {
    }

}
