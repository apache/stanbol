package eu.iksproject.rick.servicesapi;

import java.util.Collection;

import eu.iksproject.rick.servicesapi.mapping.FieldMapper;
import eu.iksproject.rick.servicesapi.mapping.FieldMapping;
import eu.iksproject.rick.servicesapi.model.EntityMapping;
import eu.iksproject.rick.servicesapi.model.Representation;
import eu.iksproject.rick.servicesapi.model.Sign;
import eu.iksproject.rick.servicesapi.model.Symbol;
import eu.iksproject.rick.servicesapi.query.Constraint;
import eu.iksproject.rick.servicesapi.query.FieldQuery;
import eu.iksproject.rick.servicesapi.query.FieldQueryFactory;
import eu.iksproject.rick.servicesapi.query.QueryResultList;
import eu.iksproject.rick.servicesapi.yard.Yard;

/**
 * <p>The Rick manages all management specific metadata for the <b>R</b>eference
 * <b>I</b>nfrastrucutre for <b>C</b>ontent and <b>K</b>nowledge.</p>
 * <p>It allows to register/create
 * <ul>
 * <li>{@link Symbol}: Entities as defined by the local CMS</li>
 * <li>{@link EntityMapping}: External Entities that are aligned to {@link Symbol}
 *      used by the local CMS</li>
 * <li> {@link Sites}: External sites that manage entities. Such sites also define 
 *      default valued for MappedEntities and Symbols created based on Entities
 *      they manage</li>
 * </ul>
 * 
 * TODO's<ul>
 *  <li> Query Interface for Symbols and Entities. Probably a 
 *       field -> value pattern based query language would be a good start
 *       (e.g. label="Par*" type="ns:Location")
 *  <li> How to deal with content (references, base62 encoded Strings,
 *       {@link InputStream}s ...
 *  <li> Serialising of Representations to different Formats (especially RDF and
 *       JSON)
 *  </ul>
 * @author Rupert Westenthaler
 *
 */
public interface Rick {
	
	String DEFAUTL_RICK_PREFIX = "urn:eu.iksproject:rick";
	
	/**
	 * Getter for the Yard storing the data (Symbols, MappedEntities) of the Rick.
	 * @return The yard instance used to store the data of the Rick - the RickYard
	 */
	public Yard getRickYard();

