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

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.metaxa.core.MetaxaCore;
import org.apache.stanbol.enhancer.engines.metaxa.core.RDF2GoUtils;
import org.apache.stanbol.enhancer.engines.metaxa.core.html.BundleURIResolver;
import org.apache.stanbol.enhancer.engines.metaxa.core.html.HtmlExtractorFactory;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSink;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.ontoware.aifbcommons.collection.ClosableIterator;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.BlankNode;
import org.ontoware.rdf2go.model.node.DatatypeLiteral;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
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
@Component(immediate = true, metatype = true, inherit = true)
@Service
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME, value="metaxa")
})
public class MetaxaEngine 
        extends AbstractEnhancementEngine<IOException, RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    private static final Logger log = LoggerFactory.getLogger(MetaxaEngine.class);
    /**
     * The default charset
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * Plain text content of a content item.
      */
    public static final IRI NIE_PLAINTEXTCONTENT = new IRI(NamespaceEnum.nie + "plainTextContent");
    private static final URIImpl NIE_PLAINTEXT_PROPERTY = new URIImpl(NIE_PLAINTEXTCONTENT.getUnicodeString());
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING}
     */
    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING;

    /**
     * name of a file defining the available docuemnt extractors for Metaxa. By default, the builtin file 'extractionregistry.xml' is used.
     */
    @Property(value=MetaxaEngine.DEFAULT_EXTRACTION_REGISTRY)
    public static final String GLOBAL_EXTRACTOR_REGISTRY = "org.apache.stanbol.enhancer.engines.metaxa.extractionregistry";

    /**
     * name of a file that defines the set of extractors for HTML documents. By default, the builtin file 'htmlextractors.xml' is used."
     */
    @Property(value=MetaxaEngine.DEFAULT_HTML_EXTRACTOR_REGISTRY)
    public static final String HTML_EXTRACTOR_REGISTRY = "org.apache.stanbol.enhancer.engines.metaxa.htmlextractors";

    @Property(value={"text/plain"},cardinality=1000)
    public static final String IGNORE_MIME_TYPES = "org.apache.stanbol.enhancer.engines.metaxa.ignoreMimeTypes";

    /**
     * a boolean option whether extracted text should be included in the metadata as value of the NIE.plainTextContent property
     */
    @Property(boolValue=false)
    public static final String INCLUDE_TEXT_IN_METADATA = "org.apache.stanbol.enhancer.engines.metaxa.includeText";
    
    /**
     * Internally used to create additional {@link Blob} for transformed
     * versions af the original content
     */
    @Reference
    private ContentItemFactory ciFactory;
    
    private MetaxaCore extractor;
    
    BundleContext bundleContext;

    public static final String DEFAULT_EXTRACTION_REGISTRY = "extractionregistry.xml";
    public static final String DEFAULT_HTML_EXTRACTOR_REGISTRY = "htmlextractors.xml";
    
    private Set<String> ignoredMimeTypes;
    private boolean includeText = false;
    
    /**
     * The activate method.
     *
     * @param ce the {@link ComponentContext}
     * @throws IOException if initializing fails
     */
    protected void activate(ComponentContext ce) throws ConfigurationException, IOException {
        super.activate(ce);
        String extractionRegistry = DEFAULT_EXTRACTION_REGISTRY;
        String htmlExtractors = DEFAULT_HTML_EXTRACTOR_REGISTRY;
        this.bundleContext = ce.getBundleContext();
        BundleURIResolver.BUNDLE = this.bundleContext.getBundle();
        try {
            Dictionary<String, Object> properties = ce.getProperties();
            String confFile = (String)properties.get(GLOBAL_EXTRACTOR_REGISTRY);
            if (confFile != null && confFile.trim().length() > 0) {
                extractionRegistry = confFile;
            }
            confFile = (String)properties.get(HTML_EXTRACTOR_REGISTRY);
            if (confFile != null && confFile.trim().length() > 0) {
                htmlExtractors = confFile;
            }
            this.extractor = new MetaxaCore(extractionRegistry);
            HtmlExtractorFactory.REGISTRY_CONFIGURATION = htmlExtractors;
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
            throw e;
        }
        Object value = ce.getProperties().get(IGNORE_MIME_TYPES);
        if(value instanceof String[]){
            ignoredMimeTypes = new HashSet<String>(Arrays.asList((String[])value));
        } else if(value instanceof Iterable<?>){
            ignoredMimeTypes = new HashSet<String>();
            for(Object mimeType : (Iterable<?>)value){
                if(mimeType != null){
                    ignoredMimeTypes.add(mimeType.toString());
                }
            }
        } else if(value != null && !value.toString().isEmpty()){
            ignoredMimeTypes = Collections.singleton(value.toString());
        } else {
            ignoredMimeTypes = Collections.singleton("text/plain");
        }
        value = ce.getProperties().get(INCLUDE_TEXT_IN_METADATA);
        if (value instanceof Boolean) {
          includeText = ((Boolean)value).booleanValue();
          log.info("Include Text set to: {}",value);
        }
    }

    /**
     * The deactivate method.
     *
     * @param ce the {@link ComponentContext}
     */
    protected void deactivate(ComponentContext ce) {
        super.deactivate(ce);
        this.extractor = null;
    }

    public int canEnhance(ContentItem ci) throws EngineException {
        String mimeType = ci.getMimeType();
        if (!ignoredMimeTypes.contains(mimeType) && 
                this.extractor.isSupported(mimeType)) {
            return ENHANCE_ASYNC; //supports now asynchronous execution!
        }
        return CANNOT_ENHANCE;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        // get model from the extraction
        URIImpl docId;
        Model m = null;
        ci.getLock().readLock().lock();
        try {
            docId = new URIImpl(ci.getUri().getUnicodeString());
            m = this.extractor.extract(ci.getStream(), docId, ci.getMimeType());
        } catch (ExtractorException e) {
            throw new EngineException("Error while processing ContentItem "
                + ci.getUri()+" with Metaxa",e);
        } catch (IOException e) {
            throw new EngineException("Error while processing ContentItem "
                    + ci.getUri()+" with Metaxa",e);
        } finally {
            ci.getLock().readLock().unlock();
        }
        // Convert the RDF2go model to a Clerezza ImmutableGraph and also extract
        // the extracted plain text from the model
        if (null == m) {
            log.debug("Unable to preocess ContentItem {} (mime type {}) with Metaxa",
                ci.getUri(),ci.getMimeType());
            return;
        }
        ContentSink plainTextSink;
        try {
            plainTextSink = ciFactory.createContentSink("text/plain");
        } catch (IOException e) {
            m.close();
            throw new EngineException("Unable to initialise Blob for storing" +
            		"the plain text content",e);
        }
        HashMap<BlankNode, BlankNode> blankNodeMap = new HashMap<BlankNode, BlankNode>();
        RDF2GoUtils.urifyBlankNodes(m);
        ClosableIterator<Statement> it = m.iterator();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
            plainTextSink.getOutputStream(), UTF8));
        boolean textExtracted = false; //used to detect if some text was extracted
        try {
            Graph g = new SimpleGraph(); //first add to a temporary graph
            while (it.hasNext()) {
                Statement oneStmt = it.next();
                //we need to treat triples that provide the plain/text
                //version differently. Such Objects need to be added to
                //the plain text Blob!
                if(oneStmt.getSubject().equals(docId) && 
                        oneStmt.getPredicate().equals(NIE_PLAINTEXT_PROPERTY)){
                    String text = oneStmt.getObject().toString();
                    if(text != null && !text.isEmpty()){
                        try {
                            out.write(oneStmt.getObject().toString());
                        } catch (IOException e) {
                            throw new EngineException("Unable to write extracted" +
                            		"plain text to Blob (blob impl: "
                                    + plainTextSink.getBlob().getClass()+")",e);
                        }
                        textExtracted = true;
                        if (includeText) {
                            BlankNodeOrIRI subject = (BlankNodeOrIRI) asClerezzaResource(oneStmt.getSubject(), blankNodeMap);
                            IRI predicate = (IRI) asClerezzaResource(oneStmt.getPredicate(), blankNodeMap);
                            RDFTerm object = asClerezzaResource(oneStmt.getObject(), blankNodeMap);
                            g.add(new TripleImpl(subject, predicate, object));
                        }
                    }
                } else { //add metadata to the metadata of the contentItem
                    BlankNodeOrIRI subject = (BlankNodeOrIRI) asClerezzaResource(oneStmt.getSubject(), blankNodeMap);
                    IRI predicate = (IRI) asClerezzaResource(oneStmt.getPredicate(), blankNodeMap);
                    RDFTerm object = asClerezzaResource(oneStmt.getObject(), blankNodeMap);

                    if (null != subject && null != predicate && null != object) {
                        Triple t = new TripleImpl(subject, predicate, object);
                        g.add(t);
                        log.debug("added " + t.toString());
                    }
                }
            }
            //add the extracted triples to the metadata of the ContentItem
            ci.getLock().writeLock().lock();
            try { 
                ci.getMetadata().addAll(g);
                g = null;
            } finally {
                ci.getLock().writeLock().unlock();
            }
        } finally {
            it.close();
            m.close();
            IOUtils.closeQuietly(out);
        }
        if(textExtracted){
            //add plain text to the content item
            IRI blobUri = new IRI("urn:metaxa:plain-text:"+randomUUID());
            ci.addPart(blobUri, plainTextSink.getBlob());
        }
    }

    /**
     * Converts the given RDF2Go node into a corresponding Clerezza object.
     *
     * @param node a {@link Node}
     * @return a {@link RDFTerm}
     */
    public static RDFTerm asClerezzaResource(Node node, HashMap<BlankNode, BlankNode> blankNodeMap) {

        if (node instanceof URI) {
            return new IRI(node.asURI().toString());
        } else if (node instanceof BlankNode) {
            BlankNode bNode = blankNodeMap.get(node);
            if (bNode == null) {
                bNode = new BlankNode();
                blankNodeMap.put(node.asBlankNode(), bNode);
            }
            return bNode;
        } else if (node instanceof DatatypeLiteral) {
            DatatypeLiteral dtl = node.asDatatypeLiteral();
            return new TypedLiteralImpl(dtl.getValue(), new IRI(dtl.getDatatype().asURI().toString()));
        } else if (node instanceof PlainLiteral) {
            return new PlainLiteralImpl(node.asLiteral().getValue());
        }

        return null;
    }

    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    }

}
