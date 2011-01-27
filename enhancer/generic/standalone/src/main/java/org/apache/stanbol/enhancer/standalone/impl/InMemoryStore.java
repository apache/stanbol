package org.apache.stanbol.enhancer.standalone.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Trivial in-memory Store for standalone Stanbol Enhancement server */
@Component(immediate = false)
@Service
// Use a low service ranking so that "real" stores replace this
@Property(name = "service.ranking", intValue = -1000)
public class InMemoryStore implements Store {

    private final Logger log = LoggerFactory.getLogger(InMemoryStore.class);

    private final Map<String, ContentItem> data = new HashMap<String, ContentItem>();

    @Reference
    private WeightedTcProvider tcProvider;

    @Property(value = "http://stanbol.apache.org/enhancer/defaultEnhancementsGraphID")
    public static final String GRAPH_URI = "org.apache.stanbol.enhancer.standalone.store.graphUri";

    public InMemoryStore() {
        super();
    }

    public ContentItem create(String id, byte[] content, String mimeType) {
        UriRef uri = id == null ? ContentItemHelper.makeDefaultUrn(content)
                : new UriRef(id);
        log.debug("create ContentItem for id " + uri + " on TC Manager= "
                + tcProvider);
        final MGraph g = new SimpleMGraph();
        return new InMemoryContentItem(uri.getUnicodeString(), content, mimeType, g);
    }

    public ContentItem get(String id) {
        ContentItem result;
        synchronized (data) {
            result = data.get(id);
        }
        return result;
    }

    public String put(ContentItem ci) {
        synchronized (data) {

            data.put(ci.getId(), ci);

            // remove any previously stored data about ci
            MGraph g = getEnhancementGraph();
            UriRef uri = new UriRef(ci.getId());
            Iterator<Triple> toRemove = g.filter(uri, null, null);
            while (toRemove.hasNext()) {
                toRemove.next();
                toRemove.remove();
            }
            toRemove = g.filter(null, null, uri);
            while (toRemove.hasNext()) {
                toRemove.next();
                toRemove.remove();
            }
            // TODO: how to handle orphan indirect triples?

            // accumulate all triples recently collected
            getEnhancementGraph().addAll(ci.getMetadata());
        }
        return ci.getId();
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    public MGraph getEnhancementGraph() {
        final UriRef graphUri = new UriRef(GRAPH_URI);
        try {
            return tcProvider.getMGraph(graphUri);
        } catch (NoSuchEntityException e) {
            return tcProvider.createMGraph(graphUri);
        }
    }
}