	/**
	 * Getter for an Symbol for an reference to a {@link Sign}. If parsed
	 * reference refers a Symbol, than this Symbol is returned. In any other
	 * case this Method searches if the parsed reference is mapped to a
	 * Symbol and returns the Symbol instead. To check if the parsed reference
	 * is a {@link Symbol} simple check if {@link Symbol#getId()} equals to the
	 * parsed reference.
	 * @param reference the id of the referenced Sign
	 * @return the symbol representing the parsed entity or <code>null</code> if
	 * no symbol for the parsed entity is available
	 * @throws RickException On any error while performing the operation
	 */
	Symbol lookupSymbol(String reference) throws RickException;
	/**
	 * Getter for an Symbol for an reference to a {@link Sign}. If parsed
	 * reference refers a Symbol, than this Symbol is returned. In any other
	 * case this Method searches if the parsed reference is mapped to a
	 * Symbol and returns the Symbol instead. To check if the parsed reference
	 * is a {@link Symbol} simple check if {@link Symbol#getId()} equals to the
	 * parsed reference.<br>
	 * If <code>create=true</code> and no {@link EntityMapping} is present for
	 * the parsed reference, than a new Symbol is created and returned.
	 * 
	 * @param reference the id of the referenced Sign
	 * @param create if <code>true</code> the {@link Rick} will try to create a
	 * {@link Symbol} if necessary
	 * @return the symbol or <code>null</code> if the parsed reference is not
	 * known by any referenced sites.
	 * @throws IllegalArgumentException If the referenced {@link Sign} was found, no
	 * existing {@link EntityMapping} is present, but it is not possible to
	 * create an {@link Symbol} for this {@link Sign} (normally because the
	 * {@link Representation} of the {@link Sign} provides insufficient data).
	 * @throws RickException On any error while performing the operation
	 */
	Symbol lookupSymbol(String reference, boolean create) throws IllegalArgumentException, RickException;
	/**
	 * Getter for a Symbol by ID. This method does only work with IDs of
	 * Symbols managed by the Rick. To lookup Symbols by the ID of a Symbol,
	 * a mappedEntity or an Entity of an referenced site use the
	 * {@link #lookupSymbol(String, boolean)} method.
	 * @param symbolId the ID of the Symbol
	 * @return the Symbol or <code>null</code> if no {@link Symbol} with that
	 * ID is managed by the Rick.
	 * @throws IllegalArgumentException if <code>null</code> or an empty String
	 * is parsed as symbolId
	 * @throws RickException On any error while performing the operation
	 */
	Symbol getSymbol(String symbolId) throws IllegalArgumentException, RickException;
	/**
	 * Creates a Symbol for the parsed reference. If there is already a Symbol
	 * present for the parsed reference, than this Method throws an
	 * {@link IllegalStateException}. If no Sign can be found for the parsed
	 * Reference, than <code>null</code> is returned. 
	 * If the referenced {@link Sign} provides
	 * insufficient data to create a {@link Symbol}, than an 
	 * {@link IllegalArgumentException} is thrown.
	 * @param reference the id of the {@link Sign}
	 * @return the Symbol or <code>null</code> if the no {@link Sign} was found
	 * for the parsed reference.
	 * @throws IllegalStateException if there exists already a {@link Symbol} for
	 * the parsed reference
	 * @throws IllegalArgumentException  If the referenced {@link Sign} was found, no
	 * existing {@link EntityMapping} is present, but it is not possible to
	 * create an {@link Symbol} for this {@link Sign} (normally because the
	 * {@link Representation} of the {@link Sign} provides insufficient data).
	 * @throws RickException On any error while performing the operation
	 */
	Symbol createSymbol(String reference) throws IllegalStateException,IllegalArgumentException,RickException;
	/**
	 * Getter for a MappedEntity based on the ID
	 * @param id the id of the mapped entity
	 * @return the MappedEntity or <code>null</code> if none was found
	 * @throws RickException On any error while performing the operation
	 */
	EntityMapping getMappingById(String id) throws RickException;
	/**
	 * Getter for all mappings for a entity
	 * TODO: check if an Entity can be mapped to more than one Symbol
	 * @param reference the ID of the referred entity
	 * @return Iterator over all the Mappings defined for this entity
	 * @throws RickException On any error while performing the operation
	 */
	EntityMapping getMappingByEntity(String reference) throws RickException;
	/**
	 * Getter for the {@link FieldQueryFactory} instance of the Rick. Typical
	 * implementation will return the factory of the RickYard.
	 * @return the query factory
	 */
	FieldQueryFactory getQueryFavtory();
	/**
	 * Getter for the FieldMappings configured for this Site
	 * @return The {@link FieldMapping} present for this Site.
	 */
	FieldMapper getFieldMappings();
//	/**
//	 * Getter for the Configuration for the RICK
//	 * @return the configuration of the RICK
//	 */
//	RickConfiguration getRickConfiguration();
	/**
	 * Getter for all the mappings of the parsed reference to a {@link Symbol}
	 * @param symbol the reference to the symbol
	 * @return the mappings for the parsed Symbol
	 * @throws RickException On any error while performing the operation
	 */
	public Collection<EntityMapping> getMappingsBySymbol(String symbol) throws RickException;
	
	/**
	 * Searches for symbols based on the parsed {@link FieldQuery} and returns
	 * the references (ids). Note that selected fields of the query are ignored.
	 * @param query the query
	 * @return the references of the found symbols
	 * @throws RickException On any error while performing the operation
	 */
	QueryResultList<String> findSymbolReferences(FieldQuery query) throws RickException;
	/**
	 * Searches for symbols based on the parsed {@link FieldQuery} and returns
	 * representations as defined by the selected fields of the query. Note that
	 * if the query defines also {@link Constraint}s for selected fields, that
	 * the returned representation will only contain values selected by such
	 * constraints.
	 * @param query the query
	 * @return the found symbols as representation containing only the selected
	 * fields and there values.
	 * @throws RickException On any error while performing the operation
	 */
	QueryResultList<Representation> find(FieldQuery query) throws RickException;
	/**
	 * Searches for Signs based on the parsed {@link FieldQuery} and returns
	 * the selected Signs including the whole representation. Note that selected 
	 * fields of the query are ignored.
	 * @param query the query
	 * @return All Entities selected by the Query.
	 * @throws RickException On any error while performing the operation
	 */
	QueryResultList<Symbol> findSymbols(FieldQuery query) throws RickException;

}
