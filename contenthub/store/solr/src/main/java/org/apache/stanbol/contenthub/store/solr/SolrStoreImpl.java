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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
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
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.core.utils.ContentItemIDOrganizer;
import org.apache.stanbol.contenthub.core.utils.sparql.QueryGenerator;
import org.apache.stanbol.contenthub.servicesapi.enhancements.vocabulary.EnhancementGraphVocabulary;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.sinaci
 * 
 */
@Component(immediate = false)
@Service
public class SolrStoreImpl implements SolrStore {

    private static final Logger logger = LoggerFactory.getLogger(SolrStoreImpl.class);

    @Reference
    private ManagedSolrServer managedSolrServer;

    @Reference
    private TcManager tcManager;

    @Reference
    private EnhancementJobManager jobManager;

    private RegisteredSolrServerTracker serverTracker = null;
    
    public static final String SOLR_SERVER_NAME = "contenthub";

    @Activate
    public void activate(ComponentContext context) throws IllegalArgumentException, IOException, InvalidSyntaxException {
        if (!managedSolrServer.isManagedIndex(SOLR_SERVER_NAME)) {
            managedSolrServer.createSolrIndex(SOLR_SERVER_NAME, SOLR_SERVER_NAME, null);
        }
        serverTracker = new RegisteredSolrServerTracker(context.getBundleContext(), 
            new IndexReference(managedSolrServer.getServerName(), SOLR_SERVER_NAME));
        serverTracker.open();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if(serverTracker != null){
            serverTracker.close();
            serverTracker = null;
        }
        managedSolrServer = null;
    }
    
    protected SolrServer getServer(){
        SolrServer server = serverTracker != null ? serverTracker.getService() : null;
        if(server == null){
            throw new IllegalStateException("The SolrServer for the Contenthub " +
                    "is currently not available!");
        } else {
            return server;
        }
    }

    @Override
    public SolrContentItem create(String id, byte[] content, String contentType) {
        return create(id, "", content, contentType, null);
    }

    @Override
    public MGraph getEnhancementGraph() {
        final UriRef graphUri = new UriRef(EnhancementGraphVocabulary.ENHANCEMENTS_GRAPH_URI);
        MGraph enhancementGraph = null;
        try {
            enhancementGraph = tcManager.getMGraph(graphUri);
        } catch (NoSuchEntityException e) {
            logger.debug("Creating the enhancement graph!");
            enhancementGraph = tcManager.createMGraph(graphUri);
        }
        return enhancementGraph;
    }

    @Override
    public SolrContentItem create(String id,
                                  String title,
                                  byte[] content,
                                  String contentType,
                                  Map<String,List<Object>> constraints) {
        UriRef uri;
        if (id == null || id.isEmpty()) {
            uri = ContentItemHelper.makeDefaultUri(ContentItemIDOrganizer.CONTENT_ITEM_URI_PREFIX, content);
        } else {
            uri = new UriRef(ContentItemIDOrganizer.attachBaseURI(id));
        }
        logger.debug("Created ContentItem with id:{} and uri:{}", id, uri);
        final MGraph g = new SimpleMGraph();
        return new SolrContentItemImpl(uri.getUnicodeString(), title, content, contentType, g, constraints);
    }

    private Object inferObjectType(Object val) {
        Object ret = null;
        try {
            ret = DateFormat.getInstance().parse(val.toString());
        } catch (Exception e) {
            try {
                ret = Long.valueOf(val.toString());
            } catch (Exception e1) {
                try {
                    ret = Double.valueOf(val.toString());
                } catch (Exception e2) {
                    try {
                        ret = String.valueOf(val.toString());
                    } catch (Exception e3) {}
                }
            }
        }

        if (ret == null) ret = val;
        return ret;
    }

    private String addSolrDynamicFieldProperties(String fieldName, Object[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = inferObjectType(values[i]);
        }
        Object typed = values[0];
        String dynamicFieldName = fieldName;
        if (typed instanceof String) {
            dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_TEXT;
        } else if (typed instanceof Long) {
            dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_LONG;
        } else if (typed instanceof Double) {
            dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_DOUBLE;
        } else if (typed instanceof Date) {
            dynamicFieldName += SolrVocabulary.SOLR_DYNAMIC_FIELD_DATE;
        }
        return dynamicFieldName;
    }

