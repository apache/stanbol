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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.collector;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;

import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.impl.CoreSpaceImpl;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.impl.CustomSpaceImpl;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.impl.ScopeImpl;
import org.apache.stanbol.ontologymanager.servicesapi.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace.SpaceType;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeEventListener;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeFactory;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link OntologySpaceFactory} based on Clerezza.
 * 
 * @author alexdma
 * 
 */
@Component(immediate = true, metatype = false)
@Service({OntologySpaceFactory.class, ScopeFactory.class})
public class ClerezzaCollectorFactory implements OntologySpaceFactory, ScopeFactory {

    protected Collection<ScopeEventListener> listeners;

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected IRI namespace;

    @Reference
    private OntologyProvider<TcProvider> ontologyProvider;

    public ClerezzaCollectorFactory() {
        listeners = new HashSet<ScopeEventListener>();
    }

    public ClerezzaCollectorFactory(OntologyProvider<TcProvider> provider,
                                    Dictionary<String,Object> configuration) {
        this();
        this.ontologyProvider = provider;
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
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + ClerezzaCollectorFactory.class + " activate with context " + context);
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
        log.debug(ClerezzaCollectorFactory.class + " activated.");
    }

    @Override
    public void addScopeEventListener(ScopeEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void clearScopeEventListeners() {
        listeners.clear();
    }

    private void configureSpace(OntologySpace s, String scopeID) {
        // Make sure someone is listening to ontology additions before core ontologies are added.
        if (ontologyProvider instanceof OntologyCollectorListener) s
                .addOntologyCollectorListener((OntologyCollectorListener) ontologyProvider);
        else s.addOntologyCollectorListener(ontologyProvider.getOntologyNetworkDescriptor());
        // s.setUp();
    }

    /**
     * Utility method for configuring ontology spaces after creating them.
     * 
     * @param s
     * @param scopeID
     * @param rootSource
     */
    private void configureSpace(OntologySpace s, String scopeID, OntologyInputSource<?>... ontologySources) {
        configureSpace(s, scopeID);
        if (ontologySources != null) try {
            for (OntologyInputSource<?> src : ontologySources)
                s.addOntology(src);
        } catch (UnmodifiableOntologyCollectorException e) {
            log.error("Ontology space " + s.getID() + " was found locked at creation time!", e);
        }
        // s.setUp();
    }

    @Override
    public OntologySpace createCoreOntologySpace(String scopeId, OntologyInputSource<?>... coreSources) {
        OntologySpace s = new CoreSpaceImpl(scopeId, namespace, ontologyProvider);
        configureSpace(s, scopeId, coreSources);
        return s;
    }

    @Override
    public OntologySpace createCustomOntologySpace(String scopeId, OntologyInputSource<?>... customSources) {
        OntologySpace s = new CustomSpaceImpl(scopeId, namespace, ontologyProvider);
        configureSpace(s, scopeId, customSources);
        return s;
    }

    @Override
    public Scope createOntologyScope(String scopeID, OntologyInputSource<?>... coreOntologies) throws DuplicateIDException {
        // Scope constructor also creates core and custom spaces
        Scope scope = new ScopeImpl(scopeID, getDefaultNamespace(), this, coreOntologies);
        fireScopeCreated(scope);
        return scope;
    }

    @Override
    public OntologySpace createOntologySpace(String scopeId,
                                             SpaceType type,
                                             OntologyInputSource<?>... ontologySources) {
        switch (type) {
            case CORE:
                return createCoreOntologySpace(scopeId, ontologySources);
            case CUSTOM:
                return createCustomOntologySpace(scopeId, ontologySources);
            default:
                return null;
        }
    }

    /**
     * Deactivation of the ONManagerImpl resets all its resources.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        namespace = null;
        log.info("in " + ClerezzaCollectorFactory.class + " deactivate with context " + context);
    }

    protected void fireScopeCreated(Scope scope) {
        for (ScopeEventListener l : listeners)
            l.scopeCreated(scope);
    }

    @Override
    public IRI getDefaultNamespace() {
        return this.namespace;
    }

    @Override
    public String getID() {
        return this.toString();
    }

    @Override
    public IRI getNamespace() {
        return getDefaultNamespace();
    }

    @Override
    public Collection<ScopeEventListener> getScopeEventListeners() {
        return listeners;
    }

    @Override
    public void removeScopeEventListener(ScopeEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setDefaultNamespace(IRI namespace) {
        this.namespace = namespace;
    }

    @Override
    public void setNamespace(IRI namespace) {
        setDefaultNamespace(namespace);
    }

}
