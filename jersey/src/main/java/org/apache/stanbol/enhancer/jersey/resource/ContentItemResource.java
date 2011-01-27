package org.apache.stanbol.enhancer.jersey.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;


import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PERSON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.GEO_LAT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.GEO_LONG;

@ImplicitProduces(TEXT_HTML + ";qs=2")
public class ContentItemResource extends NavigationMixin {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(getClass());

    // TODO make this configurable trough a property
    public static final UriRef SUMMARY = new UriRef(
            "http://www.w3.org/2000/01/rdf-schema#comment");

    // TODO make this configurable trough a property
    public static final UriRef THUMBNAIL = new UriRef(
            "http://dbpedia.org/ontology/thumbnail");

    public static final Map<UriRef, String> DEFAULT_THUMBNAILS = new HashMap<UriRef, String>();
    static {
        DEFAULT_THUMBNAILS.put(DBPEDIA_PERSON, "/static/images/user_48.png");
        DEFAULT_THUMBNAILS.put(DBPEDIA_ORGANISATION, "/static/images/organization_48.png");
        DEFAULT_THUMBNAILS.put(DBPEDIA_PLACE, "/static/images/compass_48.png");
    }

    protected ContentItem contentItem;

    protected String localId;

    protected String textContent;

    protected URI imageSrc;

    protected URI downloadHref;

    protected URI metadataHref;

    protected final TcManager tcManager;

    protected final Serializer serializer;

    protected String serializationFormat = SupportedFormat.RDF_XML;

    protected final TripleCollection remoteEntityCache;

    protected Collection<EntityExtractionSummary> people;

    protected Collection<EntityExtractionSummary> organizations;

    protected Collection<EntityExtractionSummary> places;

    public ContentItemResource(String localId, ContentItem ci,
            TripleCollection remoteEntityCache, UriInfo uriInfo,
            TcManager tcManager, Serializer serializer) throws IOException {
        this.contentItem = ci;
        this.localId = localId;
        this.uriInfo = uriInfo;
        this.tcManager = tcManager;
        this.serializer = serializer;
        this.remoteEntityCache = remoteEntityCache;

        if (localId != null) {
            URI rawURI = UriBuilder.fromPath("/store/raw/" + localId).build();
            if (ci.getMimeType().equals("text/plain")) {
                this.textContent = IOUtils.toString(ci.getStream(), "UTF-8");
            } else if (ci.getMimeType().startsWith("image/")) {
                this.imageSrc = rawURI;
            }
            this.downloadHref = rawURI;
            this.metadataHref = UriBuilder.fromPath(
                    "/store/metadata/" + localId).build();
        }
    }

