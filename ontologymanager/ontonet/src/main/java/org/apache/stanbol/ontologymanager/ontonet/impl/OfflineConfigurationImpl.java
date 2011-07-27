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
package org.apache.stanbol.ontologymanager.ontonet.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@link OfflineConfiguration}.
 */
@Component(immediate = true, metatype = true)
@Service
public class OfflineConfigurationImpl implements OfflineConfiguration {

    protected Logger log = LoggerFactory.getLogger(getClass());

    /**
     * TODO how do you use array initializers in Property annotations without causing compile errors?
     */
    @Property(name = OfflineConfiguration.ONTOLOGY_PATHS, value = {".", "/ontologies"})
    private String[] ontologyDirs;

    private List<IRI> locations = new ArrayList<IRI>();

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ONManagerConfigurationImpl instances do need to be
     * configured! YOU NEED TO USE {@link #ONManagerConfigurationImpl(Dictionary)} or its overloads, to parse
     * the configuration and then initialise the rule store if running outside an OSGI environment.
     */
    public OfflineConfigurationImpl() {}

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param configuration
     */
    public OfflineConfigurationImpl(Dictionary<String,Object> configuration) {
        this();
        activate(configuration);
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) {
        log.info("in {} activate with context {}", getClass(), context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    protected void activate(Dictionary<String,Object> configuration) {
        // Parse configuration.
        ontologyDirs = (String[]) configuration.get(OfflineConfiguration.ONTOLOGY_PATHS);
        if (ontologyDirs == null) ontologyDirs = new String[] {".", "/ontologies"};

        for (String path : ontologyDirs) {
            IRI iri = null;
            if (path.startsWith("/")) {
                try {
                    iri = IRI.create(getClass().getResource(path));
                } catch (Exception e) {
                    // TODO: Don't give up. It could still an absolute path.
                }
            } else try {
                iri = IRI.create(path);
            } catch (Exception e1) {
                try {
                    iri = IRI.create(new File(path));
                } catch (Exception e2) {
                    log.warn("Unable to obtain a path for {}. Skipping...", iri, e2);
                    iri = null;
                }
            }
            if (iri != null) locations.add(iri);
        }
        // else location stays empty.

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        ontologyDirs = null;
        log.info("in {} deactivate with context {}", getClass(), context);
    }

    @Override
    public List<IRI> getOntologySourceLocations() {
        return locations;
    }

}
