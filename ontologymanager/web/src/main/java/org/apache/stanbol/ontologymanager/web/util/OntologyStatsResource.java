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
package org.apache.stanbol.ontologymanager.web.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollector;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyUtils;
import org.apache.stanbol.ontologymanager.web.resources.AbstractOntologyAccessResource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * Will be used for human-readable rendering of OWL ontologies.
 * 
 * @author alexdma
 * 
 */
public class OntologyStatsResource extends AbstractOntologyAccessResource {

    private Set<OntologyCollector> handles;

    private Set<OWLOntologyID> identifiers;

    private OWLOntology o;

    public OntologyStatsResource(
                                 UriInfo uriInfo,
                                 OWLOntologyID key,
                                 OWLOntology o,
                                 Set<OWLOntologyID> identifiers,
                                 Set<OntologyCollector> handles) {
//        this.servletContext = context;
        this.uriInfo = uriInfo;
        this.submitted = key;
        this.o = o;
        this.identifiers = identifiers;
        this.handles = handles;
    }

    public Set<String> getAliases() {
        Set<String> aliases = new HashSet<String>();
        for (OWLOntologyID alias : identifiers)
            // if (!o.getOntologyID().equals(alias))
            aliases.add(OntologyUtils.encode(alias));
        return Collections.unmodifiableSet(aliases);
    }

    public Set<String> getScopeHandles() {
        Set<String> handles = new HashSet<String>();
        for (OntologyCollector handle : this.handles)
            if (handle instanceof OntologySpace) handles.add(handle.getID());
        return handles;
    }

    public Set<String> getSessionHandles() {
        Set<String> handles = new HashSet<String>();
        for (OntologyCollector handle : this.handles)
            if (handle instanceof Session) handles.add(handle.getID());
        return handles;
    }

    public int getTotalAxioms() {
        return o.getAxiomCount();
    }

}