    @Override
    public String enhanceAndPut(SolrContentItem sci) {
        try {
            jobManager.enhanceContent(sci);
        } catch (EnhancementException e) {
            logger.error("Cannot enhance content with id: {}", sci.getUri().getUnicodeString(), e);
        }

        updateEnhancementGraph(sci);

        return put(sci);
    }

    private void updateEnhancementGraph(SolrContentItem sci) {
        MGraph enhancementGraph = getEnhancementGraph();
        // Delete old enhancements which belong to this content item from the global enhancements graph.
        removeEnhancements(sci.getUri().getUnicodeString());
        // Add new enhancements of this content item to the global enhancements graph.
        Iterator<Triple> it = sci.getMetadata().iterator();
        while (it.hasNext()) {
            Triple triple = null;
            try {
                triple = it.next();
                enhancementGraph.add(triple);
            } catch (Exception e) {
                logger.warn("Cannot add triple {} to the TCManager.enhancementgraph", triple, e);
                continue;
            }
        }
    }

    private void removeEnhancements(String id) {
        MGraph enhancementGraph = getEnhancementGraph();
        Iterator<Triple> it = enhancementGraph.filter(new UriRef(id), null, null);
        List<Triple> willBeRemoved = new ArrayList<Triple>();
        while (it.hasNext()) {
            willBeRemoved.add(it.next());
        }
        enhancementGraph.removeAll(willBeRemoved);
    }

    @Override
    public String put(ContentItem ci) {
        if (ci.getUri().getUnicodeString() == null || ci.getUri().getUnicodeString().isEmpty()) {
            logger.debug("ID of the content item cannot be null while inserting to the SolrStore.");
            throw new IllegalArgumentException(
                    "ID of the content item cannot be null while inserting to the SolrStore.");
        }

        SolrServer solrServer = getServer();
        String content = null;
        try {
            content = IOUtils.toString(ci.getStream(), "UTF-8");
        } catch (IOException ex) {
            logger.error("Cannot read the content.", ex);
        }

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String creationDate = sdf.format(cal.getTime()).replace(" ", "T") + "Z";

        SolrInputDocument doc = new SolrInputDocument();
        doc.addField(SolrFieldName.ID.toString(), ci.getUri().getUnicodeString());
        doc.addField(SolrFieldName.CONTENT.toString(), content);
        doc.addField(SolrFieldName.MIMETYPE.toString(), ci.getMimeType());
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

        if (ci instanceof SolrContentItem) {
            SolrContentItem sci = (SolrContentItem) ci;

            // add the constraints
            if (sci.getConstraints() != null) {
                for (Entry<String,List<Object>> constraint : sci.getConstraints().entrySet()) {
                    Object[] values = constraint.getValue().toArray();
                    if (values == null || values.length == 0) continue;
                    String fieldName = constraint.getKey();
                    if (!SolrFieldName.isNameReserved(fieldName)) {
                        fieldName = addSolrDynamicFieldProperties(constraint.getKey(), values);
                    }
                    doc.addField(fieldName, values);
                }
            }

            if (sci.getMetadata() != null) {
                addSemanticFields(sci, doc);
                addFacetFields(sci, doc);
            } else {
                logger.debug("There are no enhancements for the content item {}", sci.getUri().getUnicodeString());
            }
        }

        try {
            solrServer.add(doc);
            solrServer.commit();
            logger.debug("Documents are committed to Solr Server successfully.");
        } catch (SolrServerException e) {
            logger.error("Solr Server Exception", e);
        } catch (IOException e) {
            logger.error("IOException", e);
        }

        return ci.getUri().getUnicodeString();
    }

    private void addSemanticFields(SolrContentItem sci, SolrInputDocument doc) {
        for (SolrFieldName fn : SolrFieldName.getSemanticFieldNames()) {
            addField(sci, doc, fn);
        }
    }

