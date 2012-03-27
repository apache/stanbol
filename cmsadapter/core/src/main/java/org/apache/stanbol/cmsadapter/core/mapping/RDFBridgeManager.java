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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.core.helper.TcManagerClient;
import org.apache.stanbol.cmsadapter.core.repository.SessionManager;
import org.apache.stanbol.cmsadapter.servicesapi.helper.CMSAdapterVocabulary;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridge;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFBridgeException;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.RDFMapper;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This manager class keeps track of {@link RDFBridge}s and {@link RDFMapper}s in the OSGi environment. It
 * provides methods to map populate content repository using external RDF data and mapping structure of
 * content repository into RDF. In both direction, {@link RDFBridge}s and {@link RDFMapper}s are used.
 * <p>
 * While populating the content repository, {@link RDFBridge} instances add additional information to external
 * RDF so that it can be mapped to content repository by {@link RDFMapper}s. In other direction, first
 * {@link RDFMapper} produces an RDF containing only information regarding the content repository, after that
 * {@link RDFBridge}s add other resource to generated RDF based on their implementation.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service(value = RDFBridgeManager.class)
public class RDFBridgeManager {

    private static final Logger log = LoggerFactory.getLogger(RDFBridgeManager.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = RDFBridge.class, policy = ReferencePolicy.DYNAMIC, bind = "bindRDFBridge", unbind = "unbindRDFBridge", strategy = ReferenceStrategy.EVENT)
    List<RDFBridge> rdfBridges = new CopyOnWriteArrayList<RDFBridge>();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = RDFMapper.class, policy = ReferencePolicy.DYNAMIC, bind = "bindRDFMapper", unbind = "unbindRDFMapper", strategy = ReferenceStrategy.EVENT)
    List<RDFMapper> rdfMappers = new CopyOnWriteArrayList<RDFMapper>();

    @Reference
    SessionManager sessionManager;

    @Reference
    TcManager tcManager;

    /**
     * In the first step, this method runs the {@link RDFBridge}s on the RDF data passed in a {@link Graph}
     * instance. After this execution new assertions are added to initial graph to map the graph to content
     * repository. For example, an assertion stating <b>rdf:type</b> as
     * {@link CMSAdapterVocabulary#CMS_OBJECT} is added to each resource to be mapped to the content
     * repository.
     * <p>
     * In the second step, annotated RDF is mapped to the repository by an {@link RDFMapper} instance. This
     * instance is also determined according to session object which will be obtained with the
     * <code>sessionKey</code> parameter.
     * 
     * @param sessionKey
     *            Session key to retrieve previously cached session to access the repository
     * @param rawRDFData
     *            RDF to be annotated
     * @throws RepositoryAccessException
     * @throws RDFBridgeException
     */
    public void storeRDFToRepository(String sessionKey, Graph rawRDFData) throws RepositoryAccessException,
                                                                         RDFBridgeException {
        Object session = sessionManager.getSession(sessionKey);
        storeRDFToRepository(session, rawRDFData);
    }

    /**
     * In the first step, this method runs the {@link RDFBridge}s on the RDF data passed in a {@link Graph}
     * instance. After this execution new assertions are added to initial graph to map the graph to content
     * repository. For example, an assertion stating <b>rdf:type</b> as
     * {@link CMSAdapterVocabulary#CMS_OBJECT} is added to each resource to be mapped to the content
     * repository.
     * <p>
     * In the second step, annotated RDF is mapped to the repository by an {@link RDFMapper} instance. This
     * instance is also determined according to <code>session</code> object.
     * 
     * @param session
     *            Session to access repository
     * @param rawRDFData
     *            RDF to be annotated
     * @throws RepositoryAccessException
     * @throws RDFBridgeException
     */
    public void storeRDFToRepository(Object session, Graph rawRDFData) throws RepositoryAccessException,
                                                                      RDFBridgeException {
        if (rdfBridges.size() == 0) {
            log.info("There is no RDF Bridge to execute");
            return;
        }

        // According to connection type get RDF mapper, repository accessor,
        // session
        RDFMapper mapper = getRDFMapper(session);

        // Annotate raw RDF with CMS vocabulary annotations according to bridges
        log.info("Graph annotation starting...");
        MGraph annotatedGraph = new SimpleMGraph();
        for (RDFBridge bridge : rdfBridges) {
            long startAnnotation = System.currentTimeMillis();
            annotatedGraph.addAll(bridge.annotateGraph(rawRDFData));
            log.info("Graph annotated in: " + (System.currentTimeMillis() - startAnnotation) + "ms");
        }
        log.info("Graph annotation finished");

        // Store annotated RDF in repository
        log.info("Annotated graph mapping started...");
        long startMap = System.currentTimeMillis();
        mapper.storeRDFinRepository(session, annotatedGraph);
        log.info("Annotated graph mapped in: " + (System.currentTimeMillis() - startMap) + "ms");
    }

    /**
     * This method gets the RDF from the content repository based on the path configurations of
     * {@link RDFBridge}s and annotate them using {@link RDFBridge#annotateCMSGraph(MGraph)}.
     * <p>
     * This method maps structure of content repository into an RDF
     * 
     * @param baseURI
     *            Base URI for the RDF to be generated
     * @param sessionKey
     *            Session key to retrieve previously cached session to access the repository
     * @param store
     *            If this parameter is set as <code>true</code>, the generated RDF is stored persistently
     * @param update
     *            This parameter is considered only if the <code>store</code> parameter is set
     *            <code>true</code>. If so and if this parameter is also set to true <code>true</code>, newly
     *            generated graph will be merged with the existing one having the same base URI, otherwise a
     *            new will be created.
     * @return {@link MGraph} formed by the aggregation of generated RDF for each RDF bridge
     * @throws RepositoryAccessException
     * @throws RDFBridgeException
     */
    public MGraph generateRDFFromRepository(String baseURI, String sessionKey, boolean store, boolean update) throws RepositoryAccessException,
                                                                                                             RDFBridgeException {
        Object session = sessionManager.getSession(sessionKey);
        return generateRDFFromRepository(baseURI, session, store, update);
    }

    /**
     * This method gets the RDF from the content repository based on the path configurations of
     * {@link RDFBridge}s and annotate them using {@link RDFBridge#annotateCMSGraph(MGraph)}.
     * 
     * @param baseURI
     *            Base URI for the RDF to be generated
     * @param session
     *            Session to access repository
     * @param store
     *            If this parameter is set as <code>true</code>, the generated RDF is stored persistently
     * @param update
     *            This parameter is considered only if the <code>store</code> parameter is set
     *            <code>true</code>. If so and if this parameter is also set to true <code>true</code>, newly
     *            generated graph will be merged with the existing one having the same base URI, otherwise a
     *            new will be created.
     * @return {@link MGraph} formed by the aggregation of generated RDF for each RDF bridge
     * @throws RepositoryAccessException
     * @throws RDFBridgeException
     */
    public MGraph generateRDFFromRepository(String baseURI, Object session, boolean store, boolean update) throws RepositoryAccessException,
                                                                                                          RDFBridgeException {
        if (rdfBridges.size() == 0) {
            log.info("There is no RDF Bridge to execute");
            return new SimpleMGraph();
        }

        RDFMapper mapper = getRDFMapper(session);
        MGraph cmsGraph = new SimpleMGraph();
        for (RDFBridge bridge : rdfBridges) {
            MGraph generatedGraph = mapper.generateRDFFromRepository(baseURI, session, bridge.getCMSPath());
            bridge.annotateCMSGraph(generatedGraph);
            cmsGraph.addAll(generatedGraph);
        }

        MGraph persistentGraph = null;
        if (store) {
            TcManagerClient tcManagerClient = new TcManagerClient(tcManager);
            boolean graphExists = tcManagerClient.modelExists(baseURI);
            if (update) {
                if (graphExists) {
                    log.info("Getting the existing triple collection having base URI: {}", baseURI);
                    persistentGraph = tcManager.getMGraph(new UriRef(baseURI));
                } else {
                    persistentGraph = tcManager.createMGraph(new UriRef(baseURI));
                }
            } else {
                if (graphExists) {
                    log.info("Deleting the triple collection having base URI: {}", baseURI);
                    tcManager.deleteTripleCollection(new UriRef(baseURI));
                }
                persistentGraph = tcManager.createMGraph(new UriRef(baseURI));
            }
            log.info("Saving the triple collection having base URI: {}", baseURI);
            persistentGraph.addAll(cmsGraph);
        } else {
            persistentGraph = cmsGraph;
        }
        return persistentGraph;
    }

    private RDFMapper getRDFMapper(Object session) throws RepositoryAccessException {
        RDFMapper mapper = null;
        for (RDFMapper rdfMapper : rdfMappers) {
            if (rdfMapper.canMapWith(session)) {
                mapper = (RDFMapper) rdfMapper;
            }
        }
        if (mapper == null) {
            log.warn("Failed to retrieve RDFMapper for session: {}", session);
            throw new IllegalStateException("Failed to retrieve RDFMapper for session: " + session);
        }
        return mapper;
    }

    protected void bindRDFBridge(RDFBridge rdfBridge) {
        rdfBridges.add(rdfBridge);
    }

    protected void unbindRDFBridge(RDFBridge rdfBridge) {
        rdfBridges.remove(rdfBridge);
    }

    protected void bindRDFMapper(RDFMapper rdfMapper) {
        rdfMappers.add(rdfMapper);
    }

    protected void unbindRDFMapper(RDFMapper rdfMapper) {
        rdfMappers.remove(rdfMapper);
    }
}