package org.apache.stanbol.rules.manager.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.io.AbstractOntologyInputSource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class RuleOntologyInputSource extends AbstractOntologyInputSource {

    public RuleOntologyInputSource(OWLOntology rootOntology, WeightedTcProvider weightedTcProvider) {
        this.rootOntology = rootOntology;
        try {
            this.physicalIri = rootOntology.getOntologyID().getDefaultDocumentIRI();
        } catch (Exception e) {
            // Ontology might be anonymous, no physical IRI then...
        }
        
        // FIXME : can't we just assign rootOntology = ontology?
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try {

            MGraph mGraph = weightedTcProvider.getMGraph(new UriRef(physicalIri.toString()));
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            SerializingProvider serializingProvider = new JenaSerializerProvider();

            serializingProvider.serialize(out, mGraph, SupportedFormat.RDF_XML);

            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            this.rootOntology = manager.loadOntologyFromOntologyDocument(in);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String toString() {
        return this.rootOntology.toString();
    }

}
