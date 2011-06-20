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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.model.EntityImpl;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.ReferenceConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
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
public final class EntityhubImpl implements Entityhub {//, ServiceListener {

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
     * Tracks the availability of the Yard used by the Entityhub.
     */
    private ServiceTracker entityhubYardTracker; //reference initialised in the activate method
    /**
     * The Configuration of the Entityhub
     */
    @Reference // 1..1, static
    private EntityhubConfiguration config;
    /**
     * The site manager is used to search for entities within the Entityhub framework
     */
    @Reference // 1..1, static
    private ReferencedSiteManager siteManager;

    private static final String DEFAULT_MANAGED_ENTITY_PREFIX = "entity";
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
    public EntityhubConfiguration getConfig() {
        return config;
    }
    
    @Override
    public Entity lookupLocalEntity(String entity) throws YardException{
        return lookupLocalEntity(entity,false);
    }

    @Override
    public Entity lookupLocalEntity(String entityId, boolean create) throws YardException {
        Entity entity = getEntity(entityId);
        if(entity != null){
            return entity;
        } else {
            //parsed reference was not a locally managed entity ->
            //search for an mapped Entity
            Entity entityMapping = getMappingBySource(entityId);
            if(entityMapping != null){
                entity = getEntity(EntityMapping.getTargetId(entityMapping));
                if(entity != null){
                    return entity;
                } else {
                    log.warn("Unable to find Entity for Entity Mapping "+entityMapping+"!");
                    return recoverEntity(EntityMapping.getTargetId(entityMapping));
                }
            } else if(create){
                //search if the parsed reference is known by any referenced site
                // and if YES, create a new Symbol
                Entity remoteEntity = siteManager.getEntity(entityId);
                 if(remoteEntity == null){ //id not found by any referred site
                     return null;
                 } else {
                     return importEntity(remoteEntity);
                 }
            } else {
                return null;
            }
        }
    }
    @Override
    public Entity importEntity(String reference) throws IllegalStateException, IllegalArgumentException, YardException {
        Entity entity = getEntity(reference);
        if(entity == null){
            Entity entityMapping = getMappingBySource(reference);
            if(entityMapping == null){
                Entity remoteEntity = siteManager.getEntity(reference);
                if(remoteEntity == null){
                    return null;
                } else {
                    return importEntity(remoteEntity);
                }
            } else {
                throw new IllegalStateException(String.format(
                    "The reference %s is already imported to the Entityhub " +
                    "(local Entity %s)",reference,
                    EntityMapping.getTargetId(entityMapping)));
            }
        } else {
            throw new IllegalStateException("The parsed id "+reference+" refers " +
            		"to an Entity managed by the Entityhub (entity="+entity+")!");
        }
    }
    /**
     * Recovers a missing locally managed entity based on the available 
     * {@link EntityMapping}s and the data available through referenced sites.
     * @param entityId the id of the missing locally managed entity
     * @return the recovered Entity or <code>null</code> if the recovering is 
     * not possible
     */
    private Entity recoverEntity(String entityId) {
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
    public Entity getEntity(String entityId) throws IllegalArgumentException,IllegalStateException, YardException {
        if(entityId == null || entityId.isEmpty()){
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL nor empty!");
        }
        Yard entityhubYard = lookupYard();
        Entity entity = loadEntity(entityhubYard, entityId);
        if(entity == null){
            return null;
        } else if (ManagedEntity.canWrap(entity)){
            return entity;
        } else {
            log.info("The parsed id does not represent a locally managed Entity {}", entity);
            return null;
        }
    }
    @Override
    public boolean isRepresentation(String entityId) throws EntityhubException, IllegalArgumentException {
        if(entityId == null || entityId.isEmpty()){
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL nor empty!");
        }
        return lookupYard().isRepresentation(entityId);
    }
    @Override
    public Entity store(Representation representation) throws EntityhubException, IllegalArgumentException {
        if(representation == null){
            throw new IllegalArgumentException("The parsed Representation MUST NOT be NULL!");
        }
        Yard yard = lookupYard();
        //parse only the id of the representation, because we need the current
        //stored version of the entity!
        Entity entity = loadEntity(yard, representation.getId());
        //now we need to check if the parsed representation is the data or the
        //metadata of the Entity
        ManagedEntity updated;
        if(entity == null || representation.getId().equals(entity.getRepresentation().getId())){
            //update the data or create a new entity
            updated = ManagedEntity.init(new EntityImpl(config.getID(), representation,
                entity != null ? entity.getMetadata() : null),
                config.getDefaultManagedEntityState());
            if(entity == null){ //add creation date
                updated.setCreated(new Date());
            }
        } else {
            //update the metadata
            entity = new EntityImpl(config.getID(), entity.getRepresentation(),
                representation);
            //we need to validate the metadata
            updated = ManagedEntity.init(
                entity, config.getDefaultManagedEntityState());
        }
        storeEntity(yard,updated.getWrappedEntity());
        return updated.getWrappedEntity();
    }
    @Override
    public Entity delete(String id) throws EntityhubException, IllegalArgumentException {
        if(id == null || id.isEmpty()){
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL nor emtpty!");
        }
        Yard yard = lookupYard();
        Entity entity = loadEntity(yard, id);
        if(entity != null){
            log.debug("delete Entity {} as requested by the parsed id {}",entity.getId(),id);
            //we need to remove all mappings for this Entity
            deleteMappingsbyTarget(yard,entity.getId());
            deleteEntity(yard,entity);
        } else {
            log.debug("Unable to delete Entity for id {}, because no Entity for this id is" +
            		"managed by the Entityhub",id);
        }
        return entity;
    }
    @Override
    public Entity setState(String id, ManagedEntityState state) throws EntityhubException,
                                                               IllegalArgumentException {
        if(id == null || id.isEmpty()){
            throw new IllegalStateException("The parsed id MUST NOT be NULL nor empty!");
        }
        if(state == null){
            throw new IllegalStateException("The parsed state for the Entity MUST NOT ne NULL");
        }
        Yard yard = lookupYard();
        Entity entity = loadEntity(yard, id);
        if(entity != null){
            ManagedEntity managed = new ManagedEntity(entity);
            if(managed.getState() != state){
                managed.setState(state);
                storeEntity(yard, entity);
            }
        }
        return entity;
    }
    /**
     * Deleted both the representation and the metadata of an Entity
     * @param yard the yard to delete the entity from
     * @param entity the entity to delete
     * @throws YardException an any Exception while deleting the Entity
     */
    private void deleteEntity(Yard yard,Entity entity) throws YardException {
        if(entity != null){
            yard.remove(Arrays.asList(
                entity.getRepresentation().getId(),
                entity.getMetadata().getId()));
        }
    }
    private void deleteEntities(Yard yard, Collection<String> ids) throws YardException {
        FieldQuery fieldQuery = getQueryFactory().createFieldQuery();
        Collection<String> toDelete = new HashSet<String>(ids);
        for(String id : ids){
            if(id != null && !id.isEmpty()){
                fieldQuery.setConstraint(RdfResourceEnum.aboutRepresentation.getUri(), new ReferenceConstraint(id));
                for(Iterator<String> it = yard.findReferences(fieldQuery).iterator();it.hasNext();){
                    toDelete.add(it.next());
                }
            }
        }
        if(!toDelete.isEmpty()){
            yard.remove(toDelete);
        }
        
    }

    private void deleteMappingsbyTarget(Yard yard,String id) throws YardException {
        if(id != null && !id.isEmpty()){
            FieldQuery fieldQuery = getQueryFactory().createFieldQuery();
            fieldQuery.setConstraint(RdfResourceEnum.mappingTarget.getUri(), new ReferenceConstraint(id));
            deleteEntities(yard, ModelUtils.asCollection(
                yard.findReferences(fieldQuery).iterator()));
        }
    }

    /**
     * Imports the Entity
     * @param remoteEntity the Entity to import
     * @return the Entity created and stored within the Entityhub
     * @throws YardException
     */
    protected Entity importEntity(Entity remoteEntity) throws YardException{
        if(remoteEntity == null){
            return null;
        }
        ReferencedSite site = siteManager.getReferencedSite(remoteEntity.getSite());
        if(site == null){
            log.warn("Unable to import Entity {} because the ReferencedSite {} is currently not active -> return null",
                remoteEntity.getId(),remoteEntity.getSite());
            return null;
        }
        Yard entityhubYard = lookupYard();
        ValueFactory valueFactory = entityhubYard.getValueFactory();
        //Create the locally managed Entity
        Representation localRep = entityhubYard.create(constructResourceId(DEFAULT_MANAGED_ENTITY_PREFIX));
        Entity localEntity = loadEntity(entityhubYard, localRep);
        importEntity(remoteEntity, site, localEntity, valueFactory);

        //Second create and init the Mapping
        Representation entityMappingRepresentation = entityhubYard.create(
            constructResourceId(DEFAULT_MAPPING_PREFIX));
        Entity entityMappingEntity = loadEntity(entityhubYard, entityMappingRepresentation);
        establishMapping(localEntity, remoteEntity, site, entityMappingEntity);
        
        //Store the entity and the mappedEntity in the entityhubYard
        storeEntity(entityhubYard, localEntity);
        storeEntity(entityhubYard,entityMappingEntity);
        return localEntity;
    }
    /**
     * Imports a {@link Entity} from a {@link ReferencedSite}. This Method imports
     * the {@link Representation} by applying all configured mappings. It also
     * sets the {@link ManagedEntityState} to the configured default value by the 
     * referenced site of the imported entity or the default for the Entityhub 
     * if the site does not define this configuration.<p>
     * @param remoteEntity The entity to import
     * @param site the referenced site of the entity to import
     * @param localEntity the target entity for the import
     * @param valueFactory the valusFactory used to create instance while importing
     */
    private void importEntity(Entity remoteEntity,
                              ReferencedSite site,
                              Entity localEntity,
                              ValueFactory valueFactory) {
        
        ManagedEntityState state = site.getConfiguration().getDefaultManagedEntityState();
        if(state == null){
            state =  config.getDefaultManagedEntityState();
        }
        //this wrapper allows to use an API to write metadata
        ManagedEntity managedEntity = ManagedEntity.init(localEntity, state);
        FieldMapper siteMapper = site.getFieldMapper();
        FieldMapper mapper = this.fieldMapper.clone();
        for(FieldMapping siteMapping : siteMapper.getMappings()){
            mapper.addMapping(siteMapping);
        }
        //TODO: As soon as MappingActivities are implemented we need to add such
        //      information to the EntityMapping instance!
        mapper.applyMappings(remoteEntity.getRepresentation(), 
            localEntity.getRepresentation(),valueFactory);
        //set general metadata
        managedEntity.setCreated(new Date());
        //set the metadata required by the referenced site
        managedEntity.addAttributionLink(site.getConfiguration().getAttributionUrl());
        managedEntity.addAttributionText(site.getConfiguration().getAttribution(), null);
        //TODO: maybe replace with the URL of the site
        managedEntity.addContributorName(site.getConfiguration().getName());
    }
    /**
     * Creates a new {@link EntityMapping} for the parsed source and target 
     * {@link Entity}. This also sets the State and the expire date based on the 
     * configurations of the ReferencesSite of the source entity and the defaults of the 
     * Entityhub 
     * @param localEntity the locally managed entity (target for the mapping)
     * @param remoteEntity the remote Entity (source for the mapping)
     * @param site the referenced site managing the source Entity
     * @param entityMappingRepresentation the Entity for the mapping
     * @return the EntityMapping
     */
    private EntityMapping establishMapping(Entity localEntity,
                                           Entity remoteEntity,
                                           ReferencedSite site,
                                           Entity entityMappingEntity) {
        EntityMapping entityMapping = EntityMapping.init(entityMappingEntity);
        //now init the mappingState and the expireDate based on the config of the
        //ReferencedSite of the source entity (considering also the defaults of the entityhub)
        MappingState mappingState = site.getConfiguration().getDefaultMappedEntityState();
        if(mappingState == null){
            mappingState = config.getDefaultMappingState();
        }
        entityMapping.setState(mappingState);
        long expireDuration = site.getConfiguration().getDefaultExpireDuration();
        if(expireDuration < 0){
            Date expireDate = new Date(System.currentTimeMillis()+expireDuration);
            entityMapping.setExpires(expireDate);
        }
        entityMapping.setSourceId(remoteEntity.getId());
        entityMapping.setTargetId(localEntity.getId());
        //initialise additional metadata
        entityMapping.setCreated(new Date());
        
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
    public Entity getMappingBySource(String reference) throws YardException{
        if(reference == null){
            log.warn("NULL parsed as Reference -> call to getMappingByEntity ignored (return null)");
            return null;
        }
        FieldQuery fieldQuery = getQueryFactory().createFieldQuery();
        fieldQuery.setConstraint(RdfResourceEnum.mappingSource.getUri(), new ReferenceConstraint(reference));
        Yard entityhubYard = lookupYard();
        QueryResultList<Representation> resultList = entityhubYard.findRepresentation(fieldQuery);
        if(!resultList.isEmpty()){
            Iterator<Representation> resultIterator = resultList.iterator();
            Entity mapping = loadEntity(entityhubYard, resultIterator.next());
           //print warnings in case of multiple mappings
            if(resultIterator.hasNext()){ 
                log.warn("Multiple Mappings found for Entity {}!",reference);
                log.warn("  > {} -> returned instance",mapping.getId());
                while(resultIterator.hasNext()){
                    log.warn("  > {} -> ignored",resultIterator.next());
                }
            }
            if(!EntityMapping.isValid(mapping)){
                log.warn("Entity {} is not a valid EntityMapping. -> return null",mapping);
                mapping = null;
            }
            return mapping;
        } else {
            log.debug("No Mapping found for Entity {}",reference);
            return null;
        }
    }
    @Override
    public Collection<Entity> getMappingsByTarget(String targetId) throws YardException{
        if(targetId == null){
            log.warn("NULL parsed as Reference -> call to getMappingsBySymbol ignored (return null)");
            return null;
        }
        FieldQuery fieldQuery = getQueryFactory().createFieldQuery();
        fieldQuery.setConstraint(RdfResourceEnum.mappingTarget.getUri(), new ReferenceConstraint(targetId));
        Yard enttiyhubYard = lookupYard();
        QueryResultList<Representation> resultList = enttiyhubYard.findRepresentation(fieldQuery);
        Collection<Entity> mappings = new HashSet<Entity>();
        for(Representation rep : resultList){
            mappings.add(loadEntity(enttiyhubYard, rep));
        }
        return mappings;
    }

    /**
     * Loads an Entity (Representation and Metadata) for the parsed id. In case
     * the parsed id represents metadata, than the id of the returned Entity will
     * be different from the parsed id.
     * @param entityhubYard the yard used by the Entityhub
     * @param id the id of the data or the metadata of the Entity to load
     * @return the Entity or <code>null</code> if not found
     * @throws YardException On any error with the parsed Yard.
     */
    private Entity loadEntity(Yard entityhubYard,String id) throws YardException {
        return id == null || id.isEmpty() ? null :
            loadEntity(entityhubYard,entityhubYard.getRepresentation(id));
    }
    /**
     * Loads the Entity based on the parsed representation. The parsed
     * {@link Representation} can be both the data and the metadata. In case the
     * parsed representation are metadat the id of the returned Entity will be
     * not the same as the id of the parsed {@link Representation}.
     * @param entityhubYard the yard used by the Entityhub
     * @param rep the representation or metadata of an entity
     * @return the created Entity including both data and metadata or 
     * <code>null</code> if the parsed Representation does not represent a 
     * Representation managed by the Entityhub (this may be the case if an other
     * thread has deleted that Entity in the meantime)
     * @throws YardException On any error with the parsed Yard.
     */
    private Entity loadEntity(Yard entityhubYard, Representation rep) throws YardException {
        if(rep != null){
            Representation data;
            Representation metadata = null;
            String entityId = ModelUtils.getAboutRepresentation(rep);
            if(entityId != null){
                data = entityhubYard.getRepresentation(entityId);
                metadata = rep;
            } else {
                data = rep;
                entityId = rep.getId(); //needed for logs
            }
            if(data != null){
                metadata = lookupMetadata(entityhubYard, rep.getId(),true);
                return new EntityImpl(config.getID(), data,metadata);
            } else {
                log.warn("Unable find representation for Entity {} (metadata: {}",
                    entityId,metadata);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Lookups (or initialises) the metadata for the entity with the parsed id 
     * @param entityhubYard the yard to search the metadata
     * @param entityId The id of the entity
     * @param init if the metadata should be initialised of not existing
     * @return the metadata for that Entity or <code>null</code> if not existing
     * and <code>init == false</code>
     * @throws YardException
     */
    private Representation lookupMetadata(Yard entityhubYard, String entityId, boolean init) throws YardException {
        Representation metadata;
        //TODO: check the asumption that the Metadata always use the
        //      extension ".meta"
        String metaID = entityId+".meta";
        metadata = entityhubYard.getRepresentation(metaID);
        if(metadata == null){
            metadata = entityhubYard.create(metaID);
        }
        return metadata;
    }
    /**
     * Stores both the Representation and the Metadata of the parsed Entity to the
     * parsed yard.<p>
     * This Method also updated the modification date of the Metadata.
     * @param entityhubYard the Yard used to store the Entity
     * @param entity the stored entity
     * @throws YardException
     */
    private void storeEntity(Yard entityhubYard, Entity entity) throws YardException{
        if(entity != null){
            entityhubYard.store(entity.getRepresentation());
            entity.getMetadata().set(NamespaceEnum.dcTerms+"modified", new Date());
            entityhubYard.store(entity.getMetadata());
        }
    }

    @Override
    public Entity getMappingById(String id) throws IllegalArgumentException, EntityhubException{
        if(id == null || id.isEmpty()){
            throw new IllegalArgumentException("The parsed id MUST NOT be NULL nor empty");
        }
        Yard entityhubYard = lookupYard();
        Entity mapping = loadEntity(entityhubYard, id);
        if(mapping == null){
            return null;
        } else if(mapping != null && EntityMapping.isValid(mapping)){
            return mapping;
        } else {
            log.info("The Entity {} for the parsed id does not represent an EntityMapping -> return null!",
                mapping);
            return null;
        }
    }
    @Override
    public FieldQueryFactory getQueryFactory() {
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
    public QueryResultList<String> findEntityReferences(FieldQuery query) throws YardException{
        return lookupYard().findReferences(query);
    }
    @Override
    public QueryResultList<Entity> findEntities(FieldQuery query) throws YardException{
        QueryResultList<String> references = lookupYard().findReferences(query);
        List<Entity> entities = new ArrayList<Entity>(references.size());
        for(String reference : references){
            Entity entity = lookupLocalEntity(reference);
            if(entity != null){
                entities.add(entity);
            } else {
                log.warn("Unable to create Entity for Reference {} in the Yard " +
                		"usd by the entity hub [id={}] -> ignore reference",
                		reference,config.getEntityhubYardId());
            }
        }
        return new QueryResultListImpl<Entity>(references.getQuery(), entities, Entity.class);
    }

}
