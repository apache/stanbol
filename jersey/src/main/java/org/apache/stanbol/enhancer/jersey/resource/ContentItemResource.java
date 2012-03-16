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
package org.apache.stanbol.enhancer.jersey.resource;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReference;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReferences;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getString;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PERSON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.SKOS_CONCEPT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.GEO_LAT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.GEO_LONG;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

public class ContentItemResource extends BaseStanbolResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // TODO make this configurable trough a property
    public static final UriRef SUMMARY = new UriRef("http://www.w3.org/2000/01/rdf-schema#comment");

    // TODO make this configurable trough a property
    public static final UriRef THUMBNAIL = new UriRef("http://dbpedia.org/ontology/thumbnail");
    public static final UriRef DEPICTION = new UriRef("http://xmlns.com/foaf/0.1/depiction");

    public final Map<UriRef,String> defaultThumbnails = new HashMap<UriRef,String>();

    protected ContentItem contentItem;

    protected String localId;

    protected String textContent;

    protected URI imageSrc;

    protected URI downloadHref;

    protected URI metadataHref;

    protected final TcManager tcManager;

    protected final Serializer serializer;

    protected String serializationFormat = SupportedFormat.RDF_XML;


    /**
     * Map holding the extraction mapped by {@link Properties#DC_TYPE} and the
     * {@link Properties#ENHANCER_SELECTED_TEXT}.
     * This map is initialised by {@link #initOccurrences()}.
     */
    protected Map<UriRef,Map<String,EntityExtractionSummary>> extractionsByTypeMap = 
        new HashMap<UriRef,Map<String,EntityExtractionSummary>>();

    private MGraph executionMetadata;

    private ChainExecution chainExecution;

    private ArrayList<org.apache.stanbol.enhancer.jersey.resource.ContentItemResource.Execution> engineExecutions;
    
    public ContentItemResource(String localId,
                               ContentItem ci,
                               UriInfo uriInfo,
                               String storePath,
                               TcManager tcManager,
                               Serializer serializer,
                               ServletContext servletContext) throws IOException {
        this.contentItem = ci;
        this.localId = localId;
        this.uriInfo = uriInfo;
        this.tcManager = tcManager;
        this.serializer = serializer;
        this.servletContext = servletContext;

        if (localId != null) {
            URI rawURI = uriInfo.getBaseUriBuilder().path(storePath).path("raw").path(localId).build();
            Entry<UriRef,Blob> plainTextContentPart = ContentItemHelper.getBlob(contentItem, Collections.singleton("text/plain"));
            if (plainTextContentPart != null) {
                this.textContent = ContentItemHelper.getText(plainTextContentPart.getValue());
            } 
            if (ci.getMimeType().startsWith("image/")) {
                this.imageSrc = rawURI;
            }
            this.downloadHref = rawURI;
            this.metadataHref = uriInfo.getBaseUriBuilder().path(storePath).path("metadata").path(localId).build();
        }
        defaultThumbnails.put(DBPEDIA_PERSON, getStaticRootUrl() + "/home/images/user_48.png");
        defaultThumbnails.put(DBPEDIA_ORGANISATION, getStaticRootUrl() + "/home/images/organization_48.png");
        defaultThumbnails.put(DBPEDIA_PLACE, getStaticRootUrl() + "/home/images/compass_48.png");
        defaultThumbnails.put(SKOS_CONCEPT, getStaticRootUrl() + "/home/images/black_gear_48.png");
        defaultThumbnails.put(null, getStaticRootUrl() + "/home/images/unknown_48.png");
        long start = System.currentTimeMillis();
        initOccurrences();
        //init ExecutionMetadata
        try {
            executionMetadata = ci.getPart(ExecutionMetadata.CHAIN_EXECUTION, MGraph.class);
        } catch(NoSuchPartException e){
            executionMetadata = null;
        }
        if(executionMetadata != null){
            NonLiteral ce = ExecutionMetadataHelper.getChainExecution(executionMetadata, ci.getUri());
            if(ce != null){
                chainExecution = new ChainExecution(executionMetadata, ce);
                engineExecutions = new ArrayList<Execution>();
                for(NonLiteral ex : ExecutionMetadataHelper.getExecutions(executionMetadata, ce)){
                    engineExecutions.add(new Execution(chainExecution,executionMetadata, ex));
                }
                Collections.sort(engineExecutions);
            } else {
                chainExecution = null;
                engineExecutions = null;
            }
        }
        log.info(" ... {}ms fro parsing Enhancement Reuslts",System.currentTimeMillis()-start);
    }

    public String getRdfMetadata(String mediatype) throws UnsupportedEncodingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, contentItem.getMetadata(), mediatype);
        return out.toString("utf-8");
    }

    public String getRdfMetadata() throws UnsupportedEncodingException {
        return getRdfMetadata(serializationFormat);
    }

    public ContentItem getContentItem() {
        return contentItem;
    }

    public String getLocalId() {
        return localId;
    }

    public String getTextContent() {
        return textContent;
    }

    public URI getImageSrc() {
        return imageSrc;
    }

    public URI getDownloadHref() {
        return downloadHref;
    }

    public URI getMetadataHref() {
        return metadataHref;
    }
    /**
     * Checks if there are Occurrences
     */
    public boolean hasOccurrences(){
        for(Map<String,EntityExtractionSummary> occ : extractionsByTypeMap.values()){
            if(!occ.isEmpty()){
                return true;
            }
        }
        return false;
    }
    /**
     * Used to print occurrences with other types than the natively supported
     */
    public Collection<UriRef> getOtherOccurrencyTypes(){
        Set<UriRef>  types = new HashSet<UriRef>(extractionsByTypeMap.keySet());
        types.remove(DBPEDIA_PERSON);
        types.remove(DBPEDIA_ORGANISATION);
        types.remove(DBPEDIA_PLACE);
        types.remove(SKOS_CONCEPT);
        types.remove(null); //other
        return types;
    }
    public String extractLabel(UriRef uri){
        String fullUri = uri.getUnicodeString();
        int index = Math.max(fullUri.lastIndexOf('#'),fullUri.lastIndexOf('/'));
        index = Math.max(index, fullUri.lastIndexOf(':'));
        //do not convert if the parsed uri does not contain a local name
        if(index > 0 && index+1 < fullUri.length()){
            return StringUtils.capitalize(fullUri.substring(index+1).replaceAll("[\\-_]", " "));
        } else {
            return uri.getUnicodeString();
        }
    }
    public Collection<EntityExtractionSummary> getOccurrences(UriRef type){
        Map<String,EntityExtractionSummary> typeMap = extractionsByTypeMap.get(type);
        Collection<EntityExtractionSummary> typeOccurrences;
        if(typeMap != null){
            typeOccurrences = typeMap.values();
        } else {
            typeOccurrences  = Collections.emptyList();
        }
        return typeOccurrences;
    }
    
    public Collection<EntityExtractionSummary> getPersonOccurrences() throws ParseException {
        return getOccurrences(DBPEDIA_PERSON);
    }
    public Collection<EntityExtractionSummary> getOtherOccurrences() throws ParseException {
        return getOccurrences(null);
    }

    public Collection<EntityExtractionSummary> getOrganizationOccurrences() throws ParseException {
        return getOccurrences(DBPEDIA_ORGANISATION);
    }

    public Collection<EntityExtractionSummary> getPlaceOccurrences() throws ParseException {
        return getOccurrences(DBPEDIA_PLACE);
    }
    public Collection<EntityExtractionSummary> getConceptOccurrences() throws ParseException {
        return getOccurrences(SKOS_CONCEPT);
    }
    enum EAProps {
        label,
        entity,
        confidence
    }

    private void initOccurrences() {
        MGraph graph = contentItem.getMetadata();
        LiteralFactory lf = LiteralFactory.getInstance();
        Map<UriRef,Collection<NonLiteral>> suggestionMap = new HashMap<UriRef,Collection<NonLiteral>>();
        // 1) get Entity Annotations
        Map<NonLiteral,Map<EAProps,Object>> entitySuggestionMap = new HashMap<NonLiteral,Map<EAProps,Object>>();
        Iterator<Triple> entityAnnotations = graph.filter(null, RDF.type, ENHANCER_ENTITYANNOTATION);
        while(entityAnnotations.hasNext()){
            NonLiteral entityAnnotation = entityAnnotations.next().getSubject();
            //to avoid multiple lookups (e.g. if one entityAnnotation links to+
            //several TextAnnotations) we cache the data in an intermediate Map
            Map<EAProps,Object> eaData = new EnumMap<EAProps,Object>(EAProps.class);
            eaData.put(EAProps.entity, getReference(graph, entityAnnotation, ENHANCER_ENTITY_REFERENCE));
            eaData.put(EAProps.label, getString(graph, entityAnnotation, ENHANCER_ENTITY_LABEL));
            eaData.put(EAProps.confidence, EnhancementEngineHelper.get(
                graph, entityAnnotation, ENHANCER_CONFIDENCE, Double.class, lf));
            entitySuggestionMap.put(entityAnnotation, eaData);
            Iterator<UriRef> textAnnotations = getReferences(graph, entityAnnotation, DC_RELATION);
            while(textAnnotations.hasNext()){
                UriRef textAnnotation = textAnnotations.next();
                Collection<NonLiteral> suggestions = suggestionMap.get(textAnnotation);
                if(suggestions == null){
                    suggestions = new ArrayList<NonLiteral>();
                    suggestionMap.put(textAnnotation, suggestions);
                }
                suggestions.add(entityAnnotation);
            }
        }
        // 2) get the TextAnnotations
        Iterator<Triple> textAnnotations = graph.filter(null, RDF.type, ENHANCER_TEXTANNOTATION);
        while(textAnnotations.hasNext()){
            NonLiteral textAnnotation = textAnnotations.next().getSubject();
            if (graph.filter(textAnnotation, DC_RELATION, null).hasNext()) {
                // this is not the most specific occurrence of this name: skip
                continue;
            }
            String text = getString(graph, textAnnotation, Properties.ENHANCER_SELECTED_TEXT);
            if(text == null){
                //ignore text annotations without text
                continue;
            }
            Iterator<UriRef> types = getReferences(graph, textAnnotation, DC_TYPE);
            if(!types.hasNext()){ //create an iterator over null in case no types are present
                types = Collections.singleton((UriRef)null).iterator();
            }
            while(types.hasNext()){
                UriRef type = types.next();
                Map<String,EntityExtractionSummary> occurrenceMap = extractionsByTypeMap.get(type);
                if(occurrenceMap == null){
                    occurrenceMap = new TreeMap<String,EntityExtractionSummary>(String.CASE_INSENSITIVE_ORDER);
                    extractionsByTypeMap.put(type, occurrenceMap);
                }
                EntityExtractionSummary entity = occurrenceMap.get(text);
                if(entity == null){
                    entity = new EntityExtractionSummary(text, type, defaultThumbnails);
                    occurrenceMap.put(text, entity);
                }
                Collection<NonLiteral> suggestions = suggestionMap.get(textAnnotation);
                if(suggestions != null){
                    for(NonLiteral entityAnnotation : suggestions){
                        Map<EAProps,Object> eaData = entitySuggestionMap.get(entityAnnotation);
                        entity.addSuggestion(
                            (UriRef)eaData.get(EAProps.entity),
                            (String)eaData.get(EAProps.label), 
                            (Double)eaData.get(EAProps.confidence), 
                            graph);
                    }
                }
            }
        }
    }

    public ChainExecution getChainExecution(){
        return chainExecution;
    }
    
    public Collection<Execution> getEngineExecutions(){
        return engineExecutions;
    }
    
    
    public static class EntityExtractionSummary implements Comparable<EntityExtractionSummary> {

        protected final String name;

        protected final UriRef type;

        protected List<EntitySuggestion> suggestions = new ArrayList<EntitySuggestion>();

        protected List<String> mentions = new ArrayList<String>();

        public final Map<UriRef,String> defaultThumbnails;

        public EntityExtractionSummary(String name, UriRef type, Map<UriRef,String> defaultThumbnails) {
            this.name = name;
            this.type = type;
            mentions.add(name);
            this.defaultThumbnails = defaultThumbnails;
        }

        public void addSuggestion(UriRef uri, String label, Double confidence, TripleCollection properties) {
            EntitySuggestion suggestion = new EntitySuggestion(uri, type, label, confidence, properties,
                    defaultThumbnails);
            if (!suggestions.contains(suggestion)) {
                suggestions.add(suggestion);
                Collections.sort(suggestions);
            }
        }

        public String getName() {
            EntitySuggestion bestGuess = getBestGuess();
            if (bestGuess != null) {
                return bestGuess.getLabel();
            }
            return name;
        }

        public String getUri() {
            EntitySuggestion bestGuess = getBestGuess();
            if (bestGuess != null) {
                return bestGuess.getUri();
            }
            return null;
        }

        public String getSummary() {
            if (suggestions.isEmpty()) {
                return "";
            }
            return suggestions.get(0).getSummary();
        }

        public String getThumbnailSrc() {
            if (suggestions.isEmpty()) {
                return getMissingThumbnailSrc();
            }
            return suggestions.get(0).getThumbnailSrc();
        }

        public String getMissingThumbnailSrc() {
            String source = defaultThumbnails.get(type);
            if(source == null){
                source = defaultThumbnails.get(null);//default
            }
            return source;
        }

        public EntitySuggestion getBestGuess() {
            if (suggestions.isEmpty()) {
                return null;
            }
            return suggestions.get(0);
        }

        public List<EntitySuggestion> getSuggestions() {
            return suggestions;
        }

        public List<String> getMentions() {
            return mentions;
        }

        @Override
        public int compareTo(EntityExtractionSummary o) {
            return getName().compareTo(o.getName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            EntityExtractionSummary that = (EntityExtractionSummary) o;

            return !(name != null ? !name.equals(that.name) : that.name != null);
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }

    public static class EntitySuggestion implements Comparable<EntitySuggestion> {

        protected final UriRef uri;

        protected final UriRef type;

        protected final String label;

        protected final Double confidence;

        protected TripleCollection entityProperties;

        protected final Map<UriRef,String> defaultThumbnails;

        public EntitySuggestion(UriRef uri,
                                UriRef type,
                                String label,
                                Double confidence,
                                TripleCollection entityProperties,
                                Map<UriRef,String> defaultThumbnails) {
            this.uri = uri;
            this.label = label;
            this.type = type;
            this.confidence = confidence != null ? confidence : 0.0;
            this.entityProperties = entityProperties;
            this.defaultThumbnails = defaultThumbnails;
        }

        @Override
        public int compareTo(EntitySuggestion o) {
            // order suggestions by decreasing confidence
            return -confidence.compareTo(o.confidence);
        }

        public String getUri() {
            return uri.getUnicodeString();
        }

        public Double getConfidence() {
            return confidence;
        }

        public String getLabel() {
            return label;
        }

        public String getThumbnailSrc() {
            Iterator<Triple> thumbnails = entityProperties.filter(uri, THUMBNAIL, null);
            while (thumbnails.hasNext()) {
                Resource object = thumbnails.next().getObject();
                if (object instanceof UriRef) {
                    return ((UriRef) object).getUnicodeString();
                }
            }
            //if no dbpedia ontology thumbnail was found. try the same with foaf:depiction
            thumbnails = entityProperties.filter(uri, DEPICTION, null);
            while (thumbnails.hasNext()) {
                Resource object = thumbnails.next().getObject();
                if (object instanceof UriRef) {
                    return ((UriRef) object).getUnicodeString();
                }
            }
            return getMissingThumbnailSrc();
        }

        public String getMissingThumbnailSrc() {
            String source = defaultThumbnails.get(type);
            if(source == null){
                source = defaultThumbnails.get(null);
            }
            return source;
        }

        public String getSummary() {
            Iterator<Triple> abstracts = entityProperties.filter(uri, SUMMARY, null);
            while (abstracts.hasNext()) {
                Resource object = abstracts.next().getObject();
                if (object instanceof PlainLiteral) {
                    PlainLiteral abstract_ = (PlainLiteral) object;
                    if (new Language("en").equals(abstract_.getLanguage())) {
                        return abstract_.getLexicalForm();
                    }
                }
            }
            return "";
        }

        // consider entities with same URI as equal even if we have alternate
        // label values
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((uri == null) ? 0 : uri.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            EntitySuggestion other = (EntitySuggestion) obj;
            if (uri == null) {
                if (other.uri != null) return false;
            } else if (!uri.equals(other.uri)) return false;
            return true;
        }

    }

    public void setRdfSerializationFormat(String format) {
        serializationFormat = format;
    }

    /**
     * @return an RDF/JSON descriptions of places for the word map widget
     */
    public String getPlacesAsJSON() throws ParseException, UnsupportedEncodingException {
        MGraph g = new IndexedMGraph();
        LiteralFactory lf = LiteralFactory.getInstance();
        MGraph metadata = contentItem.getMetadata();
        for (EntityExtractionSummary p : getPlaceOccurrences()) {
            EntitySuggestion bestGuess = p.getBestGuess();
            if (bestGuess == null) {
                continue;
            }
            UriRef uri = new UriRef(bestGuess.getUri());
            Iterator<Triple> latitudes = metadata.filter(uri, GEO_LAT, null);
            if (latitudes.hasNext()) {
                g.add(latitudes.next());
            }
            Iterator<Triple> longitutes = metadata.filter(uri, GEO_LONG, null);
            if (longitutes.hasNext()) {
                g.add(longitutes.next());
                g.add(new TripleImpl(uri, Properties.RDFS_LABEL, lf.createTypedLiteral(bestGuess.getLabel())));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, g, SupportedFormat.RDF_JSON);
        
        String rdfString = out.toString("utf-8");
        return rdfString;
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
        addCORSOrigin(servletContext,rb, headers);
        return rb.build();
    }
    
    public class ExecutionNode {
        
        private final NonLiteral node;
        private final TripleCollection ep;
        private final boolean optional;
        private final String engineName;
        
        public ExecutionNode(TripleCollection executionPlan, NonLiteral node) {
            this.node = node;
            this.ep = executionPlan;
            this.optional = ExecutionPlanHelper.isOptional(ep, node);
            this.engineName = ExecutionPlanHelper.getEngine(ep, node);
        }
        
        public boolean isOptional() {
            return optional;
        }
        public String getEngineName() {
            return engineName;
        }
        
        @Override
        public int hashCode() {
            return node.hashCode();
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof ExecutionNode && ((ExecutionNode)o).node.equals(node);
        }
    }
    public class Execution implements Comparable<Execution>{
        
        protected DateFormat format = new SimpleDateFormat("HH-mm-ss.SSS");
        protected final NonLiteral node;
        private final ExecutionNode executionNode;
        private final UriRef status;
        protected final TripleCollection graph;
        private final Date started;
        private final Date completed;
        private final Long duration;
        private ChainExecution chain;
        public Execution(ChainExecution parent, TripleCollection graph, NonLiteral node) {
            this.chain = parent;
            this.graph = graph;
            this.node = node;
            NonLiteral executionNode = ExecutionMetadataHelper.getExecutionNode(graph, node);
            if(executionNode != null){
                this.executionNode = new ExecutionNode(graph, executionNode);
            } else {
                this.executionNode = null;
            }
            this.status = getReference(graph, node, ExecutionMetadata.STATUS);
            this.started = ExecutionMetadataHelper.getStarted(graph, node);
            this.completed = ExecutionMetadataHelper.getCompleted(graph, node);
            if(started != null && completed != null){
                this.duration = completed.getTime() - started.getTime();
            } else {
                this.duration = null;
            }
        }

        /**
         * @return the executionNode
         */
        public ExecutionNode getExecutionNode() {
            return executionNode;
        }
        public String getStatusText(){
            if(ExecutionMetadata.STATUS_COMPLETED.equals(status)){
                return "completed";
            } else if(ExecutionMetadata.STATUS_FAILED.equals(status)){
                return "failed";
            } else if(ExecutionMetadata.STATUS_IN_PROGRESS.equals(status)){
                return "in-progress";
            } else if(ExecutionMetadata.STATUS_SCHEDULED.equals(status)){
                return "scheduled";
            } else if(ExecutionMetadata.STATUS_SKIPPED.equals(status)){
                return "skipped";
            } else {
                return "unknown";
            }
        }
        public Date getStart(){
            return started;
        }
        public Date getCompleted(){
            return completed;
        }
        public boolean isFailed(){
            return ExecutionMetadata.STATUS_FAILED.equals(status);
        }
        public boolean isCompleted(){
            return ExecutionMetadata.STATUS_COMPLETED.equals(status);
        }
        public String getOffsetText(){
            if(chain == null || chain.getStart() == null || started == null){
                return null;
            } else {
                return String.format("%6dms",started.getTime() - chain.getStart().getTime());
            }
        }
        public String getDurationText(){
            if(duration == null){
                return "[duration not available]";
            } else if(duration < 1025){
                return duration+"ms";
            } else {
                return String.format("%.2fsec",(duration.floatValue()/1000));
            }
        }
        public String getStartTime(){
            if(started != null){
                return format.format(started);
            } else {
                return "unknown";
            }
        }
        public String getCompletionTime(){
            if(completed != null){
                return format.format(completed);
            } else {
                return "unknown";
            }
        }
        @Override
        public int hashCode() {
            return node.hashCode();
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof ExecutionNode && ((ExecutionNode)o).node.equals(node);
        }
        @Override
        public int compareTo(Execution e2) {
            if(started != null && e2.started != null){
                int result = started.compareTo(e2.started);
                if(result == 0){
                    if(completed != null && e2.completed != null){
                        result = started.compareTo(e2.completed);
                        if(result == 0){
                            return node.toString().compareTo(e2.toString());
                        } else {
                            return result;
                        }
                    } else if (completed == null && e2.completed == null){
                        return node.toString().compareTo(e2.toString());
                    } else {
                        return completed == null ? -1 : 1;
                    }
                } else {
                    return result;
                }
            } else if (started == null && e2.started == null){
                return node.toString().compareTo(e2.toString());
            } else {
                return started == null ? -1 : 1;
            }
        }
    }
    public class ChainExecution extends Execution {
        
        private final String chainName;
        
        public ChainExecution(TripleCollection graph, NonLiteral node) {
            super(null,graph,node);
            NonLiteral ep = ExecutionMetadataHelper.getExecutionPlanNode(graph, node);
            if(ep != null){
                chainName = EnhancementEngineHelper.getString(graph, ep, ExecutionPlan.CHAIN);
            } else {
                chainName = null;
            }
        }
        
        public String getChainName(){
            return chainName;
        }
    }
    
}
