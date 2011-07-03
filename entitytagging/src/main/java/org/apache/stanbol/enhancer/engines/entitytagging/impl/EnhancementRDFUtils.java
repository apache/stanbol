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
package org.apache.stanbol.enhancer.engines.entitytagging.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDFS_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

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
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * Utility taken form the engine.autotagging bundle and adapted from using TagInfo to {@link Entity}.
 * 
 * @author Rupert Westenthaler
 * @author ogrisel (original utility)
 */
public class EnhancementRDFUtils {

    /**
     * @param literalFactory
     *            the LiteralFactory to use
     * @param graph
     *            the MGraph to use
     * @param contentItemId
     *            the contentItemId the enhancement is extracted from
     * @param relatedEnhancements
     *            enhancements this textAnnotation is related to
     * @param entity
     *            the related entity
     * @param nameField the field used to extract the name
     */
    public static UriRef writeEntityAnnotation(EnhancementEngine engine,
                                               LiteralFactory literalFactory,
                                               MGraph graph,
                                               UriRef contentItemId,
                                               Collection<NonLiteral> relatedEnhancements,
                                               Representation rep,
                                               String nameField) {
        // 1. check if the returned Entity does has a label -> if not return null
        // add labels (set only a single label. Use "en" if available!
        Text label = null;
        Iterator<Text> labels = rep.getText(nameField);
        while (labels.hasNext()) {
            Text actLabel = labels.next();
            if (label == null) {
                label = actLabel;
            } else {
                //use startWith to match also en-GB and en-US ...
                if (actLabel.getLanguage() != null && actLabel.getLanguage().startsWith("en")) {
                    label = actLabel;
                }
            }
        }
        if (label == null) {
            return null;
        }
        Literal literal;
        if (label.getLanguage() == null) {
            literal = new PlainLiteralImpl(label.getText());
        } else {
            literal = new PlainLiteralImpl(label.getText(), new Language(label.getLanguage()));
        }
        // Now create the entityAnnotation
        UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(graph, engine,
            contentItemId);
        // first relate this entity annotation to the text annotation(s)
        for (NonLiteral enhancement : relatedEnhancements) {
            graph.add(new TripleImpl(entityAnnotation, DC_RELATION, enhancement));
        }
        UriRef entityUri = new UriRef(rep.getId());
        // add the link to the referred entity
        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_REFERENCE, entityUri));
        // add the label parsed above
        graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_LABEL, literal));
        // TODO: add real confidence values!
        // -> in case of SolrYards this will be a Lucene score and not within the range [0..1]
        // -> in case of SPARQL there will be no score information at all.
        Object score = rep.getFirst(RdfResourceEnum.resultScore.getUri());
        Double scoreValue = new Double(-1); // use -1 if no score is available!
        if (score != null) {
            try {
                scoreValue = Double.valueOf(score.toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        graph.add(new TripleImpl(entityAnnotation, ENHANCER_CONFIDENCE, literalFactory
                .createTypedLiteral(scoreValue)));

        Iterator<Reference> types = rep.getReferences(RDF_TYPE.getUnicodeString());
        while (types.hasNext()) {
            graph.add(new TripleImpl(entityAnnotation, ENHANCER_ENTITY_TYPE, new UriRef(types.next()
                    .getReference())));
        }
        // TODO: for now add the information about this entity to the graph
        // -> this might be replaced by some additional engine at the end
        // RdfValueFactory rdfValueFactory = RdfValueFactory.getInstance();
        // RdfRepresentation representation = rdfValueFactory.toRdfRepresentation(entity.getRepresentation());
        // graph.addAll(representation.getRdfGraph());
        return entityAnnotation;
    }

}
