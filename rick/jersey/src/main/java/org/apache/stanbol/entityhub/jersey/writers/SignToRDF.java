package org.apache.stanbol.entityhub.jersey.writers;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;


final class SignToRDF {
    private SignToRDF() { /* do not create instances of utility classes */}

    static UriRef signRepresentation = new UriRef(RdfResourceEnum.signRepresentation.getUri());
    static UriRef signSite = new UriRef(RdfResourceEnum.signSite.getUri());
    static UriRef sign = new UriRef(RdfResourceEnum.Sign.getUri());
    static UriRef entityMapping = new UriRef(RdfResourceEnum.EntityMapping.getUri());
    static UriRef symbol = new UriRef(RdfResourceEnum.Symbol.getUri());
    static RdfValueFactory valueFactory = RdfValueFactory.getInstance();
    static LiteralFactory literalFactory = LiteralFactory.getInstance();

    static MGraph toRDF(Representation representation) {
        MGraph graph = new SimpleMGraph();
        addRDFTo(graph, representation);
        return graph;
    }

    static void addRDFTo(MGraph graph, Representation representation) {
        graph.addAll(valueFactory.toRdfRepresentation(representation).getRdfGraph());
    }

    static TripleCollection toRDF(Sign sign) {
        MGraph graph = new SimpleMGraph();
        addRDFTo(graph, sign);
        return graph;
    }

    static void addRDFTo(MGraph graph, Sign sign) {
        addRDFTo(graph, sign.getRepresentation());
        //now add some triples that represent the Sign
        addSignTriplesToGraph(graph, sign);
    }

    /**
     * Adds the Triples that represent the Sign to the parsed graph. Note that
     * this method does not add triples for the representation. However it adds
     * the triple (sign,singRepresentation,representation)
     *
     * @param graph the graph to add the triples
     * @param sign the sign
     */
    static void addSignTriplesToGraph(MGraph graph, Sign sign) {
        UriRef id = new UriRef(sign.getId());
        UriRef repId = new UriRef(sign.getRepresentation().getId());
        /*
         * TODO: change to URI as soon as the paths are defined
         *  e.g:
         *   - Sign: <URLofEntityhub>/site/<sing.getSignSite>
         *   - Symbol: <URLofEntityhub>/symbol/<sing.getSignSite>
         *   - EntityMapping: <URLofEntityhub>/mapping/<sing.getSignSite>
         * For now write a Literal with the ID of the Site
         */
        TypedLiteral siteName = literalFactory.createTypedLiteral(sign.getSignSite());
        graph.add(new TripleImpl(id, SignToRDF.signSite, siteName));
        graph.add(new TripleImpl(id, SignToRDF.signRepresentation, repId));
        if (sign instanceof Symbol) {
            graph.add(new TripleImpl(id, RDF.type, SignToRDF.symbol));
        } else if (sign instanceof EntityMapping) {
            graph.add(new TripleImpl(id, RDF.type, SignToRDF.entityMapping));
        } else {
            graph.add(new TripleImpl(id, RDF.type, SignToRDF.sign));
        }
    }

}
