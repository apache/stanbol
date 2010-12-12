package eu.iksproject.rick.servicesapi.site;


import java.net.URI;

import eu.iksproject.rick.servicesapi.model.EntityMapping;
import eu.iksproject.rick.servicesapi.model.Symbol;
import eu.iksproject.rick.servicesapi.yard.CacheStrategy;

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
    String ID = "eu.iksproject.rick.site.id";
    /**
     * Getter for the id of this site
     * @return
     */
    String getId();
    /**
     * The key to be used for the name of the site
     */
    String NAME = "eu.iksproject.rick.site.name";
    /**
     * The preferred name of this site (if not present use the id)
     * @return the name (or if not defined the id) of the site
     */
    String getName();
    /**
     * The key to be used for the site description
     */
    String DESCRIPTION = "eu.iksproject.rick.site.description";
    /**
     * Getter for the default short description of this site.
     * @return The description or <code>null</code> if non is defined
     */
    String getDescription();

    /**
     * Key used for the configuration of the AccessURI  for a site
     */
    String ACCESS_URI = "eu.iksproject.rick.site.accessUri";
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
    String DEREFERENCER_TYPE = "eu.iksproject.rick.site.dereferencerType";
    /**
     * The name of the {@link EntityDereferencer} to be used for accessing
     * representations of entities managed by this Site
     * TODO: need to be reviewed how to link dereferencing functionality to
     * configured Sites.
     * @return
     */
    String getDereferencerType();
    /**
     * Key used for the configuration of the default {@link SymbolState} for a site
     */
    String DEFAULT_SYMBOL_STATE = "eu.iksproject.rick.site.defaultSymbolState";
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
    String DEFAULT_MAPEED_ENTITY_STATE = "eu.iksproject.rick.site.defaultMappedEntityState";
    /**
     * The initial state for mappings of entities managed by this site
     * @return the default state for mappings to entities of this site
     */
    EntityMapping.MappingState getDefaultMappedEntityState();

    /**
     * Key used for the configuration of the default expiration duration for entities and
     * data for a site
     */
    String DEFAULT_EXPIRE_DURATION = "eu.iksproject.rick.site.defaultExpireDuration";
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
    String CACHE_STRATEGY = "eu.iksproject.rick.site.cacheStrategy";
    /**
     * The cache strategy used by for this site to be used.
     * @return the cache strategy
     */
    CacheStrategy getCacheStrategy();
    /**
     * Key used for the configuration of prefixes used by Entities managed by this Site
     */
    String ENTITY_PREFIX = "eu.iksproject.rick.site.entityPrefix";
    /**
     * Getter for the prefixes of entities managed by this Site
     * @return the entity prefixes. In case there are non an empty array is returned.
     */
    String[] getEntityPrefixes();
}
