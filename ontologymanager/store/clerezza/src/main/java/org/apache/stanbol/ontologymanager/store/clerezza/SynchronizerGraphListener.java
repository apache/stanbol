package org.apache.stanbol.ontologymanager.store.clerezza;

import java.util.ArrayList;
import java.util.List;

import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.event.GraphEvent;
import org.apache.clerezza.rdf.core.event.GraphListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.vocabulary.OWL;

public class SynchronizerGraphListener implements GraphListener {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizerGraphListener.class);
    private ClerezzaStoreSynchronizer synchronizer;
    private String graphURI;

    public SynchronizerGraphListener(ClerezzaStoreSynchronizer synchronizer, String graphURI) {
        this.synchronizer = synchronizer;
        this.graphURI = graphURI;
    }

    @Override
    public void graphChanged(List<GraphEvent> events) {
        List<String> resourceURIs = new ArrayList<String>();
        for (GraphEvent event : events) {
            Triple triple = event.getTriple();
            UriRef predicate = triple.getPredicate();
            NonLiteral subject = triple.getSubject();
            Resource object = triple.getObject();
            if (predicate.equals(OWLVocabulary.RDF_TYPE)) {
                logger.info("Listened triple change: {}", triple);
                try {
                    resourceURIs.add(((UriRef) subject).getUnicodeString());
                } catch (ClassCastException e) {
                    // Blank node, just skipping
                    logger.info("Subject " + subject.toString() + " is a blanknode");
                }
            }else if (predicate.equals(new UriRef(OWL.imports.asResource().getURI()))){
                logger.info("Listened triple change: {}", triple);
                try{
                    resourceURIs.add(((UriRef)object).getUnicodeString());
                }catch (Exception e) {
                    logger.warn("Cannot resolve import: {}", triple);
                }
            }
        }
        if (resourceURIs.size() > 0) {
            logger.info("Listener: " + this.toString() + "URIs:" + resourceURIs);
            synchronizer.synchronizeResourceOnGraph(graphURI, resourceURIs);
        }
    }

}
