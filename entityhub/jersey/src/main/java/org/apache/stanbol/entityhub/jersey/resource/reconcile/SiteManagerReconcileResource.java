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
package org.apache.stanbol.entityhub.jersey.resource.reconcile;

import javax.ws.rs.Path;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;

@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/entityhub/sites/reconcile")
public class SiteManagerReconcileResource extends BaseGoogleRefineReconcileResource {

    @Reference
    SiteManager siteManager;
    

    @Override
    protected QueryResultList<Representation> performQuery(String siteId, FieldQuery query) throws SiteException {
        return siteManager.find(query);
    }

    @Override
    protected String getSiteName(String siteId) {
        return "Referenced Site Manager (all sites)";
    }

    @Override
    protected FieldQuery createFieldQuery(String siteId) {
        return DefaultQueryFactory.getInstance().createFieldQuery();
    }

}
