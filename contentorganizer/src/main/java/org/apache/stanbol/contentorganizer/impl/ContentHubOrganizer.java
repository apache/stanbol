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
package org.apache.stanbol.contentorganizer.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.common.SolrDocument;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.contentorganizer.impl.naive.NaiveEnvironment;
import org.apache.stanbol.contentorganizer.model.Category;
import org.apache.stanbol.contentorganizer.model.Criterion;
import org.apache.stanbol.contentorganizer.servicesapi.ContentConnector;
import org.apache.stanbol.contentorganizer.servicesapi.ContentOrganizer;
import org.apache.stanbol.contentorganizer.servicesapi.ContentRetrievalException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.session.DuplicateSessionIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionLimitException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.reasoners.owlapi.OWLApiReasoningService;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredPropertyAssertionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author alexdma
 * 
 */
@Component(immediate = true, metatype = false)
@Service
public class ContentHubOrganizer implements ContentOrganizer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final static String DEFAULT_ROOT_PATH = "datafiles/contentorganizer";

    private final static String DEFAULT_FOLDER_NAME = "metadata";

    @Reference
    protected Store contentStore;

    @Reference
    protected RuleStore ruleStore;

    @Reference
    protected ONManager onMgr;

    @Reference
    protected SessionManager sesMgr;

    @Reference
    protected SolrSearch solrSearch;

    @Reference
    protected Entityhub entityHub;

    @Reference
    protected Serializer serializer;

    @Reference
    private RegistryManager regMgr;

    @Reference
    private OntologyProvider<TcProvider> ontologyProvider;

    @Reference
    private OWLApiReasoningService reasoner;

    @Reference
    protected ReferencedSiteManager siteMgr;

    private ContentConnector connector;

    private File contentMetadataDir;

    public ContentHubOrganizer() {
        super();
    }

    public ContentHubOrganizer(Store contentStore,
                               SolrSearch solrSearch,
                               Dictionary<String,Object> configuration) {
        this();
        this.contentStore = contentStore;
        this.solrSearch = solrSearch;
        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access component context.", e);
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
        log.info("in " + ContentHubOrganizer.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }

        String slingHome = context.getBundleContext().getProperty("sling.home");
        if (!slingHome.endsWith(File.separator)) slingHome += File.separator;
        contentMetadataDir = new File(slingHome + DEFAULT_ROOT_PATH, DEFAULT_FOLDER_NAME);

        // if directory for programs does not exist, create it
        if (!contentMetadataDir.exists()) {
            if (contentMetadataDir.mkdirs()) {
                log.info("Directory for metadata created succesfully");
            } else {
                log.error("Directory for metadata COULD NOT be created");
                throw new IOException("Directory : " + contentMetadataDir.getAbsolutePath()
                                      + " cannot be created");
            }
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

        // Setup OntoNet
        // String scopeId = "DBPedia";
        // OntologyScope scope = null;
        // try {
        // scope = onMgr.getOntologyScopeFactory().createOntologyScope(scopeId);
        // } catch (DuplicateIDException e) {
        // log.warn("Scope {} already exist, will use that. ", scopeId);
        // scope = onMgr.getScopeRegistry().getScope(scopeId);
        // }
        new NaiveEnvironment(onMgr, regMgr).activate();

        connector = new ContentHubConnector(contentStore, solrSearch);

        log.debug(ContentHubOrganizer.class + " activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + ContentHubOrganizer.class + " deactivate with context " + context);

        connector = null;

        Session ses = sesMgr.getSession("alexdma");
        if (ses != null) synchronized (ses) {
//            System.out.print("Now destroying session for alexdma... ");
            ses.close();
            ses.clearScopes(); // Should avoid concurrent modification exceptions.
            for (IRI ontologyIRI : ses.listManagedOntologies()) {
                UriRef ref = new UriRef(ontologyProvider.getKey(ontologyIRI));
                if (ontologyProvider.getStore().listTripleCollections().contains(ref)) ontologyProvider
                        .getStore().deleteTripleCollection(ref);
            }
            ses.tearDown();
            sesMgr.destroySession("alexdma");
//            System.out.println("DONE");
        }

        log.debug(ContentHubOrganizer.class + " deactivated.");
    }

    @Override
    public Set<Criterion> getSuitableCriteria(Collection<ContentItem> contentItems) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<ContentItem,Set<Category>> classifyContent() {

        String sessionId = "alexdma";
        Session ses = null;
        try {
            ses = sesMgr.createSession(sessionId);
        } catch (DuplicateSessionIDException e2) {
            ses = sesMgr.getSession(sessionId);
        } catch (SessionLimitException e2) {
            throw new RuntimeException("Overall session quota reached. Cannot continue.", e2);
        }

        // Do stuff here for the time being...

        Set<ContentItem> contents;
        try {
            contents = connector.getContents();
        } catch (ContentRetrievalException e1) {
            log.error("Failed to retrieve stored contents.", e1);
            contents = Collections.emptySet();
            return new HashMap<ContentItem,Set<Category>>();
        }

        MGraph mg = new IndexedMGraph();

        try {
            for (SolrDocument doc : solrSearch.search("*:*").getResults()) {

//                for (String s : doc.getFieldNames())
//                    System.out.println(">>>> " + s + " : " + doc.getFieldValue(s));

                String id = (String) doc.getFieldValue(SolrFieldName.ID.toString());
                ContentItem ci = contentStore.get(id);
                TripleCollection meta = ci.getMetadata();
//                System.out.println(meta.size());
//                Iterator<Triple> it = meta.filter(null, DC.title, null);
//                while (it.hasNext())
//                    System.out.println("found dc:title " + it.next());

                if (ci instanceof SolrContentItem) {
                    SolrContentItem sci = (SolrContentItem) ci;
                    Object obj = sci.getConstraints().get("reference_t");
                    if (obj != null && obj instanceof Collection<?>) for (Object s : (Collection<?>) obj)
                        try {
                            IRI iri = IRI.create(s.toString());
                            if (iri.isAbsolute()) mg.add(new TripleImpl(ci.getUri(), OWL.sameAs, new UriRef(
                                    s.toString())));
                        } catch (Throwable e) {
                            continue;
                        }
                }
            }

        } catch (SearchException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (StoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mg.add(new TripleImpl(ContentHubConnector.EXTRACTED_FROM, RDF.type, OWL.ObjectProperty));
        // mg.add(new TripleImpl(ContentHubConnector.EXTRACTED_FROM, RDFS.domain, OWL.ObjectProperty));

        for (ContentItem ci : contents) {
            mg.addAll(new EntityHubKnowledgeRetriever(siteMgr).aggregateKnowledge(ci));
//            System.out.println("Before : " + mg.size());
            MGraph meta = ci.getMetadata();
            Iterator<Triple> it = meta.filter(null, ContentHubConnector.EXTRACTED_FROM, null);
            while (it.hasNext()) {
                Resource obj = it.next().getObject();
                if (obj instanceof UriRef) mg.add(new TripleImpl((UriRef) obj, RDF.type,
                        ContentHubConnector.CONTENT_ITEM));
            }
            mg.addAll(meta);
//            System.out.println("After : " + mg.size());
        }

        ses.tearDown();
        try {
            ses.addOntology(new GraphSource(mg));
            ses.addOntology(new RootOntologyIRISource(
                    IRI.create("http://svn.apache.org/repos/asf/incubator/stanbol/trunk/enhancer/generic/servicesapi/src/main/resources/fise.owl")));
        } catch (UnmodifiableOntologyCollectorException e1) {
            throw new RuntimeException("cannot populate locked session for reasoning.", e1);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException("cannot retrieve enhancement vocabulary.", e);
        }

        for (OntologyScope scope : onMgr.getScopeRegistry().getActiveScopes())
            ses.attachScope(scope);

        ses.setUp();
        ses.open();

        /* ************** BEGIN debug stuff to get rid of ************** */
        File f = null;
        try {
            // All the content metadata in one file.
            f = new File(contentMetadataDir, "all.rdf");
            serializer.serialize(new FileOutputStream(f), mg, SupportedFormat.RDF_XML);
        } catch (UnsupportedFormatException e) {
            log.error("Unsupported serialization format {} ! This should not happen...",
                SupportedFormat.RDF_XML);
        } catch (FileNotFoundException e) {
            log.error("Could not obtain file {} for writing. ", f);
        }

        try {
            // All the enhancements in another file
            f = new File(contentMetadataDir, "enhancement.rdf");
            serializer.serialize(new FileOutputStream(f), contentStore.getEnhancementGraph(),
                SupportedFormat.RDF_XML);
        } catch (UnsupportedFormatException e) {
            log.error("Unsupported serialization format {} ! This should not happen...",
                SupportedFormat.RDF_XML);
        } catch (FileNotFoundException e) {
            log.error("Could not obtain file {} for writing. ", f);
        }

        // ruleStore.createRecipe(recipeID, rulesInKReSSyntax)
        List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
        gens.add(new InferredClassAssertionAxiomGenerator());
        gens.add(new InferredPropertyAssertionGenerator());
        OWLOntology o = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(contentStore
                .getEnhancementGraph());

        // reasoner.run(o, gens);

        /* ************** END debug stuff to get rid of ************** */

        // synchronized (ses) {
        // System.out.print("Now destroying session for alexdma... ");
        // ses.close();
        // ses.clearScopes(); // Should avoid concurrent modification exceptions.
        // for (IRI ontologyIRI : ses.listManagedOntologies()) {
        // UriRef ref = new UriRef(ontologyProvider.getKey(ontologyIRI));
        // if (ontologyProvider.getStore().listTripleCollections().contains(ref)) ontologyProvider
        // .getStore().deleteTripleCollection(ref);
        // }
        // ses.tearDown();
        // sesMgr.destroySession(sessionId);
        // System.out.println("DONE");
        // }

        return null;
    }
}
