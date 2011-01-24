package org.apache.stanbol.entityhub.core.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.model.DefaultEntityMappingImpl;
import org.apache.stanbol.entityhub.core.model.DefaultSymbolImpl;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.core.utils.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.servicesapi.yard.YardManager;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the Entityhub.
 * @author Rupert Westenthaler
 *
 */
@Component()
@Service
public final class EntityhubImpl implements Entityhub, ServiceListener {

    private final Logger log = LoggerFactory.getLogger(EntityhubImpl.class);

    /**
     * The OSGI component context of the Entityhub
     */
    private ComponentContext context;
    /**
     * The field mapper holding global mappings that are used for mapping
     * representations of entities for any referenced sites
     */
    protected FieldMapper fieldMapper;

    /**
     * The yard where this Entityhub instance stores its data
     * TODO: this reference is currently initialised in the activate method.
     * however there are some issues with that.
     * <ul>
     * <li> If this Component is activated, before this yard is active, the
     *      activate method throws an Exception and is therefore in the
     *      "unsatisfied" state.
     * <li> If now the needed Yard is configured this component gets not notified
     * <li> However defining a Reference is also not possible, because it would
     *      be nice to be able to change the Entityhub-Yard (e.g. to change the data
     *      set of the Entityhub at runtime)
     * <li> I could register a {@link ServiceListener} in the activate method.
     *      But I am not sure if it is allowed to have an active Service Listener
     *      on an component that is in the "unsatisfied" state.
     * </ul>
     */
    protected Yard entityhubYard; //reference initialised in the activate method
    /*
     * TODO: The YardManager is currently not used.
     */
    @Reference // 1..1, static
    protected YardManager yardManager;
    /**
     * The Configuration of the Entityhub
     * TODO: Maybe refactor this implementation to implement this interface or
     * to extend the {@link EntityhubConfigurationImpl}.
     */
    @Reference // 1..1, static
    protected EntityhubConfiguration config;
    /**
     * The site manager is used to search for entities within the Entityhub framework
     */
    @Reference // 1..1, static
    protected ReferencedSiteManager siteManager;

