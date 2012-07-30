package org.apache.stanbol.entityhub.store.jenatdb.impl;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.stanbol.commons.semanticindex.store.ChangeSet;
import org.apache.stanbol.commons.semanticindex.store.EpochException;
import org.apache.stanbol.commons.semanticindex.store.IndexingSource;
import org.apache.stanbol.commons.semanticindex.store.Store;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.utils.TimeUtils;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.datatypes.xsd.XSDDuration;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.graph.impl.LiteralLabelFactory;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import at.newmedialab.ldpath.api.backend.RDFBackend;

@Component(immediate=true,configurationFactory=true,
           policy=ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name=Store.PROPERTY_NAME,value="changeMe"),
    @Property(name=Store.PROPERTY_DESCRIPTION,value="<Optional Description>")
})
public class JenaTdbStore extends AbstractTdbBackend implements Store<Representation>, RDFBackend<Node> {

	private static final Logger log = LoggerFactory.getLogger(JenaTdbStore.class);
	
	/**
	 * The name of the Jena TDB model (the directory holding the data). 
	 * If {@link #PROPERTY_NAMED_GRAPH_STATE NamedGraph}s are enabled this will 
	 * be the default Jena TDB model of this Store implementation. Otherwise the
	 * configured {@link IndexingSource#PROPERTY_NAME name} is used as default. 
	 */
	@Property
	public static final String PROPERTY_JENA_TDB_MODEL = "org.apache.stanbol.entityhub.store.jenatdb.model";
	/**
	 * Allows to enable/disable the use of named graphs. For small and medium
	 * sized data sources one would typically like to use named graphs. For 
	 * big RDF graphs only using the default graph (deactivating named graphs)
	 * will reduce the size of the data on the disc as only SPO need to be
	 * stored.
	 */
	@Property(boolValue=JenaTdbStore.DEFAULT_NAMED_GRAPH_STATE)
	public static final String PROPERTY_NAMED_GRAPH_STATE = "org.apache.stanbol.entityhub.store.jenatdb.namedgraph.state";
	/**
	 * As a default {@link #PROPERTY_NAMED_GRAPH_STATE} is enabled
	 */
	private static final boolean DEFAULT_NAMED_GRAPH_STATE = true;
	/**
	 * The URI used for the named graph. Ignored if {@link #PROPERTY_NAMED_GRAPH_STATE}
	 * is deactivated.
	 */
	@Property
	public static final String PROPERTY_NAMED_GRAPH_URI = "org.apache.stanbol.entityhub.store.jenatdb.namedgraph.uri";
	/**
	 * Property that can not be configured, but provides the location of the
	 * TDB directory. Users can receive this by using {@link #getProperties()}
	 */
	public static final String PROPERTY_TDB_MODEL_LOCATION = "org.apache.stanbol.entityhub.store.jenatdb.modellocation";
	/**
	 * The name of the main Jena TDB model.
	 */
	protected static final String ROOT_PATH = "entityhub/store/jenatdb/";
	/**
	 * The name of the main Jena TDB model.
	 */
	public static final String MAIN_JENA_TDB_MODEL = "_main";
	
