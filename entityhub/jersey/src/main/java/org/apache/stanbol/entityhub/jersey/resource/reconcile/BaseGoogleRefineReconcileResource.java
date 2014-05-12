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
package org.apache.stanbol.entityhub.jersey.resource.reconcile;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum.resultScore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.entityhub.jersey.grefine.ReconcileProperty;
import org.apache.stanbol.entityhub.jersey.grefine.ReconcileQuery;
import org.apache.stanbol.entityhub.jersey.grefine.ReconcileValue;
import org.apache.stanbol.entityhub.jersey.grefine.Utils;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.defaults.SpecialFieldEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.SimilarityConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint.MODE;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the <a href="http://code.google.com/p/google-refine/wiki/ReconciliationServiceApi">
 * Google Refine Reconciliation API</a>
 * This base class is used to support this API for the Entityhub, ReferencedSites
 * and the ReferencedSiteManager.
 * 
 * @author Rupert Westenthaler
 *
 */
//TODO rather than having the siteId as path param here instances of this class 
//should be returned as subresource
@Component(componentAbstract = true)
public abstract class BaseGoogleRefineReconcileResource extends BaseStanbolResource {

    private final Logger log = LoggerFactory.getLogger(BaseGoogleRefineReconcileResource.class);

    private static final String NAME_FIELD = "http://www.w3.org/2000/01/rdf-schema#label";
    private static final String TYPE_FIELD = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final Collection<String> SELECTED_FIELDS = Collections.unmodifiableList(
        Arrays.asList(NAME_FIELD,TYPE_FIELD));

