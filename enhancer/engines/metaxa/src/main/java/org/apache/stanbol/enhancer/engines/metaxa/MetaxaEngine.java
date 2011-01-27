package org.apache.stanbol.enhancer.engines.metaxa;

import java.io.IOException;
import java.util.Collections;
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
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.metaxa.core.MetaxaCore;
import org.apache.stanbol.enhancer.engines.metaxa.core.RDF2GoUtils;
import org.apache.stanbol.enhancer.engines.metaxa.core.html.BundleURIResolver;
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
@Component(immediate = true, metatype = true)
@Service
public class MetaxaEngine implements EnhancementEngine, ServiceProperties {

    /**
     * This contains the logger.
     */
    private static final Logger log = LoggerFactory.getLogger(MetaxaEngine.class);

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_PRE_PROCESSING}
     */
    public static final Integer defaultOrder = ORDERING_PRE_PROCESSING;

    /**
     * This contains the Aperture extractor.
     */
    private MetaxaCore extractor;

    /**
     * The activate method.
     *
     * @param ce the {@link ComponentContext}
     * @throws IOException if initializing fails
     */
    protected void activate(ComponentContext ce) throws IOException {

        try {
            this.extractor = new MetaxaCore("extractionregistry.xml");
            BundleURIResolver.BUNDLE = ce.getBundleContext().getBundle();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
            throw e;
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
            // get the model where to add the statements
            MGraph g = ci.getMetadata();
            // create enhancement
            UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
            // set confidence value to 1.0
            LiteralFactory literalFactory = LiteralFactory.getInstance();
            g.add(new TripleImpl(textEnhancement, Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(1.0)));
            // get model from the extraction
            Model m = this.extractor.extract(ci.getStream(), ci.getId(), ci.getMimeType());
            // add the statements from this model to the Metadata model
            if (null != m) {
                /*
               String text = MetaxaCore.getText(m);
               log.info(text);
                */
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
                        log.info("added " + t.toString());
                    }
                }
                it.close();
            }
        } catch (ExtractorException e) {
            throw new EngineException(e.getLocalizedMessage(), e);
        } catch (IOException e) {
            throw new EngineException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * This converts the given RDF2Go node into a corresponding Clerezza object.
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
