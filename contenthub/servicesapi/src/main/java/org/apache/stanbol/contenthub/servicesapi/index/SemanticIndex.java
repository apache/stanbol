package org.apache.stanbol.contenthub.servicesapi.index;

import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Component;
import org.apache.stanbol.contenthub.servicesapi.store.ChangeSet;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public interface SemanticIndex {
	
	String PARAMETER_NAME = "stanbol.contenthub.index.name";
	String PARAMETER_DESCRIPTION = "stanbol.contenthub.index.description";
	
	/**
	 * The name of the Semantic Index. The same as configured by the
	 * {@link #PARAMETER_NAME} property in the OSGI component configuration
	 * @return the name;
	 */
    String getName();
    
	/**
	 * The description for the Semantic Index. The same as configured by the
	 * {@link #PARAMETER_DESCRIPTION} property in the OSGI component configuration
	 * @return the name;
	 */
    String getDescription();
    /**
     * Indexes the parsed ContentItem
     * @param ci the contentItem
     * @return <code>true</true> if the ConentItem was included in the index.
     * <code>false</code> if the ContentItem was ignored (e.g. filtered based
     * on the indexing rules).
     * @throws IndexException On any error while accessing the semantic index 
     */
    boolean index(ContentItem ci) throws IndexException;
    /**
     * Removes the {@link ContentItem} with the parsed {@link UriRef} from
     * this index. If the no content item with the parsed uri is
     * present in this index the call can be ignored.
     * @param ciURI the uri of the content item to remove
     * @throws IndexException On any error while accessing the semantic index 
     */
    void remove(UriRef ciURI) throws IndexException;
    /**
     * Persists all changes to the index and sets the revision to the parsed
     * value if the operation succeeds. 
     * <p>
     * TODO: The {@link ChangeSet} interface does NOT provide revisions for
     * each changed ContentItem but only for the whole Set. So this means that
     * this method can only be called after indexing the whole {@link ChangeSet}.
     * This might be OK, but needs further investigation (rwesten)
     * 
     * @param revision the revision
     * @throws IndexException On any error while accessing the semantic index 
     */
    void persist(long revision) throws IndexException;
    /**
     * Getter for the current revision of this SemanticIndex
     * @return the revision number or {@link Long#MIN_VALUE} if none.
     */
    long getRevision();
    /**
     * Getter for the list of fields supported by this semantic index. This
     * information is optional. Implementations that does not support this
     * can indicate that by returning <code>null</code>.
     * @return the list of filed names or <code>null</code> if not available
     */
    List<String> getFieldsNames();
    /**
     * Getter for the properties describing a specific field supported by
     * this index. Names can be retrieved by using {@link #getFieldsNames()}.
     * This information is optional. Implementations that do not support this
     * can indicate this by returning <code>null</code>.<p>
     * The keys of the returned map represent the properties. Values the
     * actual configuration of the property.
     * @param name the field name
     * @return the field properties or <code>null</code> if not available.
     */
    Map<String,Object> getFieldProperties(String name);
    
    /**
     * Getter for the RESTful search interfaces supported by this semantic index.
     * The keys represent the types of the RESTful Interfaces. See
     * the {@link EndpointType} enumeration for knows keys. The 
     * value is the URL of that service relative to to the Stanbol
     * base URI
     * @return the RESTful search interfaces supported by this semantic index.
     */
    Map<String, String> getRESTSearchEndpoints();
    /**
     * Getter for the Java search APIs supported by this semantic index.
     * The keys are the java interfaces and values are OSGI
     * {@link ServiceReference}s. This also means that instead of
     * using this method such components can be accesses by using a
     * filter on <ul>
     * <li>{@link #PARAMETER_NAME} = {@link #getName()}
     * <li> {@link Constants#OBJECTCLASS} = {@link Class#getName()}
     * </ul>
     * @return the Java search APIs supported by this semantic index.
     * Also registered as OSGI services.
     */
    Map<Class<?>, ServiceReference> getSearchEndPoints();
}