    private static final Comparator<JSONObject> resultScoreComparator = new Comparator<JSONObject>() {

        @Override
        public int compare(JSONObject o1, JSONObject o2) {
            try {
                return Double.compare(o2.getDouble("score"),o1.getDouble("score"));
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        }
        
    };
    @org.apache.felix.scr.annotations.Reference
    private NamespacePrefixService nsPrefixService;

    protected BaseGoogleRefineReconcileResource(){
    }
    
    @OPTIONS
    public final Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers);
        return res.build();
    }
    
    @POST
    public final Response queryPOST(@PathParam(value = "site") String siteId,
                          @FormParam(value="query") String query, 
                          @FormParam(value="queries")String queries,
                          @FormParam(value="callback")String callback,
                          @Context HttpHeaders header) throws WebApplicationException {
        return query(siteId, query,queries,callback,header);
    }
    @GET
    public final Response query(@PathParam(value = "site") String siteId,
                          @QueryParam(value="query") String query, 
                          @QueryParam(value="queries")String queries,
                          @QueryParam(value="callback")String callback,
                          @Context HttpHeaders header) throws WebApplicationException {
        if(callback != null){
            log.info("callback: {}",callback);
            try {
                return sendMetadata(siteId,callback,header);
            } catch (JSONException e) {
                throw new WebApplicationException(e);
            }
        }
        JSONObject jResult;
        if(query != null){
            log.debug("query: {}",query);
            try {
                jResult = reconcile(siteId, ReconcileQuery.parseQuery(query,nsPrefixService));
            } catch (JSONException e) {
                throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                        String.format("Error while writing Reconcilation results (%s: %s)",
                            JSONException.class.getSimpleName(),e.getMessage())).build());
            } catch (EntityhubException e) {
                throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                        String.format("Error while searching on %s (%s: %s)",
                            getSiteName(siteId),SiteException.class.getSimpleName(),e.getMessage())).build());
            }
        } else if(queries != null){
            log.debug("multi-query: {}",queries);
            try {
                jResult = reconcile(siteId, ReconcileQuery.parseQueries(queries,nsPrefixService));
            } catch (JSONException e) {
                throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                        String.format("Error while writing Reconcilation results (%s: %s)",
                            JSONException.class.getSimpleName(),e.getMessage())).build());
            } catch (EntityhubException e) {
                throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                        String.format("Error while searching on %s (%s: %s)",
                            getSiteName(siteId),SiteException.class.getSimpleName(),e.getMessage())).build());
            }
        } else {
            if(MediaTypeUtil.isAcceptableMediaType(header,MediaType.TEXT_HTML_TYPE)){
                ResponseBuilder rb = Response.ok(new Viewable("index", this, BaseGoogleRefineReconcileResource.class));
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
                //addCORSOrigin(servletContext, rb, header);
                return rb.build();
            }
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity(
                    "One of the 'query' or 'querues' or 'callback=jsonp' parameter MUST BE present!").build());
        }
        //return the results and enable Cors
        ResponseBuilder rb = Response.ok(jResult.toString()).type(MediaType.APPLICATION_JSON_TYPE);
        //CorsHelper.addCORSOrigin(servletContext, rb, header);
        return rb.build();

    }


    private JSONObject reconcile(@PathParam(value = "site") String siteId, Map<String,ReconcileQuery> parsedQueries) throws JSONException, EntityhubException {
        JSONObject container = new JSONObject();
        for(Entry<String,ReconcileQuery> query : parsedQueries.entrySet()){
            container.put(query.getKey(), reconcile(siteId, query.getValue()));
        }
        return container;
    }

    private JSONObject reconcile(String siteId, ReconcileQuery rQuery) throws JSONException, EntityhubException {
        FieldQuery query = createFieldQuery(siteId);
        query.addSelectedFields(SELECTED_FIELDS);
        addNameConstraint(rQuery, query);
        addTypeConstraint(rQuery, query);
        addPropertyConstraints(rQuery, query);
        query.setLimit(query.getLimit());
        QueryResultList<Representation> results = performQuery(siteId, query);
        List<JSONObject> jResultList = new ArrayList<JSONObject>(results.size());
        //we need to know the highest score to normalise between [0..1]
        double maxQueryScore = -1;
        if(!results.isEmpty()){
            for(Representation r : results){
                if(maxQueryScore < 0){
                    maxQueryScore = r.getFirst(resultScore.getUri(),Number.class).doubleValue();
                }
                JSONObject jResult = new JSONObject();
                jResult.put("id", r.getId());
                double similarity = 0.0;
                String name = null; //the name returned for the entity
                for(Iterator<Text> labels = r.getText(NAME_FIELD);labels.hasNext();){
                    Text label = labels.next();
                    if(label.getText().equalsIgnoreCase(rQuery.getQuery())){
                        name = label.getText();
                        similarity = 1.0;
                        break;
                    }
                    double curSimilarity = Utils.levenshtein(rQuery.getQuery(), label.getText());
                    if(similarity < curSimilarity){
                        name = label.getText();
                        similarity = curSimilarity;
                    }
                }
                //set the selected name
                jResult.put("name", name);
                Iterator<Reference> types = r.getReferences(TYPE_FIELD);
                if(types != null && types.hasNext()) {
                    jResult.put("type", new JSONArray(ModelUtils.asCollection(types)));
                }
                double normalisedScore = r.getFirst(resultScore.getUri(),Number.class).doubleValue();
                normalisedScore = normalisedScore*similarity/maxQueryScore;
                jResult.put("score", normalisedScore);
                jResult.put("match", similarity >= 0);
                jResultList.add(jResult);
            }
        } //else no results ... nothing todo
        //sort results based on score
        Collections.sort(jResultList, resultScoreComparator);
        JSONObject jResultContainer = new JSONObject();
        jResultContainer.put("result", new JSONArray(jResultList));
        return jResultContainer;
    }
    /**
     * @param query
     * @return
     * @throws SiteException
     */
    protected abstract QueryResultList<Representation> performQuery(String siteId, FieldQuery query) throws EntityhubException;
    
    /**
     * Getter for the name of the Site as used for logging
     * @return
     */
    protected abstract String getSiteName(String siteId);
    
    /**
     * Creates a new FieldQuery
     * @return
     */
    protected abstract FieldQuery createFieldQuery(String siteId);
    
    /**
     * @param rQuery
     * @param query
     */
    private void addPropertyConstraints(ReconcileQuery rQuery, FieldQuery query) {
        Collection<String> ids = new HashSet<String>();
        List<String> texts = new ArrayList<String>(); // keep order for texts
        Collection<Object> values = new HashSet<Object>();
        
        //hold all references for @references special property
        HashSet<String> references = new HashSet<String>();
        //holds all texts for @fullText special property
        List<String> fullText = null;
        //holds the context for the @similarity special property
        Collection<String> similarityContext = null;
        //the field used for the @similarity special property
        HashSet<String> similarityFields = new LinkedHashSet<String>();
        
        for (Entry<ReconcileProperty,Collection<ReconcileValue>> propertyEntry : rQuery.getProperties()) {
            ReconcileProperty property = propertyEntry.getKey();
            // collect the properties
            for (ReconcileValue value : propertyEntry.getValue()) {
                if (value.getId() != null) {
                    ids.add(value.getId());
                }
                if (value.getValue() instanceof String) {
                    texts.add((String) value.getValue());
                } else {
                    values.add(value.getValue());
                }
            }
            //handle supported special properties
            if(property.isSpecial()){
                if(property.getName().equalsIgnoreCase("references")){
                    //Note that multiple "references" properties might be present
                    //if Users do parse parameters - so we need to collect all values
                    if(property.getParameter() != null){
                        log.warn("parameters are not supported for @references -> ignore '{}'",property.getParameter());
                    }
                    if(ids.isEmpty()){
                        log.warn("No URI values present for parsed @references property! (values: "
                            +propertyEntry.getValue());
                    }
                    for(String id : ids){
                        references.add(id);
                    }
                } else if(property.getName().equalsIgnoreCase("fulltext")){
                    //Note that multiple "fullText" properties might be present
                    //if Users do parse parameters - so we need to collect all values
                    if(property.getParameter() != null){
                        log.warn("parameters are not supported for @fullText -> ignore '{}'",property.getParameter());
                    }
                    fullText = texts;
                } else if(property.getName().equalsIgnoreCase("similarity")){
                    String propUri = property.getParameter() != null ? 
                            nsPrefixService.getFullName(property.getParameter()) :
                                SpecialFieldEnum.fullText.getUri();
                    if(propUri != null){
                        similarityFields.add(propUri);
                    } else {
                        //TODO: maybe throw an Exception instead
                        log.warn("Unknown prefix '{}' used by Google Refine query parameter of property '{}'! "
                            + "Will use the full text field as fallback",
                            NamespaceMappingUtils.getPrefix(property.getParameter()),property);
                        similarityFields.add(SpecialFieldEnum.fullText.getUri());
                    }
                    similarityContext = texts;
                } else {
                    //TODO: implement LDPATH support
                    log.warn("ignore unsupported special property {}",property);
                }
            } else { //no special property
                // add the Constraint to the FieldQuery
                // TODO: how to deal with values of different types
                //  * currently References > Text > Datatype. First present value
                //    is used
                //  * non Reference | Text | Datatype values are ignored
                if (!ids.isEmpty()) {
                    // only references -> create reference constraint
                    query.setConstraint(property.getName(), new ReferenceConstraint(ids));
                    if (ids.size() != propertyEntry.getValue().size()) {
                        log.info("Only some of the parsed values of the field {} contain"
                                 + "references -> will ignore values with missing references");
                    }
                } else if (!texts.isEmpty()) {
                    // NOTE: This will use OR over all texts. To enforce AND one
                    // would need to parse a single string with all values e.g. by
                    // using StringUtils.join(texts," ")
                    query.setConstraint(property.getName(), new TextConstraint(texts));
                    if (ids.size() != propertyEntry.getValue().size()) {
                        log.info("Only some of the parsed values of the field {} are"
                                 + "of type String -> will ignore non-string values");
                    }
                } else if(!values.isEmpty()){
                    query.setConstraint(property.getName(), new ValueConstraint(values));
                } //else no values ... ignore property
            }
            //clean up
            ids.clear();
            values.clear();
        }
        //now add constraints for the collected special properties
        if(!references.isEmpty()){ 
            //add references constraint
            ReferenceConstraint refConstraint = new ReferenceConstraint(references, MODE.all);
            query.setConstraint(SpecialFieldEnum.references.getUri(), refConstraint);
        }
        if(fullText != null && !fullText.isEmpty()){
            TextConstraint textConstraint = new TextConstraint(fullText);
            query.setConstraint(SpecialFieldEnum.fullText.getUri(), textConstraint);
            //add full text constraint
        }
        if(similarityContext != null && !similarityContext.isEmpty()){
            //add similarity constraint
            Iterator<String> fieldIt = similarityFields.iterator();
            String field = fieldIt.next();
            SimilarityConstraint simConstraint;
            if(fieldIt.hasNext()){
                List<String> addFields = new ArrayList<String>(similarityFields.size()-1);
                while(fieldIt.hasNext()){
                    addFields.add(fieldIt.next());
                }
                simConstraint = new SimilarityConstraint(similarityContext,DataTypeEnum.Text, addFields);
            } else {
                simConstraint = new SimilarityConstraint(similarityContext,DataTypeEnum.Text);
            }
            query.setConstraint(field, simConstraint);
        }
    }
    
    
    /**
     * @param rQuery
     * @param query
     */
    private void addTypeConstraint(ReconcileQuery rQuery, FieldQuery query) {
        //maybe an other column was also mapped to the TYPE_FIELD property
        Collection<ReconcileValue> additionalTypes = rQuery.removeProperty(TYPE_FIELD);
        Set<String> queryTypes = rQuery.getTypes();
        Set<String> types = null;
        if(additionalTypes == null){
            if(queryTypes != null){
                types = queryTypes;
            }
        } else {
            types = new HashSet<String>();
            if(queryTypes != null){
                types.add(rQuery.getQuery());
            }
            for(ReconcileValue value : additionalTypes){
                if(value != null){
                    if(value.getId() != null){
                        types.add(value.getId());
                    } else if (value.getValue() instanceof String){
                        //TODO: check if the assumption that String values are
                        //good for types is valid
                        types.add((String)value.getValue());
                    }
                } //else null -> ignore
            }
        }
        if (!types.isEmpty()) {
            query.setConstraint(TYPE_FIELD, new ReferenceConstraint(types));
        }
    }
    /**
     * @param rQuery
     * @param query
     */
    private void addNameConstraint(ReconcileQuery rQuery, FieldQuery query) {
        //maybe an other column was also mapped to the NAME_FIELD property
        Collection<ReconcileValue> additionalValues = rQuery.removeProperty(NAME_FIELD);
        List<String> values;
        if(additionalValues == null){
            values = Collections.singletonList(rQuery.getQuery());
        } else {
            values = new ArrayList<String>(additionalValues.size()+1);
            values.add(rQuery.getQuery());
            for(ReconcileValue value : additionalValues){
                if(value != null && value.getValue() instanceof String){
                    values.add((String)value.getValue());
                }
            }
        }
        query.setConstraint(NAME_FIELD, new TextConstraint(values));
    }
    /**
     * Called on requests for the Metadata for the Reconciliation service
     * @param callback
     * @param header
     * @return
     * @throws JSONException
     */
    protected Response sendMetadata(String siteId, 
            String callback, HttpHeaders header) throws JSONException {
        //TODO: implement!!
        JSONObject jMetadata = new JSONObject();
        jMetadata.put("name", "Stanbol Entityhub: "+getSiteName(siteId));
        StringBuilder callbackString = new StringBuilder(callback);
        callbackString.append('(');
        callbackString.append(jMetadata.toString());
        callbackString.append(')');
        ResponseBuilder rb = Response.ok(callbackString.toString()).type(MediaType.APPLICATION_JSON_TYPE);
        //CorsHelper.addCORSOrigin(servletContext, rb, header);
        return rb.build();
    }
}