    /**
     * The valueFactory used to create {@link Representation}s, {@link Reference}s
     * and {@link Text} instances. This uses the {@link InMemoryValueFactory}
     */
    private ValueFactory valueFactory = InMemoryValueFactory.getInstance();
    /**
     * The Jena TypeMapper allows to convert Java Classes to {@link RDFDatatype}s.
     * RDFDatatypes can than be used to create {@link Node} instances representing
     * typed literals.
     */
    private TypeMapper typeMapper = TypeMapper.getInstance();
	private Dataset dataset;
	private String name;
	private String description;
	private Map<String,Object> properties;
	private Graph graph;
	/**
	 * The {@link Node} of the named graph. Will be <code>null</code> if no
	 * named graphs are used.
	 */
	private Node namedGraphNode;

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
		boolean namedGraphState;
		value = context.getProperties().get(PROPERTY_NAMED_GRAPH_STATE);
		if(value == null ||value.toString().isEmpty()){
			namedGraphState = DEFAULT_NAMED_GRAPH_STATE;
		} else if(value.getClass().isArray() || value instanceof Collection<?>){
			throw new ConfigurationException(PROPERTY_NAMED_GRAPH_STATE, "Only a single PROPERTY_NAMED_GRAPH_STATE MUST BE parsed (type: '"
					+ value.getClass() +"', values: "+value+")!");
		} else if(value instanceof Boolean){
			namedGraphState = (Boolean)value;
		} else {
			namedGraphState = Boolean.parseBoolean(value.toString());
		}
		String jenaTdbModelName;
		//parse the TDB model name
		value = context.getProperties().get(PROPERTY_JENA_TDB_MODEL);
		if(value == null ||value.toString().isEmpty()){
			jenaTdbModelName = name;
		} else if(value.getClass().isArray() || value instanceof Collection<?>){
			throw new ConfigurationException(PROPERTY_NAMED_GRAPH_STATE, "Only a single PROPERTY_NAMED_GRAPH_STATE MUST BE parsed (type: '"
					+ value.getClass() +"', values: "+value+")!");
		} else {
			jenaTdbModelName = value.toString();
		}
		if(jenaTdbModelName.equalsIgnoreCase(MAIN_JENA_TDB_MODEL)){
			throw new ConfigurationException(PROPERTY_JENA_TDB_MODEL, 
					"The configured Jena TDB model name MUST NOT be the same as the default '"
					+ MAIN_JENA_TDB_MODEL+"' (Note the name of the store is used as default if "
					+ "no specific model name is configured!");
		}
		Node namedGraphUri;
		if(namedGraphState){ //parse the URI for the named grpah
			value = context.getProperties().get(PROPERTY_NAMED_GRAPH_URI);
			if(value == null || value.toString().isEmpty()){
				namedGraphUri = Node.createURI(String.format("urn:org.apache:entityhub.store.jenatdb:%s",
						name));
			} else if(value.getClass().isArray() || value instanceof Collection<?>){
				throw new ConfigurationException(PROPERTY_NAMED_GRAPH_URI, "Only a single PROPERTY_NAMED_GRAPH_URI MUST BE parsed (type: '"
						+ value.getClass() +"', values: "+value+")!");
			} else {
				try {
					new URI(value.toString());
				} catch (URISyntaxException e) {
					throw new ConfigurationException(PROPERTY_NAMED_GRAPH_URI, "The parsed grph URI is invalid (message: "
							+ e.getMessage()+")",e);
				}
				namedGraphUri = Node.createURI(value.toString());
			}
			properties.put(PROPERTY_NAMED_GRAPH_URI, namedGraphUri.toString());
		} else {// else no named graph uri if named graphs are deactivated
			namedGraphUri = null;
		}
		
