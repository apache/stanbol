package org.apache.stanbol.entityhub.core.model;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the default implementation of MappedEntity that uses a {@link Representation}
 * to store the Data.
 * @author Rupert Westenthaler
 */
public class DefaultEntityMappingImpl extends DefaultSignImpl implements EntityMapping {

    private static final Logger log = LoggerFactory.getLogger(DefaultEntityMappingImpl.class);

    public static final Map<String,MappingState> MAPPING_STATE_MAP;
    static{
        Map<String,MappingState> tmp = new HashMap<String, MappingState>();
        for(MappingState state : MappingState.values()){
            tmp.put(state.getUri(), state);
        }
        MAPPING_STATE_MAP = Collections.unmodifiableMap(tmp);
    }
    /**
     * Creates a new EntityMapping between the parsed symbol and entity
     * @param entityhubId The ID of the Entityhub that defines this mapping
     * @param symbol the ID of the symbol
     * @param entity the ID of the entity
     * @param state the state or <code>null</code> to use the {@link EntityMapping#DEFAULT_MAPPING_STATE}
     * @param representation The representation to store the information of this EntityMapping
     * @throws IllegalArgumentException If the EntityMapping Instance can not be created based on the parsed parameter.
     * This includes <ul>
     * <li> the Entityhub ID is <code>null</code> or empty
     * <li> the symbol ID is <code>null</code> or empty
     * <li> the entity ID is <code>null</code> or empty
     * <li> the representation is <code>null</code>
     * </ul>
     * TODO: Maybe also add an valid {@link MappingState} value for the {@link EntityMapping#STATE}
     *       to the requirements
     */
    public DefaultEntityMappingImpl(String entityhubId, String symbol,String entity,MappingState state,Representation representation) throws IllegalArgumentException {
        super(entityhubId,representation);
        if(symbol == null || symbol.isEmpty()){
            throw new IllegalArgumentException("Parsed symbol ID MUST NOT be NULL nor empty");
        }
        if(entity == null || entity.isEmpty()){
            throw new IllegalArgumentException("Parsed entity ID MUST NOT be NULL nor empty");
        }
        representation.setReference(EntityMapping.SYMBOL_ID, symbol);
        representation.setReference(EntityMapping.ENTITY_ID, entity);
        if(state == null){
            state = EntityMapping.DEFAULT_MAPPING_STATE;
        }
        representation.setReference(EntityMapping.STATE, state.getUri());
    }
    /**
     *
     * @param siteId
     * @param representation The representation that holds the state for the new EntityMapping instance
     * @throws IllegalArgumentException If the EntityMapping Instance can not be created based on the parsed parameter.
     * This includes <ul>
     * <li> the Entityhub ID is <code>null</code> or empty
     * <li> the parsed representation does not define a link to an entity
     *      (provide a value for the {@link EntityMapping#ENTITY_ID} field)
     * <li> the parsed representation does not define a link to a symbol
     *      (provide a value for the {@link EntityMapping#SYMBOL_ID} field)
     * <li> the representation is <code>null</code>
     * </ul>
     */
    public DefaultEntityMappingImpl(String siteId, Representation representation) {
        super(siteId,representation);
        //check required fields
        if(getEntityId() == null){
            throw new IllegalArgumentException("Representation "+getId()+" does not define required field "+EntityMapping.ENTITY_ID);
        }
        if(getSymbolId() == null){
            throw new IllegalArgumentException("Representation "+getId()+" does not define required field "+EntityMapping.SYMBOL_ID);
        }
    }
    @Override
    public String getEntityId() {
        Object id = representation.getFirst(EntityMapping.ENTITY_ID);
        return id != null?id.toString():null;
    }

    @Override
    public Date getExpires() {
        return representation.getFirst(EntityMapping.EXPIRES, Date.class);
    }

    @Override
    public MappingState getState() {
        Reference stateUri = representation.getFirstReference(EntityMapping.STATE);
        MappingState state;
        if(stateUri != null){
            state = MAPPING_STATE_MAP.get(stateUri.getReference());
        } else {
            state = null;
        }
        if(state == null){
            log.warn("Value "+stateUri+" for field "+EntityMapping.STATE+" is not a valied MappingState! -> return null");
            return null;
        } else {
            return state;
        }
    }

    @Override
    public String getSymbolId() {
        Object id =  representation.getFirst(EntityMapping.SYMBOL_ID);
        return id != null?id.toString():null;
    }

    @Override
    public void setExpires(Date date) {
        representation.set(EntityMapping.EXPIRES, date);
    }

    @Override
    public void setState(MappingState state) throws IllegalArgumentException {
        if(state != null){
            representation.setReference(EntityMapping.STATE, state.getUri());
        } else {
            throw new IllegalArgumentException("MappingState can not be set to NULL!");
        }
    }

}
