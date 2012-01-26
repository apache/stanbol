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
package org.apache.stanbol.contenthub.search.related.referencedsite;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.search.related.RelatedKeywordImpl;
import org.apache.stanbol.contenthub.servicesapi.Constants;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeywordSearch;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.impl.Util;

/**
 * This class is constructed with an rdf model that will be queried and extracts semantically related entities
 * according to the entity type's
 * 
 * @author anil.pacaci
 */
@Component
@Service
public class ReferencedSiteSearch implements RelatedKeywordSearch {
    private static String DBPEDIA_ONT_NAMESPACE = "http://dbpedia.org/ontology/";

    /**
     * dbpedia-owl:place ranged properties for related places
     */
    public final static List<String> placeTypedProperties = Arrays
            .asList(new String[] {DBPEDIA_ONT_NAMESPACE + DBPEDIA_ONT_NAMESPACE + "country",
                                  DBPEDIA_ONT_NAMESPACE + "largestCity", DBPEDIA_ONT_NAMESPACE + "city",
                                  DBPEDIA_ONT_NAMESPACE + "state", DBPEDIA_ONT_NAMESPACE + "capital",
                                  DBPEDIA_ONT_NAMESPACE + "isPartOf", DBPEDIA_ONT_NAMESPACE + "part",
                                  DBPEDIA_ONT_NAMESPACE + "deathPlace", DBPEDIA_ONT_NAMESPACE + "birthPlace",
                                  DBPEDIA_ONT_NAMESPACE + "location"});

    /**
     * dbpedia-owl:person ranged properties for related persons
     */
    public final static List<String> personTypedProperties = Arrays
            .asList(new String[] {DBPEDIA_ONT_NAMESPACE + "leader", DBPEDIA_ONT_NAMESPACE + "leaderName",
                                  DBPEDIA_ONT_NAMESPACE + "child", DBPEDIA_ONT_NAMESPACE + "spouse",
                                  DBPEDIA_ONT_NAMESPACE + "partner", DBPEDIA_ONT_NAMESPACE + "president"});

    /**
     * dbpedia-owl:organization ranged properties for related organizations
     */
    public final static List<String> organizationTypedProperties = Arrays
            .asList(new String[] {DBPEDIA_ONT_NAMESPACE + "leaderParty",
                                  DBPEDIA_ONT_NAMESPACE + "affiliation", DBPEDIA_ONT_NAMESPACE + "team",
                                  DBPEDIA_ONT_NAMESPACE + "party", DBPEDIA_ONT_NAMESPACE + "otherParty",
                                  DBPEDIA_ONT_NAMESPACE + "associatedBand"});

    private static final Logger logger = LoggerFactory.getLogger(ReferencedSiteSearch.class);

    @Reference
    private Entityhub entityhub;

    @Reference
    private ReferencedSiteManager referencedSiteManager;

    @Override
    public Map<String,List<RelatedKeyword>> search(String keyword) throws SearchException {
        Map<String,List<RelatedKeyword>> results = new HashMap<String,List<RelatedKeyword>>();
        FieldQuery fieldQuery = getFieldQuery(keyword);
        QueryResultList<Representation> externalEnties = referencedSiteManager.find(fieldQuery);
        String entityId = null;
        if (externalEnties != null && externalEnties.size() > 0) {
            entityId = externalEnties.iterator().next().getId();
            try {
                Entity entity = entityhub.lookupLocalEntity(entityId, true);
                if (entity != null) {
                    results = getRelatedKeywordsFromEntity(entity);
                } else {
                    logger.warn("There is no obtained external entity having id: {}", entityId);
                }
            } catch (EntityhubException e) {
                logger.error("Got entityhub exception while looking up for entity: {}", entityId);
            }
        }
        return results;
    }

    @Override
    public Map<String,List<RelatedKeyword>> search(String keyword, String ontologyURI) throws SearchException {
        return search(keyword);
    }

    private FieldQuery getFieldQuery(String keyword) {
        FieldQueryFactory qf = DefaultQueryFactory.getInstance();
        FieldQuery fieldQuery = qf.createFieldQuery();
        Collection<String> selectedFields = new ArrayList<String>();
        selectedFields.add(RDFS.label.getUnicodeString());
        fieldQuery.addSelectedFields(selectedFields);
        fieldQuery.setConstraint(RDFS.label.getUnicodeString(), new TextConstraint(keyword,
                PatternType.wildcard, false, "en"));
        fieldQuery.setLimit(1);
        fieldQuery.setOffset(0);
        return fieldQuery;
    }

    private Map<String,List<RelatedKeyword>> getRelatedKeywordsFromEntity(Entity entity) {
        Map<String,List<RelatedKeyword>> results = new HashMap<String,List<RelatedKeyword>>();
        Representation entityRep = entity.getRepresentation();
        for (Iterator<String> fields = entityRep.getFieldNames(); fields.hasNext();) {
            String field = fields.next();
            processProperties(organizationTypedProperties, entity.getRepresentation(), entity.getSite()
                                                                                       + "#Organization",
                field, results);
            processProperties(personTypedProperties, entity.getRepresentation(),
                entity.getSite() + "#Person", field, results);
            processProperties(placeTypedProperties, entity.getRepresentation(), entity.getSite() + "#Place",
                field, results);
        }
        return results;
    }

    private void processProperties(List<String> semanticProperties,
                                   Representation entityRep,
                                   String source,
                                   String field,
                                   Map<String,List<RelatedKeyword>> results) {
        if (semanticProperties.contains(field)) {
            for (Iterator<Object> fieldValues = entityRep.get(field); fieldValues.hasNext();) {
                Object fieldValue = fieldValues.next();
                String fieldValueStr;
                try {
                    fieldValueStr = URLDecoder.decode(fieldValue.toString(), Constants.DEFAULT_ENCODING);
                    fieldValueStr = fieldValueStr.substring(Util.splitNamespace(fieldValueStr));
                } catch (UnsupportedEncodingException e) {
                    logger.warn("Unsupported encoding while trying to decode to related entity URI", e);
                    continue;
                }
                RelatedKeyword rkw = new RelatedKeywordImpl(fieldValueStr, 0, source);
                if (results.containsKey(source)) {
                    results.get(source).add(rkw);
                } else {
                    List<RelatedKeyword> rkwList = new ArrayList<RelatedKeyword>();
                    rkwList.add(rkw);
                    results.put(source, rkwList);
                }
            }
        }
    }

}