    private void addFacetFields(SolrContentItem sci, SolrInputDocument doc) {
        for (SolrFieldName fn : SolrFieldName.getAnnotatedEntityFieldNames()) {
            addField(sci, doc, fn);
        }
    }

    private void addField(SolrContentItem sci, SolrInputDocument doc, SolrFieldName fieldName) {
        SelectQuery query = null;
        try {
            query = (SelectQuery) QueryParser.getInstance().parse(QueryGenerator.getFieldQuery(fieldName));
        } catch (ParseException e) {
            logger.debug("Should never reach here!");
            logger.error("Cannot parse the query generated by QueryGenerator: {}",
                QueryGenerator.getFieldQuery(fieldName), e);
            return;
        }

        sci.getMetadata();
        ResultSet result = tcManager.executeSparqlQuery(query, sci.getMetadata());
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
        if (!values.isEmpty()) doc.addField(fieldName.toString(), values.toArray());
    }

    // TODO: we can use cache for "Recently uploaded Content Items"..
    @Override
    public SolrContentItem get(String id) {
        id = ContentItemIDOrganizer.attachBaseURI(id);
        SolrServer solrServer = getServer();
        String content = null;
        String mimeType = null;
        String title = null;
        Map<String,List<Object>> constraints = new HashMap<String,List<Object>>();

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
                content = (String) result.getFieldValue(SolrFieldName.CONTENT.toString());
                mimeType = (String) result.getFieldValue(SolrFieldName.MIMETYPE.toString());
                title = (String) result.getFieldValue(SolrFieldName.TITLE.toString());

                Iterator<Entry<String,Object>> itr = result.iterator();
                while (itr.hasNext()) {
                    Entry<String,Object> entry = itr.next();
                    String key = entry.getKey();
                    if (!SolrFieldName.isNameReserved(key)) {
                        List<Object> values = (List<Object>) result.getFieldValues(key);
                        constraints.put(key, values);
                    }
                }
            } else {
                logger.warn("No matching item in Solr for the given id {}.", id);
            }
        } catch (SolrServerException ex) {
            logger.error("", ex);
        }

        String enhancementQuery = QueryGenerator.getEnhancementsOfContent(id);
        SelectQuery selectQuery = null;
        try {
            selectQuery = (SelectQuery) QueryParser.getInstance().parse(enhancementQuery);
        } catch (ParseException e) {
            logger.error("", e);
        }

        ResultSet resultSet = tcManager.executeSparqlQuery(selectQuery, this.getEnhancementGraph());
        MGraph metadata = new SimpleMGraph();
        while (resultSet.hasNext()) {
            SolutionMapping mapping = resultSet.next();
            UriRef ref = (UriRef) mapping.get("enhancement");
            Iterator<Triple> tripleItr = this.getEnhancementGraph().filter(ref, null, null);
            while (tripleItr.hasNext()) {
                Triple triple = tripleItr.next();
                metadata.add(triple);
            }
        }

        byte[] contentByte = null;
        if(content != null){
        	contentByte = content.getBytes();
        }
        return new SolrContentItemImpl(id, title, contentByte, mimeType, metadata, constraints);
    }

    @Override
    public void deleteById(String id) {
        if (id == null || id.isEmpty()) return;
        SolrServer solrServer = getServer();
        id = ContentItemIDOrganizer.attachBaseURI(id);
        removeEnhancements(id);
        try {
            solrServer.deleteById(id);
            solrServer.commit();
        } catch (SolrServerException e) {
            logger.error("Solr Server Exception", e);
        } catch (IOException e) {
            logger.error("IOException", e);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    @Override
    public void deleteById(List<String> idList) {
        SolrServer solrServer = getServer();
        for (int i = 0; i < idList.size(); i++) {
            String id = ContentItemIDOrganizer.attachBaseURI(idList.get(i));
            idList.remove(i);
            idList.add(i, id);
        }
        try {
            solrServer.deleteById(idList);
            solrServer.commit();
        } catch (SolrServerException e) {
            logger.error("Solr Server Exception", e);
        } catch (IOException e) {
            logger.error("IOException", e);
        }
    }
}
