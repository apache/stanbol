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
package org.apache.stanbol.reengineer.base.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.Reengineer;
import org.apache.stanbol.reengineer.base.api.ReengineerManager;
import org.apache.stanbol.reengineer.base.api.ReengineeringException;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete implementation of the {@link org.apache.stanbol.reengineer.base.api.ReengineerManager} interface.
 * 
 * @author andrea.nuzzolese
 * 
 */

@Component(immediate = true, metatype = true)
@Service(ReengineerManager.class)
public class ReengineerManagerImpl implements ReengineerManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ArrayList<Reengineer> reengineers;

    /**
     * Default constructor EXCLUSIVE to OSGi environments with declarative services.
     */
    public ReengineerManagerImpl() {}

    /**
     * Basic constructor to be used if outside of an OSGi environment. Invokes default constructor.
     */
    public ReengineerManagerImpl(Dictionary<String,Object> configuration) {
        activate(configuration);
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + ReengineerManagerImpl.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    protected void activate(Dictionary<String,Object> configuration) {

        reengineers = new ArrayList<Reengineer>();

    }

    /**
     * @param semionReengineer
     *            {@link org.apache.stanbol.reengineer.base.api.Reengineer}
     * @return true if the reengineer is bound, false otherwise
     */
    @Override
    public boolean bindReengineer(Reengineer semionReengineer) {
        boolean found = false;
        Iterator<Reengineer> it = reengineers.iterator();
        while (it.hasNext() && !found) {
            Reengineer reengineer = it.next();
            if (reengineer.getReengineerType() == semionReengineer.getReengineerType()) {
                found = true;
            }
        }

        if (!found) {
            reengineers.add(semionReengineer);
            String info = "Reengineering Manager : " + reengineers.size() + " reengineers";
            log.info(info);
            return true;
        } else {
            log.info("Reengineer already existing");
            return false;
        }

    }

    @Override
    public int countReengineers() {
        return reengineers.size();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + ReengineerManagerImpl.class + " deactivate with context " + context);
        reengineers = null;
    }

    @Override
    public Collection<Reengineer> listReengineers() {
        return reengineers;
    }

    @Override
    public OWLOntology performDataReengineering(String graphNS,
                                                IRI outputIRI,
                                                DataSource dataSource,
                                                OWLOntology schemaOntology) throws ReengineeringException {

        OWLOntology reengineeredDataOntology = null;

        boolean reengineered = false;
        Iterator<Reengineer> it = reengineers.iterator();
        while (it.hasNext() && !reengineered) {
            Reengineer semionReengineer = it.next();
            if (semionReengineer.canPerformReengineering(schemaOntology)) {
                reengineeredDataOntology = semionReengineer.dataReengineering(graphNS, outputIRI, dataSource,
                    schemaOntology);
                reengineered = true;
            }
        }

        return reengineeredDataOntology;
    }

    public OWLOntology performReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException {

        OWLOntology reengineeredOntology = null;

        boolean reengineered = false;
        Iterator<Reengineer> it = reengineers.iterator();
        while (it.hasNext() && !reengineered) {
            Reengineer semionReengineer = it.next();
            if (semionReengineer.canPerformReengineering(dataSource)) {
                log.debug(semionReengineer.getClass().getCanonicalName() + " can perform the reengineering");
                reengineeredOntology = semionReengineer.reengineering(graphNS, outputIRI, dataSource);
                reengineered = true;
            } else {
                log.debug(semionReengineer.getClass().getCanonicalName()
                          + " cannot perform the reengineering");
            }
        }

        return reengineeredOntology;
    }

    public OWLOntology performSchemaReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException {

        OWLOntology reengineeredSchemaOntology = null;

        boolean reengineered = false;
        Iterator<Reengineer> it = reengineers.iterator();
        while (it.hasNext() && !reengineered) {
            Reengineer semionReengineer = it.next();
            if (semionReengineer.canPerformReengineering(dataSource)) {
                reengineeredSchemaOntology = semionReengineer.schemaReengineering(graphNS, outputIRI,
                    dataSource);
                if (reengineeredSchemaOntology == null) {
                    throw new ReengineeringException();
                }
                reengineered = true;
            }
        }

        return reengineeredSchemaOntology;
    }

    @Override
    public boolean unbindReengineer(int reenginnerType) {
        boolean found = false;
        for (int i = 0, j = reengineers.size(); i < j && !found; i++) {
            Reengineer reengineer = reengineers.get(i);
            if (reengineer.getReengineerType() == reenginnerType) {
                reengineers.remove(i);
                found = true;
            }
        }
        return found;
    }

    @Override
    public boolean unbindReengineer(Reengineer semionReengineer) {
        boolean found = false;
        for (int i = 0, j = reengineers.size(); i < j && !found; i++) {
            if (semionReengineer.equals(reengineers.get(i))) {
                reengineers.remove(i);
                found = true;
            }
        }
        return found;
    }

}
