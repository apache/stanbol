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
package org.apache.stanbol.enhancer.engine.disambiguation.mlt;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;

/**
 * Collects all data needed for Disambiguation
 * 
 * @author Rupert Westenthaler
 * @author Kritarth
 * 
 */
public final class DisambiguationData {

    /**
     * used by #c
     */
    private DisambiguationData() {}

    /**
     * Stores the URIs of fise:EntityAnnnotation as key and the fise:TextAnnotation they link to as value.
     * <p>
     * This is needed during writing the disambiguation results to the EnhancementStructure to know if one
     * needs to clone an fise:EntityAnnotation or not.
     */
    public Map<IRI,Set<IRI>> suggestionMap = new HashMap<IRI,Set<IRI>>();

    /**
     * Holds the center position of the fise:TextAnnotation fise:selected-text as key and the SavedEntity
     * (representing the extracted data for the fise:TextAnnotation) as value.
     * <p>
     * Intended to do fast index based lookup for other TextAnnotations when building contexts for
     * disambiguations.
     */
    public NavigableMap<Integer,SavedEntity> directoryTextAnotation = new TreeMap<Integer,SavedEntity>();
    /**
     * Collection with the 'fise:selected-text' of all 'fise:TextAnnotations' Also those that are NOT included
     * in {@link #textAnnotations} (e.g. because they are missing some required data)
     */
    public Collection<String> allSelectedTexts = new HashSet<String>();
    /**
     * List of all fise:textAnnotations that can be used for disambiguation. the key is the URI and the value
     * is the {@link SavedEntity} with the extracted information.
     */
    public Map<IRI,SavedEntity> textAnnotations = new HashMap<IRI,SavedEntity>();

    // List to contain old confidence values that are to removed
    // List<Triple> loseConfidence = new ArrayList<Triple>();
    // List to contain new confidence values to be added to metadata
    // List<Triple> gainConfidence = new ArrayList<Triple>();

    /*
     * We create a data structure that stores the mapping of text annotation to List of Uri of all possible
     * amiguations of the Text. Also it fills the list loseconfidence with confidence values of all the
     * ambiguations for all entities (which will be removed eventually)
     */
    public static DisambiguationData createFromContentItem(ContentItem ci) {
        Graph graph = ci.getMetadata();
        DisambiguationData data = new DisambiguationData();
        Iterator<Triple> it = graph.filter(null, RDF_TYPE, TechnicalClasses.ENHANCER_TEXTANNOTATION);
        while (it.hasNext()) {
            IRI uri = (IRI) it.next().getSubject();
            // TODO: rwesten: do we really want to ignore fise:TextAnnotations that link to
            // to an other one (typically two TextAnnotations that select the exact same text)
            // if (graph.filter(uri, new IRI(NamespaceEnum.dc + "relation"), null).hasNext()) {
            // continue;
            // }

            SavedEntity savedEntity = SavedEntity.createFromTextAnnotation(graph, uri);
            if (savedEntity != null) {
                // data.allEntities.add(savedEntity.getContext());
                data.directoryTextAnotation.put(
                    Integer.valueOf((savedEntity.getStart() + savedEntity.getEnd()) / 2), savedEntity);
                // add information to the #suggestionMap
                for (Suggestion s : savedEntity.getSuggestions()) {
                    Set<IRI> textAnnotations = data.suggestionMap.get(s.getEntityAnnotation());
                    if (textAnnotations == null) {
                        textAnnotations = new HashSet<IRI>();
                        data.suggestionMap.put(s.getEntityAnnotation(), textAnnotations);
                    }
                    textAnnotations.add(savedEntity.getUri());
                }
                // NOTE (rwesten):
                // changed the layout here. Now savedEntity contains the list
                // of suggestions
                data.textAnnotations.put(uri, savedEntity);
                data.allSelectedTexts.add(savedEntity.getName());
            } else { // some information are also needed for other TextAnnotations
                // like the selectedText of TextAnnotations (regardless if they
                // have suggestions or not
                String selectedText = EnhancementEngineHelper.getString(graph, uri, ENHANCER_SELECTED_TEXT);
                if (selectedText != null) {
                    data.allSelectedTexts.add(selectedText);
                }
            }

        }
        return data;
    }
}
