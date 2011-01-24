package org.apache.stanbol.entityhub.servicesapi.site;

import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;

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
     * @deprecated Not yet clear how to add referred sites via an API
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

    /**
     * Getter for the the Sign referenced by the parsed ID
     * @param id the id of the entity
     * @return the Sign or <code>null</code> if not found
     */
    Sign getSign(String reference);

    /**
     * Returns the Entities that confirm to the parsed Query
     * @param query the query
     * @return the id's of Entities
     */
    QueryResultList<Sign> findEntities(FieldQuery query);
    /**
     * Searches for Entities based on the parsed query and returns representations
     * including the selected fields and filtered values
     * @param query The query
     * @return The representations including selected fields/values
     */
    QueryResultList<Representation> find(FieldQuery query);
    /**
     * Searches for Entities based on the parsed query and returns the ids.
     * @param query The query
     * @return the ids of the selected entities
     */
    QueryResultList<String> findIds(FieldQuery query);
    /**
     * Getter for the content of the entity
     * @param entity the id of the entity
     * @param contentType the content type
     * @return the content as {@link InputStream} or <code>null</code> if no
     * entity with this ID is known by the Entityhub or no representation for 
     * the requested entity is available for the parsed content type
     */
    InputStream getContent(String entity,String contentType);
    /**
     * Getter for the Id's of all active referenced sites
     * @return Unmodifiable collections of the id#s of all referenced sites
     */
    Collection<String> getReferencedSiteIds();

    /*
     * NOTE: We need a way to add/remove referred sites. But this may be done
     * via the OSGI ManagedServiceFactory interface. Implementations of the
     * Site Interface would than also implement the ManagedService interface
     */

}
