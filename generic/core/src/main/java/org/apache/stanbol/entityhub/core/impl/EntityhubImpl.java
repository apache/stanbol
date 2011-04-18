/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.entityhub.core.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import org.apache.stanbol.entityhub.core.model.DefaultSignFactory;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
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
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Sign.SignTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the Entityhub.
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true)
@Service
public final class EntityhubImpl implements Entityhub, ServiceListener {

    private final Logger log = LoggerFactory.getLogger(EntityhubImpl.class);

    /**
     * The OSGI component context of the Entityhub
     */
    @SuppressWarnings("unused")
    private ComponentContext context;
    /**
     * The field mapper holding global mappings that are used for mapping
     * representations of entities for any referenced sites
     */
    private FieldMapper fieldMapper;

    /**
     * Used to create Sign instances
     */
    private DefaultSignFactory signFactory = DefaultSignFactory.getInstance();
    /**
     * Tracks the availability of the Yard used by the Entityhub.
     */
    private ServiceTracker entityhubYardTracker; //reference initialised in the activate method
    /**
     * The Configuration of the Entityhub
     * TODO: Maybe refactor this implementation to implement this interface or
     * to extend the {@link EntityhubConfigurationImpl}.
     */
    @Reference // 1..1, static
    private EntityhubConfiguration config;
    /**
     * The site manager is used to search for entities within the Entityhub framework
     */
    @Reference // 1..1, static
    private ReferencedSiteManager siteManager;

