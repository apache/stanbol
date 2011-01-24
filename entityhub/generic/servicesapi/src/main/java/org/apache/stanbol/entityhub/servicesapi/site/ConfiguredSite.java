package org.apache.stanbol.entityhub.servicesapi.site;


import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol.SymbolState;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

/**
 * This interface defines the getter as well as the property keys for the
 * configuration of a {@link ReferencedSite}.<p>
 *
 * TODO: No Idea how to handle that in an OSGI context.
 * @author Rupert Westenthaler
 *
 */
public interface ConfiguredSite {

    /**
     * The key to be used for the site id
     */
    String ID = "org.apache.stanbol.entityhub.site.id";
    /**
     * Getter for the id of this site
     * @return
     */
    String getId();
    /**
     * The key to be used for the name of the site
     */
    String NAME = "org.apache.stanbol.entityhub.site.name";
    /**
     * The preferred name of this site (if not present use the id)
     * @return the name (or if not defined the id) of the site
     */
    String getName();
    /**
     * The key to be used for the site description
     */
    String DESCRIPTION = "org.apache.stanbol.entityhub.site.description";
    /**
     * Getter for the default short description of this site.
     * @return The description or <code>null</code> if non is defined
     */
    String getDescription();

    /**
     * Key used for the configuration of the AccessURI  for a site
     */
    String ACCESS_URI = "org.apache.stanbol.entityhub.site.accessUri";
    /**
     * The URI used to access the Data of this Site. This is usually a different
     * URI as the ID of the site.<p>
     *
     * To give some Examples: <p>
     *
     * symbol.label: DBPedia<br>
     * symbol.id: http://dbpedia.org<br>
     * site.acessUri: http://dbpedia.org/resource/<p>
     *
     * symbol.label: Freebase<br>
     * symbol.id: http://www.freebase.com<br>
     * site.acessUri: http://rdf.freebase.com/<p>
     *
     * @return the accessURI for the data of the referenced site
     */
    String getAccessUri();
    /**
     * Key used for the configuration of the name of the dereferencer type to be
     * used for this site
     */
    String DEREFERENCER_TYPE = "org.apache.stanbol.entityhub.site.dereferencerType";
    /**
     * The name of the {@link EntityDereferencer} to be used for accessing
     * representations of entities managed by this Site
     * @return the id of the entity dereferencer implementation
     */
    String getDereferencerType();
    /**
     * Key used for the configuration of the uri to access the query service of
     * the site
     */
    String QUERY_URI = "org.apache.stanbol.entityhub.site.queryUri";
    /**
     * Getter for the queryUri of the site. IF not defined the {@link #ACCESS_URI}
     * is used.
     * @return the uri to access the query service of this site
     */
    String getQueryUri();
    /**
     * Key used for the configuration of the type of the query
     */
    String SEARCHER_TYPE = "org.apache.stanbol.entityhub.site.searcherType";
    /**
     * The name of the {@link EntitySearcher} to be used to query for
     * representations of entities managed by this Site.
     * @return the id of the entity searcher implementation.
     */
    String getQueryType();
    /**
     * Key used for the configuration of the default {@link SymbolState} for a site
     */
    String DEFAULT_SYMBOL_STATE = "org.apache.stanbol.entityhub.site.defaultSymbolState";
    /**
     * The initial state if a {@link Symbol} is created for an entity managed
     * by this site
     * @return the default state for new symbols
     */
    Symbol.SymbolState getDefaultSymbolState();
    /**
     * Key used for the configuration of the default {@link EntityMapping} state
     * ({@link EntityMapping.MappingState} for a site
     */
    String DEFAULT_MAPEED_ENTITY_STATE = "org.apache.stanbol.entityhub.site.defaultMappedEntityState";
    /**
     * The initial state for mappings of entities managed by this site
     * @return the default state for mappings to entities of this site
     */
    EntityMapping.MappingState getDefaultMappedEntityState();

    /**
     * Key used for the configuration of the default expiration duration for entities and
     * data for a site
     */
    String DEFAULT_EXPIRE_DURATION = "org.apache.stanbol.entityhub.site.defaultExpireDuration";
    /**
     * Return the duration in milliseconds or values <= 0 if mappings to entities
     * of this Site do not expire.
     * @return the duration in milliseconds or values <=0 if not applicable.
     */
    long getDefaultExpireDuration();
    /**
     * Key used for the configuration of the default expiration duration for entities and
     * data for a site
     */
    String CACHE_STRATEGY = "org.apache.stanbol.entityhub.site.cacheStrategy";
    /**
     * The cache strategy used by for this site to be used.
     * @return the cache strategy
     */
    CacheStrategy getCacheStrategy();

    /**
     * The key used for the configuration of the id for the yard used as a cache
     * for the data of a referenced Site. This property is ignored if
     * {@link CacheStrategy#none} is used.
     */
    String CACHE_ID = "org.apache.stanbol.entityhub.site.cacheId";

    /**
     * The id of the Yard used to cache data of this referenced site.
     * @return the id of the {@link Yard} used as a cache. May be <code>null</code>
     * if {@link CacheStrategy#none} is configured for this yard
     */
    String getCacheId();

    /**
     * Key used for the configuration of prefixes used by Entities managed by this Site
     */
    String ENTITY_PREFIX = "org.apache.stanbol.entityhub.site.entityPrefix";
    /**
     * Getter for the prefixes of entities managed by this Site
     * @return the entity prefixes. In case there are non an empty array is returned.
     */
    String[] getEntityPrefixes();
    /**
     * The key used to configure the FieldMappings for a Site. Note that values
     * are parsed by using {@link FieldMapping#parseFieldMapping(String)}
     */
    String SITE_FIELD_MAPPINGS = "org.apache.stanbol.entityhub.site.fieldMappings";
    /**
     * The {@link FieldMapper} as configured for this Site.
     * @return the FieldMappings
     */
    FieldMapper getFieldMapper();
}
