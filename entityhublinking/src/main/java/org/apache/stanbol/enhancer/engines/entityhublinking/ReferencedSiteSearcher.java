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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class ReferencedSiteSearcher extends TrackingEntitySearcher<Site> implements EntitySearcher {
    
    
    private final String siteId;
    private final Integer limit;
    private Map<UriRef,Collection<Resource>> originInfo;
    public ReferencedSiteSearcher(BundleContext context,String siteId, Integer limit){
        this(context,siteId,limit,null);
    }
    public ReferencedSiteSearcher(BundleContext context,String siteId, Integer limit, ServiceTrackerCustomizer customizer) {
        super(context, Site.class, 
            Collections.singletonMap(SiteConfiguration.ID,siteId),
            customizer);
        this.siteId = siteId;
        this.limit = limit != null && limit > 0 ? limit : null;
        this.originInfo = Collections.singletonMap(
            new UriRef(RdfResourceEnum.site.getUri()), 
            (Collection<Resource>)Collections.singleton(
                (Resource)new PlainLiteralImpl(siteId)));
    }
    
    @Override
    public Entity get(UriRef id,Set<UriRef> includeFields) {
        if(id == null || id.getUnicodeString().isEmpty()){
            return null;
        }
        org.apache.stanbol.entityhub.servicesapi.model.Entity entity;
        Site site = getSearchService();
        if(site == null){
            throw new IllegalStateException("ReferencedSite "+siteId+" is currently not available");
        }
        try {
            entity = site.getEntity(id.getUnicodeString());
        }  catch (SiteException e) {
            throw new IllegalStateException("Exception while getting "+id+
                " from the ReferencedSite "+site.getId(),e);
        }
        return entity == null ? null : new EntityhubEntity(entity.getRepresentation());
    }

    @Override
    public Collection<? extends Entity> lookup(UriRef field,
                                           Set<UriRef> includeFields,
                                           List<String> search,
                                           String[] languages,
                                           Integer limit) throws IllegalStateException {
        //build the query and than return the result
        Site site = getSearchService();
        if(site == null){
            throw new IllegalStateException("ReferencedSite "+siteId+" is currently not available");
        }
        FieldQuery query = EntitySearcherUtils.createFieldQuery(site.getQueryFactory(), 
            field, includeFields, search, languages);
        if(limit != null && limit > 0){
            query.setLimit(limit);
        } else if(this.limit != null){
            query.setLimit(this.limit);
        }
        QueryResultList<Representation> results;
        try {
            results = site.find(query);
        } catch (SiteException e) {
            throw new IllegalStateException("Exception while searchign for "+
                search+'@'+Arrays.toString(languages)+"in the ReferencedSite "+
                site.getId(), e);
        }
        Collection<Entity> entities = new ArrayList<Entity>(results.size());
        for(Representation result : results){
            entities.add(new EntityhubEntity(result));
        }
        return entities;
        }

    @Override
    public boolean supportsOfflineMode() {
        Site site = getSearchService();
        //Do not throw an exception here if the site is not available. Just return false
        return site == null ? false : site.supportsLocalMode();
    }

    @Override
    public Integer getLimit() {
        return limit;
    }
    
    @Override
    public Map<UriRef,Collection<Resource>> getOriginInformation() {
        return originInfo;
    }
}
