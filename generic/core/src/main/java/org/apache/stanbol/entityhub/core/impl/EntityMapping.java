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

import java.util.Date;
import java.util.Set;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper over an Entity to allow API based access to the information defined
 * for EntityMappings.<p>
 * In addition this class provides some static utilities that allow to retrieve
 * some values without creating a wrapper instance over an entity.
 * @author Rupert Westenthaler
 *
 */
public class EntityMapping extends EntityWrapper {

    private static final Logger log = LoggerFactory.getLogger(EntityMapping.class);
    /**
     * The default state for newly created instances if not otherwise configured
     */
    public static final MappingState DEFAULT_MAPPING_STATE = MappingState.proposed;
    /**
     * The property to be used for the id of the mapped entity
     */
    public static final String SOURCE = RdfResourceEnum.mappingSource.getUri();
    /**
     * The property to be used for the id of the mapped symbol
     */
    public static final String TARGET = RdfResourceEnum.mappingTarget.getUri();
    /**
     * The property to be used for the state of the MappedEntity instance
     */
    public static final String STATE = RdfResourceEnum.hasMappingState.toString();
    /**
     * The property used to hold the expires date of the representation (if any)
     */
    public static final String EXPIRES = RdfResourceEnum.expires.getUri();
    /**
     * The "rdf:type" property
     */
    private static final String RDF_TYPE = NamespaceEnum.rdf+"type";

    /**
     * Used to check if this type is present for wrapped entities
     */
    private static final Reference ENTITY_MAPPING_TYPE = 
        InMemoryValueFactory.getInstance().createReference(RdfResourceEnum.EntityMapping.getUri());
    /**
     * Creates a EntityMapping wrapper over an Entity instance
     * @param siteId
     * @param representation The representation that holds the state for the new EntityMapping instance
     * @throws IllegalArgumentException If the EntityMapping Instance can not be created based on the parsed parameter.
     * This includes <ul>
     * <li> the Entityhub ID is <code>null</code> or empty
     * <li> the parsed representation does not define a link to an entity
     *      (provide a value for the {@link EntityMapping#SOURCE} field)
     * <li> the parsed representation does not define a link to a symbol
     *      (provide a value for the {@link EntityMapping#TARGET} field)
     * <li> the representation is <code>null</code>
     * </ul>
     */
    public EntityMapping(Entity entity) {
        this(entity,true);
    }
    private EntityMapping(Entity entity, boolean validate){
        super(entity);
        if(entity == null){
            throw new IllegalArgumentException("The parsed Entity MUST NOT be NULL");
        }
        if(validate && !isValid(entity)){
            throw new IllegalArgumentException(String.format(
                "The parsed Entity %s MUST BE of rdf:type %s!",
                entity,RdfResourceEnum.EntityMapping.getUri()));
       }
    }
    /**
     * Checks if the parsed Entity can be wrapped by this EntityMapping wrapper.
     * Currently it checks only if the rdf:type entityhub:EntityMapping is
     * present
     * @param entity the entity to check
     * @return
     */
    public static boolean isValid(Entity entity){
        if(entity != null){
            Set<Reference> types = ModelUtils.asSet(entity.getRepresentation().getReferences(RDF_TYPE));
            return types.contains(ENTITY_MAPPING_TYPE);
        } else {
            return false;
        }
    }
    /**
     * If necessary adds missing requirements to the parsed entity and than
     * returns the wrapped Entity.
     * @param entity the entity
     */
    public static EntityMapping init(Entity entity){
        entity.getRepresentation().addReference(RDF_TYPE,RdfResourceEnum.EntityMapping.getUri());
        return new EntityMapping(entity,true);
    }
    /**
     * Getter for the ID of the source entity
     * @return the source entity
     */
    public final String getSourceId() {
        return getSourceId(wrappedEntity);
    }
    /**
     * Getter for the value of the {@link #SOURCE} property of the parsed
     * Entity.
     * @param entity the entity
     * @return the value or <code>null</code> if not present.
     */
    public static String getSourceId(Entity entity) {
        Object id = entity.getRepresentation().getFirst(SOURCE);
        return id != null?id.toString():null;
    }
    /**
     * Setter for the source of the mapping
     * @param source the source
     * @throws IllegalArgumentException if the parsed source is <code>null</code>
     * or an empty string
     */
    public final void setSourceId(String source) throws IllegalArgumentException {
        if(source == null || source.isEmpty()){
            throw new IllegalArgumentException("The ID of the source MUST NOT be NULL nor empty");
        }
        wrappedEntity.getRepresentation().setReference(SOURCE, source);
    }
    
    /**
     * Getter for the date this representation expires. If this representation
     * does not expire this method returns <code>null</code>.
     * @return the expire date or <code>null</code> if not applicable.
     */
    public final Date getExpires() {
        return wrappedEntity.getRepresentation().getFirst(EXPIRES, Date.class);
    }

    /**
     * The state of this mapping
     * @return the state
     */
    public final MappingState getState() {
        Reference stateUri = wrappedEntity.getRepresentation().getFirstReference(STATE);
        if(stateUri != null){
            if(MappingState.isState(stateUri.getReference())){
                return MappingState.getState(stateUri.getReference());
            } else {
                log.warn("Value {} for field {} is not a valied MappingState! -> return null",
                    stateUri,STATE);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Getter for the ID of the target entity
     * @return the target entity of this mapping
     */
    public final String getTargetId() {
        return getTargetId(wrappedEntity);
    }
    /**
     * Getter for the {@link #TARGET} value of the parsed Entity.
     * @param entity the entity
     * @return the value of <code>null</code> if not present
     */
    public static final String getTargetId(Entity entity){
        Object id =  entity.getRepresentation().getFirst(TARGET);
        return id != null?id.toString():null;
    }
    
    /**
     * Setter for the target of this mapping
     * @param target the id of the target
     * @throws IllegalArgumentException if the parsed target is <code>null</code>
     * or an empty string
     */
    public final void setTargetId(String target) throws IllegalArgumentException {
        if(target == null || target.isEmpty()){
            throw new IllegalArgumentException("The parsed target id MUST NOT be NULL nor empty!");
        }
        wrappedEntity.getRepresentation().setReference(TARGET, target);
    }

    /**
     * Setter for the expire date for this representation.
     * @param date the date or <code>null</code> if this representation does not
     * expire
     */
    public final void setExpires(Date date) {
        wrappedEntity.getRepresentation().set(EXPIRES, date);
    }

    /**
     * Setter for the mapping state
     * @param state the new state
     * @throws IllegalArgumentException if the parsed state is <code>null</code>
     */
    public final void setState(MappingState state) throws IllegalArgumentException {
        if(state != null){
            wrappedEntity.getRepresentation().setReference(STATE, state.getUri());
        } else {
            throw new IllegalArgumentException("MappingState can not be set to NULL!");
        }
    }
    /**
     * Setter for the ID of the Entity (and the Representation) this metadata
     * are about
     * @param id the id of the entity
     */
    public final void setEntityId(String id){
        wrappedEntity.getRepresentation().setReference(
            RdfResourceEnum.aboutRepresentation.getUri(), id);
    }
    /**
     * Getter for the ID of the Entity (and the Representation) this metadata
     * are about
     * @return the id of the entity this metadata are about
     */
    public final String getEntityId(){
        Reference ref = wrappedEntity.getRepresentation().getFirstReference(
            RdfResourceEnum.aboutRepresentation.getUri());
        return ref == null ? null : ref.getReference();
    }
}
