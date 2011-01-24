package org.apache.stanbol.entityhub.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.site.AbstractEntityDereferencer;
import org.apache.stanbol.entityhub.core.utils.ModelUtils;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.ConfiguredSite;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteException;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This in the Default implementation of the {@link ReferencedSite} interface.
 * However this implementation forwards calls to methods defined within the
 * {@link EntityDereferencer} and {@link EntitySearcher} to sub components
 * (See the detailed description below).<p>
 * Each {@link ReferencedSite} with an {@link CacheStrategy} other than
 * {@link CacheStrategy#none} needs an associated {@link Cache}.
 * <p>
 * The Initialisation of the sub-components:
 * <ul>
 * <li> <b>{@link EntityDereferencer}:</b> Implementations of this interface are
 *      specific to the used protocol/technology of the referenced site.
 *      Because of that calls to methods defined in this interface are forwarded
 *      to an site specific instance of the {@link EntityDereferencer} interface
 *      as configured by the {@link ConfiguredSite#DEREFERENCER_TYPE} property.<br>
 *      During activation the the {@link BundleContext} is used to
 *      search for {@link ComponentFactory} with the configuration <code>
 *      "component.name= {@link ComponentContext#getProperties()}.get(
 *      {@link ConfiguredSite#DEREFERENCER_TYPE})</code>. This factory is used
 *      to create an instance of {@link EntityDereferencer}. <br>
 *      Note also, that the configuration of this instance that is covered
 *      by the {@link ConfiguredSite} interface are parsed to the
 *      {@link EntityDereferencer} instance.
 * <li> <b> {@link EntitySearcher}:</b> Implementations of this interface are
 *      also specific to the used protocol/technology of the referenced site.
 *      Because of that calls to methods defined in this interface are forwarded
 *      to an site specific instance of the {@link EntitySearcher} interface
 *      as configured by the {@link ConfiguredSite#SEARCHER_TYPE} property.<br>
 *      The initialisation of this instance works similar as described for the
 *      {@link EntityDereferencer}. However if the value of the {@link ConfiguredSite#SEARCHER_TYPE}
 *      is equals to {@link ConfiguredSite#DEREFERENCER_TYPE} or the
 *      {@link ConfiguredSite#SEARCHER_TYPE} is not defined at all, than the
 *      Dereferencer Instance is also used as {@link EntitySearcher}. If the
 *      according cast does not succeed, an {@link ConfigurationException} for the
 *      {@link ConfiguredSite#SEARCHER_TYPE} property is thrown.
 * <li> <b>{@link Cache}: </b> An instance of a {@link Cache} is used to
 *      cache {@link Representation}s loaded form the Site. A cache is a wrapper
 *      over a {@link Yard} instance that allows to configure what data are
 *      stored for each representation cached form this referenced site. A
 *      {@link ServiceTracker} is used for managing the dependency with the cache.
 *      So if a cache is no longer available a referenced site can still be used -
 *      only the local cache can not be used to retrieve entity representations.
 * </ul>
 *
 * TODO: implement {@link MetaTypeProvider} for this Component!
 * The Goal is to dynamically provide the PropertyOptions for
 *  - Properties that use Enumerations
 *  - available EntityDereferencer Types
 *  - available EntitySearcher Types
 * @author Rupert Westenthaler
 *
 */
@Component(
        name="org.apache.stanbol.entityhub.site.referencedSite",
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE, //the baseUri is required!
        specVersion="1.1",
        metatype = true,
        immediate = true
        )
@Service(value=ReferencedSite.class)
@Properties(value={
        @Property(name=ConfiguredSite.ID,value="dbPedia"),
        @Property(name=ConfiguredSite.NAME,value="DB Pedia"),
        @Property(name=ConfiguredSite.DESCRIPTION, value="The OLD Endpoint for Wikipedia"),
        /*
         * TODO: can't use Integer.MAX_VALUE here, because I get a NumberFormatException
         * in den maven scr plugin. For now use a big number instead
         */
        @Property(name=ConfiguredSite.ENTITY_PREFIX, cardinality=10000, value={
                "http://dbpedia.org/resource/","http://dbpedia.org/ontology/"
        }),
        @Property(name=ConfiguredSite.ACCESS_URI, value="http://dbpedia.org/sparql/"),
        @Property(name=ConfiguredSite.DEREFERENCER_TYPE,
            options={
                @PropertyOption(
                        value='%'+ConfiguredSite.DEREFERENCER_TYPE+".option.none",
                        name=""),
                @PropertyOption(
                    value='%'+ConfiguredSite.DEREFERENCER_TYPE+".option.sparql",
                    name="org.apache.stanbol.entityhub.site.SparqlDereferencer"),
                @PropertyOption(
                        value='%'+ConfiguredSite.DEREFERENCER_TYPE+".option.coolUri",
                        name="org.apache.stanbol.entityhub.site.CoolUriDereferencer")
            },value="org.apache.stanbol.entityhub.site.SparqlDereferencer"),
        @Property(name=ConfiguredSite.QUERY_URI, value="http://dbpedia.org/sparql"), //the deri server has better performance
        @Property(name=ConfiguredSite.SEARCHER_TYPE,
            options={
                @PropertyOption(
                        value='%'+ConfiguredSite.SEARCHER_TYPE+".option.none",
                        name=""),
                @PropertyOption(
                    value='%'+ConfiguredSite.SEARCHER_TYPE+".option.sparql",
                    name="org.apache.stanbol.entityhub.site.SparqlSearcher"),
                @PropertyOption(
                        value='%'+ConfiguredSite.SEARCHER_TYPE+".option.sparql-virtuoso",
                        name="org.apache.stanbol.entityhub.site.VirtuosoSearcher"),
                @PropertyOption(
                        value='%'+ConfiguredSite.SEARCHER_TYPE+".option.sparql-larq",
                        name="org.apache.stanbol.entityhub.site.LarqSearcher")
            },value="org.apache.stanbol.entityhub.site.VirtuosoSearcher"),
        @Property(name=ConfiguredSite.DEFAULT_SYMBOL_STATE,
            options={
                @PropertyOption( //seems, that name and value are exchanged ...
                        value='%'+ConfiguredSite.DEFAULT_SYMBOL_STATE+".option.proposed",
                        name="proposed"),
                @PropertyOption(
                        value='%'+ConfiguredSite.DEFAULT_SYMBOL_STATE+".option.active",
                        name="active")
                //the other states make no sense for new symbols
            }, value="proposed"),
        @Property(name=ConfiguredSite.DEFAULT_MAPEED_ENTITY_STATE,
            options={
                @PropertyOption(
                        value='%'+ConfiguredSite.DEFAULT_MAPEED_ENTITY_STATE+".option.proposed",
                        name="proposed"),
                @PropertyOption(
                        value='%'+ConfiguredSite.DEFAULT_MAPEED_ENTITY_STATE+".option.confirmed",
                        name="confirmed")
                //the other states make no sense for new symbols
            }, value="proposed"),
        @Property(name=ConfiguredSite.DEFAULT_EXPIRE_DURATION,
            options={
                @PropertyOption(
                        value='%'+ConfiguredSite.DEFAULT_EXPIRE_DURATION+".option.oneMonth",
                        name=""+(1000L*60*60*24*30)),
                @PropertyOption(
                        value='%'+ConfiguredSite.DEFAULT_EXPIRE_DURATION+".option.halfYear",
                        name=""+(1000L*60*60*24*183)),
                @PropertyOption(
                        value='%'+ConfiguredSite.DEFAULT_EXPIRE_DURATION+".option.oneYear",
                        name=""+(1000L*60*60*24*365)),
                @PropertyOption(
                        value='%'+ConfiguredSite.DEFAULT_EXPIRE_DURATION+".option.none",
                        name="0")
            }, value="0"),
        @Property(name=ConfiguredSite.CACHE_STRATEGY,
            options={
                @PropertyOption(
                        value='%'+ConfiguredSite.CACHE_STRATEGY+".option.none",
                        name="none"),
                @PropertyOption(
                        value='%'+ConfiguredSite.CACHE_STRATEGY+".option.used",
                        name="used"),
                @PropertyOption(
                        value='%'+ConfiguredSite.CACHE_STRATEGY+".option.all",
                        name="all")
            }, value="none"),
        @Property(name=ConfiguredSite.CACHE_ID),
        @Property(name=ConfiguredSite.SITE_FIELD_MAPPINGS,cardinality=1000, //positive number to use an Array
            value={
                "dbp-ont:*",
                "dbp-ont:thumbnail | d=xsd:anyURI > foaf:depiction",
                "dbp-prop:latitude | d=xsd:decimal > geo:lat",
                "dbp-prop:longitude | d=xsd:decimal > geo:long",
                "dbp-prop:population | d=xsd:integer",
                "dbp-prop:website | d=xsd:anyURI > foaf:homepage"
            })
        })
public class ReferencedSiteImpl implements ReferencedSite {
    static final int maxInt = Integer.MAX_VALUE;
    protected final Logger log;
    protected ComponentContext context;
    protected Dictionary<String,?> properties;
    protected FieldMapper fieldMappings;

    private final Object searcherAndDereferencerLock = new Object();
    private Boolean dereferencerEqualsEntitySearcherComponent;
    private ComponentFactoryListener dereferencerComponentFactoryListener;
    private ComponentFactoryListener searcherComponentFactoryListener;

    private String dereferencerComponentName;
    private ComponentInstance dereferencerComponentInstance;
    protected EntityDereferencer dereferencer;

    private String entitySearcherComponentName;
    private EntitySearcher entitySearcher;
    private ComponentInstance entitySearcherComponentInstace;

    private String accessUri;
    private String queryUri;
    private CacheStrategy cacheStrategy;
    private String cacheId;
    private ServiceTracker cacheTracker;

    public ReferencedSiteImpl(){
        this(LoggerFactory.getLogger(ReferencedSiteImpl.class));
    }
    protected ReferencedSiteImpl(Logger log){
        this.log = log;
           log.info("create instance of "+this.getClass().getName());
    }



    @Override
    public final String getAccessUri() {
        return accessUri;
    }

    @Override
    public final CacheStrategy getCacheStrategy() {
        return cacheStrategy;
    }

    /**
     * This implementation returns the ExpireDuration. 0 as default if no
     * configuration is present. -1 in case the configuration can not be converted
     * to a number.
     */
    @Override
    public final long getDefaultExpireDuration() {
        Object durationObject = properties.get(DEFAULT_EXPIRE_DURATION);
        if(durationObject == null){
            return 0;
        } else {
            try {
                return Long.parseLong(durationObject.toString());
            } catch (NumberFormatException e) {
                log.warn("Configuration "+DEFAULT_EXPIRE_DURATION+"="+durationObject+" can not be converted to an Number -> return -1",e);
                return -1;
            }
        }
    }

    @Override
    public final EntityMapping.MappingState getDefaultMappedEntityState() {
        Object stateObject = properties.get(DEFAULT_MAPEED_ENTITY_STATE);
        if(stateObject == null){
            return EntityMapping.DEFAULT_MAPPING_STATE;
        } else {
            try {
                return EntityMapping.MappingState.valueOf(stateObject.toString());
            } catch (IllegalArgumentException e) {
                log.warn("Configuration "+DEFAULT_MAPEED_ENTITY_STATE+"="+stateObject+" dose not match any entry in the "+
                        EntityMapping.MappingState.class+" Enumeration ( one of "+
                        Arrays.toString(EntityMapping.MappingState.values())+") " +
                        "-> return the default state "+EntityMapping.DEFAULT_MAPPING_STATE,e);
                return EntityMapping.DEFAULT_MAPPING_STATE;
            }
        }
    }

    @Override
    public final Symbol.SymbolState getDefaultSymbolState() {
        Object stateObject = properties.get(DEFAULT_SYMBOL_STATE);
        if(stateObject == null){
            return Symbol.DEFAULT_SYMBOL_STATE;
        } else {
            try {
                return Symbol.SymbolState.valueOf(stateObject.toString());
            } catch (IllegalArgumentException e) {
                log.warn("Configuration "+DEFAULT_SYMBOL_STATE+"="+stateObject+" dose not match any entry in the "+
                        Symbol.SymbolState.class+" Enumeration ( one of "+
                        Arrays.toString(Symbol.SymbolState.values())+") " +
                        "-> return the default state "+Symbol.DEFAULT_SYMBOL_STATE,e);
                return Symbol.DEFAULT_SYMBOL_STATE;
            }
        }
    }

    @Override
    public final String getDereferencerType() {
        return properties.get(DEREFERENCER_TYPE).toString();
    }

    @Override
    public final String getDescription() {
        return ""+properties.get(DESCRIPTION); //use ""+ because value might be null
    }

    @Override
    public final String getId() {
        return properties.get(ID).toString();
    }

    @Override
    public final String getName() {
        Object name = properties.get(NAME);
        return name != null ? name.toString() : getId();
    }

    @Override
    public final String[] getEntityPrefixes() {
        Object prefixes = properties.get(ENTITY_PREFIX);
        if(prefixes == null){
            return new String[]{};
        } else {
            return (String[])prefixes;
        }
    }
    @Override
    public String getQueryType() {
        Object queryType = properties.get(SEARCHER_TYPE);
        return queryType != null?queryType.toString():null;
    }
    @Override
    public String getQueryUri() {
        return queryUri;
    }
    @Override
    public QueryResultList<Sign> findSigns(FieldQuery query) throws ReferencedSiteException {
        List<Sign> results;
        if(cacheStrategy == CacheStrategy.all){
            //TODO: check if query can be executed based on the base configuration of the Cache
            Cache cache = getCache();
            if(cache != null){
                try {
                    //When using the Cache, directly get the representations!
                    QueryResultList<Representation> representations = cache.findRepresentation((query));
                    results = new ArrayList<Sign>(representations.size());
                    for(Representation result : representations){
                        results.add(ModelUtils.createSign(result, getId()));
                    }
                    return new QueryResultListImpl<Sign>(query, results, Sign.class);
                } catch (YardException e) {
                    if(entitySearcherComponentName==null){
                        throw new ReferencedSiteException("Unable to execute query on Cache "+cacheId,e);
                    } else {
                        log.warn(String.format("Error while performing query on Cache %s! Try to use remote site %s as fallback!",cacheId,queryUri),e);
                    }
                }
            } else {
                if(entitySearcherComponentName==null){
                    throw new ReferencedSiteException(String.format("Cache %s not active and no EntitySeracher configured that could be used as Fallback",cacheId));
                } else {
                    log.warn(String.format("Cache %s currently not active will query remote Site %s as fallback",cacheId,queryUri));
                }
            }
        }
        QueryResultList<String> entityIds;
        try {
            entityIds = entitySearcher.findEntities(query);
        } catch (IOException e) {
            throw new ReferencedSiteException(String.format("Unable to execute query on remote site %s with entitySearcher %s!",
                    queryUri,entitySearcherComponentName), e);
        }
        List<Sign> entities = new ArrayList<Sign>(entityIds.size());
        int errors = 0;
        ReferencedSiteException lastError = null;
        for(String id : entityIds){
            Sign entity;
            try {
                entity = getSign(id);
                if(entity == null){
                    log.warn("Unable to create Entity for ID that was selected by an FieldQuery (id="+id+")");
                }
                entities.add(entity);
            } catch (ReferencedSiteException e) {
                lastError = e;
                errors++;
                log.warn(String.format("Unable to get Representation for Entity %s. -> %d Error%s for %d Entities in QueryResult (Reason:%s)",
                        id,errors,errors>1?"s":"",entityIds.size(),e.getMessage()));
            }
        }
        if(lastError != null){
            if(entities.isEmpty()){
                throw new ReferencedSiteException("Unable to get anly Representations for Entities selected by the parsed Query (Root-Cause is the last Exception trown)",lastError);
            } else {
                log.warn(String.format("Unable to get %d/%d Represetnations for selected Entities.",errors,entityIds.size()));
                log.warn("Stack trace of the last Exception:",lastError);
            }
        }
        return new QueryResultListImpl<Sign>(query, entities,Sign.class);
    }
    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws ReferencedSiteException{
        if(cacheStrategy == CacheStrategy.all){
            //TODO: check if query can be executed based on the base configuration of the Cache
            Cache cache = getCache();
            if(cache != null){
                try {
                    return cache.find(query);
                } catch (YardException e) {
                    if(entitySearcherComponentName==null){
                        throw new ReferencedSiteException("Unable to execute query on Cache "+cacheId,e);
                    } else {
                        log.warn(String.format("Error while performing query on Cache %s! Try to use remote site %s as fallback!",cacheId,queryUri),e);
                    }
                }
            } else {
                if(entitySearcherComponentName==null){
                    throw new ReferencedSiteException(String.format("Cache %s not active and no EntitySeracher configured that could be used as Fallback",cacheId));
                } else {
                    log.warn(String.format("Cache %s currently not active will query remote Site %s as fallback",cacheId,queryUri));
                }
            }
        }
        if(entitySearcher == null){
            throw new ReferencedSiteException(String.format("EntitySearcher %s not available for remote site %s!",entitySearcherComponentName,queryUri));
        } else {
            try {
                return entitySearcher.find(query);
            } catch (IOException e) {
                throw new ReferencedSiteException("Unable execute Query on remote site "+queryUri,e);
            }
        }
    }
    @Override
    public QueryResultList<String> findReferences(FieldQuery query) throws ReferencedSiteException {
        if(cacheStrategy == CacheStrategy.all){
            //TODO: check if query can be executed based on the base configuration of the Cache
            Cache cache = getCache();
            if(cache != null){
                try {
                    return cache.findReferences(query);
                } catch (YardException e) {
                    if(entitySearcherComponentName==null){
                        throw new ReferencedSiteException("Unable to execute query on Cache "+cacheId,e);
                    } else {
                        log.warn(String.format("Error while performing query on Cache %s! Try to use remote site %s as fallback!",cacheId,queryUri),e);
                    }
                }
            } else {
                if(entitySearcherComponentName==null){
                    throw new ReferencedSiteException(String.format("Cache %s not active and no EntitySeracher configured that could be used as Fallback",cacheId));
                } else {
                    log.warn(String.format("Cache %s currently not active will query remote Site %s as fallback",cacheId,queryUri));
                }
            }
        }
        if(entitySearcher == null){
            throw new ReferencedSiteException(String.format("EntitySearcher %s not available for remote site %s!",entitySearcherComponentName,queryUri));
        } else {
            try {
                return entitySearcher.findEntities(query);
            } catch (IOException e) {
                throw new ReferencedSiteException("Unable execute Query on remote site "+queryUri,e);
            }
        }
    }
    @Override
    public InputStream getContent(String id, String contentType) throws ReferencedSiteException {
        if(dereferencerComponentName == null){
            throw new ReferencedSiteException(String.format("Unable to get Content for Entity %s because No dereferencer configured for ReferencedSite %s",
                    id,getId()));
        }
        if(dereferencer == null){
            throw new ReferencedSiteException(String.format("Dereferencer %s for remote site %s is not available",dereferencerComponentName,accessUri));
        } else {
            try {
                return dereferencer.dereference(id, contentType);
            } catch (IOException e) {
                throw new ReferencedSiteException(String.format("Unable to load content for Entity %s and mediaType %s from remote site %s by using dereferencer %s",
                        id,contentType,accessUri,entitySearcherComponentName),e);
            }
        }
    }
    @Override
    public Sign getSign(String id) throws ReferencedSiteException {
        Cache cache = getCache();
        Representation rep = null;
        long start = System.currentTimeMillis();
        if (cache != null) {
            try {
                rep = cache.getRepresentation(id);
            } catch (YardException e) {
                if (dereferencerComponentName == null) {
                    throw new ReferencedSiteException(String.format("Unable to get Represetnation %s form Cache %s", id, cacheId), e);
                } else {
                    log.warn(String.format("Unable to get Represetnation %s form Cache %s. Will dereference from remote site %s",
                            id, cacheId, getAccessUri()), e);
                }
            }
        } else {
            if (dereferencerComponentName == null) {
                throw new ReferencedSiteException(String.format("Unable to get Represetnation %s because configured Cache %s is currently not available",
                        id, cacheId));
            } else {
                log.warn(String.format("Cache %s is currently not available. Will use remote site %s to load Representation %s",
                        cacheId, dereferencerComponentName, id));
            }
        }
        if (rep == null) { // no cache or not found in cache
            if(dereferencer == null){
                throw new ReferencedSiteException(String.format("Entity Dereferencer %s for accessing remote site %s is not available",
                        dereferencerComponentName,accessUri));
            } else {
                try {
                    rep = dereferencer.dereference(id);
                } catch (IOException e) {
                    throw new ReferencedSiteException(String.format("Unable to load Representation for entity %s form remote site %s with dereferencer %s",
                            id, accessUri, dereferencerComponentName), e);
                }
            }
            //representation loaded from remote site and cache is available
            if (rep != null && cache != null) {// -> cache the representation
                try {
                    start = System.currentTimeMillis();
                    // reassigning the Representation here will remove all
                    // values not stored in the cache.
                    // TODO: I am not sure if that is a good or bad thing to do.
                    rep = cache.store(rep);
                    log.info(String.format("  - cached Representation %s in %d ms",    id, (System.currentTimeMillis() - start)));
                } catch (YardException e) {
                    log.warn(String.format("Unable to cache Represetnation %s in Cache %s! Representation not cached!",    id, cacheId), e);
                }
            }
        } else {
            log.info(String.format("  - loaded Representation %s from Cache in %d ms",
                    id, (System.currentTimeMillis() - start)));
        }
        return rep != null ? ModelUtils.createSign(rep, getId()) : null;
    }

    @Override
    public String toString() {
        return getName();
    }
    @Override
    public int hashCode() {
        return getId().hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof ReferencedSite && ((ReferencedSite)obj).getId().equals(getId());
    }
    @Override
    public FieldMapper getFieldMapper() {
        return fieldMappings;
    }
    @Override
    public String getCacheId() {
        return cacheId;
    }

    /**
     * In case {@link CacheStrategy#all} this Method returns the
     * query factory of the Cache.
     * Otherwise it returns {@link DefaultQueryFactory#getInstance()}.
     */
    @Override
    public FieldQueryFactory getQueryFactory() {
        FieldQueryFactory factory = null;
        if(cacheStrategy == CacheStrategy.all){
            Cache cache = getCache();
            if(cache != null){
                factory = cache.getQueryFactory();
            }
        }
        if(factory == null){
            factory = DefaultQueryFactory.getInstance();
        }
        return factory;
    }
    /**
     * Internally used to get the Cache for this site. If
     * {@link CacheStrategy#none}, this methods always returns <code>null</code>,
     * otherwise it returns the Cache for the configured Yard or <code>null</code>
     * if no such Cache is available.
     * @return the cache or <code>null</code> if {@link CacheStrategy#none} or
     * the configured cache instance is not available.
     */
    protected Cache getCache(){
        return cacheStrategy == CacheStrategy.none?null:(Cache)cacheTracker.getService();
    }

    /*--------------------------------------------------------------------------
     *  OSGI LIFECYCLE and LISTENER METHODS
     *--------------------------------------------------------------------------
     */

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(final ComponentContext context) throws ConfigurationException, YardException, InvalidSyntaxException {
        log.info("in "+ReferencedSiteImpl.class+" activate with properties "+context.getProperties());
        if(context == null || context.getProperties() == null){
            throw new IllegalStateException("No Component Context and/or Dictionary properties object parsed to the acticate methode");
        }
        this.context = context;
        this.properties = context.getProperties();
        //check and init all required properties!
        accessUri = OsgiUtils.checkProperty(properties,ConfiguredSite.ACCESS_URI).toString();
        //accessURI is the default for the Query URI
        queryUri = OsgiUtils.checkProperty(properties,ConfiguredSite.QUERY_URI,accessUri).toString();
        OsgiUtils.checkProperty(properties,ID);
        dereferencerComponentName = OsgiUtils.checkProperty(context.getProperties(), ConfiguredSite.DEREFERENCER_TYPE).toString();
        if(dereferencerComponentName.isEmpty() || dereferencerComponentName.equals("none")){
            dereferencerComponentName = null;
        }
        entitySearcherComponentName = OsgiUtils.checkProperty(this.properties, ConfiguredSite.SEARCHER_TYPE).toString();
        if(entitySearcherComponentName.isEmpty() || entitySearcherComponentName.equals("none")){
            entitySearcherComponentName = null;
        }
        //if the accessUri is the same as the queryUri and both the dereferencer and
        //the entitySearcher uses the same component, than we need only one component
        //for both dependencies.
        this.dereferencerEqualsEntitySearcherComponent = accessUri.equals(queryUri)
            && dereferencerComponentName != null &&
                dereferencerComponentName.equals(entitySearcherComponentName);

        cacheStrategy = OsgiUtils.checkEnumProperty(CacheStrategy.class, properties, ConfiguredSite.CACHE_STRATEGY);
        //check if the congfig is valid
        if(this.cacheStrategy != CacheStrategy.none){
            //check if the cacheId is configured if cacheStrategy != none
            this.cacheId = OsgiUtils.checkProperty(this.properties, ConfiguredSite.CACHE_ID).toString();
        }
        //check that both dereferencer and searcher are configured if cacheStrategy != all
        if(cacheStrategy != CacheStrategy.all &&
                (dereferencerComponentName==null || entitySearcherComponentName == null)){
            throw new ConfigurationException(ConfiguredSite.CACHE_STRATEGY, String.format("If the EntitySearcher and/or the EntityDereferencer are set to \"none\", than the used CacheStragegy MUST BE \"all\"! (entitySearcher=%s | dereferencer=%s | cacheStrategy=%s",
                    dereferencerComponentName==null?"none":dereferencerComponentName,
                    entitySearcherComponentName==null?"none":entitySearcherComponentName,
                    cacheStrategy));
        }
        //parse the field mappings
        initFieldmappings(context);

        //now init the referenced Services
        initDereferencerAndEntitySearcher();

        // If a cache is configured init the ServiceTracker used to manage the
        // Reference to the cache!
        if(cacheId != null){
            String cacheFilter = String.format("(&(%s=%s)(%s=%s))",
                    Constants.OBJECTCLASS,Cache.class.getName(),
                    Cache.CACHE_YARD,cacheId);
            cacheTracker = new ServiceTracker(context.getBundleContext(),
                    context.getBundleContext().createFilter(cacheFilter), null);
            cacheTracker.open();
        }
    }
    /**
     * @param context
     * @throws ConfigurationException
     * @throws InvalidSyntaxException
     */
    private void initFieldmappings(final ComponentContext context) throws ConfigurationException, InvalidSyntaxException {
        //create the FieldMappings config
        fieldMappings = new DefaultFieldMapperImpl(ValueConverterFactory.getInstance());
        Object configuredMappingsObject = properties.get(ConfiguredSite.SITE_FIELD_MAPPINGS);
        log.info(" > Parse FieldMappungs");
        if(configuredMappingsObject != null){
            if(configuredMappingsObject instanceof String[]){
                for(String configuredMapping : (String[])configuredMappingsObject){
                    FieldMapping mapping = FieldMappingUtils.parseFieldMapping(configuredMapping);
                    if(mapping != null){
                        log.info("   - add FieldMapping "+mapping);
                        fieldMappings.addMapping(mapping);
                    }
                }
            } else { //TODO maybe write an utility method that get values from arrays and collections
                log.warn("Configured Mappings are not parsed as String[] (type="+configuredMappingsObject.getClass()+" value="+configuredMappingsObject+")");
            }
        } else {
            log.info("   <- no FieldMappngs configured");
        }
    }

    /**
     * Initialise the dereferencer and searcher component as soon as the according
     * {@link ComponentFactory} gets registered.<p>
     * First this Methods tries to find the according {@link ServiceReference}s
     * directly. If they are not available (e.g. because the component factories
     * are not yet started) than it adds a {@link ServiceListener} for the missing
     * {@link ComponentFactory} that calls the {@link #createDereferencerComponent(ComponentFactory)}
     * and {@link #createEntitySearcherComponent(ComponentFactory)} as soon as
     * the factory gets registered.
     * @throws InvalidSyntaxException if the #entitySearcherComponentName or the
     * {@link #dereferencerComponentName} somehow cause an invalid formated string
     * that can not be used to parse a {@link Filter}.
     */
    private void initDereferencerAndEntitySearcher() throws InvalidSyntaxException {
        if(entitySearcherComponentName != null) {
            String componentNameFilterString = String.format("(%s=%s)",
                    "component.name",entitySearcherComponentName);
            String filterString = String.format("(&(%s=%s)%s)",
                    Constants.OBJECTCLASS,ComponentFactory.class.getName(),
                    componentNameFilterString);
            ServiceReference[] refs = context.getBundleContext().getServiceReferences(ComponentFactory.class.getName(),componentNameFilterString);
            if(refs != null && refs.length>0){
                createEntitySearcherComponent((ComponentFactory)context.getBundleContext().getService(refs[0]));
            } else { //service factory not yet available -> add servicelistener
                this.searcherComponentFactoryListener = new ComponentFactoryListener(context.getBundleContext());
                context.getBundleContext().addServiceListener(this.searcherComponentFactoryListener,filterString); //NOTE: here the filter MUST include also the objectClass!
            }
            //context.getComponentInstance().dispose();
            //throw an exception to avoid an successful activation
        }
        if(dereferencerComponentName != null && !this.dereferencerEqualsEntitySearcherComponent){
            String componentNameFilterString = String.format("(%s=%s)",
                    "component.name",dereferencerComponentName);
            String filterString = String.format("(&(%s=%s)%s)",
                    Constants.OBJECTCLASS,ComponentFactory.class.getName(),
                    componentNameFilterString);
            ServiceReference[] refs = context.getBundleContext().getServiceReferences(ComponentFactory.class.getName(),componentNameFilterString);
            if(refs != null && refs.length>0){
                createDereferencerComponent((ComponentFactory)context.getBundleContext().getService(refs[0]));
            } else { //service factory not yet available -> add servicelistener
                this.dereferencerComponentFactoryListener = new ComponentFactoryListener(context.getBundleContext());
                this.context.getBundleContext().addServiceListener(this.dereferencerComponentFactoryListener,filterString); //NOTE: here the filter MUST include also the objectClass!
            }
        }
    }
    /**
     * Creates the entity searcher component used by this {@link ReferencedSite}
     * (and configured via the {@link ConfiguredSite#SEARCHER_TYPE} property).<p>
     * If the {@link ConfiguredSite#DEREFERENCER_TYPE} is set to the same vale
     * and the {@link #accessUri} also equals the {@link #queryUri}, than the
     * component created for the entity searcher is also used as dereferencer.
     * @param factory The component factory used to create the
     * {@link #entitySearcherComponentInstace}
     */
    @SuppressWarnings("unchecked")
    protected void createEntitySearcherComponent(ComponentFactory factory){
        //both create*** methods sync on the searcherAndDereferencerLock to avoid
        //multiple component instances because of concurrent calls
        synchronized (this.searcherAndDereferencerLock ) {
            if(entitySearcherComponentInstace == null){
                this.entitySearcherComponentInstace = factory.newInstance(OsgiUtils.copyConfig(context.getProperties()));
                this.entitySearcher = (EntitySearcher)entitySearcherComponentInstace.getInstance();
            }
            if(dereferencerEqualsEntitySearcherComponent){
                this.dereferencer = (EntityDereferencer) entitySearcher;
            }
        }
    }
    /**
     * Creates the entity dereferencer component used by this {@link ReferencedSite}.
     * The implementation used as the dereferencer is configured by the
     * {@link ConfiguredSite#DEREFERENCER_TYPE} property.
     * @param factory the component factory used to create the {@link #dereferencer}
     */
    @SuppressWarnings("unchecked")
    protected void createDereferencerComponent(ComponentFactory factory){
        //both create*** methods sync on searcherAndDereferencerLock to avoid
        //multiple component instances because of concurrent calls
        synchronized (this.searcherAndDereferencerLock) {
            if(dereferencerComponentInstance == null){
                dereferencerComponentInstance=factory.newInstance(OsgiUtils.copyConfig(context.getProperties()));
                this.dereferencer = (EntityDereferencer)dereferencerComponentInstance.getInstance();
            }
        }
    }

    /**
     * Simple {@link ServiceListener} implementation that is used to get notified
     * if one of the {@link ComponentFactory component factories} for the
     * configured implementation of the {@link EntityDereferencer} or
     * {@link EntitySearcher} interfaces get registered.
     * @author Rupert Westenthaler
     *
     */
    private class ComponentFactoryListener implements ServiceListener {
        private BundleContext bundleContext;
        protected ComponentFactoryListener(BundleContext bundleContext){
            if(bundleContext == null){
                throw new IllegalArgumentException("The BundleContext MUST NOT be NULL!");
            }
            this.bundleContext = bundleContext;
        }
        @Override
        public void serviceChanged(ServiceEvent event) {
            Object eventComponentName = event.getServiceReference().getProperty("component.name");
            if(event.getType() == ServiceEvent.REGISTERED){
                log.info(String.format("Process ServceEvent for ComponentFactory %s and State REGISTERED",
                        eventComponentName));
                ComponentFactory factory = (ComponentFactory)bundleContext.getService(event.getServiceReference());
                if(dereferencerComponentName != null &&
                        dereferencerComponentName.equals(eventComponentName)){
                    createDereferencerComponent(factory);
                }
                if(entitySearcherComponentName!= null &&
                entitySearcherComponentName.equals(eventComponentName)){
                    createEntitySearcherComponent(factory);
                }
            } else {
                log.info(String.format("Ignore ServceEvent for ComponentFactory %s and state %s",
                        eventComponentName,
                        event.getType()==ServiceEvent.MODIFIED?"MODIFIED":event.getType()==ServiceEvent.UNREGISTERING?"UNREGISTERING":"MODIFIED_ENDMATCH"));
            }
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in "+AbstractEntityDereferencer.class.getSimpleName()+" deactivate with context "+context);
        this.dereferencer = null;
        if(this.dereferencerComponentInstance != null){
            this.dereferencerComponentInstance.dispose();
            this.dereferencerComponentInstance = null;
        }
        this.entitySearcher = null;
        if(this.entitySearcherComponentInstace != null){
            this.entitySearcherComponentInstace.dispose();
            this.entitySearcherComponentInstace = null;
        }
        if(searcherComponentFactoryListener != null){
            context.getBundleContext().removeServiceListener(searcherComponentFactoryListener);
            searcherComponentFactoryListener = null;
        }
        if(dereferencerComponentFactoryListener != null){
            context.getBundleContext().removeServiceListener(dereferencerComponentFactoryListener);
            dereferencerComponentFactoryListener = null;
        }
        this.cacheStrategy = null;
        this.cacheId = null;
        if(cacheTracker != null){
            cacheTracker.close();
            cacheTracker = null;
        }
        this.fieldMappings = null;
        this.accessUri = null;
        this.queryUri = null;
        this.context = null;
        this.properties = null;
    }
}
