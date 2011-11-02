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
package org.apache.stanbol.ontologymanager.ontonet.impl.owlapi;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OWLOntologyManagerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author alexdma
 * @deprecated use the static methods of {@link org.apache.stanbol.owl.OWLOntologyManagerFactory} instead.
 */
public class OWLOntologyManagerFactoryImpl implements OWLOntologyManagerFactory {

    private List<OWLOntologyIRIMapper> iriMappers;

    private Logger log = LoggerFactory.getLogger(getClass());

    public OWLOntologyManagerFactoryImpl() {
        this(null);
    }

    /**
     * 
     * @param dirs
     */
    public OWLOntologyManagerFactoryImpl(List<String> dirs) {

        if (dirs != null) {
            iriMappers = new ArrayList<OWLOntologyIRIMapper>(dirs.size());
            for (String path : dirs) {
                File dir = null;
                if (path.startsWith("/")) {
                    try {
                        dir = new File(getClass().getResource(path).toURI());
                    } catch (Exception e) {
                        // Don't give up. It could still an absolute path.
                    }
                } else try {
                    dir = new File(path);
                } catch (Exception e1) {
                    try {
                        dir = new File(URI.create(path));
                    } catch (Exception e2) {
                        log.warn("Unable to obtain a path for {}", dir, e2);
                    }
                }
                if (dir != null && dir.isDirectory()) iriMappers.add(new AutoIRIMapper(dir, true));
            }
        }
    }

    @Override
    public OWLOntologyManager createOntologyManager(boolean withOfflineSupport) {
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        if (withOfflineSupport) for (OWLOntologyIRIMapper mapper : getLocalIRIMapperList())
            mgr.addIRIMapper(mapper);
        return mgr;
    }

    @Override
    public List<OWLOntologyIRIMapper> getLocalIRIMapperList() {
        return iriMappers;
    }

}
