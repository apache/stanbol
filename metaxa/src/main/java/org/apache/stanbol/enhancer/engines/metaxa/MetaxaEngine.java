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
package org.apache.stanbol.enhancer.engines.metaxa;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.metaxa.core.MetaxaCore;
import org.apache.stanbol.enhancer.engines.metaxa.core.RDF2GoUtils;
import org.apache.stanbol.enhancer.engines.metaxa.core.html.BundleURIResolver;
import org.apache.stanbol.enhancer.engines.metaxa.core.html.HtmlExtractorFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.DatatypeLiteral;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.URI;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * {@link MetaxaEngine}
 *
 * @author Joerg Steffen, DFKI
 * @version $Id$
 */
@Component(immediate = true, metatype = true,
    label="Apache Stanbol Text and Metadata Extraction Engine",
    description="Extract plain text and embedded metadata form various document types and formats")
@Service
public class MetaxaEngine implements EnhancementEngine, ServiceProperties {

    private static final Logger log = LoggerFactory.getLogger(MetaxaEngine.class);

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING}
     */
    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING;

    /**
     * name of a file defining the available docuemnt extractors for Metaxa. By defualt, the builtin file 'extractionregistry.xml' is used.
     */
    @Property(label="ExtractorRegistry",
        description="The path of a resource on the bundle classpath that specifies which extractors to use.",
        value="extractionregistry.xml")
    public static final String GLOBAL_EXTRACTOR_REGISTRY = "org.apache.stanbol.enhancer.engines.metaxa.extractionregistry";

    /**
     * name of a file that defines the set of extractors for HTML documents. By default, the builtin file 'htmlextractors.xml' is used."
     */
    @Property(label="HtmlExtractors",value="htmlextractors.xml",
        description="The path of a resource on the bundle classpath that specifies which extractors are used for HTML pages.")
    public static final String HTML_EXTRACTOR_REGISTRY = "org.apache.stanbol.enhancer.engines.metaxa.htmlextractors";

    private MetaxaCore extractor;
    
    BundleContext bundleContext;

    public static final String DEFAULT_EXTRACTION_REGISTRY = "extractionregistry.xml";
    public static final String DEFAULT_HTML_EXTRACTOR_REGISTRY = "htmlextractors.xml";
    
    /**
     * The activate method.
     *
     * @param ce the {@link ComponentContext}
     * @throws IOException if initializing fails
     */
    protected void activate(ComponentContext ce) throws IOException {
        String extractionRegistry = DEFAULT_EXTRACTION_REGISTRY;
        String htmlExtractors = DEFAULT_HTML_EXTRACTOR_REGISTRY;
        if (ce != null) {
            this.bundleContext = ce.getBundleContext();
            BundleURIResolver.BUNDLE = this.bundleContext.getBundle();
            try {
                Dictionary<String, String> properties = ce.getProperties();
                String confFile = properties.get(GLOBAL_EXTRACTOR_REGISTRY);
                if (confFile != null && confFile.trim().length() > 0) {
                    extractionRegistry = confFile;
                }
                confFile = properties.get(HTML_EXTRACTOR_REGISTRY);
                if (confFile != null && confFile.trim().length() > 0) {
                    htmlExtractors = confFile;
                }
                this.extractor = new MetaxaCore(extractionRegistry);
                HtmlExtractorFactory.REGISTRY_CONFIGURATION = htmlExtractors;
            } catch (IOException e) {
                log.error(e.getLocalizedMessage(), e);
                throw e;
            }
        }
    }

    /**
     * The deactivate method.
     *
     * @param ce the {@link ComponentContext}
     */
    protected void deactivate(@SuppressWarnings("unused") ComponentContext ce) {
        this.extractor = null;
    }

    public int canEnhance(ContentItem ci) throws EngineException {
        String mimeType = ci.getMimeType().split(";", 2)[0];
        if (this.extractor.isSupported(mimeType)) {
            return ENHANCE_SYNCHRONOUS;
        }
        return CANNOT_ENHANCE;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {

        try {
            // get model from the extraction
            Model m = this.extractor.extract(ci.getStream(), ci.getUri().getUnicodeString(), ci.getMimeType());
            // add the statements from this model to the Metadata model
            if (null != m) {
                /*
               String text = MetaxaCore.getText(m);
               log.info(text);
                */
                // get the model where to add the statements
                MGraph g = ci.getMetadata();
                // create enhancement
                UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
                // set confidence value to 1.0
                LiteralFactory literalFactory = LiteralFactory.getInstance();
                g.add(new TripleImpl(textEnhancement, Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(1.0)));
                RDF2GoUtils.urifyBlankNodes(m);
                HashMap<BlankNode, BNode> blankNodeMap = new HashMap<BlankNode, BNode>();
                ClosableIterator<Statement> it = m.iterator();
                while (it.hasNext()) {
                    Statement oneStmt = it.next();

                    NonLiteral subject = (NonLiteral) asClerezzaResource(oneStmt.getSubject(), blankNodeMap);
                    UriRef predicate = (UriRef) asClerezzaResource(oneStmt.getPredicate(), blankNodeMap);
                    Resource object = asClerezzaResource(oneStmt.getObject(), blankNodeMap);

                    if (null != subject && null != predicate && null != object) {
                        Triple t = new TripleImpl(subject, predicate, object);
                        g.add(t);
                        log.debug("added " + t.toString());
                    }
                }
                it.close();
                m.close();
            }
        } catch (ExtractorException e) {
            throw new EngineException(e.getLocalizedMessage(), e);
        } catch (IOException e) {
            throw new EngineException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Converts the given RDF2Go node into a corresponding Clerezza object.
     *
     * @param node a {@link Node}
     * @return a {@link Resource}
     */
    public static Resource asClerezzaResource(Node node, HashMap<BlankNode, BNode> blankNodeMap) {

        if (node instanceof URI) {
            return new UriRef(node.asURI().toString());
        } else if (node instanceof BlankNode) {
            BNode bNode = blankNodeMap.get(node);
            if (bNode == null) {
                bNode = new BNode();
                blankNodeMap.put(node.asBlankNode(), bNode);
            }
            return bNode;
        } else if (node instanceof DatatypeLiteral) {
            DatatypeLiteral dtl = node.asDatatypeLiteral();
            return new TypedLiteralImpl(dtl.getValue(), new UriRef(dtl.getDatatype().asURI().toString()));
        } else if (node instanceof PlainLiteral) {
            return new PlainLiteralImpl(node.asLiteral().getValue());
        }

        return null;
    }

    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    }

}