    private String DEFAULT_SYMBOL_PREFIX = "symbol";
    private String DEFAULT_MAPPING_PREFIX = "mapping";
    /**
     * Activates the Entityhub (OSGI Lifecycle method)
     * @param context the OSGI component context (stored in {@link #context})
     * @throws ConfigurationException On any error during the activation of
     * the Entityhub
     */
    @Activate
    protected void activate(ComponentContext context) throws ConfigurationException {
        log.info("activating Entityhub ...");
        if(context == null){
            throw new IllegalStateException("Unable to activate if parsed ComponentContext is NULL");
        } else {
            this.context = context;
        }
        //First check the entityhub ID and
        log.info(" ... init Basic Properties");
        if(config.getID() == null || config.getID().isEmpty()){
            throw new ConfigurationException(EntityhubConfiguration.ID, "The Entityhub Configuration does not define a ID for the Entityhub");
        } else {
            log.info("   + id: "+config.getID());
        }
        if(config.getName() == null || config.getName().isEmpty()){
            throw new ConfigurationException(EntityhubConfiguration.NAME, "The Entityhub Configuration does not define a name for the Entityhub");
        } else {
            log.info("   + id: "+config.getName());
        }
        if(config.getDescription() != null){
            log.info("   + id: "+config.getDescription());
        }
        if(config.getEntityhubPrefix() == null){
            throw new ConfigurationException(EntityhubConfiguration.PREFIX, "The Entityhub Configuration does not define a Prefix for the Entityhub");
        }
        try {
            new URI(config.getEntityhubPrefix());
            log.info("   + prefix: "+config.getEntityhubPrefix());
        } catch (URISyntaxException e1) {
            throw new ConfigurationException(EntityhubConfiguration.PREFIX, "The Prefix configured for the Entityhub is not an valied URI (prefix="+config.getEntityhubPrefix()+")");
        }
        //next get the reference to the configured EntityhubYard
        log.info(" ... init EntityhubYard");
        if(yardManager.isYard(config.getEntityhubYardId())){
            this.entityhubYard = yardManager.getYard(config.getEntityhubYardId());
            String entityhubYardFilterString = '('+Yard.ID+'='+config.getEntityhubYardId()+')';
            try {
                context.getBundleContext().addServiceListener(this,entityhubYardFilterString);
            } catch (InvalidSyntaxException e) {
                log.warn(String.format("Unable to set Filter %s to ServiceListener for EntityhubYard! -> add ServiceListener without Filter",
                    entityhubYardFilterString),e);
                context.getBundleContext().addServiceListener(this);
            }
        } else {
            throw new ConfigurationException(EntityhubConfiguration.ENTITYHUB_YARD_ID, "Unable to get Yard for parsed value "+config.getEntityhubYardId());
        }
        //at last get the FieldMappingConfig and create the FieldMappings instance
        // -> we need to do that after the init of the Entityhub-yard, because than we
        //    can use the valueFactory of the configured Yard to create instances
        //    of converted values!
        log.info(" ... init FieldMappings");
        fieldMapper = new DefaultFieldMapperImpl(ValueConverterFactory.getInstance(entityhubYard.getValueFactory()));
        for(String mappingString : config.getFieldMappingConfig()){
            FieldMapping mapping = FieldMappingUtils.parseFieldMapping(mappingString);
            if(mapping != null){
                log.info("   + mapping: "+mapping);
                fieldMapper.addMapping(mapping);
            }
        }
    }
    /**
     * TODO: currently only for debugging. Intended to be used for tracking
     * the state of dependencies
     */
    @Override
    public void serviceChanged(ServiceEvent event) {
        log.info("Print Service Event for "+event.getSource());
        for(String key : event.getServiceReference().getPropertyKeys()){
            log.info("  > "+key+"="+event.getServiceReference().getProperty(key));
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("!!deactivate");
        if(this.entityhubYard != null){
            //unregister the serviceListener
            this.context.getBundleContext().removeServiceListener(this);
        }
        this.fieldMapper = null;
        this.entityhubYard = null;
        this.context = null;
    }

//    @Override
//    public EntityhubConfiguration getEntityhubConfiguration() {
//        return config;
//    }
    @Override
    public Yard getYard() {
        return entityhubYard;
    }

    @Override
    public Symbol lookupSymbol(String entity) throws YardException{
        return lookupSymbol(entity,false);
    }

    @Override
    public Symbol lookupSymbol(String entity, boolean create) throws YardException {
        Symbol symbol = getSymbol(entity);
        if(symbol != null){
            return symbol;
        } else {
            //parsed reference was not a symbol. search for an mapped Entity
            EntityMapping entityMapping = getMappingByEntity(entity);
            if(entityMapping != null){
                symbol = getSymbol(entityMapping.getSymbolId());
                if(symbol != null){
                    return symbol;
                } else {
                    log.warn("Unable to find Symbol for Entity Mapping "+entityMapping+"!");
                    return recoverSymbol(entityMapping.getSymbolId());
                }
            } else if(create){
                //search if the parsed reference is known by any referenced site
                // and if YES, create a new Symbol
                Sign sign = siteManager.getSign(entity);
                 if(sign == null){ //id not found by any referred site
                     return null;
                 } else {
                     return createSymbol(sign);
                 }
            } else {
                return null;
            }
        }
    }
    @Override
    public Symbol createSymbol(String reference) throws IllegalStateException, IllegalArgumentException, YardException {
        Symbol symbol = getSymbol(reference);
        if(symbol == null){
            EntityMapping entityMapping = getMappingByEntity(reference);
            if(entityMapping == null){
                Sign sign = siteManager.getSign(reference);
                if(sign == null){
                    return null;
                } else {
                    return createSymbol(sign);
                }
            } else {
                throw new IllegalStateException("There exists already an EntityMappting for the parsed reference (mapping="+entityMapping+")");
            }
        } else {
            throw new IllegalStateException("The parsed reference is an Symbol (symbol="+symbol+")!");
        }
    }
    /**
     * Recovers an symbol based on the available {@link EntityMapping}s
     * @param symbolId the id of the Symbol
     * @return the Symbol or <code>null</code> if the recovering is unsucessfull.
     */
    private Symbol recoverSymbol(String symbolId) {
        /*
         * TODO: recover the Symbol by recreating it based on the available
         *       mapped Entities
         * 1) search for all EntityMappings with this SymbolId
         * 2) get all mapped Entities
         * 3) apply the FieldMappings
         * 4) store the Symbol
         * 5) return the recovered Symbol
         */
        return null;
    }
    /*
     * @throws IllegalArgumentException if a {@link Representation} for the parsed ID is present in the
     *  {@link #entityhubYard}, but the representation can not be wrapped by an {@link Symbol}.
     */
    @Override
    public Symbol getSymbol(String symbolId) throws IllegalArgumentException,IllegalStateException, YardException {
        if(symbolId == null || symbolId.isEmpty()){
            throw new IllegalArgumentException("The parsed symbolID MUST NOT be NULL nor empty!");
        }
        Representation rep = entityhubYard.getRepresentation(symbolId);
        if(rep != null){
            try {
                return new DefaultSymbolImpl(config.getEntityhubPrefix(),rep);
            } catch(IllegalArgumentException e){
                String msg = "Unable to create Symbol based on the representation as stored in the Entityhub";
                log.warn(msg);
                if(log.isWarnEnabled()){
                    log.warn(ModelUtils.getRepresentationInfo(rep));
                }
                throw new IllegalStateException(msg,e); //re-throw for now
            }
        } else {
            return null;
        }
    }
    protected Symbol createSymbol(Sign sign) throws YardException{
        if(sign == null){
            return null;
        }
        ReferencedSite signSite = siteManager.getReferencedSite(sign.getSignSite());
        if(signSite == null){
            log.warn("Unable to create Symbol because the ReferencedSite "+sign.getSignSite()+" for sign "+sign.getId()+" is currently not active -> return null");
            return null;
        }
        Representation symbolRep = entityhubYard.create(constructResourceId(DEFAULT_SYMBOL_PREFIX));
        //and set the initial state
        symbolRep.addReference(Symbol.STATE, config.getDefaultSymbolState().getUri());
        //create a FieldMapper containing the Entityhub Mappings and the Site specific mappings
        //TODO: one could cache such instances
        FieldMapper siteMapper = signSite.getFieldMapper();
        FieldMapper mapper = this.fieldMapper.clone();
        for(FieldMapping siteMapping : siteMapper.getMappings()){
            mapper.addMapping(siteMapping);
        }
        Representation signRep = sign.getRepresentation();
        //TODO: As soon as MappingActivities are implemented we need to add such
        //      information to the EntityMapping instance!
        mapper.applyMappings(signRep, symbolRep);
        //Second create the symbol and init the data
        Symbol symbol;
        try {
            symbol = new DefaultSymbolImpl(config.getEntityhubPrefix(),symbolRep);
        } catch (IllegalArgumentException e){
            //unable to create Symbol based on available Information
            // -> clean up and return null;
            log.warn("Unable to create Symbol for Representation "+symbolRep.getId()+
                    ", because of missing required fields! -> return null (see more Infos after Stack Trace)",e);
            if(log.isWarnEnabled()){
                log.warn(ModelUtils.getRepresentationInfo(symbolRep));
            }
            try { //try to remove the created representation in the store
                entityhubYard.remove(symbolRep.getId());
            } catch (YardException e1) {
                log.warn("Unable to remove Representation "+symbolRep.getId(),e1);
            }
            return null;
        }
        //Third create and init the mapped Entity
        EntityMapping entityMapping = new DefaultEntityMappingImpl(
                config.getEntityhubPrefix(), symbolRep.getId(), signRep.getId(),
                config.getDefaultMappingState(),
                entityhubYard.create(constructResourceId(DEFAULT_MAPPING_PREFIX)));
        //Store the symbol and the mappedEntity in the entityhubYard
        entityhubYard.store(symbol.getRepresentation());
        entityhubYard.store(entityMapping.getRepresentation());
        return symbol;

        //we need to set the label and the description!
    }
    /**
     * Uses the Prefix as configured by the {@link #config} and the parsed
     * prefix for the type to create an unique ID for a resource.
     * @param typePrefix the prefix of the type
     * @return An id in the form <code> {@link EntityhubConfiguration#getEntityhubPrefix()}
     *  + typePrefix + '.' + {@link ModelUtils#randomUUID()}</code>. Note that between
     *  the entity hub prefix and the type prefix a separation chars are added
     *  if it is not already defined by the {@link EntityhubConfiguration#getEntityhubPrefix()}
     *  value.
     */
    private String constructResourceId(String typePrefix) {
        StringBuilder id = new StringBuilder();
        String prefix = config.getEntityhubPrefix();
        if(prefix == null || prefix.isEmpty()){
            prefix = Entityhub.DEFAUTL_ENTITYHUB_PREFIX;
        }
        id.append(prefix);
        switch(prefix.charAt(prefix.length()-1)){
        case '#':
        case ':':
            break;
        default: //add a separator
            if(prefix.startsWith("urn:")){
                id.append(':'); //use a point for now
            } else {
                id.append('/'); //use '/' instead of '#' because one needs not to escape it in GET requests
            }
        }
        if(typePrefix != null && !typePrefix.isEmpty()){
            id.append(typePrefix);
            id.append('.');
        }
        id.append(ModelUtils.randomUUID());
        return id.toString();
    }


    @Override
    public EntityMapping getMappingByEntity(String reference) throws YardException{
        if(reference == null){
            log.warn("NULL parsed as Reference -> call to getMappingByEntity ignored (return null)");
            return null;
        }
        FieldQuery fieldQuery = getQueryFavtory().createFieldQuery();
        fieldQuery.setConstraint(RdfResourceEnum.mappedEntity.getUri(), new ReferenceConstraint(reference));
        QueryResultList<String> resultList = entityhubYard.findReferences(fieldQuery);
        if(!resultList.isEmpty()){
            Iterator<String> resultIterator = resultList.iterator();
            EntityMapping entityMapping = getEntityMappingFromYard(resultIterator.next());
            if(resultIterator.hasNext()){
                log.warn("Multiple Mappings found for Entity "+reference+"!");
                log.warn("  > "+entityMapping.getId()+" -> returned instance");
                while(resultIterator.hasNext()){
                    log.warn("  > "+resultIterator.next()+" -> ignored");
                }
            }
            return entityMapping;
        } else {
            log.debug("No Mapping found for Entity "+reference);
            return null;
        }
    }
    @Override
    public Collection<EntityMapping> getMappingsBySymbol(String symbol) throws YardException{
        if(symbol == null){
            log.warn("NULL parsed as Reference -> call to getMappingsBySymbol ignored (return null)");
            return null;
        }
        FieldQuery fieldQuery = getQueryFavtory().createFieldQuery();
        fieldQuery.setConstraint(RdfResourceEnum.mappedSymbol.getUri(), new ReferenceConstraint(symbol));
        QueryResultList<String> resultList = entityhubYard.findReferences(fieldQuery);
        Collection<EntityMapping> mappings = new HashSet<EntityMapping>();
        for(String mappingId : resultList){
            EntityMapping entityMapping = getEntityMappingFromYard(mappingId);
            if(entityMapping != null){
                mappings.add(entityMapping);
            } else {
                log.info("Unable to getEntityMapping for "+mappingId+" (id was returned as result for a query for EntityMappings -> so that should only happen if the Mapping was deleted in the meantime)");
            }
        }
        return mappings;
    }
    /**
     * Getter for the EntityMapping by ID
     * @param id the ID
     * @return the EntityMapping or <code>null</code> if no Sign is present 
     * within the EntityhubYard for the parsed ID
     * @throws IllegalArgumentException if the Sign referenced by the parsed ID 
     * is not an valid {@link EntityMapping}.
     */
    protected EntityMapping getEntityMappingFromYard(String id) throws IllegalArgumentException,YardException {
        Representation rep = entityhubYard.getRepresentation(id);
        if(rep != null){
            return new DefaultEntityMappingImpl(config.getEntityhubPrefix(),rep);
        } else {
            return null;
        }
    }

    @Override
    public EntityMapping getMappingById(String id) throws EntityhubException{
        return getEntityMappingFromYard(id);
    }
    @Override
    public FieldQueryFactory getQueryFavtory() {
        return entityhubYard.getQueryFactory();
    }
    @Override
    public FieldMapper getFieldMappings() {
        return fieldMapper;
    }
    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws YardException{
        return entityhubYard.find(query);
    }
    @Override
    public QueryResultList<String> findSymbolReferences(FieldQuery query) throws YardException{
        return entityhubYard.findReferences(query);
    }
    @Override
    public QueryResultList<Symbol> findSymbols(FieldQuery query) throws YardException{
        QueryResultList<String> references = entityhubYard.findReferences(query);
        List<Symbol> symbols = new ArrayList<Symbol>(references.size());
        for(String reference : references){
            Symbol symbol = lookupSymbol(reference);
            if(symbol != null){
                symbols.add(symbol);
            } else {
                log.warn("Unable to create Symbol for Reference "+reference+" in the Yard usd by the entity hub [id="+entityhubYard.getId()+"] -> ignore reference");
            }
        }
        return new QueryResultListImpl<Symbol>(references.getQuery(), symbols, Symbol.class);
    }

}