    public String getRdfMetadata(String mediatype)
            throws UnsupportedEncodingException {
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

    public Collection<EntityExtractionSummary> getPersonOccurrences()
            throws ParseException {
        if (people == null) {
            people = getOccurrences(DBPEDIA_PERSON);
        }
        return people;
    }

    public Collection<EntityExtractionSummary> getOrganizationOccurrences()
            throws ParseException {
        if (organizations == null) {
            organizations = getOccurrences(DBPEDIA_ORGANISATION);
        }
        return organizations;
    }

    public Collection<EntityExtractionSummary> getPlaceOccurrences()
            throws ParseException {
        if (places == null) {
            places = getOccurrences(DBPEDIA_PLACE);
        }
        return places;
    }

    public Collection<EntityExtractionSummary> getOccurrences(UriRef type)
            throws ParseException {
        MGraph graph = contentItem.getMetadata();
        String q = "PREFIX enhancer: <http://fise.iks-project.eu/ontology/> "
                + "PREFIX dc:   <http://purl.org/dc/terms/> "
                + "SELECT ?textAnnotation ?text ?entity ?entity_label ?confidence WHERE { "
                + "  ?textAnnotation a enhancer:TextAnnotation ."
                + "  ?textAnnotation dc:type %s ."
                + "  ?textAnnotation enhancer:selected-text ?text ."
                + " OPTIONAL {"
                + "   ?entityAnnotation dc:relation ?textAnnotation ."
                + "   ?entityAnnotation a enhancer:EntityAnnotation . "
                + "   ?entityAnnotation enhancer:entity-reference ?entity ."
                + "   ?entityAnnotation enhancer:entity-label ?entity_label ."
                + "   ?entityAnnotation enhancer:confidence ?confidence . }"
                + "} ORDER BY ?text ";
        q = String.format(q, type);

        SelectQuery query = (SelectQuery) QueryParser.getInstance().parse(q);
        ResultSet result = tcManager.executeSparqlQuery(query, graph);
        Map<String, EntityExtractionSummary> occurrenceMap = new TreeMap<String, EntityExtractionSummary>();
        LiteralFactory lf = LiteralFactory.getInstance();
        while (result.hasNext()) {
            SolutionMapping mapping = result.next();

            UriRef textAnnotationUri = (UriRef) mapping.get("textAnnotation");
            if (graph.filter(textAnnotationUri, Properties.DC_RELATION, null).hasNext()) {
                // this is not the most specific occurrence of this name: skip
                continue;
            }
            // TODO: collect the selected text and contexts of subsumed
            // annotations

            TypedLiteral textLiteral = (TypedLiteral) mapping.get("text");
            String text = lf.createObject(String.class, textLiteral);

            EntityExtractionSummary entity = occurrenceMap.get(text);
            if (entity == null) {
                entity = new EntityExtractionSummary(text, type);
                occurrenceMap.put(text, entity);
            }
            UriRef entityUri = (UriRef) mapping.get("entity");
            if (entityUri != null) {
                String label = ((Literal) mapping.get("entity_label")).getLexicalForm();
                Double confidence = lf.createObject(Double.class,
                        (TypedLiteral) mapping.get("confidence"));
                Graph properties = new GraphNode(entityUri, remoteEntityCache).getNodeContext();
                entity.addSuggestion(entityUri, label, confidence, properties);
            }
        }
        return occurrenceMap.values();
    }

    public static class EntityExtractionSummary implements
            Comparable<EntityExtractionSummary> {

        protected final String name;

        protected final UriRef type;

        protected List<EntitySuggestion> suggestions = new ArrayList<EntitySuggestion>();

        protected List<String> mentions = new ArrayList<String>();

        public EntityExtractionSummary(String name, UriRef type) {
            this.name = name;
            this.type = type;
            mentions.add(name);
        }

        public void addSuggestion(UriRef uri, String label, Double confidence,
                TripleCollection properties) {
            EntitySuggestion suggestion = new EntitySuggestion(uri, type,
                    label, confidence, properties);
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
                return DEFAULT_THUMBNAILS.get(type);
            }
            return suggestions.get(0).getThumbnailSrc();
        }

        public String getMissingThumbnailSrc() {
            return DEFAULT_THUMBNAILS.get(type);
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

    public static class EntitySuggestion implements
            Comparable<EntitySuggestion> {

        protected final UriRef uri;

        protected final UriRef type;

        protected final String label;

        protected final Double confidence;

        protected TripleCollection entityProperties;

        public EntitySuggestion(UriRef uri, UriRef type, String label,
                Double confidence, TripleCollection entityProperties) {
            this.uri = uri;
            this.label = label;
            this.type = type;
            this.confidence = confidence;
            this.entityProperties = entityProperties;
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
            Iterator<Triple> abstracts = entityProperties.filter(uri,
                    THUMBNAIL, null);
            while (abstracts.hasNext()) {
                Resource object = abstracts.next().getObject();
                if (object instanceof UriRef) {
                    return ((UriRef) object).getUnicodeString();
                }
            }
            return DEFAULT_THUMBNAILS.get(type);
        }

        public String getMissingThumbnailSrc() {
            return DEFAULT_THUMBNAILS.get(type);
        }

        public String getSummary() {
            Iterator<Triple> abstracts = entityProperties.filter(uri, SUMMARY,
                    null);
            while (abstracts.hasNext()) {
                Resource object = abstracts.next().getObject();
                if (object instanceof PlainLiteral) {
                    PlainLiteral abstract_ = (PlainLiteral) object;
                    if (abstract_.getLanguage().equals(new Language("en"))) {
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
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EntitySuggestion other = (EntitySuggestion) obj;
            if (uri == null) {
                if (other.uri != null)
                    return false;
            } else if (!uri.equals(other.uri))
                return false;
            return true;
        }

    }

    public void setRdfSerializationFormat(String format) {
        serializationFormat = format;
    }

    /**
     * @return an RDF/JSON descriptions of places for the word map widget
     */
    public String getPlacesAsJSON() throws ParseException,
            UnsupportedEncodingException {
        MGraph g = new SimpleMGraph();
        if (remoteEntityCache != null) {
            LiteralFactory lf = LiteralFactory.getInstance();
            for (EntityExtractionSummary p : getPlaceOccurrences()) {
                EntitySuggestion bestGuess = p.getBestGuess();
                if (bestGuess == null) {
                    continue;
                }
                UriRef uri = new UriRef(bestGuess.getUri());
                Iterator<Triple> latitudes = remoteEntityCache.filter(uri,
                        GEO_LAT, null);
                if (latitudes.hasNext()) {
                    g.add(latitudes.next());
                }
                Iterator<Triple> longitutes = remoteEntityCache.filter(uri,
                        GEO_LONG, null);
                if (longitutes.hasNext()) {
                    g.add(longitutes.next());
                    g.add(new TripleImpl(uri, Properties.RDFS_LABEL,
                            lf.createTypedLiteral(bestGuess.getLabel())));
                }
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, g, SupportedFormat.RDF_JSON);
        return out.toString("utf-8");
    }

}
