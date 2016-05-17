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
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReference;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReferences;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getString;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PERSON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DC_LINGUISTIC_SYSTEM;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.SKOS_CONCEPT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_RELATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.GEO_LAT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.GEO_LONG;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_ENTITYANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.viewable.Viewable;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.resource.LayoutConfiguration;
import org.apache.stanbol.commons.web.base.resource.TemplateLayoutConfiguration;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.execution.ChainExecution;
import org.apache.stanbol.enhancer.servicesapi.helper.execution.Execution;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentItemResource extends TemplateLayoutConfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // TODO make this configurable trough a property
    public static final IRI SUMMARY = new IRI("http://www.w3.org/2000/01/rdf-schema#comment");

    // TODO make this configurable trough a property
    public static final IRI THUMBNAIL = new IRI("http://dbpedia.org/ontology/thumbnail");
    public static final IRI DEPICTION = new IRI("http://xmlns.com/foaf/0.1/depiction");

    public final Map<IRI,String> defaultThumbnails = new HashMap<IRI,String>();

    protected ContentItem contentItem;

    protected String localId;

    protected String textContent;

    protected URI imageSrc;

    protected URI downloadHref;

    protected URI metadataHref;

    protected final Serializer serializer;

    protected String serializationFormat = SupportedFormat.RDF_XML;
    /**
     * Used to format dates on the UI
     */
    protected DateFormat format = new SimpleDateFormat("HH-mm-ss.SSS");

    /**
     * Map holding the extraction mapped by {@link Properties#DC_TYPE} and the
     * {@link Properties#ENHANCER_SELECTED_TEXT}.
     * This map is initialised by {@link #initOccurrences()}.
     */
    protected Map<IRI,Map<EntityExtractionSummary,EntityExtractionSummary>> extractionsByTypeMap = 
        new HashMap<IRI,Map<EntityExtractionSummary,EntityExtractionSummary>>();

    private Graph executionMetadata;

    private ChainExecution chainExecution;

    private List<org.apache.stanbol.enhancer.servicesapi.helper.execution.Execution> engineExecutions;

    private EnhancementException enhancementException;
    private LayoutConfiguration layoutConfiguration;
    private UriInfo uriInfo;
    
    public ContentItemResource(String localId,
                               ContentItem ci,
                               UriInfo uriInfo,
                               String storePath,
                               Serializer serializer,
                               LayoutConfiguration layoutConfiguration,
                               EnhancementException enhancementException) throws IOException {
        this.contentItem = ci;
        this.localId = localId;
        this.uriInfo = uriInfo;
        this.serializer = serializer;
        this.layoutConfiguration = layoutConfiguration;
        //this.servletContext = servletContext;
        this.enhancementException = enhancementException;
        if (localId != null) {
            URI rawURI = uriInfo.getBaseUriBuilder().path(storePath).path("raw").path(localId).build();
            Entry<IRI,Blob> plainTextContentPart = ContentItemHelper.getBlob(contentItem, Collections.singleton("text/plain"));
            if (plainTextContentPart != null) {
                this.textContent = ContentItemHelper.getText(plainTextContentPart.getValue());
            } 
            if (ci.getBlob().getMimeType().startsWith("image/")) {
                this.imageSrc = rawURI;
            }
            this.downloadHref = rawURI;
            this.metadataHref = uriInfo.getBaseUriBuilder().path(storePath).path("metadata").path(localId).build();
        }
        defaultThumbnails.put(DBPEDIA_PERSON, getStaticRootUrl() + "/home/images/user_48.png");
        defaultThumbnails.put(DBPEDIA_ORGANISATION, getStaticRootUrl() + "/home/images/organization_48.png");
        defaultThumbnails.put(DBPEDIA_PLACE, getStaticRootUrl() + "/home/images/compass_48.png");
        defaultThumbnails.put(SKOS_CONCEPT, getStaticRootUrl() + "/home/images/black_gear_48.png");
        defaultThumbnails.put(DC_LINGUISTIC_SYSTEM, getStaticRootUrl()+"/home/images/language_48.png");
        defaultThumbnails.put(null, getStaticRootUrl() + "/home/images/unknown_48.png");
        long start = System.currentTimeMillis();
        if(enhancementException == null){
            initOccurrences();
        }
        //init ExecutionMetadata
        try {
            executionMetadata = ci.getPart(ExecutionMetadata.CHAIN_EXECUTION, Graph.class);
        } catch(NoSuchPartException e){
            executionMetadata = null;
        }
        if(executionMetadata != null){
            BlankNodeOrIRI ce = ExecutionMetadataHelper.getChainExecution(executionMetadata, ci.getUri());
            if(ce != null){
                chainExecution = new ChainExecution(executionMetadata, ce);
                engineExecutions = new ArrayList<Execution>();
                for(BlankNodeOrIRI ex : ExecutionMetadataHelper.getExecutions(executionMetadata, ce)){
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
    
    @Override
    protected LayoutConfiguration getLayoutConfiguration() {
        return layoutConfiguration;
    }
    
    @Override
    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    public String getRdfMetadata(String mediatype) throws UnsupportedEncodingException {
        if(enhancementException == null){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            serializer.serialize(out, contentItem.getMetadata(), mediatype);
            return out.toString("utf-8");
        } else {//in case of an exception print the stacktrace
            StringWriter writer = new StringWriter();
            enhancementException.printStackTrace(new PrintWriter(writer));
            return writer.toString();
        }
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
        for(Map<EntityExtractionSummary,EntityExtractionSummary> occ : extractionsByTypeMap.values()){
            if(!occ.isEmpty()){
                return true;
            }
        }
        return false;
    }
    /**
     * Used to print occurrences with other types than the natively supported
     */
    public Collection<IRI> getOtherOccurrencyTypes(){
        Set<IRI>  types = new HashSet<IRI>(extractionsByTypeMap.keySet());
        types.remove(DBPEDIA_PERSON);
        types.remove(DBPEDIA_ORGANISATION);
        types.remove(DBPEDIA_PLACE);
        types.remove(SKOS_CONCEPT);
        types.remove(DC_LINGUISTIC_SYSTEM);
        types.remove(null); //other
        return types;
    }
    public static String extractLabel(IRI uri){
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
    public Collection<EntityExtractionSummary> getOccurrences(IRI type){
        Map<EntityExtractionSummary,EntityExtractionSummary> typeMap = extractionsByTypeMap.get(type);
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
    /**
     * Returns the Language Annotations
     * @since 0.11.0
     * @return
     * @throws ParseException
     */
    public Collection<EntityExtractionSummary> getLanguageOccurrences() throws ParseException {
        return getOccurrences(OntologicalClasses.DC_LINGUISTIC_SYSTEM);
    }
    enum EAProps {
        label,
        entity,
        confidence
    }

    private void initOccurrences() {
        Graph graph = contentItem.getMetadata();
        LiteralFactory lf = LiteralFactory.getInstance();
        Map<IRI,Collection<BlankNodeOrIRI>> suggestionMap = new HashMap<IRI,Collection<BlankNodeOrIRI>>();
        // 1) get Entity Annotations
        Map<BlankNodeOrIRI,Map<EAProps,Object>> entitySuggestionMap = new HashMap<BlankNodeOrIRI,Map<EAProps,Object>>();
        Iterator<Triple> entityAnnotations = graph.filter(null, RDF.type, ENHANCER_ENTITYANNOTATION);
        while(entityAnnotations.hasNext()){
            BlankNodeOrIRI entityAnnotation = entityAnnotations.next().getSubject();
            //to avoid multiple lookups (e.g. if one entityAnnotation links to+
            //several TextAnnotations) we cache the data in an intermediate Map
            Map<EAProps,Object> eaData = new EnumMap<EAProps,Object>(EAProps.class);
            eaData.put(EAProps.entity, getReference(graph, entityAnnotation, ENHANCER_ENTITY_REFERENCE));
            eaData.put(EAProps.label, getString(graph, entityAnnotation, ENHANCER_ENTITY_LABEL));
            eaData.put(EAProps.confidence, EnhancementEngineHelper.get(
                graph, entityAnnotation, ENHANCER_CONFIDENCE, Double.class, lf));
            entitySuggestionMap.put(entityAnnotation, eaData);
            Iterator<IRI> textAnnotations = getReferences(graph, entityAnnotation, DC_RELATION);
            while(textAnnotations.hasNext()){
                IRI textAnnotation = textAnnotations.next();
                Collection<BlankNodeOrIRI> suggestions = suggestionMap.get(textAnnotation);
                if(suggestions == null){
                    suggestions = new ArrayList<BlankNodeOrIRI>();
                    suggestionMap.put(textAnnotation, suggestions);
                }
                suggestions.add(entityAnnotation);
            }
        }
        // 2) get the TextAnnotations
        Iterator<Triple> textAnnotations = graph.filter(null, RDF.type, ENHANCER_TEXTANNOTATION);
        while(textAnnotations.hasNext()){
            BlankNodeOrIRI textAnnotation = textAnnotations.next().getSubject();
            //we need to process those to show multiple mentions
//            if (graph.filter(textAnnotation, DC_RELATION, null).hasNext()) {
//                // this is not the most specific occurrence of this name: skip
//                continue;
//            }
            String text = getString(graph, textAnnotation, Properties.ENHANCER_SELECTED_TEXT);
            //TextAnnotations without fise:selected-text are no longer ignored
//            if(text == null){
//                //ignore text annotations without text
//                continue;
//            }
            Integer start = EnhancementEngineHelper.get(graph,textAnnotation, 
                ENHANCER_START,Integer.class,lf);
            Integer end = EnhancementEngineHelper.get(graph,textAnnotation, 
                ENHANCER_END,Integer.class,lf);
            Double confidence = EnhancementEngineHelper.get(graph, textAnnotation, 
                ENHANCER_CONFIDENCE, Double.class, lf);
            Iterator<IRI> types = getReferences(graph, textAnnotation, DC_TYPE);
            if(!types.hasNext()){ //create an iterator over null in case no types are present
                types = Collections.singleton((IRI)null).iterator();
            }
            while(types.hasNext()){
                IRI type = types.next();
                Map<EntityExtractionSummary,EntityExtractionSummary> occurrenceMap = extractionsByTypeMap.get(type);
                if(occurrenceMap == null){
                    occurrenceMap = new TreeMap<EntityExtractionSummary,EntityExtractionSummary>();
                    extractionsByTypeMap.put(type, occurrenceMap);
                }
                //in case of a language annotation use the detected language as label
                if(DC_LINGUISTIC_SYSTEM.equals(type)){
                    text = EnhancementEngineHelper.getString(graph, textAnnotation, 
                        DC_LANGUAGE);
                }
                EntityExtractionSummary entity = new EntityExtractionSummary(text, type, start,end,confidence,defaultThumbnails);
                Collection<BlankNodeOrIRI> suggestions = suggestionMap.get(textAnnotation);
                if(suggestions != null){
                    for(BlankNodeOrIRI entityAnnotation : suggestions){
                        Map<EAProps,Object> eaData = entitySuggestionMap.get(entityAnnotation);
                        entity.addSuggestion(
                            (IRI)eaData.get(EAProps.entity),
                            (String)eaData.get(EAProps.label), 
                            (Double)eaData.get(EAProps.confidence), 
                            graph);
                    }
                }
                EntityExtractionSummary existingSummary = occurrenceMap.get(entity);
                if(existingSummary == null){//new extraction summary
                    occurrenceMap.put(entity, entity);
                } else {
                    //extraction summary with this text and suggestions already
                    //present ... only add a mention to the existing
                    existingSummary.addMention(new Mention(text, start, end, confidence));
                }
            }
        }
    }
    /**
     * Mentions of {@link EntityExtractionSummary EntityExtractionSummaries}. 
     * @author Rupert Westenthaler
     *
     */
    public static class Mention implements Comparable<Mention>{
        private String name;
        private Integer start;
        private Integer end;
        private Double conf;

        Mention(String name,Integer start, Integer end, Double confidence){
            if(name == null && start == null && end == null){
                this.name = "[global]";
                //throw new IllegalStateException("The name for a Mention MUST NOT be NULL!");
            } else if(name == null) {
                this.name = "[section]";
            } else {
                this.name = name;
            }
            this.start = start;
            this.end = end;
            this.conf = confidence;
        }
        
        public String getName() {
            return name;
        }
        public Integer getStart() {
            return start;
        }
        public Integer getEnd() {
            return end;
        }
        public Double getConfidence() {
            return conf;
        }
        public boolean hasOccurrence() {
            return start != null && end != null;
        }
        public boolean hasConfidence(){
            return conf != null;
        }
        @Override
        public int hashCode() {
            return name.hashCode() + 
                    (start != null ? start.hashCode() : 0) +
                    (end != null ? end.hashCode() : 0);
        }
        
        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Mention){
                Mention o = (Mention)obj;
                if(o.name.equals(name)){
                    if((o.start != null && o.start.equals(start)) ||
                            (o.start == null && start == null)){
                        if(o.end != null && o.end.equals(end)){
                            return true;
                        } else {
                            return o.end == null && end == null;
                        }
                    }
                }
            }
            return false;
        }
        
        @Override
        public int compareTo(Mention o) {
            int c = String.CASE_INSENSITIVE_ORDER.compare(o.name, this.name);
            if(c == 0){
                if(start != null && o.start != null){
                    c = start.compareTo(o.start);
                } else if(o.start != null){
                    c = 1;
                } else if(start != null){
                    c = -1;
                }
                if(c == 0){
                    if(o.end != null && end != null){
                        c = end.compareTo(o.end);
                    } else if(o.end != null){
                        c = -1;
                    } else if(end != null){
                        c = 1;
                    }
                }
            }
            return c;
        }
    }
    
    public ChainExecution getChainExecution(){
        return chainExecution;
    }
    
    public Collection<Execution> getEngineExecutions(){
        return engineExecutions;
    }
    public String getExecutionOffsetText(Execution ex){
        if(ex.getChain() == null || ex.getChain().getStarted() == null || ex.getStarted() == null){
            return null;
        } else {
            return String.format("%6dms",ex.getStarted().getTime() - ex.getChain().getStarted().getTime());
        }
    }
    public String getExecutionDurationText(Execution ex){
        if(ex.getDuration() == null){
            return "[duration not available]";
        } else if(ex.getDuration() < 1025){
            return ex.getDuration()+"ms";
        } else {
            return String.format("%.2fsec",(ex.getDuration().floatValue()/1000));
        }
    }
    public String getExecutionStartTime(Execution ex){
        if(ex.getStarted() != null){
            return format.format(ex.getStarted());
        } else {
            return "unknown";
        }
    }
    public String getExecutionCompletionTime(Execution ex){
        if(ex.getCompleted() != null){
            return format.format(ex.getCompleted());
        } else {
            return "unknown";
        }
    }    
    public String getExecutionStatusText(Execution ex){
        if(ExecutionMetadata.STATUS_COMPLETED.equals(ex.getStatus())){
            return "completed";
        } else if(ExecutionMetadata.STATUS_FAILED.equals(ex.getStatus())){
            return "failed";
        } else if(ExecutionMetadata.STATUS_IN_PROGRESS.equals(ex.getStatus())){
            return "in-progress";
        } else if(ExecutionMetadata.STATUS_SCHEDULED.equals(ex.getStatus())){
            return "scheduled";
        } else if(ExecutionMetadata.STATUS_SKIPPED.equals(ex.getStatus())){
            return "skipped";
        } else {
            return "unknown";
        }
    }    
    public static class EntityExtractionSummary implements Comparable<EntityExtractionSummary> {

        protected final String name;

        
        protected final IRI type;

        protected List<EntitySuggestion> suggestions = new ArrayList<EntitySuggestion>();
        protected Set<IRI> suggestionSet = new HashSet<IRI>();

        protected List<Mention> mentions = new ArrayList<Mention>();

        public final Map<IRI,String> defaultThumbnails;


        private Integer start;

        private Integer end;


        private Double confidence;

        public EntityExtractionSummary(String name, IRI type, Integer start, Integer end, Double confidence, Map<IRI,String> defaultThumbnails) {
            if(name == null){
                this.name = extractLabel(type);
            } else {
                this.name = name;
            }
            this.type = type;
            mentions.add(new Mention(name, start, end, confidence));
            this.defaultThumbnails = defaultThumbnails;
            this.start = start;
            this.end = end;
            this.confidence = confidence;
        }

        public void addSuggestion(IRI uri, String label, Double confidence, Graph properties) {
            EntitySuggestion suggestion = new EntitySuggestion(uri, type, label, confidence, properties,
                    defaultThumbnails);
            suggestionSet.add(uri);
            if (!suggestions.contains(suggestion)) {
                suggestions.add(suggestion);
                Collections.sort(suggestions);
            }
        }
        public void addMention(Mention mention){
            if(!mentions.contains(mention)){
                mentions.add(mention);
                Collections.sort(mentions);
            }
        }

        public String getName() {
            EntitySuggestion bestGuess = getBestGuess();
            if (bestGuess != null) {
                return bestGuess.getLabel();
            }
            return name;
        }
        public String getSelected(){
            return name;
        }
        public String getUri() {
            EntitySuggestion bestGuess = getBestGuess();
            if (bestGuess != null) {
                return bestGuess.getUri();
            }
            return null;
        }
        public Double getConfidence(){
            EntitySuggestion bestGuess = getBestGuess();
            if (bestGuess != null) {
                return bestGuess.getConfidence();
            }
            return confidence;
        }

        public String getSummary() {
            if (suggestions.isEmpty()) {
                return "";
            }
            return suggestions.get(0).getSummary();
        }
        public Integer getStart() {
            return start;
        }
        public Integer getEnd() {
            return end;
        }
        public boolean hasOccurrence(){
            return start != null && end != null;
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

        public List<Mention> getMentions() {
            return mentions;
        }

        @Override
        public int compareTo(EntityExtractionSummary o) {
            int c = String.CASE_INSENSITIVE_ORDER.compare(getName(),o.getName());
            if(c == 0){
                if(suggestionSet.equals(o.suggestionSet)){
                    return 0; //assume as equals if name and suggestionSet is the same
                } else { //sort by mention
                    if(start != null && o.start != null){
                        c = start.compareTo(o.start);
                    } else if(o.start != null){
                        c = 1;
                    } else if(start != null){
                        c = -1;
                    }
                    if(c == 0){
                        if(o.end != null && end != null){
                            c = end.compareTo(o.end);
                        } else if(o.end != null){
                            c = -1;
                        } else if(end != null){
                            c = 1;
                        }
                    }
                }
            }
            return c;
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
            //if name and suggestions are the same ... consider as equals
            if(getName().equalsIgnoreCase(getName())){
                return suggestionSet.equals(that.suggestionSet);
            } else {
                return false;
            }
            //return !(name != null ? !name.equals(that.name) : that.name != null);
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }

    public static class EntitySuggestion implements Comparable<EntitySuggestion> {

        protected final IRI uri;

        protected final IRI type;

        protected final String label;

        protected final Double confidence;

        protected Graph entityProperties;

        protected final Map<IRI,String> defaultThumbnails;

        public EntitySuggestion(IRI uri,
                                IRI type,
                                String label,
                                Double confidence,
                                Graph entityProperties,
                                Map<IRI,String> defaultThumbnails) {
            this.uri = uri;
            if(label == null){
                this.label = extractLabel(uri);
            } else {
                this.label = label;
            }
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
                RDFTerm object = thumbnails.next().getObject();
                if (object instanceof IRI) {
                    return ((IRI) object).getUnicodeString();
                }
            }
            //if no dbpedia ontology thumbnail was found. try the same with foaf:depiction
            thumbnails = entityProperties.filter(uri, DEPICTION, null);
            while (thumbnails.hasNext()) {
                RDFTerm object = thumbnails.next().getObject();
                if (object instanceof IRI) {
                    return ((IRI) object).getUnicodeString();
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
                RDFTerm object = abstracts.next().getObject();
                if (object instanceof Literal) {
                    Literal abstract_ = (Literal) object;
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
        Graph g = new IndexedGraph();
        LiteralFactory lf = LiteralFactory.getInstance();
        Graph metadata = contentItem.getMetadata();
        for (EntityExtractionSummary p : getPlaceOccurrences()) {
            EntitySuggestion bestGuess = p.getBestGuess();
            if (bestGuess == null) {
                continue;
            }
            IRI uri = new IRI(bestGuess.getUri());
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
//        addCORSOrigin(servletContext,rb, headers);
        return rb.build();
    }
    
}
