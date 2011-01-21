package org.apache.stanbol.entityhub.servicesapi;

import java.util.Collection;

import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

/**
 * <p>The Entityhub defines an interface that allows to work with Content and
 * Knowledge of referenced Entities. Entities managed by this Hub are called
 * {@link Symbol}s and typically define {@link EntityMapping mappings} to
 * information provided by other {@link ReferencedSite sites}.</p>
 * <p>This interface allows to manage<ul>
 * <li>{@link Symbol}: The {@link Representation} (Content and Knowledge)
 *      available for entities managed by an entity hub</li>
 * <li>{@link EntityMapping}: External Entities that are aligned to {@link Symbol}s
 *      managed by this entity hub.</li>
 * </ul>
 * The entity hub also uses the {@link ReferencedSiteManager} to search/retrieve
 * entities/entity representations form other {@link ReferencedSite}s.<p>
 * Most of the functionality defined by this interface is also available via 
 * RESTful service.
 * TODO's<ul>
 *  <li> How to deal with content (references, base62 encoded Strings,
 *       {@link InputStream}s ...
 *  </ul>
 * @author Rupert Westenthaler
 *
 */
public interface Entityhub {

    String DEFAUTL_ENTITYHUB_PREFIX = "urn:org.apache.stanbol:entityhub";

    /**
     * Getter for the Yard storing the data (Symbols, MappedEntities) of the 
     * Entityhub.
     * @return The yard instance used to store the data of the Entityhub
     *  - the EntityhubYard
     */
    Yard getYard();

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
     * @throws EntityhubException On any error while performing the operation
     */
    Symbol lookupSymbol(String reference) throws EntityhubException;
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
     * @param create if <code>true</code> the {@link Entityhub} will try to create a
     * {@link Symbol} if necessary
     * @return the symbol or <code>null</code> if the parsed reference is not
     * known by any referenced sites.
     * @throws IllegalArgumentException If the referenced {@link Sign} was found, no
     * existing {@link EntityMapping} is present, but it is not possible to
     * create an {@link Symbol} for this {@link Sign} (normally because the
     * {@link Representation} of the {@link Sign} provides insufficient data).
     * @throws EntityhubException On any error while performing the operation
     */
    Symbol lookupSymbol(String reference, boolean create) throws IllegalArgumentException, EntityhubException;
    /**
     * Getter for a Symbol by ID. This method does only work with IDs of
     * Symbols managed by the Entityhub. To lookup Symbols by the ID of a Symbol,
     * a mappedEntity or an Entity of an referenced site use the
     * {@link #lookupSymbol(String, boolean)} method.
     * @param symbolId the ID of the Symbol
     * @return the Symbol or <code>null</code> if no {@link Symbol} with that
     * ID is managed by the Entityhub.
     * @throws IllegalArgumentException if <code>null</code> or an empty String
     * is parsed as symbolId
     * @throws EntityhubException On any error while performing the operation
     */
    Symbol getSymbol(String symbolId) throws IllegalArgumentException, EntityhubException;
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
     * @throws EntityhubException On any error while performing the operation
     */
    Symbol createSymbol(String reference) throws IllegalStateException,IllegalArgumentException,EntityhubException;
    /**
     * Getter for a MappedEntity based on the ID
     * @param id the id of the mapped entity
     * @return the MappedEntity or <code>null</code> if none was found
     * @throws EntityhubException On any error while performing the operation
     */
    EntityMapping getMappingById(String id) throws EntityhubException;
    /**
     * Getter for all mappings for a entity
     * TODO: check if an Entity can be mapped to more than one Symbol
     * @param reference the ID of the referred entity
     * @return Iterator over all the Mappings defined for this entity
     * @throws EntityhubException On any error while performing the operation
     */
    EntityMapping getMappingByEntity(String reference) throws EntityhubException;
    /**
     * Getter for the {@link FieldQueryFactory} instance of the Entityhub. Typical
     * implementation will return the factory implementation used by the current
     * {@link Yard} used by the entity hub.
     * @return the query factory
     */
    FieldQueryFactory getQueryFavtory();
    /**
     * Getter for the FieldMappings configured for this Site
     * @return The {@link FieldMapping} present for this Site.
     */
    FieldMapper getFieldMappings();
//    /**
//     * Getter for the Configuration for the entity hub
//     * @return the configuration of the entity hub
//     */
//    EntityhubConfiguration getEntityhubConfiguration();
    /**
     * Getter for all the mappings of the parsed reference to a {@link Symbol}
     * @param symbol the reference to the symbol
     * @return the mappings for the parsed Symbol
     * @throws EntityhubException On any error while performing the operation
     */
    Collection<EntityMapping> getMappingsBySymbol(String symbol) throws EntityhubException;

    /**
     * Searches for symbols based on the parsed {@link FieldQuery} and returns
     * the references (ids). Note that selected fields of the query are ignored.
     * @param query the query
     * @return the references of the found symbols
     * @throws EntityhubException On any error while performing the operation
     */
    QueryResultList<String> findSymbolReferences(FieldQuery query) throws EntityhubException;
    /**
     * Searches for symbols based on the parsed {@link FieldQuery} and returns
     * representations as defined by the selected fields of the query. Note that
     * if the query defines also {@link Constraint}s for selected fields, that
     * the returned representation will only contain values selected by such
     * constraints.
     * @param query the query
     * @return the found symbols as representation containing only the selected
     * fields and there values.
     * @throws EntityhubException On any error while performing the operation
     */
    QueryResultList<Representation> find(FieldQuery query) throws EntityhubException;
    /**
     * Searches for Signs based on the parsed {@link FieldQuery} and returns
     * the selected Signs including the whole representation. Note that selected
     * fields of the query are ignored.
     * @param query the query
     * @return All Entities selected by the Query.
     * @throws EntityhubException On any error while performing the operation
     */
    QueryResultList<Symbol> findSymbols(FieldQuery query) throws EntityhubException;

}
