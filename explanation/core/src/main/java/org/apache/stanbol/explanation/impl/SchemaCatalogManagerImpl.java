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
package org.apache.stanbol.explanation.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.explanation.api.SchemaCatalog;
import org.apache.stanbol.explanation.api.SchemaCatalogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class SchemaCatalogManagerImpl implements SchemaCatalogManager {

    private static final String _ODP_CATALOG_DEFAULT = "http://www.ontologydesignpatterns.org/registry/explanation.owl";

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = SchemaCatalog.class, strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, bind = "bindSchemaCatalog", unbind = "unbindSchemaCatalog")
    private Map<String,SchemaCatalog> catalogs = Collections.emptyMap();

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void addCatalog(SchemaCatalog catalog) {
        Map<String,SchemaCatalog> tmp = new HashMap<String,SchemaCatalog>(catalogs);
        tmp.put(catalog.getId(), catalog);
        this.catalogs = Collections.unmodifiableMap(tmp);
    }

    /**
     * Invoked automatically by OSGi-DS when a new {@link SchemaCatalog} is registered. For non-DS-aware
     * environments, just use the inherited interface method {@link #addCatalog(SchemaCatalog)}.
     * 
     * @param catalog
     *            the schema catalog to be bound with this manager.
     */
    protected void bindSchemaCatalog(SchemaCatalog catalog) {
        if (catalog != null) {
            log.info("Binding catalog {} to catalog manager.", catalog.getId());
            addCatalog(catalog);
        }
    }

    @Override
    public Set<SchemaCatalog> getCatalogs() {
        return new HashSet<SchemaCatalog>(catalogs.values());
    }

    @Override
    public void removeCatalog(String id) {
        Map<String,SchemaCatalog> tmp = new HashMap<String,SchemaCatalog>(catalogs);
        tmp.remove(id);
        this.catalogs = Collections.unmodifiableMap(tmp);
    }

    /**
     * Invoked automatically by OSGi-DS when a {@link SchemaCatalog} is deregistered. For non-DS-aware
     * environments, just use the inherited interface method {@link #removeCatalog(String)}.
     * 
     * @param catalog
     *            the schema catalog to be unbound from this manager.
     */
    protected void unbindSchemaCatalog(SchemaCatalog catalog) {
        if (catalog != null && catalogs.containsKey(catalog.getId())) {
            log.info("Unbinding catalog {} from catalog manager.", catalog.getId());
            removeCatalog(catalog.getId());
        }
    }

}
