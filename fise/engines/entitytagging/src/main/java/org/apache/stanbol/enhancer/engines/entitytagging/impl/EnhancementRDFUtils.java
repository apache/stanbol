package org.apache.stanbol.enhancer.engines.entitytagging.impl;

import java.util.Collection;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;


import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.*;

/**
 * Utility taken form the engine.autotagging bundle and adapted from
 * using TagInfo to {@link Sign}.
 *
 * @author Rupert Westenthaler
 * @author ogrisel (original utility)
 */
public class EnhancementRDFUtils {

    /**
     * @param literalFactory the LiteralFactory to use
     * @param graph the MGraph to use
     * @param contentItemId the contentItemId the enhancement is extracted from
     * @param relatedEnhancements enhancements this textAnnotation is related to
     * @param entity the related entity
     */
    public static UriRef writeEntityAnnotation(EnhancementEngine engine, LiteralFactory literalFactory,
            MGraph graph, UriRef contentItemId, Collection<NonLiteral> relatedEnhancements, Sign entity) {
        //1. check if the returned Entity does has a label -> if not return null
        //add labels (set only a single label. Use "en" if available!
        Text label = null;
        Iterator<Text> labels = entity.getRepresentation().getText(RDFS_LABEL.getUnicodeString());
        while (labels.hasNext()) {
            Text actLabel  = labels.next();
            if(label == null){
                label = actLabel;
            } else {
                if("en".equals(actLabel.getLanguage())){
                    label = actLabel;
                }
            }
        }
        if (label == null){
            return null;
        }
        Literal literal;
        if (label.getLanguage() == null){
            literal = new PlainLiteralImpl(label.getText());
        } else {
            literal = new PlainLiteralImpl(label.getText(), new Language(label.getLanguage()));
        }
        //Now create the entityAnnotation
        UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(
                graph, engine, contentItemId);
        // first relate this entity annotation to the text annotation(s)
        for (NonLiteral enhancement: relatedEnhancements){
            graph.add(new TripleImpl(entityAnnotation, DC_RELATION, enhancement));
        }
        UriRef entityUri = new UriRef(entity.getId());
        // add the link to the referred entity
        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_REFERENCE, entityUri));
        //add the label parsed above
        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_LABEL, literal));
        //TODO: add real confidence values!
        // -> in case of SolrYards this will be a Lucene score and not within the range [0..1]
        // -> in case of SPARQL there will be no score information at all.
        Object score = entity.getRepresentation().getFirst(RdfResourceEnum.resultScore.getUri());
        Double scoreValue = new Double(-1); //use -1 if no score is available!
        if (score != null){
            try {
                scoreValue = Double.valueOf(score.toString());
            } catch (NumberFormatException e) {
                //ignore
            }
        }
        graph.add(new TripleImpl(entityAnnotation,
                ENHANCER_CONFIDENCE,
                literalFactory.createTypedLiteral(scoreValue)));

        Iterator<Reference> types = entity.getRepresentation().getReferences(RDF_TYPE.getUnicodeString());
        while (types.hasNext()) {
            graph.add(new TripleImpl(entityAnnotation,
                    ENHANCER_ENTITY_TYPE, new UriRef(types.next().getReference())));
        }
        //TODO: for now add the information about this entity to the graph
        // -> this might be replaced by some additional engine at the end
//        RdfValueFactory rdfValueFactory = RdfValueFactory.getInstance();
//        RdfRepresentation representation = rdfValueFactory.toRdfRepresentation(entity.getRepresentation());
//        graph.addAll(representation.getRdfGraph());
        return entityAnnotation;
    }

}
