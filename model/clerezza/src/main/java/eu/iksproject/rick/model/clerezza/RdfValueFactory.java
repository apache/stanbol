package eu.iksproject.rick.model.clerezza;

import java.util.Iterator;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;

import eu.iksproject.rick.servicesapi.model.Representation;
import eu.iksproject.rick.servicesapi.model.ValueFactory;
/**
 * Factory for creating instances of the RDF implementation of the Rick model
 * based on Clerezza.
 * TODO: Check if it makes sense to add a instance cache for {@link RdfReference}
 *       instances.
 * @author Rupert Westenthaler
 *
 */
public class RdfValueFactory implements ValueFactory {

    private static RdfValueFactory instance;
    /**
     * TODO:Currently implements the singleton pattern. This might change in the
     * future if ValueFactoy becomes an own OSGI Service
     * @return
     */
    public static RdfValueFactory getInstance() {
        if(instance == null){
            instance = new RdfValueFactory();
        }
        return instance;
    }

    private RdfValueFactory(){
        super();
    }

    @Override
    public RdfReference createReference(Object value) {
        if (value == null) {
            throw new NullPointerException("The parsed value MUST NOT be NULL");
        } else if (value instanceof UriRef) {
            return new RdfReference((UriRef) value);
        } else {
            return new RdfReference(value.toString());
        }
    }

    @Override
    public RdfText createText(Object value) {
        if (value == null) {
            throw new NullPointerException("The parsed value MUST NOT be NULL");
        } else if (value instanceof Literal) {
            return new RdfText((Literal) value);
        } else {
            return createText(value.toString(), null);
        }
    }

    @Override
    public RdfText createText(String text, String language) {
        return new RdfText(text, language);
    }

    @Override
    public RdfRepresentation createRepresentation(String id) {
        if (id == null){
           throw new NullPointerException("The parsed id MUST NOT be NULL!");
        } else if(id.isEmpty()){
            throw new IllegalArgumentException("The parsed id MUST NOT be empty!");
        } else {
            return createRdfRepresentation(new UriRef(id), new SimpleMGraph());
        }
    }

    /**
     * {@link RdfRepresentation} specific create Method based on an existing
     * RDF Graph.
     *
     * @param node The node of the node used for the representation. If this
     *     node is not part of the parsed graph, the resulting representation
     *     will be empty
     * @param graph the graph.
     * @return The representation based on the state of the parsed graph
     */
    public RdfRepresentation createRdfRepresentation(UriRef node, TripleCollection graph) {
        if (node == null) {
            throw new NullPointerException("The parsed id MUST NOT be NULL!");
        }
        if(graph == null){
            throw new NullPointerException("The parsed graph MUST NOT be NULL!");
        }
        return new RdfRepresentation(node, graph);
    }

    /**
     * Extracts the Graph for {@link RdfRepresentation} or creates a {@link Graph}
     * for all other implementations of {@link Representation}.
     *
     * @param representation the representation
     * @return the read only RDF Graph.
     */
    public RdfRepresentation toRdfRepresentation(Representation representation) {
        if (representation instanceof RdfRepresentation) {
            return (RdfRepresentation) representation;
        } else {
            //create the Clerezza Represenation
            RdfRepresentation clerezzaRep = createRepresentation(representation.getId());
            //Copy all values field by field
            for (Iterator<String> fields = representation.getFieldNames(); fields.hasNext();) {
                String field = fields.next();
                for (Iterator<Object> fieldValues = representation.get(field); fieldValues.hasNext();) {
                    clerezzaRep.add(field, fieldValues.next());
                }
            }
            return clerezzaRep;
        }
    }

}
