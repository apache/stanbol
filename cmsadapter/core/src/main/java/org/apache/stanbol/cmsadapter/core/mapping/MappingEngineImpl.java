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
package org.apache.stanbol.cmsadapter.core.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.core.decorated.DObjectFactoryImp;
import org.apache.stanbol.cmsadapter.core.helper.TcManagerClient;
import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingConfiguration;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.NamingStrategy;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.AdapterMode;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.processor.Processor;
import org.apache.stanbol.cmsadapter.servicesapi.processor.ProcessorProperties;
import org.apache.stanbol.cmsadapter.servicesapi.processor.TypeLifter;
import org.apache.stanbol.cmsadapter.servicesapi.processor.TypeLifterManager;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

@Component(factory = "org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngineFactory")
@Service
public class MappingEngineImpl implements MappingEngine {
    private static final Logger logger = LoggerFactory.getLogger(MappingEngineImpl.class);

    private static final ProcessorComparator COMPARATOR = new ProcessorComparator();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, referenceInterface = Processor.class, policy = ReferencePolicy.DYNAMIC, bind = "bindProcessor", unbind = "unbindProcessor")
    private List<Processor> processors = new ArrayList<Processor>();

    @Reference
    private TypeLifterManager typeLifterManager;

    @Reference
    private RepositoryAccessManager accessManager;

    @Reference
    private TcManager tcManager;

    private RepositoryAccess accessor;
    private Object session;
    private OntModel ontModel;
    private String ontologyURI;
    private BridgeDefinitions bridgeDefinitions;
    private OntologyResourceHelper ontologyResourceHelper;
    private DObjectAdapter adapter;
    private NamingStrategy namingStrategy;

    private void runProcessors(List<Object> cmsObjects, String mode) {
        Iterator<Processor> processorIterator;
        synchronized (processors) {
            processorIterator = processors.iterator();

            while (processorIterator.hasNext()) {
                Processor processor = processorIterator.next();
                long t1 = System.currentTimeMillis();
                try {
                    if (mode.contentEquals("create")) {
                        processor.createObjects(cmsObjects, this);
                    } else if (mode.contentEquals("delete")) {
                        processor.deleteObjects(cmsObjects, this);
                    }
                } finally {
                    logger.debug("{} processor completed execution in {} miliseconds", processor.toString(),
                        System.currentTimeMillis() - t1);
                }
            }
        }
    }

    @Override
    public void mapCR(MappingConfiguration conf) throws RepositoryAccessException {
        initializeEngine(conf);

        long t1 = System.currentTimeMillis();
        String connectionType = conf.getConnectionInfo().getConnectionType();
        if (connectionType != null
            && !(connectionType.contentEquals("JCR") || connectionType.contentEquals("CMIS"))) {
            throw new IllegalArgumentException("Connection type must be one of JCR or CMIS.");
        }

        // lift type defintions
        TypeLifter typeLifter = typeLifterManager.getRepositoryAccessor(connectionType);
        typeLifter.liftNodeTypes(this);

        runProcessors(null, "create");
        OntologyResourceHelper.saveConnectionInfo(conf.getConnectionInfo(), this.ontModel);
        OntologyResourceHelper.saveBridgeDefinitions(conf.getBridgeDefinitions(), this.ontModel);
        logger.debug("Total process time for ontology {} is {} ms", ontologyURI, System.currentTimeMillis()
                                                                                 - t1);
        storeOntology();
    }

    @Override
    public void createModel(MappingConfiguration conf) {

        try {
            initializeEngine(conf);
        } catch (RepositoryAccessException e) {
            logger.warn("Failed to obtain session for ontologyURI {}", ontologyURI, e);
            return;
        }

        long t1 = System.currentTimeMillis();
        runProcessors(conf.getObjects(), "create");
        logger.debug("Total process time for ontology {} is {} ms", ontologyURI, System.currentTimeMillis()
                                                                                 - t1);
        storeOntology();
    }

    @Override
    public void updateModel(MappingConfiguration conf) {

        try {
            initializeEngine(conf);
        } catch (RepositoryAccessException e) {
            logger.warn("Failed to obtain session for ontologyURI {}", ontologyURI, e);
            return;
        }

        long t1 = System.currentTimeMillis();
        runProcessors(conf.getObjects(), "delete");
        runProcessors(conf.getObjects(), "create");
        logger.debug("Total process time for ontology {} is {} ms", ontologyURI, System.currentTimeMillis()
                                                                                 - t1);
        storeOntology();
    }

    @Override
    public void deleteModel(MappingConfiguration conf) {
        try {
            initializeEngine(conf);
        } catch (RepositoryAccessException e) {
            logger.warn("Failed to initialized Mapping Engine", e);
        }

        long t1 = System.currentTimeMillis();
        runProcessors(conf.getObjects(), "delete");
        logger.debug("Total process time for ontology {} is {} ms", ontologyURI, System.currentTimeMillis()
                                                                                 - t1);
        storeOntology();
    }

    @Override
    public RepositoryAccessManager getRepositoryAccessManager() {
        return accessManager;
    }

    @Override
    public DObjectAdapter getDObjectAdapter() {
        return adapter;
    }

    @Override
    public Object getSession() {
        return session;
    }

    @Override
    public OntModel getOntModel() {
        return ontModel;
    }

    @Override
    public BridgeDefinitions getBridgeDefinitions() {
        return bridgeDefinitions;
    }

    @Override
    public OntologyResourceHelper getOntologyResourceHelper() {
        return ontologyResourceHelper;
    }

    @Override
    public String getOntologyURI() {
        return ontologyURI;
    }

    @Override
    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    @Override
    public RepositoryAccess getRepositoryAccess() {
        return accessor;
    }

    private void initializeEngine(MappingConfiguration conf) throws RepositoryAccessException {
        if (conf.getOntModel() != null) {
            this.ontModel = conf.getOntModel();
        } else {
            this.ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        }

        AdapterMode adapterMode = conf.getAdapterMode();
        if (adapterMode == null) {
            adapterMode = AdapterMode.ONLINE;
        }

        ConnectionInfo connectionInfo = conf.getConnectionInfo();
        if (connectionInfo == null) {
            if (!adapterMode.equals(AdapterMode.STRICT_OFFLINE)) {
                connectionInfo = OntologyResourceHelper.getConnectionInfo(this.ontModel);
                if (connectionInfo == null) {
                    logger.warn("Failed to retrieve connection info from ontmodel");
                    throw new RuntimeException("Failed to retrieve connection info from ontmodel");
                }
            }
        }

        List<Object> offlineObjects = conf.getObjects();
        if (adapterMode.equals(AdapterMode.ONLINE)) {
            accessor = accessManager.getRepositoryAccessor(connectionInfo);
            this.session = accessor.getSession(connectionInfo);
            this.adapter = new DObjectFactoryImp(accessor, session);

        } else if (adapterMode.equals(AdapterMode.TOLERATED_OFFLINE)) {
            try {
                accessor = accessManager.getRepositoryAccessor(connectionInfo);
                this.session = accessor.getSession(connectionInfo);
                RepositoryAccess offlineAccess = accessManager.getRepositoryAccess(offlineObjects);
                this.adapter = new DObjectFactoryImp(accessor, offlineAccess, session, adapterMode);

            } catch (RepositoryAccessException e) {
                accessor = accessManager.getRepositoryAccess(offlineObjects);
                this.adapter = new DObjectFactoryImp(accessor, session, AdapterMode.STRICT_OFFLINE);
            }

        } else if (adapterMode.equals(AdapterMode.STRICT_OFFLINE)) {
            accessor = accessManager.getRepositoryAccess(offlineObjects);
            this.adapter = new DObjectFactoryImp(accessor, session, adapterMode);
        }

        this.bridgeDefinitions = conf.getBridgeDefinitions();
        this.ontologyURI = conf.getOntologyURI();
        this.namingStrategy = new DefaultNamingStrategy(accessor, session, this.ontModel);
        this.ontologyResourceHelper = new OntologyResourceHelper(this);
    }

    private void storeOntology() {
        TcManagerClient tcManagerClient = new TcManagerClient(tcManager);
        tcManagerClient.saveOntology(ontModel, ontologyURI);
    }

    protected void bindProcessor(Processor processor) {
        synchronized (processors) {
            processors.add(processor);
            Collections.sort(processors, COMPARATOR);
        }
    }

    protected void unbindProcessor(Processor processor) {
        synchronized (processors) {
            processors.remove(processor);
        }
    }

    private static final class ProcessorComparator implements Comparator<Processor> {

        @Override
        public int compare(Processor proc1, Processor proc2) {
            Integer order1 = getOrder(proc1);
            Integer order2 = getOrder(proc2);
            return order1.compareTo(order2);
        }

        public int getOrder(Processor engine) {
            if (engine == null) {
                throw new IllegalArgumentException("Engine can not be null");
            }

            if (engine instanceof ProcessorProperties) {
                Object value = ((ProcessorProperties) engine).getProcessorProperties().get(
                    ProcessorProperties.PROCESSING_ORDER);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            }
            return ProcessorProperties.CMSOBJECT_POST;
        }
    }
}
