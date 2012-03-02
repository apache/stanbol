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
package org.apache.stanbol.explanation.impl.clerezza;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.explanation.api.Configuration;
import org.apache.stanbol.explanation.api.Schema;
import org.apache.stanbol.explanation.api.SchemaMatcher;
import org.apache.stanbol.explanation.impl.ClerezzaSchemaMatcher;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.BlankOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true)
@Service(SchemaMatcher.class)
public class ClerezzaSchemaMatcherImpl implements ClerezzaSchemaMatcher {

    private Graph knowledgeBase;

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ONManager onm;

    private OntologyScope scopeSchemaMatching = null;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ExplanationGeneratorImpl instances do need to be
     * configured! YOU NEED TO USE {@link #ExplanationEnvironmentConfiguration(ONManager, Dictionary)} or its
     * overloads, to parse the configuration and then initialise the rule store if running outside an OSGI
     * environment.
     */
    public ClerezzaSchemaMatcherImpl() {
        super();
    }

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param onm
     * @param configuration
     */
    public ClerezzaSchemaMatcherImpl(ONManager onm, Dictionary<String,Object> configuration) {
        this();
        this.onm = onm;
        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     *             if there is no valid component context.
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + Configuration.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {
        // TODO load at least the explanation metamodel here.
        String id = "ExplanationSchemaMatching";

        try {
            scopeSchemaMatching = onm.getOntologyScopeFactory().createOntologyScope(id,
                new BlankOntologySource());
        } catch (DuplicateIDException e) {
            scopeSchemaMatching = onm.getScopeRegistry().getScope(e.getDuplicateID());
        }
        onm.getScopeRegistry().registerScope(scopeSchemaMatching, true);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + Configuration.class + " deactivate with context " + context);
    }

    @Override
    public Set<Schema> getSatisfiableSchemas(Set<Library> catalogs, UriRef entity) {

        for (Library l : catalogs) {
            log.info("Library {}", l);
            try {
                for (OWLOntology o : l.getOntologies())
                    log.info("\t{}", o);
            } catch (RegistryContentException ex) {
                log.warn("Invalid content in library " + l, ex);
                continue;
            }
        }

        Graph ctx = new GraphNode(entity, knowledgeBase).getNodeContext();

        Iterator<Triple> it = ctx.iterator();
        while (it.hasNext()) {
            Triple t = it.next();
            log.info("In l=1 context of {} : triple {}", entity, t);
        }

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setKnowledgeBase(Graph knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
        OntologySpace cs = null;
        try {
            cs = scopeSchemaMatching.getCustomSpace();
            cs.tearDown();
            // FIXME Ugly
            scopeSchemaMatching.getCustomSpace().addOntology(
                new RootOntologySource(OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(new GraphSource(
                        knowledgeBase).getRootOntology())));
        } catch (UnmodifiableOntologyCollectorException e) {
            log.error("Failed to change knowledge base in unmodifiable ontolgy space {}",
                e.getOntologyCollector());
        } finally {
            cs.setUp();
        }
    }

}
