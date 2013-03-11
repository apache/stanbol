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

package org.apache.stanbol.contenthub.store.solr;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.servicesapi.Constants;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDPathException;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.contenthub.store.solr.manager.SolrCoreManager;
import org.apache.stanbol.contenthub.store.solr.util.QueryGenerator;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.sinaci
 * 
 */
@Component(immediate = true)
@Service
public class SolrStoreImpl implements SolrStore {

    private static final Logger log = LoggerFactory.getLogger(SolrStoreImpl.class);

    private static final Set<String> SUPPORTED_MIMETYPES = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList("text/html", "text/plain", "text/xml")));

    @Reference
    private ManagedSolrServer managedSolrServer;

    @Reference
    private ContentItemFactory ciFactory;

    @Reference
    private TcManager tcManager;

    @Reference
    private EnhancementJobManager jobManager;

    @Reference
    private SemanticIndexManager semanticIndexManager;

    @Reference
    private ChainManager chainManager;

    private BundleContext bundleContext;

    private ServiceRegistration enhancementGraphRegistry;

    @Activate
    protected void activate(ComponentContext context) throws StoreException {
        if (managedSolrServer == null) {
            throw new IllegalStateException("ManagedSolrServer cannot be referenced within SolrServerImpl.");
        }
        this.bundleContext = context.getBundleContext();
        SolrCoreManager.getInstance(bundleContext, managedSolrServer).createDefaultSolrServer();

        // create and register the enhancement graph
        createEnhancementGraph();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        managedSolrServer = null;
        enhancementGraphRegistry.unregister();
    }

    @Override
    public ContentItem create(String id, byte[] content, String contentType) throws StoreException {
        return create(content, id, "", contentType);
    }

    @Override
    public MGraph getEnhancementGraph() {
        final UriRef graphUri = new UriRef(Constants.ENHANCEMENTS_GRAPH_URI);
        MGraph enhancementGraph = null;
        try {
            enhancementGraph = tcManager.getMGraph(graphUri);
        } catch (NoSuchEntityException e) {
            log.error("Enhancement Graph must be exist");
        }
        return enhancementGraph;
    }

    public void createEnhancementGraph() throws StoreException {
        final UriRef graphUri = new UriRef(Constants.ENHANCEMENTS_GRAPH_URI);
        MGraph enhancementGraph = null;
        try {
            enhancementGraph = tcManager.getMGraph(graphUri);
            String filter = String.format("(%s=%s)", "graph.uri", graphUri.getUnicodeString());
            ServiceReference[] sr = bundleContext.getServiceReferences(TripleCollection.class.getName(),
                filter);
            if (sr == null) {
                registerEnhancementGraph(graphUri, enhancementGraph);
            }
        } catch (NoSuchEntityException e) {
            log.debug("Creating the enhancement graph!");
            enhancementGraph = tcManager.createMGraph(graphUri);
            registerEnhancementGraph(graphUri, enhancementGraph);

        } catch (InvalidSyntaxException e) {
            log.error("Failed to get ServiceReference for TripleCollection");
            throw new StoreException("Failed to get ServiceReference for TripleCollection", e);
        }
    }

    private void registerEnhancementGraph(UriRef graphUri, MGraph enhancementGraph) {
        Dictionary<String,Object> props = new Hashtable<String,Object>();
        props.put("graph.uri", graphUri);
        props.put("graph.name", "Enhancement Graph");
        props.put("graph.description",
            "This graph stores enhancements of all content items stored within Contenthub.");
        props.put(org.osgi.framework.Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        enhancementGraphRegistry = bundleContext.registerService(TripleCollection.class.getName(),
            enhancementGraph, props);
        log.debug("Enhancement graph is registered to the OSGi environment");
    }

    @Override
    public ContentItem create(byte[] content, String id, String title, String contentType) throws StoreException {
        UriRef uri;
        if (id == null || id.isEmpty()) {
            uri = ContentItemHelper.makeDefaultUrn(content);
        } else {
            uri = new UriRef(id);
        }
        log.debug("Created ContentItem with id:{} and uri:{}", id, uri);
        final MGraph g = new IndexedMGraph();
        ContentItem ci = null;
        try {
            ci = ciFactory.createContentItem(uri, new ByteArraySource(content, contentType), g);
        } catch (IOException e) {
            log.error("Failed to create contentitem with uri: {}", uri.getUnicodeString());
            throw new StoreException(String.format("Failed to create contentitem with uri: %s",
                uri.getUnicodeString()));
        }
        if (title != null && !title.trim().isEmpty()) {
            ci.addPart(TITLE_URI, title.trim());
        }
        return ci;
    }

    private void enhance(ContentItem ci, String chainName) throws StoreException {
        try {
            if (chainName == null || chainName.trim().isEmpty()) {
                jobManager.enhanceContent(ci);
            } else {
                Chain chain = chainManager.getChain(chainName.trim());
                if (chain == null) {
                    String msg = String.format("Failed to get chain with name: %s", chainName);
                    log.error(msg);
                    throw new StoreException(msg);
                }
                jobManager.enhanceContent(ci, chain);
            }
        } catch (EnhancementException e) {
            String msg = String.format("Cannot enhance content with id: %s", ci.getUri().getUnicodeString());
            log.error(msg, e);
            throw new StoreException(msg, e);
        }
    }

    private void removeEnhancements(String id) throws StoreException {
        MGraph enhancementGraph = getEnhancementGraph();
        String enhancementQuery = QueryGenerator.getEnhancementsOfContent(id);
        SelectQuery selectQuery = null;
        try {
            selectQuery = (SelectQuery) QueryParser.getInstance().parse(enhancementQuery);
        } catch (ParseException e) {
            String msg = "Cannot parse the SPARQL while trying to delete the enhancements of the ContentItem";
            log.error(msg, e);
            throw new StoreException(msg, e);
        }

        List<Triple> willBeRemoved = new ArrayList<Triple>();
        ResultSet resultSet = tcManager.executeSparqlQuery(selectQuery, enhancementGraph);
        while (resultSet.hasNext()) {
            SolutionMapping mapping = resultSet.next();
            UriRef ref = (UriRef) mapping.get("enhID");
            Iterator<Triple> tripleItr = this.getEnhancementGraph().filter(ref, null, null);
            while (tripleItr.hasNext()) {
                Triple triple = tripleItr.next();
                willBeRemoved.add(triple);
            }
        }

        enhancementGraph.removeAll(willBeRemoved);
    }

    private void updateEnhancementGraph(ContentItem ci) throws StoreException {
        MGraph enhancementGraph = getEnhancementGraph();
        // Delete old enhancements which belong to this content item from the
        // global enhancements graph.
        removeEnhancements(ci.getUri().getUnicodeString());
        // Add new enhancements of this content item to the global enhancements
        // graph.
        enhancementGraph.addAll(ci.getMetadata());
    }

    @Override
    public String enhanceAndPut(ContentItem ci, String chainName) throws StoreException {
        enhance(ci, chainName);
        return put(ci);
    }

    @Override
    public String enhanceAndPut(ContentItem ci, String ldProgramName, String chain) throws StoreException {
        enhance(ci, chain);
        return put(ci, ldProgramName);
    }

    /**
     * Put the ContentItem into the default Solr core
     */
    @Override
    public String put(ContentItem ci) throws StoreException {
        return put(ci, null);
    }

    /**
     * Put the ContentItem into the Solr core identify by ldProgramName. If ldProgramName is null, put the CI
     * to the default Solr core. Also save the ContentItem enhancements in the EnhancementGraph
     */
    @Override
    public String put(ContentItem ci, String ldProgramName) throws StoreException {
        SolrInputDocument doc = new SolrInputDocument();
        addDefaultFields(ci, doc);
        SolrServer solrServer;
        if (ldProgramName == null || ldProgramName.isEmpty()
            || ldProgramName.equals(SolrCoreManager.CONTENTHUB_DEFAULT_INDEX_NAME)) {

            addSolrSpecificFields(ci, doc);
            solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer();
        } else {
            addSolrSpecificFields(ci, doc, ldProgramName);
            solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(
                ldProgramName);
        }
        updateEnhancementGraph(ci);
        try {
            solrServer.add(doc);
            solrServer.commit();
            log.debug("Documents are committed to Solr Server successfully.");
        } catch (SolrServerException e) {
            log.error("Solr Server Exception", e);
            throw new StoreException(e.getMessage(), e);
        } catch (IOException e) {
            log.error("IOException", e);
            throw new StoreException(e.getMessage(), e);
        }
        return ci.getUri().getUnicodeString();
    }

    private void addDefaultFields(ContentItem ci, SolrInputDocument doc) throws StoreException {
        if (ci.getUri().getUnicodeString() == null || ci.getUri().getUnicodeString().isEmpty()) {
            log.debug("ID of the content item cannot be null while inserting to the SolrStore.");
            throw new IllegalArgumentException(
                    "ID of the content item cannot be null while inserting to the SolrStore.");
        }

        // get content
        String content = "";
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMETYPES);
        if (contentPart != null) {
            try {
                content = ContentItemHelper.getText(contentPart.getValue());
            } catch (IOException ex) {
                String msg = "Cannot read the stream of the ContentItem.";
                log.error(msg, ex);
                throw new StoreException(msg, ex);
            }
        }
        InputStream binaryContent = ci.getStream();

        if (content.equals("") && binaryContent == null) {
            throw new StoreException("No textual or binary content for the ContentItem");
        }

        try {
            doc.addField(SolrFieldName.CONTENT.toString(), content);
            doc.addField(SolrFieldName.BINARYCONTENT.toString(), IOUtils.toByteArray(binaryContent));
        } catch (IOException e) {
            throw new StoreException("Failed to get bytes of conten item stream", e);
        }

        doc.addField(SolrFieldName.ID.toString(), ci.getUri().getUnicodeString());
        doc.addField(SolrFieldName.MIMETYPE.toString(), ci.getMimeType());

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String creationDate = sdf.format(cal.getTime());
        doc.addField(SolrFieldName.CREATIONDATE.toString(), creationDate);

        // add the number of enhancemets to the content item
        long enhancementCount = 0;
        Iterator<Triple> it = ci.getMetadata().filter(null, Properties.ENHANCER_EXTRACTED_FROM,
            new UriRef(ci.getUri().getUnicodeString()));
        while (it.hasNext()) {
            it.next();
            enhancementCount++;
        }
        doc.addField(SolrFieldName.ENHANCEMENTCOUNT.toString(), enhancementCount);
    }

    private void addSolrSpecificFields(ContentItem ci, SolrInputDocument doc, String ldProgramName) {
        String title = null;
        try {
            title = ci.getPart(TITLE_URI, String.class);
        } catch (NoSuchPartException e) {
            title = ci.getUri().getUnicodeString();
        }
        doc.addField(SolrFieldName.TITLE.toString(), title);
        try {
            Iterator<Triple> it = ci.getMetadata().filter(null, Properties.ENHANCER_ENTITY_REFERENCE, null);
            Set<String> contexts = new HashSet<String>();
            while (it.hasNext()) {
                Resource r = it.next().getObject();
                if (r instanceof UriRef) {
                    contexts.add(((UriRef) r).getUnicodeString());
                }
            }
            Map<String,Collection<?>> results = semanticIndexManager.executeProgram(ldProgramName, contexts,
                ci);
            for (Entry<String,Collection<?>> entry : results.entrySet()) {
                doc.addField(entry.getKey(), entry.getValue());
            }
        } catch (LDPathException e) {
            log.error("Cannot execute the ldPathProgram on ContentItem's metadata", e);
        }
    }

    private void addSolrSpecificFields(ContentItem ci, SolrInputDocument doc) {
        String title = null;
        try {
            title = ci.getPart(TITLE_URI, String.class);
        } catch (NoSuchPartException e) {
            title = ci.getUri().getUnicodeString();
        }
        doc.addField(SolrFieldName.TITLE.toString(), title);
        if (ci.getMetadata() != null) {
            addSemanticFields(ci, doc);
            addAnnotatedEntityFieldNames(ci, doc);
        } else {
            log.debug("There are no enhancements for the content item {}", ci.getUri().getUnicodeString());
        }
    }

    private void addSemanticFields(ContentItem ci, SolrInputDocument doc) {
        for (SolrFieldName fn : SolrFieldName.getSemanticFieldNames()) {
            addField(ci, doc, fn);
        }
    }

    private void addAnnotatedEntityFieldNames(ContentItem ci, SolrInputDocument doc) {
        for (SolrFieldName fn : SolrFieldName.getAnnotatedEntityFieldNames()) {
            addField(ci, doc, fn);
        }
    }

    private void addField(ContentItem ci, SolrInputDocument doc, SolrFieldName fieldName) {
        SelectQuery query = null;
        try {
            query = (SelectQuery) QueryParser.getInstance().parse(QueryGenerator.getFieldQuery(fieldName));
        } catch (ParseException e) {
            log.debug("Should never reach here!");
            log.error("Cannot parse the query generated by QueryGenerator: {}",
                QueryGenerator.getFieldQuery(fieldName), e);
            return;
        }

        ResultSet result = tcManager.executeSparqlQuery(query, ci.getMetadata());
        List<String> values = new ArrayList<String>();
        while (result.hasNext()) {
            SolutionMapping sol = result.next();
            Resource res = sol.get(fieldName.toString());
            if (res == null) continue;
            String value = res.toString();
            if (res instanceof Literal) {
                value = ((Literal) res).getLexicalForm();
            }
            value = value.replaceAll("_", " ");
            values.add(value);
        }
        if (!values.isEmpty()) {
            String fn = fieldName.toString();
            Object[] valArr = values.toArray();
            doc.addField(fn, valArr);
            // Now add for the text indexing dynamic field
            addIndexedTextDynamicField(doc, fn, valArr);
        }
    }

    private void addIndexedTextDynamicField(SolrInputDocument doc, String fn, Object[] valArr) {
        if (fn.endsWith(SolrVocabulary.SOLR_DYNAMIC_FIELD_TEXT)) {
            // replace the last "_t" with "_i"
            String ifn = SolrVocabulary.STANBOLRESERVED_PREFIX
                         + fn.substring(0, fn.lastIndexOf(SolrVocabulary.SOLR_DYNAMIC_FIELD_TEXT))
                         + SolrVocabulary.SOLR_DYNAMIC_FIELD_INDEXEDTEXT;
            doc.addField(ifn, valArr);
        }
    }

    @Override
    public ContentItem get(String id) throws StoreException {
        return get(id, SolrCoreManager.CONTENTHUB_DEFAULT_INDEX_NAME);
    }

    // TODO: we can use cache for "Recently uploaded Content Items"..
    @Override
    public ContentItem get(String id, String ldProgramName) throws StoreException {
        if (id == null) {
            throw new IllegalArgumentException("Id of the requested ContentItem cannot be null");
        }
        SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(
            ldProgramName);
        byte[] content = null;
        String mimeType = null;

        SolrQuery query = new SolrQuery();
        StringBuilder queryString = new StringBuilder();
        queryString.append(SolrFieldName.ID.toString());
        queryString.append(":\"");
        queryString.append(id);
        queryString.append('\"');
        query.setQuery(queryString.toString());
        QueryResponse response;
        try {
            response = solrServer.query(query);
            SolrDocumentList results = response.getResults();
            if (results != null && results.size() > 0) {
                SolrDocument result = results.get(0);
                content = (byte[]) result.getFieldValue(SolrFieldName.BINARYCONTENT.toString());
                mimeType = (String) result.getFieldValue(SolrFieldName.MIMETYPE.toString());
            } else {
                log.warn("No matching item in Solr for the given id {}.", id);
                return null;
            }
        } catch (SolrServerException ex) {
            log.error("", ex);
            throw new StoreException(ex.getMessage(), ex);
        }

        String enhancementQuery = QueryGenerator.getEnhancementsOfContent(id);
        SelectQuery selectQuery = null;
        try {
            selectQuery = (SelectQuery) QueryParser.getInstance().parse(enhancementQuery);
        } catch (ParseException e) {
            String msg = "Cannot parse the SPARQL while trying to retrieve the enhancements of the ContentItem";
            log.error(msg, e);
            throw new StoreException(msg, e);
        }

        ResultSet resultSet = tcManager.executeSparqlQuery(selectQuery, this.getEnhancementGraph());
        MGraph metadata = new IndexedMGraph();
        while (resultSet.hasNext()) {
            SolutionMapping mapping = resultSet.next();
            UriRef ref = (UriRef) mapping.get("enhID");
            Iterator<Triple> tripleItr = this.getEnhancementGraph().filter(ref, null, null);
            while (tripleItr.hasNext()) {
                Triple triple = tripleItr.next();
                metadata.add(triple);
            }
        }
        ContentItem ci = null;
        try {
            ci = ciFactory
                    .createContentItem(new UriRef(id), new ByteArraySource(content, mimeType), metadata);
        } catch (IOException e) {
            log.error("Failed to create contentitem with uri: {}", id);
            throw new StoreException(String.format("Failed to create contentitem with uri: %s", id));
        }
        return ci;
    }

    @Override
    public void deleteById(String id, String ldProgramName) throws StoreException {
        if (id == null || id.isEmpty()) return;
        SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(
            ldProgramName);
        removeEnhancements(id);
        try {
            solrServer.deleteById(id);
            solrServer.commit();
        } catch (SolrServerException e) {
            log.error("Solr Server Exception", e);
            throw new StoreException(e.getMessage(), e);
        } catch (IOException e) {
            log.error("IOException", e);
            throw new StoreException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(String id) throws StoreException {
        deleteById(id, SolrCoreManager.CONTENTHUB_DEFAULT_INDEX_NAME);
    }

    @Override
    public void deleteById(List<String> idList, String ldProgramName) throws StoreException {
        SolrServer solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(
            ldProgramName);
        for (int i = 0; i < idList.size(); i++) {
            String id = idList.get(i);
            idList.remove(i);
            idList.add(i, id);
        }
        try {
            solrServer.deleteById(idList);
            solrServer.commit();
        } catch (SolrServerException e) {
            log.error("Solr Server Exception", e);
            throw new StoreException(e.getMessage(), e);
        } catch (IOException e) {
            log.error("IOException", e);
            throw new StoreException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(List<String> idList) throws StoreException {
        deleteById(idList, SolrCoreManager.CONTENTHUB_DEFAULT_INDEX_NAME);
    }

}
