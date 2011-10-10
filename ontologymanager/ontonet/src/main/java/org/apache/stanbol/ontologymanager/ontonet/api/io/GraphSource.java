package org.apache.stanbol.ontologymanager.ontonet.api.io;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.owl.transformation.OWLAPIToClerezzaConverter;

/**
 * An {@link OntologyInputSource} that gets ontologies from either a Clerezza {@link Graph} (or {@link MGraph}
 * ), or its identifier and an optionally supplied triple collection manager.
 * 
 * @author alexdma
 * 
 */
public class GraphSource extends AbstractOntologyInputSource {

    public GraphSource(UriRef graphId) {
        this(graphId, TcManager.getInstance());
    }

    public GraphSource(Graph graph) {
        bindRootOntology(OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(graph));
        try {
            bindPhysicalIri(rootOntology.getOntologyID().getDefaultDocumentIRI());
        } catch (Exception e) {
            // Ontology might be anonymous, no physical IRI then...
            bindPhysicalIri(null);
        }
    }

    /**
     * This constructor can be used to hijack ontologies using a physical IRI other than their default one.
     * 
     * @param rootOntology
     * @param phyicalIRI
     */
    public GraphSource(UriRef graphId, TcManager tcManager) {
        this(tcManager.getGraph(graphId));
    }

    @Override
    public String toString() {
        return "GRAPH<" + rootOntology.getOntologyID() + ">";
    }

}
