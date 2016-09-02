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
package org.apache.stanbol.entityhub.yard.solr.impl;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.solr.utils.SolrUtil;
import org.apache.stanbol.commons.solr.utils.StreamQueryRequest;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.yard.AbstractYard;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.yard.solr.defaults.IndexDataTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrQueryFactory.SELECT;
import org.apache.stanbol.entityhub.yard.solr.model.FieldMapper;
import org.apache.stanbol.entityhub.yard.solr.model.IndexField;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValueFactory;
import org.apache.stanbol.entityhub.yard.solr.model.NoConverterException;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link Yard} interface based on a Solr Server.
 * <p>
 * This Yard implementation supports to store data of multiple yard instances within the same Solr index. The
 * {@link FieldMapper#getDocumentDomainField()} with the value of the Yard ID ({@link #getId()}) is used to
 * mark documents stored by the different Yards using the same Index. Queries are also restricted to documents
 * stored by the actual Yard by adding a <a
 * href="http://wiki.apache.org/solr/CommonQueryParameters#fq">FilterQuery</a>
 * <code>fq=fieldMapper.getDocumentDomainField()+":"+getId()</code> to all queries. This feature can be
 * activated by setting the {@link #MULTI_YARD_INDEX_LAYOUT} in the configuration. However this requires, that
 * the documents in the index are already marked with the ID of the Yard. So setting this property makes
 * usually only sense when the Solr index do not contain any data.
 * <p>
 * Also note, that the different Yards using the same index MUST NOT store Representations with the same ID.
 * If that happens, that the Yard writing the Representation last will win and the Representation will be
 * deleted for the other Yard!
 * <p>
 * The SolrJ library is used for the communication with the SolrServer.
 * <p>
 * TODO: There is still some refactoring needed, because a lot of the code within this bundle is more generic
 * and usable regardless what kind of "document based" store is used. Currently the Solr specific stuff is in
 * the impl and the default packages. All the other classes are intended to be generally useful. However there
 * might be still some unwanted dependencies.
 * <p>
 * TODO: It would be possible to support for multi cores (see http://wiki.apache.org/solr/CoreAdmin for more
 * Information)<br>
 * However it is not possible to create cores on the fly (at least not directly; one would need to create
 * first the needed directories and than call CREATE via the CoreAdmin). As soon as Solr is directly started
 * via OSGI and we do know the Solr home, than it would be possible to implement "on the fly" generation of
 * new cores. this would also allow a configuration where - as default - a new core is created automatically
 * on the integrated Solr Server for any configured SolrYard.
 * 
 * @author Rupert Westenthaler
 * 
 */
public class SolrYard extends AbstractYard implements Yard {

    /**
     * What a surprise it's the logger!
     */
    private Logger log = LoggerFactory.getLogger(SolrYard.class);
    /**
     * The SolrServer used for this Yard.
     */
    private final SolrServer server;
    
    /**
     * The {@link FieldMapper} is responsible for converting fields of {@link Representation} to fields in the
     * {@link SolrInputDocument} and vice versa
     */
    private final SolrFieldMapper fieldMapper;
    /**
     * The {@link IndexValueFactory} is responsible for converting values of fields in the
     * {@link Representation} to the according {@link IndexValue}. One should note, that some properties of
     * the {@link IndexValue} such as the language ({@link IndexValue#getLanguage()}) and the dataType (
     * {@link IndexValue#getType()}) are encoded within the field name inside the {@link SolrInputDocument}
     * and {@link SolrDocument}. This is done by the configured {@link FieldMapper}.
     */
    private IndexValueFactory indexValueFactory;
    /**
     * The {@link SolrQueryFactory} is responsible for converting the {@link Constraint}s of a query to
     * constraints in the index. This requires usually that a single {@link Constraint} is described by
     * several constraints in the index (see {@link IndexConstraintTypeEnum}).
     * <p>
     * TODO: The encoding of such constraints is already designed correctly, the {@link SolrQueryFactory} that
     * implements logic of converting the Incoming {@link Constraint}s and generating the {@link SolrQuery}
     * needs to undergo some refactoring!
     * 
     */
    private final SolrQueryFactory solrQueryFactoy;
    /**
     * Used to store the name of the field used to get the {@link SolrInputDocument#setDocumentBoost(float)}
     * for a Representation. This name is available via {@link SolrYardConfig#getDocumentBoostFieldName()}
     * however it is stored here to prevent lookups for field of every stored {@link Representation}.
     */
    private final String documentBoostFieldName;
    /**
     * Map used to store boost values for fields. The default Boost for fields is 1.0f. This is used if this
     * map is <code>null</code>, a field is not a key in this map, the value of a field in that map is
     * <code>null</code> or lower equals zero. Also NOTE that the boost for fields is multiplied with the
     * boost for the Document if present.
     */
    private final Map<String,Float> fieldBoostMap;

    /**
     * If update(..) and store(..) calls should be immediately committed.
     */
    private boolean immediateCommit = DEFAULT_IMMEDIATE_COMMIT_STATE;
    /**
     * If <code>{@link #immediateCommit} == false</code> this is the time in ms parsed to Solr until the
     * documents parsed to update(..) and store(..) need to be committed.
     */
    private int commitWithin = DEFAULT_COMMIT_WITHIN_DURATION;

    private final SolrYardConfig config;
    private boolean closed;
    /**
     * Creates a new SolrYard by parsing the SolrServer, the SolrYard config and
     * optionally a namespace prefix service
     * @param server the {@link SolrServer} used by this Yard
     * @param config the configuration
     * @param nsPrefixService the {@link NamespacePrefixService} or <code>null</code>
     * if not available.
     */
    public SolrYard(SolrServer server, SolrYardConfig config, 
            NamespacePrefixService nsPrefixService) {
        super();
        if(server == null){
            throw new IllegalArgumentException("The parsed SolrServer instance" +
                    "MUST NOT be NULL!");
        }
        this.server = server;
        if(config == null){
            throw new IllegalArgumentException("The parsed SolrYard configuration" +
                    "MUST NOT be NULL");
        }
        this.config = config;
        //set the value/query/indexValue factory
        activate(InMemoryValueFactory.getInstance(), DefaultQueryFactory.getInstance(), config);
        this.indexValueFactory = IndexValueFactory.getInstance();
        // Set often accessed fields based on config
        this.immediateCommit = config.isImmediateCommit();
        this.commitWithin = config.getCommitWithinDuration();
        this.documentBoostFieldName = config.getDocumentBoostFieldName();
        this.fieldBoostMap = config.getFieldBoosts();
        //init fieldMapper and queryFactory
        this.fieldMapper = new SolrFieldMapper(this.server, nsPrefixService);
        this.solrQueryFactoy = new SolrQueryFactory(getValueFactory(), indexValueFactory, fieldMapper);
    }

    /**
     * This will case the SolrIndex to be optimised
     * @throws YardException on any error while optimising
     */
    public final void optimize() throws YardException {
        if(closed){
            throw new IllegalStateException("The SolrYard is already closed!");
        }
        try {
            server.optimize();
        } catch (SolrServerException e) {
            throw new YardException("Unable to optimise SolrIndex!", e);
        } catch (IOException e) {
            throw new YardException("Unable to optimise SolrIndex!", e);
        }
    }
    /**
     * can be used outside of the OSGI environment to deactivate this instance. Thiw will cause the SolrIndex
     * to be committed and optimised.
     */
    public void close() {
        if(closed){
            return;
        }
        log.info("... deactivating SolrYard " + config.getName() + " (id=" + config.getId() + ")");
        try {
            server.commit();
        } catch (SolrServerException e) {
            log.warn("Unable to perform final commit during deactivation",e);
        } catch (IOException e) {
            log.warn("Unable to perform final commit during deactivation",e);
        }
        closed = true;
    }

    /**
     * Calls the {@link #deactivate(ComponentContext)} with <code>null</code> as component context
     */
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public final QueryResultList<Representation> find(final FieldQuery parsedQuery) throws YardException {
        return find(parsedQuery, SELECT.QUERY);
    }

    private QueryResultList<Representation> find(final FieldQuery parsedQuery, SELECT select) throws YardException {
        //create a clone of the query, because we need to refine it because the
        //query (as executed) needs to be included in the result set
        FieldQuery fieldQuery = parsedQuery.clone();
        log.debug("find " + fieldQuery);
        long start = System.currentTimeMillis();
        final Set<String> selected;
        if (select == SELECT.QUERY) {
            // if query set the fields to add to the result Representations
            selected = new HashSet<String>(fieldQuery.getSelectedFields());
            // add the score to query results!
            selected.add(RdfResourceEnum.resultScore.getUri());
        } else {
            // otherwise add all fields
            selected = null;
        }

        final SolrQuery query = solrQueryFactoy.parseFieldQuery(fieldQuery, select);
        long queryGeneration = System.currentTimeMillis();
        if(closed){
            log.warn("The SolrYard '{}' was already closed!",config.getName());
        }
        QueryResponse response;
        try {
            response = AccessController.doPrivileged(new PrivilegedExceptionAction<QueryResponse>() {
                public QueryResponse run() throws IOException, SolrServerException {
                    StreamQueryRequest request = new StreamQueryRequest(query);
                     return request.process(server);
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof SolrServerException){
                if ("unknown handler: /mlt".equals(e.getCause().getMessage())) {
                    throw new YardException("Solr is missing '<requestHandler name=\"/mlt\""
                        + " class=\"solr.MoreLikeThisHandler\" startup=\"lazy\" />'"
                        + " in 'solrconfig.xml'", e);
                }
                throw new YardException("Error while performing Query on SolrServer: " + query.getQuery(), e);
            } else if(e instanceof IOException){
                throw new YardException("Unable to access SolrServer",e);
            } else {
                throw RuntimeException.class.cast(e);
            }
        }
        if(SolrQueryFactory.MLT_QUERY_TYPE.equals(query.getRequestHandler())){
            log.debug("{}",response);
        }
        long queryTime = System.currentTimeMillis();
        // return a queryResultList
        QueryResultListImpl<Representation> resultList = new QueryResultListImpl<Representation>(fieldQuery,
        // by adapting SolrDocuments to Representations
                new AdaptingIterator<SolrDocument,Representation>(response.getResults().iterator(),
                // inline Adapter Implementation
                        new AdaptingIterator.Adapter<SolrDocument,Representation>() {
                            @Override
                            public Representation adapt(SolrDocument doc, Class<Representation> type) {
                                // use this method for the conversion!
                                return createRepresentation(doc, selected);
                            }
                        }, Representation.class), Representation.class);
        long resultProcessing = System.currentTimeMillis();
        log.debug(String.format(
            "  ... done [queryGeneration=%dms|queryTime=%dms|resultProcessing=%dms|sum=%dms]",
            (queryGeneration - start), (queryTime - queryGeneration), (resultProcessing - queryTime),
            (resultProcessing - start)));
        return resultList;
    }

    @Override
    public final QueryResultList<String> findReferences(FieldQuery parsedQuery) throws YardException {
        //create a clone of the query, because we need to refine it because the
        //query (as executed) needs to be included in the result set
        FieldQuery fieldQuery = parsedQuery.clone();
        final SolrQuery query = solrQueryFactoy.parseFieldQuery(fieldQuery, SELECT.ID);
        if(closed){
            log.warn("The SolrYard '{}' was already closed!",config.getName());
        }
        QueryResponse response;
        try {
            response = AccessController.doPrivileged(new PrivilegedExceptionAction<QueryResponse>() {
                public QueryResponse run() throws IOException, SolrServerException {
                        return server.query(query, METHOD.POST);
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof SolrServerException){
                throw new YardException("Error while performing query on the SolrServer (query: "
                        + query.getQuery()+")!", e);
            } else if(e instanceof IOException){
                throw new YardException("Unable to access SolrServer",e);
            } else {
                throw RuntimeException.class.cast(e);
            }
        }
        // return a queryResultList
        return new QueryResultListImpl<String>(fieldQuery,
        // by adapting SolrDocuments to Representations
                new AdaptingIterator<SolrDocument,String>(response.getResults().iterator(),
                // inline Adapter Implementation
                        new AdaptingIterator.Adapter<SolrDocument,String>() {
                            @Override
                            public String adapt(SolrDocument doc, Class<String> type) {
                                // use this method for the conversion!
                                return doc.getFirstValue(fieldMapper.getDocumentIdField()).toString();
                            }
                        }, String.class), String.class);
    }

    @Override
    public final QueryResultList<Representation> findRepresentation(FieldQuery parsedQuery) throws YardException {
        return find(parsedQuery, SELECT.ALL);
    }

    @Override
    public final Representation getRepresentation(String id) throws YardException {
        if (id == null) {
            throw new IllegalArgumentException("The parsed Representation id MUST NOT be NULL!");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("The parsed Representation id MUST NOT be empty!");
        }
        if(closed){
            log.warn("The SolrYard '{}' was already closed!",config.getName());
        }
        SolrDocument doc;
        long start = System.currentTimeMillis();
        try {
            doc = getSolrDocument(id);
        } catch (SolrServerException e) {
            throw new YardException("Error while getting SolrDocument for id" + id, e);
        } catch (IOException e) {
            throw new YardException("Unable to access SolrServer", e);
        }
        long retrieve = System.currentTimeMillis();
        Representation rep;
        if (doc != null) {
            // create an Representation for the Doc! retrieve
            log.debug(String.format("Create Representation %s from SolrDocument",
                doc.getFirstValue(fieldMapper.getDocumentIdField())));
            rep = createRepresentation(doc, null);
        } else {
            rep = null;
        }
        long create = System.currentTimeMillis();
        log.debug(String.format("  ... %s [retrieve=%dms|create=%dms|sum=%dms]", rep == null ? "not found"
                : "done", (retrieve - start), (create - retrieve), (create - start)));
        return rep;
    }

    /**
     * Creates the Representation for the parsed SolrDocument!
     * 
     * @param doc
     *            The Solr Document to convert
     * @param fields
     *            if NOT NULL only this fields are added to the Representation
     * @return the Representation
     */
    protected final Representation createRepresentation(SolrDocument doc, Set<String> fields) {
        if(fieldMapper == null){
            throw new IllegalArgumentException("The parsed FieldMapper MUST NOT be NULL!");
        }
        if(doc == null){
            throw new IllegalArgumentException("The parsed SolrDocument MUST NOT be NULL!");
        }
        Object id = doc.getFirstValue(fieldMapper.getDocumentIdField());
        if (id == null) {
            throw new IllegalStateException(String.format(
                "The parsed Solr Document does not contain a value for the %s Field!",
                fieldMapper.getDocumentIdField()));
        }
        Representation rep = getValueFactory().createRepresentation(id.toString());
        for (String fieldName : doc.getFieldNames()) {
            IndexField indexField = fieldMapper.getField(fieldName);
            if (indexField != null && indexField.getPath().size() == 1) {
                String lang = indexField.getLanguages().isEmpty() ? null : indexField.getLanguages()
                        .iterator().next();
                if (fields == null || fields.contains(indexField.getPath().get(0))) {
                    for (Object value : doc.getFieldValues(fieldName)) {
                        if (value != null) {
                            IndexDataTypeEnum dataTypeEnumEntry = IndexDataTypeEnum.forIndexType(indexField
                                    .getDataType());
                            if (dataTypeEnumEntry != null) {
                                Object javaValue = indexValueFactory.createValue(
                                    dataTypeEnumEntry.getJavaType(), indexField.getDataType(), value, lang);
                                if (javaValue != null) {
                                    rep.add(indexField.getPath().iterator().next(), javaValue);
                                } else {
                                    log.warn(String.format("java value=null for index value %s", value));
                                }
                            } else {
                                log.warn(String.format(
                                    "No DataType Configuration found for Index Data Type %s!",
                                    indexField.getDataType()));
                            }
                        } // else index value == null -> ignore
                    } // end for all values
                }
            } else {
                if (indexField != null) {
                    log.warn(String.format("Unable to prozess Index Field %s (for IndexDocument Field: %s)",
                        indexField, fieldName));
                }
            }
        } // end for all fields
        return rep;
    }

    @Override
    public final boolean isRepresentation(String id) throws YardException {
        if (id == null) {
            throw new IllegalArgumentException("The parsed Representation id MUST NOT be NULL!");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("The parsed Representation id MUST NOT be empty!");
        }
        try {
            return getSolrDocument(id, Arrays.asList(fieldMapper.getDocumentIdField())) != null;
        } catch (SolrServerException e) {
            throw new YardException("Error while performing getDocumentByID request for id " + id, e);
        } catch (IOException e) {
            throw new YardException("Unable to access SolrServer", e);
        }
    }

    /**
     * Checks what of the documents referenced by the parsed IDs are present in the Solr Server
     * 
     * @param ids
     *            the ids of the documents to check
     * @return the ids of the found documents
     * @throws SolrServerException
     *             on any exception of the SolrServer
     * @throws IOException
     *             an any IO exception while accessing the SolrServer
     */
    protected final Set<String> checkRepresentations(Set<String> ids) 
                                                     throws SolrServerException, IOException {
        Set<String> found = new HashSet<String>();
        String field = fieldMapper.getDocumentIdField();
        for (SolrDocument foundDoc : getSolrDocuments(ids, Arrays.asList(field))) {
            Object value = foundDoc.getFirstValue(field);
            if (value != null) {
                found.add(value.toString());
            }
        }
        return found;
    }

    @Override
    public final void remove(final String id) throws YardException, IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("The parsed Representation id MUST NOT be NULL!");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("The parsed Representation id MUST NOT be empty!");
        }
        final SolrYardConfig config = (SolrYardConfig)getConfig();
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws IOException, SolrServerException {
                    if(config.isMultiYardIndexLayout()){
                        //make sure we only delete the Entity only if it is  managed by 
                        //this Yard. Entities of other Yards MUST NOT be deleted!
                        server.deleteByQuery(String.format("%s:%s AND %s:\"%s\"",
                            fieldMapper.getDocumentDomainField(),
                            SolrUtil.escapeSolrSpecialChars(getId()),
                            fieldMapper.getDocumentIdField(),
                            SolrUtil.escapeSolrSpecialChars(id)));
                    } else {
                        server.deleteById(id);
                    }
                    server.commit();
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof SolrServerException){
                throw new YardException("Error while deleting document " + id + " from the Solr server", e);
            } else if(e instanceof IOException){
                throw new YardException("Unable to access SolrServer",e);
            } else {
                throw RuntimeException.class.cast(e);
            }
        }
        // NOTE: We do not need to update all Documents that refer this ID, because
        // only the representation of the Entity is deleted and not the
        // Entity itself. So even that we do no longer have an representation
        // the entity still exists and might be referenced by others!
    }

    @Override
    public final void remove(Iterable<String> ids) throws IllegalArgumentException, YardException {
        if (ids == null) {
            throw new IllegalArgumentException("The parsed IDs MUST NOT be NULL");
        }
        final List<String> toRemove = new ArrayList<String>();
        for (String id : ids) {
            if (id != null && !id.isEmpty()) {
                toRemove.add(id);
            }
        }
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws IOException, SolrServerException {
                    if(config.isMultiYardIndexLayout()){
                        //make sure we only delete Entities managed by this Yard
                        //if someone parses an ID managed by an other yard we MUST NOT
                        //delete it!
                        for(String id : toRemove){
                            server.deleteByQuery(String.format("%s:%s AND %s:\"%s\"",
                                fieldMapper.getDocumentDomainField(),
                                SolrUtil.escapeSolrSpecialChars(getId()),
                                fieldMapper.getDocumentIdField(),
                                SolrUtil.escapeSolrSpecialChars(id)));
                        }
                    } else {
                        server.deleteById(toRemove);
                    }
                    server.commit();
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof SolrServerException){
                throw new YardException("Error while deleting documents from the Solr server", e);
            } else if(e instanceof IOException){
                throw new YardException("Unable to access SolrServer",e);
            } else {
                throw RuntimeException.class.cast(e);
            }
        }
        // NOTE: We do not need to update all Documents that refer this ID, because
        // only the representation of the Entity is deleted and not the
        // Entity itself. So even that we do no longer have an representation
        // the entity still exists and might be referenced by others!
    }
    @Override
    public void removeAll() throws YardException {
        if(closed){
            log.warn("The SolrYard '{}' was already closed!",config.getName());
        }
        try {
            //delete all documents
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws IOException, SolrServerException, YardException {
                    //ensures that the fildMapper is initialised and reads the
                    //namespace config before deleting all documents
                    if(config.isMultiYardIndexLayout()){
                        //only delete entities of this referenced site
                        server.deleteByQuery(String.format("%s:%s", 
                            fieldMapper.getDocumentDomainField(),
                            SolrUtil.escapeSolrSpecialChars(getId())));
                    } else { //we can delete all
                        server.deleteByQuery("*:*");
                    }
                    //ensure that the namespace config is stored again after deleting
                    //all documents
                    fieldMapper.saveNamespaceConfig(false);
                    server.commit();
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof SolrServerException){
                throw new YardException("Error while deleting documents from the Solr server", e);
            } else if(e instanceof IOException){
                throw new YardException("Unable to access SolrServer",e);
            } else if(e instanceof YardException){
                throw (YardException)e;
            } else {
                throw RuntimeException.class.cast(e);
            }
        }        
    }

    @Override
    public final Representation store(Representation representation) throws YardException, IllegalArgumentException {
        log.debug("Store {}", representation != null ? representation.getId() : null);
        if (representation == null) {
            throw new IllegalArgumentException("The parsed Representation MUST NOT be NULL!");
        }
        long start = System.currentTimeMillis();
        final SolrInputDocument inputDocument = createSolrInputDocument(representation);
        long create = System.currentTimeMillis();
        if(closed){
            log.warn("The SolrYard '{}' was already closed!",config.getName());
        }
        try {
            final UpdateRequest update = new UpdateRequest();
            if (!immediateCommit) {
                update.setCommitWithin(commitWithin);
            }
            update.add(inputDocument);
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws IOException, SolrServerException {
                    update.process(server);
                    if (immediateCommit) {
                        server.commit();
                    }
                    return null; // nothing to return
                }
            });
            long stored = System.currentTimeMillis();
            log.debug("  ... done [create={}ms|store={}ms|sum={}ms]", 
                new Object[] {(create - start),(stored - create),(stored - start)});
        } catch (PrivilegedActionException pae){
            if(pae.getException() instanceof SolrServerException){
                throw new YardException(String.format("Exception while adding Document to Solr",
                    representation.getId()), pae.getException());
            } else if( pae.getException() instanceof IOException){
                throw new YardException("Unable to access SolrServer", pae.getException());
            } else {
                throw RuntimeException.class.cast(pae.getException());
            }
        }
        return representation;
    }

    @Override
    public final Iterable<Representation> store(Iterable<Representation> representations) throws IllegalArgumentException,
                                                                                         YardException {
        if (representations == null) {
            throw new IllegalArgumentException("The parsed Representations MUST NOT be NULL!");
        }
        Collection<Representation> added = new HashSet<Representation>();
        long start = System.currentTimeMillis();
        Collection<SolrInputDocument> inputDocs = new HashSet<SolrInputDocument>();
        for (Representation representation : representations) {
            if (representation != null) {
                inputDocs.add(createSolrInputDocument(representation));
                added.add(representation);
            }
        }
        if(inputDocs.isEmpty()){ //empty data sent ... nothing to do
            log.debug("strore called with empty collection of Representations");
            return representations;
        }
        long created = System.currentTimeMillis();
        if(closed){
            log.warn("The SolrYard '{}' was already closed!",config.getName());
        }
        final UpdateRequest update = new UpdateRequest();
        if (!immediateCommit) {
            update.setCommitWithin(commitWithin);
        }
        update.add(inputDocs);
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws IOException, SolrServerException {
                    update.process(server);
                    if (immediateCommit) {
                        server.commit();
                    }
                    return null;
                }
            });
            long ready = System.currentTimeMillis();
            log.debug(String.format(
                "Processed store request for %d documents in %dms (created %dms| stored%dms)", inputDocs.size(),
                ready - start, created - start, ready - created));
        } catch (PrivilegedActionException pae){
            if(pae.getException() instanceof SolrServerException){
                throw new YardException("Exception while adding Documents to the Solr Server!", pae.getException());
            } else if( pae.getException() instanceof IOException){
                throw new YardException("Unable to access SolrServer", pae.getException());
            } else {
                throw RuntimeException.class.cast(pae.getException());
            }
        }
        return added;
    }

    /**
     * Internally used to create Solr input documents for parsed representations.
     * <p>
     * This method supports boosting of fields. The boost is calculated by combining
     * <ol>
     * <li>the boot for the whole representation - by calling {@link #getDocumentBoost(Representation)}
     * <li>the boost of each field - by using the configured {@link #fieldBoostMap}
     * </ol>
     * 
     * @param representation
     *            the representation
     * @return the Solr document for indexing
     */
    protected final SolrInputDocument createSolrInputDocument(Representation representation) {
        SolrYardConfig config = (SolrYardConfig) getConfig();
        SolrInputDocument inputDocument = new SolrInputDocument();
        // If multiYardLayout is active, than we need to add the YardId as
        // domain for all added documents!
        if (config.isMultiYardIndexLayout()) {
            inputDocument.addField(fieldMapper.getDocumentDomainField(), config.getId());
        } // else we need to do nothing
        inputDocument.addField(fieldMapper.getDocumentIdField(), representation.getId());
        // first process the document boost
        Float documentBoost = getDocumentBoost(representation);
        //NOTE: Do not use DocumentBoost, because FieldBoost will override
        //      document boosts and are not multiplied with with document boosts
        if(documentBoost != null){
            inputDocument.setDocumentBoost(documentBoost);
        }
        for (Iterator<String> fields = representation.getFieldNames(); fields.hasNext();) {
            // TODO: maybe add some functionality to prevent indexing of the
            // field configured as documentBoostFieldName!
            // But this would also prevent the possibility to intentionally
            // override the boost.
            String field = fields.next();
            /*
             * With STANBOL-1027 the calculation of the boost has changed to
             * consider multiple values for Representation#get(field).
             */
            float baseBoost; //the boost without considering the number of values per solr field
            Float fieldBoost = fieldBoostMap == null ? null : fieldBoostMap.get(field);
            final Map<String,int[]> fieldsToBoost; //used to keep track of field we need boost
            if(fieldBoost != null){
                baseBoost = documentBoost != null ? fieldBoost * documentBoost : fieldBoost;
                fieldsToBoost = new HashMap<String,int[]>();
            } else { 
                baseBoost = -1;
                fieldsToBoost = null;
            }
            //NOTE: Setting a boost requires two iteration
            //  (1) we add the values to the SolrInputDocument without an boost
            //  (2) set the boost by using doc.setField(field,doc.getFieldValues(),boost)
            //  Holding field values in an own map does not make sense as the SolrInputDocument
            //  does already exactly that (in an more efficient way)
            for (Iterator<Object> values = representation.get(field); values.hasNext();) {
                // now we need to get the indexField for the value
                Object next = values.next();
                IndexValue value;
                try {
                    value = indexValueFactory.createIndexValue(next);
                    for (String fieldName : fieldMapper.getFieldNames(Arrays.asList(field), value)) {
                        //In step (1) of boosting just keep track of the field
                        if(fieldBoost != null){ //wee need to boost in (2)
                            int[] numValues = fieldsToBoost.get(fieldName);
                            if(numValues == null){
                                numValues = new int[]{1};
                                fieldsToBoost.put(fieldName, numValues);
                                //the first time add the document with the baseBoost
                                //as this will be the correct boost for single value fields
                                inputDocument.addField(fieldName, value.getValue(),baseBoost);
                            } else {
                                numValues[0]++;
                                //for multi valued fields the correct boost is set in (2)
                                //so we can add here without an boost
                                inputDocument.addField(fieldName, value.getValue());
                            }
                        } else {
                            //add add the values without boost
                            inputDocument.addField(fieldName, value.getValue());
                        }
                    }
                } catch (NoConverterException e) {
                    log.warn(
                        String.format("Unable to convert value %s (type:%s) for field %s!", next,
                            next.getClass(), field), e);
                } catch (IllegalArgumentException e) { //usually because the Object is NULL or empty
                    if(log.isDebugEnabled()){
                        log.debug(String.format("Illegal Value %s (type:%s) for field %s!", next,
                                next.getClass(), field), e);
                    }
                } catch (RuntimeException e) {
                    log.warn(
                        String.format("Unable to process value %s (type:%s) for field %s!", next,
                            next.getClass(), field), e);
                }
            }
            if(fieldBoost != null){ //we need still to do part (2) of setting the correct boost
                for(Entry<String,int[]> entry : fieldsToBoost.entrySet()){
                    if(entry.getValue()[0] > 1) { //adapt the boost only for multi valued fields
                        SolrInputField solrField = inputDocument.getField(entry.getKey());
                        //the correct bosst is baseBoost (representing entity boost with field
                        //boost) multiplied with the sqrt(fieldValues). The 2nd part aims to
                        //compensate the Solr lengthNorm (1/sqrt(fieldTokens))
                        //see STANBOL-1027 for details
                        solrField.setBoost(baseBoost*(float)Math.sqrt(entry.getValue()[0]));
                    }
                }
            }
        }
        return inputDocument;
    }

    /**
     * Extracts the document boost from a {@link Representation}.
     * 
     * @param representation
     *            the representation
     * @return the Boost or <code>null</code> if not found or lower equals zero
     */
    private Float getDocumentBoost(Representation representation) {
        if (documentBoostFieldName == null) {
            return null;
        }
        Float documentBoost = null;
        for (Iterator<Object> values = representation.get(documentBoostFieldName); 
                values.hasNext() && documentBoost == null;) {
            Object value = values.next();
            if (value instanceof Float) {
                documentBoost = (Float) value;
            } else {
                try {
                    documentBoost = Float.parseFloat(value.toString());
                } catch (NumberFormatException e) {
                    log.warn(String
                            .format(
                                "Unable to parse the Document Boost from field %s=%s[type=%s] -> The Document Boost MUST BE a Float value!",
                                documentBoostFieldName, value, value.getClass()));
                }
            }
        }
        return documentBoost == null ? null : documentBoost >= 0 ? documentBoost : null;
    }

    @Override
    public final Representation update(Representation representation) 
            throws IllegalArgumentException, NullPointerException, YardException {
        if (representation == null) {
            throw new IllegalArgumentException("The parsed Representation MUST NOT be NULL!");
        }
        boolean found = isRepresentation(representation.getId());
        if (found) {
            return store(representation); // there is no "update" for solr
        } else {
            throw new IllegalArgumentException("Parsed Representation " + representation.getId()
                    + " in not managed by this Yard " + getName() + "(id=" + getId() + ")");
        }
    }

    @Override
    public final Iterable<Representation> update(Iterable<Representation> representations)
            throws YardException, IllegalArgumentException, NullPointerException {
        if (representations == null) {
            throw new IllegalArgumentException("The parsed Iterable over Representations MUST NOT be NULL!");
        }
        long start = System.currentTimeMillis();
        Set<String> ids = new HashSet<String>();

        for (Representation representation : representations) {
            if (representation != null) {
                ids.add(representation.getId());
            }
        }
        if(closed){
            log.warn("The SolrYard '{}' was already closed!",config.getName());
        }
        int numDocs = ids.size(); // for debuging
        try {
            ids = checkRepresentations(ids); // returns the ids found in the solrIndex
        } catch (SolrServerException e) {
            throw new YardException("Error while searching for alredy present documents "
                    + "before executing the actual update for the parsed Representations", e);
        } catch (IOException e) {
            throw new YardException("Unable to access SolrServer", e);
        }
        long checked = System.currentTimeMillis();
        List<SolrInputDocument> inputDocs = new ArrayList<SolrInputDocument>(ids.size());
        List<Representation> updated = new ArrayList<Representation>();
        for (Representation representation : representations) {
            if (representation != null && ids.contains(representation.getId())) { // null parsed or not
                                                                                  // already present
                inputDocs.add(createSolrInputDocument(representation));
                updated.add(representation);
            }
        }
        long created = System.currentTimeMillis();
        if (!inputDocs.isEmpty()) {
            try {
                final UpdateRequest update = new UpdateRequest();
                if (!immediateCommit) {
                    update.setCommitWithin(commitWithin);
                }
                update.add(inputDocs);
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    public UpdateResponse run() throws IOException, SolrServerException {
                        update.process(server);
                        if (immediateCommit) {
                            server.commit();
                        }
                        return null;
                    }
                });
            } catch (PrivilegedActionException pae){
                if(pae.getException() instanceof SolrServerException){
                    throw new YardException("Error while adding updated Documents to the SolrServer", 
                        pae.getException());
                } else if( pae.getException() instanceof IOException){
                    throw new YardException("Unable to access SolrServer", pae.getException());
                } else {
                    throw RuntimeException.class.cast(pae.getException());
                }
            }
        }
        long ready = System.currentTimeMillis();
        log.info(String.format( "Processed updateRequest for %d documents (%d in index "
                + "| %d updated) in %dms (checked %dms|created %dms| stored%dms)",
                numDocs, ids.size(), updated.size(), ready - start, checked - start, 
                created - checked, ready - created));
        return updated;
    }

    /**
     * Stores the parsed document within the Index. This Method is also used by other classes within this
     * package to store configurations directly within the index
     * 
     * @param inputDoc
     *            the document to store
     */
    protected final void storeSolrDocument(final SolrInputDocument inputDoc) 
                                           throws SolrServerException,IOException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<UpdateResponse>() {
                public UpdateResponse run() throws IOException, SolrServerException {
                    return server.add(inputDoc);
                }
            });
        } catch (PrivilegedActionException pae) {
            if(pae.getException() instanceof SolrServerException){
                throw (SolrServerException)pae.getException();
            } else if( pae.getException() instanceof IOException){
                throw (IOException)pae.getException();
            } else {
                throw RuntimeException.class.cast(pae.getException());
            }
        }
    }

    /**
     * Getter for a SolrDocument based on the ID. This Method is also used by other classes within this
     * package to load configurations directly from the index
     * 
     * @param inputDoc
     *            the document to store
     */
    protected final SolrDocument getSolrDocument(String uri) throws SolrServerException, IOException {
        return getSolrDocument(uri, null);
    }

    protected final Collection<SolrDocument> getSolrDocuments(Collection<String> uris, 
                                                              Collection<String> fields) 
                                                              throws SolrServerException, IOException {
        SolrYardConfig config = (SolrYardConfig) getConfig();
        final SolrQuery solrQuery = new SolrQuery();
        if (fields == null || fields.isEmpty()) {
            solrQuery.addField("*"); // select all fields
        } else {
            for (String field : fields) {
                if (field != null && !field.isEmpty()) {
                    solrQuery.addField(field);
                }
            }
        }
        // NOTE: If there are more requested documents than allowed boolean
        // clauses in one query, than we need to send several requests!
        Iterator<String> uriIterator = uris.iterator();
        int maxClauses  = config.getMaxBooleanClauses();
        int num = 0;
        StringBuilder queryBuilder = new StringBuilder();
        boolean myList = false;
        Collection<SolrDocument> resultDocs = null;
        // do while more uris
        while (uriIterator.hasNext()) {
            // do while more uris and free boolean clauses
            // num <= maxClauses because 1-items boolean clauses in the query!
            while (uriIterator.hasNext() && num <= maxClauses) {
                String uri = uriIterator.next();
                if (uri != null) {
                    if (num > 0) {
                        queryBuilder.append(" OR ");
                    }
                    queryBuilder.append(String.format("%s:\"%s\"", fieldMapper.getDocumentIdField(),
                        SolrUtil.escapeSolrSpecialChars(uri)));
                    num++;
                }
            }
            log.info("Get SolrDocuments for Query: " + queryBuilder.toString());
            // no more items or all boolean clauses used -> send a request
            solrQuery.setQuery(queryBuilder.toString());
            queryBuilder = new StringBuilder(); // and a new StringBuilder
            // set the number of results to the number of parsed IDs.
            solrQuery.setRows(num);
            num = 0; // reset to 0
            QueryResponse queryResponse;
            try {
                queryResponse = AccessController.doPrivileged(new PrivilegedExceptionAction<QueryResponse>() {
                    public QueryResponse run() throws IOException, SolrServerException {
                        return server.query(solrQuery, METHOD.POST);
                    }
                });
            } catch (PrivilegedActionException pae) {
                Exception e = pae.getException();
                if(e instanceof SolrServerException){
                    throw (SolrServerException)e;
                } else if(e instanceof IOException){
                    throw (IOException)e;
                } else {
                    throw RuntimeException.class.cast(e);
                }
            }
            if (resultDocs == null) {
                resultDocs = queryResponse.getResults();
            } else {
                if (!myList) {
                    // most of the time there will be only one request, so only
                    // create my own list when the second response is processed
                    resultDocs = new ArrayList<SolrDocument>(resultDocs);
                    myList = true;
                }
                resultDocs.addAll(queryResponse.getResults());
            }
        } // end while more uris
        return resultDocs;
    }

    protected final SolrDocument getSolrDocument(String uri, 
                                                 Collection<String> fields) 
                                                 throws SolrServerException, IOException {
        final SolrQuery solrQuery = new SolrQuery();
        if (fields == null || fields.isEmpty()) {
            solrQuery.addField("*"); // select all fields
        } else {
            for (String field : fields) {
                if (field != null && !field.isEmpty()) {
                    solrQuery.addField(field);
                }
            }
        }
        solrQuery.setRows(1); // we query for the id, there is only one result
        String queryString = String.format("%s:\"%s\"", fieldMapper.getDocumentIdField(),
            SolrUtil.escapeSolrSpecialChars(uri));
        solrQuery.setQuery(queryString);
        QueryResponse queryResponse;
        try {
            queryResponse = AccessController.doPrivileged(new PrivilegedExceptionAction<QueryResponse>() {
                public QueryResponse run() throws IOException, SolrServerException {
                    return server.query(solrQuery, METHOD.POST);
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof SolrServerException){
                throw (SolrServerException)e;
            } else if(e instanceof IOException){
                throw (IOException)e;
            } else {
                throw RuntimeException.class.cast(e);
            }
        }
        if (queryResponse.getResults().isEmpty()) {
            return null;
        } else {
            return queryResponse.getResults().get(0);
        }
    }
    
    /*
     * Deprecated Constants -- moved to SolrYardConfig
     */
    /**
     * The key used to configure the URL for the SolrServer
     * @deprecated use {@link SolrYardConfig#SOLR_SERVER_LOCATION} instead
     */
    @Deprecated
    public static final String SOLR_SERVER_LOCATION = SolrYardConfig.SOLR_SERVER_LOCATION;
    /**
     * The key used to configure if data of multiple Yards are stored within the same index (
     * <code>default=false</code>)
     * @deprecated use {@link SolrYardConfig#MULTI_YARD_INDEX_LAYOUT} instead
     */
    public static final String MULTI_YARD_INDEX_LAYOUT = SolrYardConfig.MULTI_YARD_INDEX_LAYOUT;
    /**
     * The maximum boolean clauses as configured in the solrconfig.xml of the SolrServer. The default value
     * for this config in Solr 1.4 is 1024.
     * <p>
     * This value is important for generating queries that search for multiple documents, because it
     * determines the maximum number of OR combination for the searched document ids.
     * @deprecated use {@link SolrYardConfig#MAX_BOOLEAN_CLAUSES} instead
     */
    public static final String MAX_BOOLEAN_CLAUSES = SolrYardConfig.MAX_BOOLEAN_CLAUSES;
    /**
     * This property allows to define a field that is used to parse the boost for the parsed representation.
     * Typically this will be the pageRank of that entity within the referenced site (e.g.
     * {@link Math#log1p(double)} of the number of incoming links
     * @deprecated use {@link SolrYardConfig#DOCUMENT_BOOST_FIELD} instead
     */
    public static final String DOCUMENT_BOOST_FIELD = SolrYardConfig.DOCUMENT_BOOST_FIELD;
    /**
     * Key used to configure {@link Entry Entry&lt;String,Float&gt;} for fields with the boost. If no Map is
     * configured or a field is not present in the Map, than 1.0f is used as Boost. If a Document boost is
     * present than the boost of a Field is documentBoost*fieldBoost.
     * @deprecated use {@link SolrYardConfig#FIELD_BOOST_MAPPINGS} instead
     */
    public static final String FIELD_BOOST_MAPPINGS = SolrYardConfig.FIELD_BOOST_MAPPINGS;
    /**
     * Key used to to enable/disable the default configuration. If this is enabled,
     * that the index will get initialised with the configuration as specified by
     * the configuration name.
     * @deprecated use {@link SolrYardConfig#ALLOW_INITIALISATION_STATE} instead
     */
    public static final String SOLR_INDEX_DEFAULT_CONFIG = SolrYardConfig.ALLOW_INITIALISATION_STATE;
    /**
     * By default the use of an default configuration is disabled!
     * @deprecated use {@link SolrYardConfig#DEFAULT_ALLOW_INITIALISATION_STATE} instead
     */
    public static final boolean DEFAULT_SOLR_INDEX_DEFAULT_CONFIG_STATE = SolrYardConfig.DEFAULT_ALLOW_INITIALISATION_STATE;
    /**
     * The name of the configuration use as default. 
     * @deprecated use {@link SolrYardConfig#DEFAULT_SOLR_INDEX_CONFIGURATION_NAME} instead
     */
    public static final String DEFAULT_SOLR_INDEX_CONFIGURATION_NAME = SolrYardConfig.DEFAULT_SOLR_INDEX_CONFIGURATION_NAME;
    /**
     * Allows to configure the name of the index used for the configuration of the Solr Core.
     * @deprecated use {@link SolrYardConfig#SOLR_INDEX_CONFIGURATION_NAME} instead
     */
    public static final String SOLR_INDEX_CONFIGURATION_NAME = SolrYardConfig.SOLR_INDEX_CONFIGURATION_NAME;
    /**
     * The default value for the maxBooleanClauses of SolrQueries. Set to {@value #defaultMaxBooleanClauses}
     * the default of Slor 1.4
     * @deprecated use {@link SolrYardConfig#DEFAULT_MAX_BOOLEAN_CLAUSES} instead
     */
    protected static final int defaultMaxBooleanClauses = SolrYardConfig.DEFAULT_MAX_BOOLEAN_CLAUSES;
    /**
     * Key used to enable/disable committing of update(..) and store(..) operations. Enabling this ensures
     * that indexed documents are immediately available for searches, but it will also decrease the
     * performance for updates.
     * @deprecated use {@link SolrYardConfig#IMMEDIATE_COMMIT} instead
     */
    public static final String IMMEDIATE_COMMIT = SolrYardConfig.IMMEDIATE_COMMIT;
    /**
     * By default {@link #IMMEDIATE_COMMIT} is enabled
     * @deprecated use {@link SolrYardConfig#DEFAULT_IMMEDIATE_COMMIT_STATE} instead
     */
    public static final boolean DEFAULT_IMMEDIATE_COMMIT_STATE = SolrYardConfig.DEFAULT_IMMEDIATE_COMMIT_STATE;
    /**
     * If {@link #IMMEDIATE_COMMIT} is deactivated, than this time is parsed to update(..) and store(..)
     * operations as the maximum time (in ms) until a commit.
     * @deprecated use {@link SolrYardConfig#COMMIT_WITHIN_DURATION} instead
     */
    public static final String COMMIT_WITHIN_DURATION = SolrYardConfig.COMMIT_WITHIN_DURATION;
    /**
     * The default value for the {@link #COMMIT_WITHIN_DURATION} parameter is 10 sec.
     * @deprecated use {@link SolrYardConfig#DEFAULT_COMMIT_WITHIN_DURATION} instead
     */
    public static final int DEFAULT_COMMIT_WITHIN_DURATION = SolrYardConfig.DEFAULT_COMMIT_WITHIN_DURATION;
}
