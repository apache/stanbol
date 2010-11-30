package eu.iksproject.rick.servicesapi.site;

import java.util.Collection;
import java.util.Dictionary;

public interface ReferencedSiteManager {
	
	/**
	 * Returns if a site with the parsed id is referenced
	 * @param baseUri the base URI
	 * @return <code>true</code> if a site with the parsed ID is present. 
	 * Otherwise <code>false</code>.
	 */
	boolean isReferred(String id);
	
	/**
	 * Getter for the referenced site based on the id
	 * @param baseUri the base URI of the referred Site
	 * @return the {@link ReferencedSite} or <code>null</code> if no site is 
	 * present for the parsed base ID.
	 */
	ReferencedSite getReferencedSite(String id);
	
	/**
	 * TODO's:
	 * <ul> 
	 *  <li> use the ReferenceManager specific Exception to this Method
	 *  <li> maybe use an own data structure instead of the generic Dictionary class
	 *  <li> review the API based creation of Sites
	 *  </ul>
	 * @param baseUri
	 * @param properties
	 */
	void addReferredSite(String baseUri,Dictionary<String,?> properties);
	/**
	 * Getter for Sites that manages entities with the given ID. A Site can 
	 * define a list of prefixes of Entities ID it manages. This method can
	 * be used to retrieve all the Site that may be able to dereference the
	 * parsed entity id
	 * @param entityUri the ID of the entity
	 * @return A list of referenced sites that may manage the entity in question.
	 */
	Collection<ReferencedSite> getReferencedSitesByEntityPrefix(String entityUri);

	/*
	 * NOTE: We need a way to add/remove referred sites. But this may be done
	 * via the OSGI ManagedServiceFactory interface. Implementations of the
	 * Site Interface would than also implement the ManagedService interface
	 */

}
