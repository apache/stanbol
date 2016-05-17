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
package org.apache.stanbol.enhancer.engines.entityhublinking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcherException;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class EntityhubSearcher extends TrackingEntitySearcher<Entityhub> implements EntitySearcher {
    
    private final Integer limit;
    private Map<IRI,Collection<RDFTerm>> originInfo;

    public EntityhubSearcher(BundleContext context, Integer limit) {
        this(context,limit,null);
    }
    public EntityhubSearcher(BundleContext context, Integer limit,ServiceTrackerCustomizer customizer) {
        super(context,Entityhub.class,null,customizer);
        this.limit = limit != null && limit > 0 ? limit : null;
        this.originInfo = Collections.singletonMap(
            new IRI(RdfResourceEnum.site.getUri()), 
            (Collection<RDFTerm>)Collections.singleton(
                (RDFTerm)new PlainLiteralImpl("entityhub")));
    }
    
    @Override
    public Entity get(IRI id,Set<IRI> fields, String...languages) throws EntitySearcherException {
        if(id == null || id.getUnicodeString().isEmpty()){
            return null;
        }
        Entityhub entityhub = getSearchService();
        if(entityhub == null){
            throw new EntitySearcherException("The Entityhub is currently not active");
        }
        org.apache.stanbol.entityhub.servicesapi.model.Entity entity;
        try {
            entity = entityhub.getEntity(id.getUnicodeString());
        }  catch (EntityhubException e) {
            throw new EntitySearcherException("Exception while getting "+id+
                " from the Entityhub",e);
        }
        if(entity != null){
            Set<String> languageSet;
            if(languages == null || languages.length < 1){
                languageSet = null;
            } else if (languages.length == 1){
                languageSet = Collections.singleton(languages[0]);
            } else {
                languageSet = new HashSet<String>(Arrays.asList(languages));
            }
            return new EntityhubEntity(entity.getRepresentation(), fields, languageSet);
        } else {
            return null;
        }
    }

    @Override
    public Collection<? extends Entity> lookup(IRI field,
                                           Set<IRI> includeFields,
                                           List<String> search,
                                           String[] languages,
                                           Integer limit, Integer offset) throws EntitySearcherException {
        Entityhub entityhub = getSearchService();
        if(entityhub == null){
            throw new EntitySearcherException("The Entityhub is currently not active");
        }
        FieldQuery query = EntitySearcherUtils.createFieldQuery(entityhub.getQueryFactory(),
            field, includeFields, search, languages);
        if(limit != null && limit > 0){
            query.setLimit(limit);
        } else if(this.limit != null){
            query.setLimit(this.limit);
        }
        if(offset != null && offset.intValue() > 0){
            query.setOffset(offset.intValue());
        }
        QueryResultList<Representation> results;
        try {
            results = entityhub.find(query);
        } catch (EntityhubException e) {
            throw new EntitySearcherException("Exception while searchign for "+
                search+'@'+Arrays.toString(languages)+"in the Entityhub", e);
        }
        if(!results.isEmpty()){
            Set<String> languagesSet = new HashSet<String>(Arrays.asList(languages));
            Collection<Entity> entities = new ArrayList<Entity>(results.size());
            for(Representation result : results){
                entities.add(new EntityhubEntity(result, null, languagesSet));
            }
            return entities;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean supportsOfflineMode() {
        return true; //the entityhub is always offline
    }

    @Override
    public Integer getLimit() {
        return limit;
    }

    @Override
    public Map<IRI,Collection<RDFTerm>> getOriginInformation() {
        return originInfo;
    }
    
}
