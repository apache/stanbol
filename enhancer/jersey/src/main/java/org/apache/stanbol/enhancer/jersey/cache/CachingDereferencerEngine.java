package org.apache.stanbol.enhancer.jersey.cache;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.UriBuilder;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;

/**
 * Simple engine that does not enhance content items but fetches resources
 * metadata from remote sites to cache them locally for the sole purpose of
 * displaying up to date data in the user interface.
 * <p>
 * This engine might be replaced by a proper dereferencer engine in a future
 * version of enhancer.
 *
 * @author Olivier Grisel
 */
@Component(immediate = true, metatype = true)
@Service
public class CachingDereferencerEngine implements EnhancementEngine,
        ServiceProperties, EntityCacheProvider {

    public static final String ENTITY_CACHE_GRAPH_NAME = "enhancerEntityCache";

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link EnhancementJobManager#DEFAULT_ORDER}
     */
    public static final Integer defaultOrder = ORDERING_POST_PROCESSING;

    @Reference
    protected Parser parser;

    @Reference
    protected Store store;

    @Reference
    protected WeightedTcProvider tcProvider;

    protected ThreadPoolExecutor executor;

    protected BlockingQueue<TripleCollection> serializationQueue;

    protected Thread serializer;

    protected boolean serializerActive = false;

    protected LinkedBlockingQueue<Runnable> fetchTaskQueue;

    protected void activate(ComponentContext ce) throws IOException {
        fetchTaskQueue = new LinkedBlockingQueue<Runnable>();
        executor = new ThreadPoolExecutor(4, 10, 5, TimeUnit.MINUTES,
                fetchTaskQueue);
        serializationQueue = new LinkedBlockingQueue<TripleCollection>();
        final MGraph entityCache = getEntityCache();
        serializerActive = true;
        serializer = new Thread() {
            @Override
            public void run() {
                while (serializerActive) {
                    try {
                        entityCache.addAll(serializationQueue.take());
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        };
        serializer.start();
    }

    protected void deactivate(ComponentContext ce) throws IOException {
        executor.shutdownNow();
        executor = null;
        fetchTaskQueue = null;
        // stop the serialization queue by sending it a last job
        serializerActive = false;
        serializationQueue.add(new SimpleMGraph());
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        MGraph metadata = ci.getMetadata();
        Set<UriRef> references = new HashSet<UriRef>();
        Iterator<Triple> entities = metadata.filter(null,
                Properties.ENHANCER_ENTITY_REFERENCE, null);
        while (entities.hasNext()) {
            references.add((UriRef) entities.next().getObject());
        }

        final MGraph entityCache = getEntityCache();
        for (final UriRef reference : references) {
            if (entityCache.filter(reference, null, null).hasNext()) {
                // already in cache
                continue;
            }
            if (fetchTaskQueue.contains(reference)) {
                // optim: do not try to submit twice the same job
                continue;
            }

            // asynchronously dereference (fire and forget)
            executor.execute(new Runnable() {
                @Override
                public boolean equals(Object other) {
                    // overridden to implement queue membership to avoid
                    // duplicate submissions for dereferencing of the same URI
                    if (other instanceof UriRef) {
                        return reference.equals(other);
                    }
                    return this == other;
                }

                @Override
                public void run() {
                    try {
                        serializationQueue.add(dereference(reference));
                    } catch (IOException e) {
                        log.warn("unable to dereference " + reference + " : "
                                + e.getMessage());
                        log.debug(e.getMessage(), e);
                    }
                }
            });
        }
    }

    public Graph dereferenceHTTP(UriRef reference) throws IOException {
        final URL url = new URL(reference.getUnicodeString());
        final URLConnection con = url.openConnection();
        con.addRequestProperty("Accept", RDF_XML);
        return parser.parse(con.getInputStream(), RDF_XML);
    }

    public Graph dereferenceSPARQL(String endpointURL, UriRef reference)
            throws IOException {

        StringBuilder query = new StringBuilder();
        query.append("CONSTRUCT { ");
        query.append(reference);
        query.append(" ?p ?o } WHERE { ");
        query.append(reference);
        query.append(" ?p ?o }");

        String format = RDF_XML;
        final URI uri = UriBuilder.fromUri(endpointURL).queryParam("query",
                "{query}").queryParam("format", "{format}").build(
                query.toString(), format);
        final URLConnection con = uri.toURL().openConnection();
        con.addRequestProperty("Accept", format);
        return parser.parse(con.getInputStream(), format);
    }

    public Graph dereference(UriRef reference) throws IOException {
        log.debug("dereferencing: " + reference);
        // TODO: make the switch between SPARQL and HTTP configurable
        if (reference.getUnicodeString().startsWith(
                "http://dbpedia.org/resource/")) {
            // special handling of dbpedia references using SPARQL since the
            // basic HTTP dereference run the risk of a truncated output
            return dereferenceSPARQL("http://dbpedia.org/sparql", reference);
        } else {
            return dereferenceHTTP(reference);
        }
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        return ENHANCE_SYNCHRONOUS;
    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING,
                (Object) defaultOrder));
    }

    public MGraph getEntityCache() {
        final UriRef graphUri = new UriRef(ENTITY_CACHE_GRAPH_NAME);
        try {
            return tcProvider.getMGraph(graphUri);
        } catch (NoSuchEntityException e) {
            return tcProvider.createMGraph(graphUri);
        }
    }

}
