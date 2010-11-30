package eu.iksproject.rick.servicesapi.site;

import java.io.IOException;
import java.io.InputStream;

import eu.iksproject.rick.servicesapi.model.Representation;

public interface ReferencedSite extends ConfiguredSite,EntityDereferencer {
	
//	/**
//	 * Whether the parsed entity ID can be dereferenced by this Dereferencer or
//	 * not.<br>
//	 * The implementation may not directly check if the parsed URI is present by
//	 * a query to the site, but only check some patterns of the parsed URI. 
//	 * @param uri the URI to be checked
//	 * @return <code>true</code> of URIs of that kind can be typically dereferenced
//	 * by this service instance. 
//	 */
//	boolean canDereference(String uri);
//	/**
//	 * Generic getter for the data of the parsed entity id
//	 * @param uri the entity to dereference
//	 * @param contentType the content type of the data
//	 * @return the data or <code>null</code> if not present or wrong data type
//	 * TODO: we should use exceptions instead of returning null!
//	 */
//	InputStream dereference(String uri,String contentType) throws IOException;
//	/**
//	 * Direct Support for Rdf Triples
//	 * TODO: do we need that, or should that be the responsibility of an other
//	 * component
//	 * TODO: I would like to remove the dependency to {@link TripleCollection} here.
//	 *       It would be better to use {@link Representation} instead!
//	 * @param uri
//	 * @return
//	 */
//	Representation dereference(String uri) throws IOException;
}
