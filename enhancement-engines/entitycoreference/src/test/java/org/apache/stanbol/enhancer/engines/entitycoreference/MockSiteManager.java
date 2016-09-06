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

package org.apache.stanbol.enhancer.engines.entitycoreference;

import java.io.InputStream;
import java.util.Collection;

import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;

/**
 * 
 * @author Cristian Petroaca
 *
 */
public class MockSiteManager implements SiteManager {

	@Override
	public boolean isReferred(String id) {
		return false;
	}

	@Override
	public Site getSite(String id) {
		if (id.equals(MockEntityCorefDbpediaSite.SITE_ID)) {
			return new MockEntityCorefDbpediaSite();
		}

		return null;
	}

	@Override
	public Collection<Site> getSitesByEntityPrefix(String entityUri) {
		return null;
	}

	@Override
	public Entity getEntity(String reference) {
		return null;
	}

	@Override
	public QueryResultList<Entity> findEntities(FieldQuery query) {
		return null;
	}

	@Override
	public QueryResultList<Representation> find(FieldQuery query) {
		return null;
	}

	@Override
	public QueryResultList<String> findIds(FieldQuery query) {
		return null;
	}

	@Override
	public InputStream getContent(String entity, String contentType) {
		return null;
	}

	@Override
	public Collection<String> getSiteIds() {
		return null;
	}

}
