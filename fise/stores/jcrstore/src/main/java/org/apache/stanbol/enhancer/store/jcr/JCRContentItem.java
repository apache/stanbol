package org.apache.stanbol.enhancer.store.jcr;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.VersionException;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.event.FilterTriple;
import org.apache.clerezza.rdf.core.event.GraphEvent;
import org.apache.clerezza.rdf.core.event.GraphListener;
import org.apache.clerezza.rdf.core.impl.SimpleGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.store.JCRStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JCRContentItem implements ContentItem, GraphListener {

    private static final String OBJECT = "object";
    private static final String PREDICATE = "predicate";
    private static final String SUBJECT = "subject";
    private static final String JCR_DATA = "jcr:data";
    private static final String JCR_MIME_TYPE = "jcr:mimeType";
    public static final String ENHANCER_ID_PROP = "stanbolEnhancerId";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Node jcrNode;

    // private static byte[] data;

    public JCRContentItem(String id, Node parent) throws InvalidQueryException,
            RepositoryException {
        jcrNode = JCRStore.findNodeById(id, parent);
        log.info("constructor with id: " + id);
        if (id == null || jcrNode == null) {
            log.info("found no node for id " + id + " creating new one");
            createNode(id, null, null, null, parent);
        }
    }

    public JCRContentItem(String id, byte[] content, String mimeType,
            MGraph metadata, Node parent) throws ItemExistsException,
            PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // first check if the id already exists
        jcrNode = JCRStore.findNodeById(id, parent);
        log.info("full constructor with id: " + id);
        if (id == null || jcrNode == null) {
            log.info("found no node for id " + id + " creating new one");
            createNode(id, content, mimeType, metadata, parent);
        }
    }

    private void createNode(String id, byte[] content, String mimeType,
            MGraph metadata, Node parent) throws ItemExistsException,
            PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException,
            ValueFormatException, AccessDeniedException,
            InvalidItemStateException, NoSuchNodeTypeException {
        if (metadata == null) {
            metadata = new SimpleMGraph();
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        if (content == null) {
            content = new byte[0];
        }

        jcrNode = parent.addNode(System.currentTimeMillis() + "");
        jcrNode.setProperty(ENHANCER_ID_PROP, id);
        jcrNode.setProperty(JCR_DATA, new ByteArrayInputStream(content));

        jcrNode.setProperty(JCR_MIME_TYPE, mimeType);
        // graph nodes
        Iterator<Triple> iterator = metadata.iterator();
        int counter = 0;
        while (iterator.hasNext()) {
            Triple triple = iterator.next();
            log.info("triple " + triple.getSubject().toString() + " "
                    + triple.getPredicate().toString() + " "
                    + triple.getObject().toString());
            persistTriple(counter + "", triple);
            counter++;
        }

        log.info("creating node at path: " + jcrNode.getPath()
                + " with num of triples " + counter);

        jcrNode.getSession().save();

    }

    private void persistTriple(String nameHint, Triple triple)
            throws ItemExistsException, PathNotFoundException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException, ValueFormatException {
        Random r = new Random();
        String name = Long.toString(Math.abs(r.nextLong()), 36);
        if (nameHint != null && !nameHint.equals("")) {
            if (!jcrNode.hasNode(nameHint)) {
                name = nameHint;
            }
        }

        Node tripleNode = jcrNode.addNode(name);
        /*
         * TODO: Rupert Westenthaler 25.01.2011
         * Using the toString method of the subject, predicate and object is
         * not sufficient here. One needs to treat UriRefs, PlainLiterals,
         * TypedLiterals and especially BNodes (blank nodes) differently.
         * For Plain literals it is important to also store the language. For
         * Typed Literals it is important to store the xsd:dataType (or even
         * map the xsd:dataType to the according jcr:type.
         * For BNodes one needs to keep a bidirectional mapping between the
         * BNode instance and the JCR:Property (e.g. by using an random ID as
         * value and map this value to an BNode instance.
         */
        tripleNode.setProperty(SUBJECT, triple.getSubject().toString());
        tripleNode.setProperty(PREDICATE, triple.getPredicate().toString());
        tripleNode.setProperty(OBJECT, triple.getObject().toString());
        log.info("persisted triple " + triple.getSubject().toString() + " "
                + triple.getPredicate().toString() + " "
                + triple.getObject().toString());
        jcrNode.getSession().save();
    }

    public String getId() {
        try {
            if (jcrNode == null) {
                log.warn("entering getId, but no node initialized");
            }
            if (jcrNode.hasProperty(ENHANCER_ID_PROP)) {
                log.info("found id: "
                        + jcrNode.getProperty(ENHANCER_ID_PROP).getValue()
                                .getString());
                return jcrNode.getProperty(ENHANCER_ID_PROP).getValue().getString();
            } else {
                log.warn("entering getId, but node has no ID value");
                return null;
            }

        } catch (ValueFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public MGraph getMetadata() {
        try {
            if (jcrNode == null) {
                log.warn("entering getMetadata, but no node initialized");
            }
            MGraph graph = new SimpleMGraph();

            // loop over children
            NodeIterator children = jcrNode.getNodes();
            while (children.hasNext()) {
                Node childNode = children.nextNode();
                if (childNode.hasProperty(SUBJECT)
                        && childNode.hasProperty(PREDICATE)
                        && childNode.hasProperty(OBJECT)) {
                    /*
                     * TODO: Rupert Westenthaler 25.01.2011
                     * Triples may use
                     *  - any kind of NonLiteral as Subject (BNode or UriRef)
                     *  - only an UriRef as property (Predicate)
                     *  - any Resource (PlainLiteral, TypedLiteral, NBode or UriRef)
                     *    as Object.
                     *  The Node representing the triple need to store the required 
                     *  information and this mapping implementation need to be
                     *  adapted accordingly.
                     *  See also TODO for createNode(..)
                     */
                    graph.add(new TripleImpl(new UriRef(childNode.getProperty(
                            SUBJECT).getValue().getString()), new UriRef(
                            childNode.getProperty(PREDICATE).getValue()
                                    .getString()), new UriRef(childNode
                            .getProperty(OBJECT).getValue().getString())));
                }
            }
            graph.addGraphListener(this, new FilterTriple(null, null, null));
            return graph;

        } catch (ValueFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    public String getMimeType() {
        try {
            if (jcrNode == null) {
                log.warn("entering getMimeType, but no node initialized");
            }
            if (jcrNode.hasProperty(JCR_MIME_TYPE)) {
                log.info("found mimetype: "
                        + jcrNode.getProperty(JCR_MIME_TYPE).getValue()
                                .getString());
                return jcrNode.getProperty(JCR_MIME_TYPE).getValue()
                        .getString();
            } else {
                log.warn("entering getId, but node has no ID value");
                return null;
            }

        } catch (ValueFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public InputStream getStream() {
        try {
            if (jcrNode == null) {
                log.warn("entering getStream, but no node initialized");
            }
            if (jcrNode.hasProperty(JCR_DATA)) {
                log.info("found content");
                return jcrNode.getProperty(JCR_DATA).getValue().getStream();
            } else {
                log.warn("entering getStream, but node has no ID value");
                return null;
            }

        } catch (ValueFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * For now this handles only cases when triples are added, but not removed
     */
    public void graphChanged(List<GraphEvent> events) {
        for (Iterator<GraphEvent> i = events.iterator(); i.hasNext();) {
            GraphEvent ge = i.next();
            log.warn("event: " + ge.getTriple());
            try {
                persistTriple(null, ge.getTriple());
            } catch (ItemExistsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (PathNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (VersionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ConstraintViolationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (LockException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ValueFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
