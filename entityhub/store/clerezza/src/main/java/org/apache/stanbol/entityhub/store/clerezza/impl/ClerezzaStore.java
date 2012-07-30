package org.apache.stanbol.entityhub.store.clerezza.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.ldpath.clerezza.ClerezzaBackend;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.EpochException;
import org.apache.stanbol.commons.semanticindex.store.IndexingSource;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.api.backend.RDFBackend;

/**
 * {@link Store} implementation for Clerezza {@link TripleCollection}. In case
 * the configured graph URI points to a {@link Graph} the Store will be
 * read-only (basically only support the {@link IndexingSource} part of the
 * {@link Store} interface. This also implements {@link RDFBackend} for
 * {@link Resource} over the configured Graph. The Data of this Store can 
 * therefore be used with {@link LDPath}.<p>
 * This implementation internally supports {@link BNode}s. Internally a
 * {@link Representation} is defined as all outgoing triples including 
 * recursively all outgoing triples of {@link BNode} used as 
 * {@link Triple#getObject() object}. This Store also adds Triples with
 * {@link BNode}s to the {@link RdfRepresentation#getRdfGraph() rdf graph} of
 * {@link RdfRepresentation}s returned by {@link #get(String)}. However via
 * the {@link Representation} interface Users will have no direct access to
 * those information.<p>
 * 
 * <b>TODO</b>s:<ul>
 * <li> This dies not yet support {@link #changes(long, long, int)} and Epochs.
 * I need to discuss with SUAT if it is possible to make the FileRevisionManager
 * useable by any {@link Store} implementation. This would dramatically 
 * reduce the coding requirements for Store Implementations!
 * <li> No unit tests for now ...
 * </ul>
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,configurationFactory=true,
		   policy=ConfigurationPolicy.REQUIRE)
@Properties({
	@Property(name=Store.PROPERTY_NAME,value="changeMe"),
	@Property(name=Store.PROPERTY_DESCRIPTION,value="<Optional Description>")
})
public class ClerezzaStore extends ClerezzaBackend implements Store<Representation>, RDFBackend<Resource> {
	private static final Logger log = LoggerFactory.getLogger(ClerezzaStore.class);
	/**
	 * Allows to configure the Graph URI used to store the data
	 */
	@Property
	public static final String PROPERTY_GRAPH_URI = "org.apache.stanbol.entityhub.store.clerezza.graphuri";
    @Reference
    private TcManager tcManager;

    private TripleCollection graph;
    
    private ServiceRegistration graphRegistration;
    
    private final RdfValueFactory valueFactory = RdfValueFactory.getInstance();

	private String name;
	private String description;
	/**
	 * Unmodifiable properties as set in the {@link #activate(ComponentContext)}
	 * method
	 */
	private Map<String,Object> properties;
	
	@Activate
	protected final void activate(ComponentContext context) throws ConfigurationException {
		Object value = context.getProperties().get(PROPERTY_NAME);
		Map<String,Object> properties = new HashMap<String,Object>();
		if(value == null || value.toString().isEmpty()){
			throw new ConfigurationException(PROPERTY_NAME, "The parsed name of the Store MUST NOT be NULL nor empty!");
		} else if(value.getClass().isArray() || value instanceof Collection<?>){
			throw new ConfigurationException(PROPERTY_NAME, "Only a single name for the Store MUST BE parsed (type: '"
					+ value.getClass() +"', values: "+value+")!");
		} else {
			this.name = value.toString();
			properties.put(PROPERTY_NAME, name);
		}
		value = context.getProperties().get(Store.PROPERTY_DESCRIPTION);
		this.description = value != null ? value.toString() : null;
		if(this.description != null){
			properties.put(PROPERTY_DESCRIPTION, description);
		}
		value = context.getProperties().get(PROPERTY_GRAPH_URI);
		UriRef graphUri;
		if(value == null || value.toString().isEmpty()){
			graphUri = new UriRef(String.format("urn:org.apache:entityhub.store.clerezza:%s",
					name));
		} else if(value.getClass().isArray() || value instanceof Collection<?>){
			throw new ConfigurationException(PROPERTY_GRAPH_URI, "Only a single PROPERTY_GRAPH_URI MUST BE parsed (type: '"
					+ value.getClass() +"', values: "+value+")!");
		} else {
			try {
				new URI(value.toString());
			} catch (URISyntaxException e) {
				throw new ConfigurationException(PROPERTY_GRAPH_URI, "The parsed grph URI is invalid (message: "
						+ e.getMessage()+")",e);
			}
			graphUri = new UriRef(value.toString());
		}
		properties.put(PROPERTY_GRAPH_URI, graphUri);
        try {
            this.graph = tcManager.getTriples(graphUri);
            log.info("  ... (re)use existing Graph {} for Yard {}",
            		graphUri,name);
            if(!(graph instanceof LockableMGraph)){
                log.info("        > NOTE: this ClerezzaStore is read-only");
            }
        } catch (NoSuchEntityException e) {
            log.info("   ... create new Graph {} for ClerezzaStore {}",graphUri,name);
            this.graph =  tcManager.createMGraph(graphUri);
        }
        //set the graph also to the parent ClerezzaRepository
        setGraph(graph);
        //register the store with the Stanbol SPARQL endpoint
        Dictionary<String,Object> graphRegProp = new Hashtable<String,Object>();
        graphRegProp.put("graph.uri", graphUri.getUnicodeString());
        graphRegProp.put(Constants.SERVICE_RANKING, new Integer(-100));
        graphRegProp.put("graph.name", name);
        if(description != null){
            graphRegProp.put("graph.description", description);
        }
        graphRegistration = context.getBundleContext().registerService(
            TripleCollection.class.getName(), graph, graphRegProp);

        
		log.info("Activate {} '{}' with grpah URI {}");
		properties.put(PROPERTY_ITEM_TYPE, Representation.class);
		this.properties = Collections.unmodifiableMap(properties);
	}
	
	@Deactivate
	protected final void deactivate(ComponentContext context){
		if(graphRegistration != null){
			graphRegistration.unregister();
		}
		this.graph = null;
		setGraph(null);//remove the graph also form the parent ClerezzaRepository
		this.name = null;
		this.description = null;
		this.properties = null;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Class<Representation> getItemType() {
		return Representation.class;
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}

	@Override
	public Representation get(String uri) throws StoreException {
        final Lock readLock = readLockGraph();
        try {
        	return getRepresentation(new UriRef(uri), null);
        } finally {
        	if(readLock != null){ //on read-only Graphs this will be null
        		readLock.unlock();
        	}
        }
        
    }

	@Override
	public long getEpoch() {
		// TODO implement
		throw new UnsupportedOperationException("TODO: implement!!");
	}

	@Override
	public ChangeSet<Representation> changes(long epoch, long revision,
			int batchSize) throws StoreException, EpochException {
		// TODO implement
		throw new UnsupportedOperationException("TODO: implement!!");
	}

	@Override
	public Representation remove(String uri) throws StoreException {
        final Lock writeLock = writeLockGraph();
        try {
        	Representation resource = get(uri);
        	if(resource != null){
        		removeResource(new UriRef(uri));
        	}
        	return resource;
        } finally {
            writeLock.unlock();
        }
	}

	@Override
	public String put(Representation representation) throws StoreException {
        log.debug("put Representation " + representation.getId());
        final Lock writeLock = writeLockGraph();
        try {
            return add(representation);
        } finally {
            writeLock.unlock();
        }
    }

	@Override
	public Iterable<String> put(Iterable<Representation> items)
			throws StoreException {
		List<String> processed = new ArrayList<String>();
        final Lock writeLock = writeLockGraph();
        try {
        	//remove all existing
			for(Representation item : items){
				removeResource(new UriRef(item.getId()));
			}
			//add all parsed
			for(Representation item : items){
				add(item);
				processed.add(item.getId());
			}
        } finally {
            writeLock.unlock();
        }
		return processed;
	}

	@Override
	public void remove(Iterable<String> uris) throws StoreException {
        final Lock writeLock = writeLockGraph();
        try {
	    	//remove all existing
			for(String uri : uris){
				removeResource(new UriRef(uri));
			}
        } finally {
            writeLock.unlock();
        }
	}

	@Override
	public void removeAll() throws StoreException {
        final Lock writeLock = writeLockGraph();
        try {
        	graph.clear();
        } finally {
            writeLock.unlock();
        }
	}

	
	/* -----------------------------------------------------------------------
	 * Utility methods for getting/adding/removing Resources 
	 * (including support for BNodes)
	 * -----------------------------------------------------------------------
	 */
	/**
	 * Adds the representation (without deleting). Assumes a writeLock
	 * @param representation
	 * @return
	 */
	protected final String add(Representation representation) {
		//get the graph for the Representation and add it to the store
		RdfRepresentation toAdd = ((RdfValueFactory)valueFactory).toRdfRepresentation(representation);
		//log.info("  > add "+toAdd.size()+" triples to Yard "+getId());

		addResource(toAdd.getRdfGraph(),toAdd.getNode());
		return representation.getId();
	}
	
	/**
	 * Removes the {@link Representation#getId() resource} and also recursively 
	 * all {@link BNode}s referenced by outgoing triples. This ensures that
	 * BNodes added to the graph are also deleted of the {@link Representation}
	 * is deleted.
     * <p>
     * This method assumes a read lock on {@link #graph}
	 * @param resource
	 * @return
	 */
	private boolean removeResource(UriRef resource) {
		Iterator<Triple> current = graph.filter(resource, null, null);
		Set<BNode> bNodes = new HashSet<BNode>();
		boolean contains = current.hasNext();
		while(current.hasNext()){ //delete current
		    Triple t = current.next();
		    current.remove();
		    if(t.getObject() instanceof BNode){
		    	BNode node = (BNode)t.getObject();
		    	if(!graph.filter(null, null, node).hasNext()){
		    		bNodes.add(node);
		    	} //other resources to refer the same BNode ... do not delete
		    }
		}
		//remove all outgoing references with BNodes as object
		while(!bNodes.isEmpty()){
			Iterator<BNode> it = bNodes.iterator();
			boolean modified = false;
			while(it.hasNext() && //still more BNodes to remove
					!modified){ // if the bNodes set is modified we need a new Iterator!
				BNode node = it.next();
				it.remove(); //remove the BNode before processing as afterwards
				//the Set might already be modified!
				modified = removeBNode(node,bNodes);
			}
		}
		return contains;
	}
	
	
	/**
	 * Removes all triples for this {@link BNode} and adds all other
	 * {@link BNode}s referenced by {@link Triple#getObject()}.<p>
	 * This allows to recursively remove all BNodes of outgoing triples without
	 * recursively calling this method
	 * @param resource the BNode to remove
	 * @param set other BNodes
	 * @return if the parsed set was modified by this method
	 */
	private boolean removeBNode(BNode resource, Set<BNode> set){
		Iterator<Triple> current = graph.filter(resource, null, null);
		boolean modified = false;
		while(current.hasNext()){ //delete current
		    Resource obj = current.next().getObject();
		    current.remove();
		    if(obj instanceof BNode && !obj.equals(resource)){
		    	BNode node = (BNode)obj;
		    	if(!node.equals(resource) && !graph.filter(null, null, node).hasNext()){
		    		modified = set.add(node) || modified;
		    	} //else ignore because
		    	// the Bnode of the object is the same as the subject or
		    	// an other resources to refer the same BNode
		    }
		}
		return modified;
	}

	/**
	 * Adds all outgoing triples for the parsed resource. In addition it also
	 * adds triples for {@link BNode}s that are {@link Triple#getObject()}s of
	 * added triples.
     * <p>
     * This method assumes a read lock on {@link #graph}
	 * @param source the source graph
	 * @param resource the resource to add
	 */
	private void addResource(TripleCollection source, UriRef resource) {
		Iterator<Triple> triples = source.filter(resource, null, null);
		Set<BNode> bNodes = new HashSet<BNode>();
		while(triples.hasNext()){
			Triple t = triples.next();
			if(t.getObject() instanceof BNode){
				bNodes.add((BNode)t.getObject());
			}
		    graph.add(t);
		}
		while(!bNodes.isEmpty()){
			boolean modified = false;
			Iterator<BNode> it = bNodes.iterator();
			while(!modified && it.hasNext()){
				BNode node = it.next();
				it.remove();
				modified = addBNode(source, node, bNodes);
			}
		}
	}
	private boolean addBNode(TripleCollection source, BNode node, Set<BNode> set) {
		boolean modified = false;
		Iterator<Triple> triples = source.filter(node, null, null);
		while(triples.hasNext()){
			Triple t = triples.next();
		    graph.add(t);
			if(t.getObject() instanceof BNode){
				modified = set.add((BNode)t.getObject());
			}
		}
		return modified;
	}
    /**
     * Creates a {@link RdfRepresentation} for the parsed resource. If target
     * is not <code>null</code> it will be used to create the representation.
     * This follows all outgoing {@link Triple} as well as recursively 
     * outgoing triples of {@link BNode}s used as {@link Triple#getObject()}.
     * <p>
     * This method assumes a read lock on {@link #graph}
     */
    private RdfRepresentation getRepresentation(UriRef resource, MGraph target){
        //we need all the outgoing relations and also want to follow bNodes until
        //the next UriRef. However we are not interested in incoming relations!
    	if(target == null){
    		target = new IndexedMGraph();
    	}
    	Set<BNode> bNodes = new HashSet<BNode>();
        Iterator<Triple> triples = graph.filter(resource, null, null);
        if(!triples.hasNext()){
        	return null;
        }
        while (triples.hasNext()) {
            Triple triple = triples.next();
            target.add(triple);
            Resource object = triple.getObject();
            if(object instanceof BNode){
            	//collect BNodes to process them later
            	bNodes.add((BNode)object);
            }
        }
        Set<BNode> visited = new HashSet<BNode>();
        while(!bNodes.isEmpty()){
        	Iterator<BNode> it = bNodes.iterator();
        	boolean modified = false;
        	while(!modified && it.hasNext()){
        		BNode node = it.next();
        		it.remove();
        		modified = collectTriples(target, node, visited, bNodes);
        	}
        }
        return valueFactory.createRdfRepresentation(resource, target);
    }
    private boolean collectTriples(MGraph target, BNode node, Set<BNode> visited, Set<BNode> set){
    	visited.add(node);
        Iterator<Triple> triples = graph.filter(node, null, null);
        boolean modified = false;
        while (triples.hasNext()) {
            Triple triple = triples.next();
            target.add(triple);
            Resource object = triple.getObject();
            if(object instanceof BNode && !visited.contains(object)){
            	//an other BNode we need to follow
            	modified = set.add((BNode)object) || modified;
            }
        }
        return modified;
    }

	/* -----------------------------------------------------------------------
	 * Utils for read/write locks on the TripleCollection
	 * -----------------------------------------------------------------------
	 */

    /**
     * @return
     * @throws StoreException in case this store is read-only
     */
    private Lock writeLockGraph() throws StoreException {
        final Lock writeLock;
        if(graph instanceof LockableMGraph){
            writeLock = ((LockableMGraph)graph).getLock().writeLock();
            writeLock.lock();
        } else {
            throw new StoreException("Unable modify data in ClerezzaStore '"+name
                + "' because the backing RDF graph '"+properties.get(PROPERTY_GRAPH_URI)
                + "' is read-only!");
        }
        return writeLock;
    }
    /**
     * @return the readLock or <code>null</code>if no read lock is needed
     */
    private Lock readLockGraph() {
        final Lock readLock;
        if(graph instanceof LockableMGraph){
            readLock = ((LockableMGraph)graph).getLock().readLock();
            readLock.lock();
        } else {
            readLock = null;
        }
        return readLock;
    }
}
