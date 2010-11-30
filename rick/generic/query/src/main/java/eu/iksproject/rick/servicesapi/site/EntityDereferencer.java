package eu.iksproject.rick.servicesapi.site;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

import eu.iksproject.rick.servicesapi.Rick;
import eu.iksproject.rick.servicesapi.model.Representation;
import eu.iksproject.rick.servicesapi.yard.Yard;

/**
 * The {@link ConfiguredSite} defines what external data sources (sites) can
 * be used to access entities. Sites are identified and registers with the
 * {@link ReferencedSiteManager}. The baseURI is therefore the ID of a Site. Site 
 * Implementations are responsible for the access and the retrieval of data.<p> 
 * Users typically need not to border with this Interface, because they use
 * the {@link Rick} to access Symbols, map Entities or retrieve Representations.
 * The {@link Rick} is responsible to forward requests to the {@link Yard} (local
 * cache) or directly to the corresponding {@link EntityDereferencer}. 
 * @author Rupert Westenthaler
 *
 */
public interface EntityDereferencer {
	/**
	 * The key used to define the baseUri for instances of this Service.
	 * This constants actually uses the value of {@link ReferencedSite#ACCESS_URI}
	 */
	String ACCESS_URI = ConfiguredSite.ACCESS_URI;

	/**
	 * The base uri used to access this site
	 * @return
	 */
	String getAccessUri();
	/**
	 * Whether the parsed entity ID can be dereferenced by this Dereferencer or
	 * not.<br>
	 * The implementation may not directly check if the parsed URI is present by
	 * a query to the site, but only check some patterns of the parsed URI. 
	 * @param uri the URI to be checked
	 * @return <code>true</code> of URIs of that kind can be typically dereferenced
	 * by this service instance. 
	 */
	boolean canDereference(String uri);
	/**
	 * Generic getter for the data of the parsed entity id
	 * @param uri the entity to dereference
	 * @param contentType the content type of the data
	 * @return the data or <code>null</code> if not present or wrong data type
	 * TODO: we should use exceptions instead of returning null!
	 */
	InputStream dereference(String uri,String contentType) throws IOException;
	/**
	 * Dereferences the Representation of the referred Entity
	 * @param uri the uri of the referred entity
	 * @return the representation of <code>null</code> if no Entity was found
	 * for the parsed entity reference.
	 */
	Representation dereference(String uri) throws IOException;
	
//	/**
//	 * NOTE Moved to ReferencedSite
//	 * @return
//	 */
//	Dictionary<String, ?> getSiteConfiguration();
	
}
