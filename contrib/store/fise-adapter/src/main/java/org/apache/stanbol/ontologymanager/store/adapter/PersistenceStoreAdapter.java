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
package org.apache.stanbol.ontologymanager.store.adapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.ontologymanager.store.adapter.util.IOUtil;
import org.apache.stanbol.ontologymanager.store.adapter.util.Triple;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.apache.stanbol.ontologymanager.store.model.AdministeredOntologies;
import org.apache.stanbol.ontologymanager.store.model.OntologyMetaInformation;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class PersistenceStoreAdapter implements Store {

    private static final Logger logger = LoggerFactory.getLogger(PersistenceStoreAdapter.class.getName());

    @Reference
    private PersistenceStore pStore;

    private ContentManager cm;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Parser parser;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Serializer serializer;

    @Activate
    public void activate(ComponentContext cc) {

        // FIXME Need to bind Jena Parsing Provider here?
        // FIXME Need to bind Jena Serializing Provider here?

        this.cm = new ContentManager(cc.getBundleContext().getDataFile("Content Items"));
    }

    @Deactivate
    public void deactivate(ComponentContext cc) {
        this.cm.store();
        logger.info("Persistence Store Adaptor deactivated successfully");
    }

    public ContentItem create(String id, byte[] content, String contentType) {
        return SimpleContentItem.create(id, content, contentType);
    }

    public ContentItem get(String id) {
        Triple<String,byte[],String> contentEntry = cm.getContent(id);
        if (contentEntry == null) {
            logger.warn("Content Item Not Found");
            return null;
        }
        byte[] content = contentEntry.getEntry2();
        String mimeType = contentEntry.getEntry1();
        String URI = contentEntry.getEntry3();
        MGraph mg = readModel(URI);
        if (mg == null) {
            logger.warn("Content item not found");
            return null;
        }

        if (content == null || mimeType == null) {
            return null;
        } else {
            return assemble(id, mg, contentEntry.getEntry1(), contentEntry.getEntry2());
        }
    }

    public String put(ContentItem ci) {
        MGraph mg = ci.getMetadata();
        IOUtil ioUtil = IOUtil.getInstance();
        String content;
        String uri;

        content = writeModel(mg);
        if (ci.getId() == null || "".equals(ci.getId())) {
            // FIXME Here assign a new id
            throw new RuntimeException("How to assign a new id with current interface");
        }

        uri = ContentItemHelper.ensureUri(ci).getUnicodeString();
        try {
            Boolean deleted = pStore.deleteOntology(uri);
            logger.info("Previous ontology deleted: {}", deleted.toString());
        } catch (Exception e1) {
            // No need to log since it is actually an existence check
        }
        try {
            pStore.saveOntology(content, uri, "UTF-8");
            cm.put(ci.getId(), new Triple<String,byte[],String>(ci.getMimeType(), ioUtil
                    .convertStreamToString(ci.getStream()).getBytes(), uri));

        } catch (IOException e) {
            logger.error("Exception in saving the ontology", e);
        } catch (Exception e) {
            logger.error("Exception in saving the ontology", e);
        }
        return ci.getId();
    }

    private ContentItem assemble(final String id,
                                 final MGraph metadata,
                                 final String contentType,
                                 final byte[] content) {
        return new SimpleContentItem(id, metadata, contentType, content);
    }

    private String writeModel(MGraph mg) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        this.serializer.serialize(bos, mg, "application/rdf+xml");
        return bos.toString();
    }

    private MGraph readModel(String URI) {
        // Model model = null;
        MGraph model = new SimpleMGraph();
        String ontologyContent = null;
        try {
            ontologyContent = pStore.retrieveOntology(URI, "RDF/XML", false);
        } catch (Exception e1) {
            logger.error("Exception in retrieving the ontology " + URI, e1);
            return null;
        }
        try {
            Graph graph = this.parser.parse(new ByteArrayInputStream(ontologyContent.getBytes()),
                "application/rdf+xml", new UriRef(URI));
            model.addAll(graph);
            return model;
        } catch (Exception e) {
            logger.error("Exception in reading the model", e);
            return null;
        }
    }

    @Override
    public MGraph getEnhancementGraph() {
        try {
            AdministeredOntologies onts = pStore.retrieveAdministeredOntologies();
            MGraph mgraph = new SimpleMGraph();
            for (OntologyMetaInformation ont : onts.getOntologyMetaInformation()) {
                String content = pStore.retrieveOntology(ont.getURI(), "RDF/XML", false);
                Graph gr = this.parser.parse(new ByteArrayInputStream(content.getBytes()),
                    "application/rdf+xml", new UriRef(ont.getURI()));
                mgraph.addAll(gr);

            }
            return mgraph;
        } catch (Exception e) {
            logger.error("Exception in retrieveing enhancement graph", e);
        }
        return new SimpleMGraph();
    }
}