		//initialise the Jena TDB dataset/graph
		String modelRoot = FilenameUtils.concat(
				context.getBundleContext().getProperty("sling.home"),
				ROOT_PATH);
		String modelLocation;
		if(namedGraphState){
			modelLocation = FilenameUtils.concat(modelRoot, 
					jenaTdbModelName == null ? name : jenaTdbModelName);
			this.namedGraphNode = namedGraphUri; //set the node of the named graph
		} else {
			modelLocation = FilenameUtils.concat(modelRoot, 
					jenaTdbModelName == null ? MAIN_JENA_TDB_MODEL : jenaTdbModelName);
			this.namedGraphNode = null; //no named graph
		}
		properties.put(PROPERTY_TDB_MODEL_LOCATION, modelLocation);
		//init the TDB dataset
		dataset = Utils.initTDBDataset(new File(modelLocation));
		dataset.getLock().enterCriticalSection(false);
		try {
			graph = Utils.initGraph(dataset,namedGraphNode);
		} finally {
			dataset.getLock().leaveCriticalSection();
		}
		this.properties = Collections.unmodifiableMap(properties);
	}
	
	@Deactivate
	protected final void deactivate(ComponentContext context){
		TDB.sync(graph);
		graph.close();
		graph = null;
		if(namedGraphNode == null){
			TDB.sync(dataset);
			dataset.close();
		} //else ... other graphs might still be open ... do not close
		dataset = null;
		namedGraphNode = null;
		properties = null;
		description = null;
		name = null;
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
        Node resource = Node.createURI(uri);
        Representation source = valueFactory.createRepresentation(uri);
		dataset.getLock().enterCriticalSection(true);
		try {
	        ExtendedIterator<Triple> outgoing = graph.find(resource, null, null);
	        boolean found = outgoing.hasNext();
	        while(outgoing.hasNext()){ //iterate over the statements for that resource
	            Triple statement = outgoing.next();
	            Node predicate = statement.getPredicate();
	            if(predicate == null || !predicate.isURI()){
	                log.warn("Ignore field {} for resource {} because it is null or not an URI!",
	                    predicate,resource);
	            } else {
	                String field = predicate.getURI();
	                Node value = statement.getObject();
	                processValue(value, source, field);
	            } //end else predicate != null
	        } //end iteration over resource triple
	        if(found) {
	            return source;
	            //log.info("Resource: \n"+ModelUtils.getRepresentationInfo(source));
	        } else {
	            log.debug("No Statements found for Entity {}!",uri);
	            return null;
	        }
		} finally {
			dataset.getLock().leaveCriticalSection();
		}
	}

	@Override
	public long getEpoch() {
		// TODO not yet implemented
		throw new UnsupportedOperationException("TODO: implement!!");
	}

	@Override
	public ChangeSet<Representation> changes(long epoch, long revision, int batchSize)
			throws StoreException, EpochException {
		// TODO not yet implemented
		throw new UnsupportedOperationException("TODO: implement!!");
	}

	@Override
	public String put(Representation item) throws StoreException {
		dataset.getLock().enterCriticalSection(false);
		try {
			Node node = Node.createURI(item.getId());
			removeResource(node); //remove the old data
			addResource(item); //add the new data
		} finally {
			dataset.getLock().leaveCriticalSection();
		}
		return item.getId();
	}

	/**
	 * Adds an Resource based on the data of the parsed Representation. Does
	 * NOT delete any present information. Assumes a Write Lock.
	 * @param item
	 */
	private void addResource(Representation item) {
		Node subject = Node.createURI(item.getId());
		for(Iterator<String> fields =item.getFieldNames(); fields.hasNext();){
			String field = fields.next();
			Node property = Node.createURI(field);
			for(Iterator<Object> values = item.get(field);values.hasNext();){
				Object value = values.next();
				Node object = getNode(value);
				graph.add(new Triple(subject,property,object));
			}
		}
	}
	
	@Override
	public Iterable<String> put(Iterable<Representation> items) throws StoreException {
		List<String> added = new ArrayList<String>(); 
		dataset.getLock().enterCriticalSection(false);
		try {
			for(Representation item : items){
				try {
					Node node = Node.createURI(item.getId());
					removeResource(node); //remove the old data
					addResource(item); //add the new data
					added.add(item.getId());
				}catch (RuntimeException e) {
					log.warn("Unable to add Representation '"+item.getId()
							+"' Message: "+e.getMessage(),e);
				}
			}
		} finally {
			dataset.getLock().leaveCriticalSection();
		}
		return added;
	}

	@Override
	public Representation remove(String uri) throws StoreException {
		Representation removed;
		dataset.getLock().enterCriticalSection(false);
		try {
			removed = get(uri);
			if(removed != null){
				removeResource(Node.createURI(uri));
			}
		} finally {
			dataset.getLock().leaveCriticalSection();
		}
		return removed;
	}

	@Override
	public void remove(Iterable<String> uris) throws StoreException {
		dataset.getLock().enterCriticalSection(false);
		try {
			for(String uri : uris){
				removeResource(Node.createURI(uri));
			}
		} finally {
			dataset.getLock().leaveCriticalSection();
		}
		
	}

	@Override
	public void removeAll() throws StoreException {
		dataset.getLock().enterCriticalSection(false);
		try {
			graph = Utils.cleanGraph(dataset, namedGraphNode);
		} finally {
			dataset.getLock().leaveCriticalSection();
		}
	}

    /* ----------------------------------------------------------------------
     *     RDF Backend implementation
     * ----------------------------------------------------------------------
     */
    @Override
	public Collection<Node> listObjects(Node subject, Node property) {
        Collection<Node> nodes = new ArrayList<Node>();
		dataset.getLock().enterCriticalSection(true);
		ExtendedIterator<Triple> it = null;
		try {
	        it = graph.find(subject, property, null);
	        while(it.hasNext()){
	            nodes.add(it.next().getObject());
	        }
		} finally {
			if(it != null){
				it.close();
			}
			dataset.getLock().leaveCriticalSection();
		}
		return nodes;
	}

	@Override
	public Collection<Node> listSubjects(Node property, Node object) {
        Collection<Node> nodes = new ArrayList<Node>();
		dataset.getLock().enterCriticalSection(true);
		ExtendedIterator<Triple> it = null;
		try {
	        it = graph.find(null, property, object);
	        while(it.hasNext()){
	            nodes.add(it.next().getSubject());
	        }
		} finally {
			if(it != null) {
				it.close();
			}
			dataset.getLock().leaveCriticalSection();
		}
        return nodes;
	}

	/* ------------------------------------------------------------------------
	 * Methods for converting TDB values to Representation Objects
	 * ------------------------------------------------------------------------
	 */
    /**
     * Processes a {@link Node} and adds the according value to the parsed
     * Representation.
     * @param value The node to convert to an value for the Representation
     * @param source the representation (MUST NOT be <code>null</code>
     * @param field the field (MUST NOT be <code>null</code>)
     */
    private void processValue(Node value, Representation source, String field) {
        if(value == null){
            log.warn("Encountered NULL value for field {} and entity {}",
                    field,source.getId());
        } else if(value.isURI()){ //add a reference
            source.addReference(field, value.getURI());
        } else if(value.isLiteral()){ //add a value or a text depending on the dataType
            LiteralLabel ll = value.getLiteral();
//            log.debug("LL: lexical {} | value {} | dataType {} | language {}",
//                new Object[]{ll.getLexicalForm(),ll.getValue(),ll.getDatatype(),ll.language()});
            //if the dataType == null , than we can expect a plain literal
            RDFDatatype dataType = ll.getDatatype();
            if(dataType != null){ //add a value
                Object literalValue;
                try {
                    literalValue = ll.getValue();
                    if(literalValue instanceof BaseDatatype.TypedValue){
                        //used for unknown data types
                        // -> in such cases just use the lexical type
                        String lexicalValue = ((BaseDatatype.TypedValue)literalValue).lexicalValue;
                        if(lexicalValue != null && !lexicalValue.isEmpty()){
                            source.add(field,lexicalValue);
                        }
                    } else if(literalValue instanceof XSDDateTime) {
                        source.add(field, ((XSDDateTime)literalValue).asCalendar().getTime()); //Entityhub uses the time
                    } else if(literalValue instanceof XSDDuration) {
                        String duration = literalValue.toString();
                        if(duration != null && !duration.isEmpty()) {
                            source.add(field, literalValue.toString());
                        }
                    } else {
                        source.add(field, literalValue);
                    }
                } catch (DatatypeFormatException e) {
                    log.warn(" Unable to convert {} to {} -> use lecicalForm",
                        ll.getLexicalForm(),ll.getDatatype());
                    literalValue = ll.getLexicalForm();
                }
            } else { //add a text
                String lexicalForm = ll.getLexicalForm();
                if(lexicalForm != null && !lexicalForm.isEmpty()){
                    String language = ll.language();
                    if(language!=null && language.length()<1){
                        language = null;
                    }
                    source.addNaturalText(field, lexicalForm, language);
                } //else ignore empty literals
            }
            // "" is parsed if there is no language
        } else {
            if(value.isBlank()){
                log.info("ignoreing blank node value {} for field {} and Resource {}!",
                        new Object[]{value,field,source.getId()});
            } else {
                log.warn("ignoreing value {} for field {} and Resource {} because it is of an unsupported type!",
                        new Object[]{value,field,source.getId()});
            }
        } //end different value node type
    }
    /**
     * Creates Jena {@link Node} instances for {@link Representation} values
     * @param value the value
     * @return the Node
     * TODO: Error Handling
     */
	private Node getNode(Object value) {
		if(value instanceof Reference){
			return Node.createURI(((Reference)value).getReference());
		} else if(value instanceof Text){
			return Node.createLiteral(LiteralLabelFactory.create(
					((Text)value).getText(), ((Text)value).getLanguage()));
		} else if(value instanceof Date){
			//TODO: looks like we can only handle xsd:dateTime ...
			return Node.createLiteral(
					TimeUtils.toString(DataTypeEnum.DateTime, (Date)value),
					null,XSDDatatype.XSDdateTime);
		} else {//other typed Literal
			RDFDatatype dataType = typeMapper.getTypeByClass(value.getClass());
			if(dataType == null){
				dataType = XSDDatatype.XSDstring;
			}
			return Node.createLiteral(value.toString(), null, dataType);
		}
	}    

	/* -------------------------------------------------------------------------
	 * Code for removing Resources following recursively referenced BNodes.
	 * -------------------------------------------------------------------------
	 */
    
	/**
	 * Removes the {@link Representation#getId() resource} and also recursively 
	 * all blank {@link Node}s referenced by outgoing triples. This ensures that
	 * BNodes added to the graph are also deleted of the {@link Representation}
	 * is deleted.
     * <p>
     * This method assumes a read lock on {@link #graph}
	 * @param resource
	 * @return
	 */
	private boolean removeResource(Node resource) {
		ExtendedIterator<Triple> current = graph.find(resource, null, null);
		Set<Node> bNodes = new HashSet<Node>();
		boolean contains = current.hasNext();
		while(current.hasNext()){ //delete current
		    Node obj = current.removeNext().getObject();
		    if(obj.isBlank()){
		    	if(!graph.contains(null, null, obj)){
		    		bNodes.add(obj);
		    	} //other resources to refer the same BNode ... do not delete
		    }
		}
		//remove all outgoing references with BNodes as object
		while(!bNodes.isEmpty()){
			Iterator<Node> it = bNodes.iterator();
			boolean modified = false;
			while(it.hasNext() && //still more BNodes to remove
					!modified){ // if the bNodes set is modified we need a new Iterator!
				Node node = it.next();
				it.remove(); //remove the BNode before processing as afterwards
				//the Set might already be modified!
				modified = removeBNode(node,bNodes);
			}
		}
		return contains;
	}
	
	/**
	 * Removes all triples for the parsed bNode and adds all other
	 * {@link BNode}s referenced by {@link Triple#getObject()}.<p>
	 * This allows to recursively remove all BNodes of outgoing triples without
	 * recursively calling this method
	 * @param bNode the Node to be removed. It is expected to be a blank node
	 * @param set other blank {@link Node}s
	 * @return if the parsed set was modified by this method
	 */
	private boolean removeBNode(Node bNode, Set<Node> set){
		ExtendedIterator<Triple> current = graph.find(bNode, null, null);
		boolean modified = false;
		while(current.hasNext()){ //delete current
		    Node obj = current.removeNext().getObject();
		    if(obj.isBlank() && !obj.equals(bNode)){
		    	if(graph.contains(null, null, obj)){
		    		modified = set.add(obj) || modified;
		    	} //else ignore because
		    	// the Bnode of the object is the same as the subject or
		    	// an other resources to refer the same BNode
		    }
		}
		return modified;
	}
}