    private static final String DEFAULT_SYMBOL_PREFIX = "symbol";
    private static final String DEFAULT_MAPPING_PREFIX = "mapping";
    /**
     * Activates the Entityhub (OSGI Lifecycle method)
     * @param context the OSGI component context (stored in {@link #context})
     * @throws ConfigurationException On any error during the activation of
     * the Entityhub
     */
    @Activate
    protected void activate(final ComponentContext context) throws ConfigurationException {
        if(context == null){
            throw new IllegalStateException("Unable to activate if parsed ComponentContext is NULL");
        }
        log.info("activating Entityhub with configuration "+context.getProperties());
        this.context = context;
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
        if(config.getEntityhubPrefix() == null){
            throw new ConfigurationException(EntityhubConfiguration.ENTITYHUB_YARD_ID, "The ID of the Yard used by the Entityhub MUST NOT be NULL");
        }
        if(config.getEntityhubYardId().isEmpty()){
            throw new ConfigurationException(EntityhubConfiguration.ENTITYHUB_YARD_ID, "The ID of the Yard used by the Entityhub MUST NOT be empty");
        }
        String entityhubYardFilterString = String.format("(&(%s=%s)(%s=%s))",
            Constants.OBJECTCLASS,Yard.class.getName(),
            Yard.ID,config.getEntityhubYardId());
        log.info(" ... tracking EntityhubYard by Filter:"+entityhubYardFilterString);
        try {
            entityhubYardTracker = new ServiceTracker(context.getBundleContext(), 
                context.getBundleContext().createFilter(entityhubYardFilterString), null);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Got Invalid Syntax Exception for Entityhub filter ",e);
        }
        entityhubYardTracker.open(); //start the tracking
        //at last get the FieldMappingConfig and create the FieldMappings instance
        // -> we need to do that after the init of the Entityhub-yard, because than we
        //    can use the valueFactory of the configured Yard to create instances
        //    of converted values!
        log.info(" ... init FieldMappings");
        fieldMapper = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
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
        log.info("deactivate "+EntityhubImpl.class);
        this.fieldMapper = null;
        if(entityhubYardTracker != null){
            this.entityhubYardTracker.close();
            this.entityhubYardTracker = null;
        }
        this.context = null;
    }

//    @Override
//    public EntityhubConfiguration getEntityhubConfiguration() {
//        return config;
//    }
    @Override
    public Yard getYard() {
        return (Yard)entityhubYardTracker.getService();
    }
    /**
     * Internally used to lookup the yard. This throws an {@link YardException}
     * in case the yard is currently not available
     * @return the yard
     * @throws YardException in case the yard is not active
     */
    private Yard lookupYard() throws YardException {
        Yard yard = getYard();
        if(yard == null){
            throw new YardException("The Entityhub Yard (ID="+config.getEntityhubYardId()+") is not active! Please check the configuration!!");
        }
        return yard;
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
        Sign symbol = getSignFormYard(symbolId);
        if(symbol == null || symbol instanceof Symbol){
            return (Symbol)symbol;
        } else {
            log.info("The parsed id does not represent a Symbol, but a {} (uri={})! -> return null",
                symbol.getType(),symbol.getType().getUri());
            return null;
        }
    }
    /**
     * Creates a new Symbol (internal Entity) by importing the parsed sign
     * @param sign the sign to import
     * @return the imported and stored symbol. 
     * @throws YardException
     */
    protected Symbol createSymbol(Sign sign) throws YardException{
        if(sign == null){
            return null;
        }
        ReferencedSite signSite = siteManager.getReferencedSite(sign.getSignSite());
        if(signSite == null){
            log.warn("Unable to create Symbol because the ReferencedSite "+sign.getSignSite()+" for sign "+sign.getId()+" is currently not active -> return null");
            return null;
        }
        Yard entityhubYard = lookupYard();
        ValueFactory valueFactory = entityhubYard.getValueFactory();
        //Create the Symbol
        Representation symbolRep = entityhubYard.create(constructResourceId(DEFAULT_SYMBOL_PREFIX));
        Symbol symbol = importSign(sign, signSite, symbolRep, valueFactory);

        //Second create and init the Mapping
        Representation entityMappingRepresentation = entityhubYard.create(
            constructResourceId(DEFAULT_MAPPING_PREFIX));
        EntityMapping entityMapping = establishMapping(symbol, sign, signSite, entityMappingRepresentation);
        
        //Store the symbol and the mappedEntity in the entityhubYard
        entityhubYard.store(symbol.getRepresentation());
        entityhubYard.store(entityMapping.getRepresentation());
        return symbol;
    }
    /**
     * Imports a {@link Sign} from a {@link ReferencedSite}. This Method imports
     * the {@link Representation} by applying all configured mappings. It also
     * sets the SymbolState to the configured default value by the referenced
     * site of the Sign or the default for the Entityhub if the site does not
     * define this configuration.<p>
     * @param sign The sign (external entity) to import
     * @param signSite the referenced site of the sign (external entity)
     * @param symbolRep the Representation used to store the imported information
     * @param valueFactory the valusFactory used to create instance while importing
     * @return the symbol (internal Entity)
     */
    private Symbol importSign(Sign sign,
                              ReferencedSite signSite,
                              Representation symbolRep,
                              ValueFactory valueFactory) {
        Symbol symbol = signFactory.createSign(config.getID(),Symbol.class, symbolRep);
        Symbol.SymbolState state = signSite.getConfiguration().getDefaultSymbolState();
        if(state == null){
            state =  config.getDefaultSymbolState();
        }
        symbol.setState(state);
        //and set the initial state
        symbolRep.addReference(Symbol.STATE, config.getDefaultSymbolState().getUri());
        //create a FieldMapper containing the Entityhub Mappings and the Site specific mappings
        FieldMapper siteMapper = signSite.getFieldMapper();
        FieldMapper mapper = this.fieldMapper.clone();
        for(FieldMapping siteMapping : siteMapper.getMappings()){
            mapper.addMapping(siteMapping);
        }
        Representation signRep = sign.getRepresentation();
        //TODO: As soon as MappingActivities are implemented we need to add such
        //      information to the EntityMapping instance!
        mapper.applyMappings(signRep, symbolRep,valueFactory);
        return symbol;
    }
    /**
     * Creates a new {@link EntityMapping} for the parsed Sign and Symbol. This
     * also sets the State and the expire date based on the configurations of the
     * ReferencesSite of the Sign and the defaults of the Entityhub 
     * @param symbol the Symbol (internal Entity) to link
     * @param sign the Sign (external Entity)  to link
     * @param signSite the referenced site of the Sign (external Entity)
     * @param entityMappingRepresentation the Representation used to store the data
     * @return the EntityMapping
     */
    private EntityMapping establishMapping(Symbol symbol,
                                           Sign sign,
                                           ReferencedSite signSite,
                                           Representation entityMappingRepresentation) {
        EntityMapping entityMapping = signFactory.createSign(config.getID(),
            EntityMapping.class, entityMappingRepresentation);
        //now init the mappingState and the expireDate based on the config of the
        //ReferencedSite of the sign (considering also the defaults of the entityhub)
        EntityMapping.MappingState mappingState = signSite.getConfiguration().getDefaultMappedEntityState();
        if(mappingState == null){
            mappingState = config.getDefaultMappingState();
        }
        entityMapping.setState(mappingState);
        long expireDuration = signSite.getConfiguration().getDefaultExpireDuration();
        if(expireDuration < 0){
            Date expireDate = new Date(System.currentTimeMillis()+expireDuration);
            entityMapping.setExpires(expireDate);
        }
        entityMapping.getRepresentation().setReference(EntityMapping.ENTITY_ID, sign.getId());
        entityMapping.getRepresentation().setReference(EntityMapping.SYMBOL_ID, symbol.getId());
        return entityMapping;
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
        QueryResultList<Representation> resultList = lookupYard().findRepresentation(fieldQuery);
        if(!resultList.isEmpty()){
            Iterator<Representation> resultIterator = resultList.iterator();
            Sign mapping = signFactory.getSign(config.getID(), resultIterator.next());
            if(!(mapping.getType() == SignTypeEnum.EntityMapping)){
                // This query should only return EntityMappings 
                //  ... something very bad happens :(
                log.error("Unable to create EntityMapping {} for Entity {} because SignType != EntityMapping but {}!",
                    new Object[]{
                                 mapping.getId(),
                                 reference,
                                 mapping.getType()
                    });
            }
          //print warnings in case of multiple mappings
            if(resultIterator.hasNext()){ 
                log.warn("Multiple Mappings found for Entity {}!",reference);
                log.warn("  > {} -> returned instance",mapping.getId());
                while(resultIterator.hasNext()){
                    log.warn("  > {} -> ignored",resultIterator.next());
                }
            }
            return (EntityMapping)mapping;
        } else {
            log.debug("No Mapping found for Entity {}",reference);
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
        QueryResultList<Representation> resultList = lookupYard().findRepresentation(fieldQuery);
        Collection<EntityMapping> mappings = new HashSet<EntityMapping>();
        for(Representation rep : resultList){
            Sign entityMapping = signFactory.getSign(config.getID(), rep);
            if(entityMapping.getType() == SignTypeEnum.EntityMapping){
                mappings.add((EntityMapping)entityMapping);
            } else {
                // This query should only return EntityMappings 
                //  ... something very bad happens :(
                log.error("Unable to create EntityMapping {} for Symbol {} because SignType != EntityMapping but {}!",
                    new Object[]{
                                 entityMapping.getId(),
                                 symbol,
                                 entityMapping.getType()
                    });
            }
        }
        return mappings;
    }

    protected Sign getSignFormYard(String id) throws YardException {
        Representation rep = lookupYard().getRepresentation(id);
        if(rep != null){
            return signFactory.getSign(config.getID(), rep);
        } else {
            return null;
        }
    }
    

    @Override
    public EntityMapping getMappingById(String id) throws IllegalArgumentException, EntityhubException{
        if(id == null || id.isEmpty()){
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL nor empty");
        }
        Sign mapping = getSignFormYard(id);
        if(mapping == null || mapping instanceof EntityMapping){
            return (EntityMapping)mapping;
        } else {
            log.info("The parsed id does not represent an Mapping, but a {} (uri={}) -> return null!",
                mapping.getType(),mapping.getType().getUri());
            return null;
        }
    }
    @Override
    public FieldQueryFactory getQueryFavtory() {
        Yard entityhubYard = getYard();
        return entityhubYard==null? //if no yard available
                DefaultQueryFactory.getInstance(): //use the default
                    entityhubYard.getQueryFactory(); //else return the query factory used by the yard
    }
    @Override
    public FieldMapper getFieldMappings() {
        return fieldMapper;
    }
    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws YardException{
        return lookupYard().find(query);
    }
    @Override
    public QueryResultList<String> findSymbolReferences(FieldQuery query) throws YardException{
        return lookupYard().findReferences(query);
    }
    @Override
    public QueryResultList<Symbol> findSymbols(FieldQuery query) throws YardException{
        QueryResultList<String> references = lookupYard().findReferences(query);
        List<Symbol> symbols = new ArrayList<Symbol>(references.size());
        for(String reference : references){
            Symbol symbol = lookupSymbol(reference);
            if(symbol != null){
                symbols.add(symbol);
            } else {
                log.warn("Unable to create Symbol for Reference "+reference+" in the Yard usd by the entity hub [id="+config.getEntityhubYardId()+"] -> ignore reference");
            }
        }
        return new QueryResultListImpl<Symbol>(references.getQuery(), symbols, Symbol.class);
    }

}
