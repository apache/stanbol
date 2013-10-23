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
import java.util.Iterator;

import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.util.ToStringIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * wrapper over an Entity that allows API based read/write access
 * to metadata typically needed by the Entityhub Implementation.
 * @author Rupert Westenthaler
 *
 */
public class ManagedEntity extends EntityWrapper {

    private static final Logger log = LoggerFactory.getLogger(ManagedEntity.class);
    /**
     * The default state for new symbols if not defined otherwise
     */
    public static final ManagedEntityState DEFAULT_SYMBOL_STATE = ManagedEntityState.proposed;
    /**
     * The property to be used for the symbol label
     */
    public static final String LABEL = RdfResourceEnum.label.getUri();
    /**
     * The property to be used for the symbol description
     */
    public static final String DESCRIPTION = RdfResourceEnum.description.getUri();
    /**
     * The property to be used for the symbol state
     */
    public static final String STATE = RdfResourceEnum.hasState.getUri();
    /**
     * The property used for linking to successors
     */
    public static final String SUCCESSOR = RdfResourceEnum.successor.getUri();
    /**
     * The property used for linking to predecessors
     */
    public static final String PREDECESSOR = RdfResourceEnum.predecessor.getUri();


    private String defaultLanguage = null;

    private static final String[] ALT_LABEL_LANGUAGES = new String[]{null,"en"};

    /**
     * Holds a reference to the {@link Entity#getMetadata()} of the
     * wrapped entity
     */
    private final Representation metadata;
    /**
     * Creates a wrapper over an Entity that allows API based read/write access
     * to metadata typically needed by the Entityhub Implementation.
     * @param entity the wrapped entity
     * @throws IllegalArgumentException if the parsed Entity is <code>null</code>
     */
    public ManagedEntity(Entity entity) throws IllegalArgumentException {
        this(entity,true);
    }
    /**
     * Internally used to allow parsing <code>validate == false</code> in cases
     * the validation is not necessary. 
     * @param entity
     * @param validate
     */
    private ManagedEntity(Entity entity,boolean validate){
        super(entity);
//        if(entity == null){
//            throw new IllegalArgumentException("The parsed Entity MUST NOT be NULL");
//        }
        if(validate && !canWrap(entity)){
            throw new IllegalArgumentException(String.format(
                "Unable to wrap Entity %s",entity));
        }
        this.metadata = wrappedEntity.getMetadata();
    }
    /**
     * Checks if the parsed Entity can be wrapped as a locally managed entity.
     * This checks currently of a {@link ManagedEntityState} is defined by the
     * metadata.
     * @param entity the entity to check
     * @return the state
     */
    public static boolean canWrap(Entity entity) {
        //check the metadata for
        //if the entity is managed locally
        //if the entity has an state
        Reference stateUri = entity.getMetadata().getFirstReference(STATE);
        if(stateUri == null ||
                !ManagedEntityState.isState(stateUri.getReference())){
            return false;
        }
        //check the about
        String entityId = ModelUtils.getAboutRepresentation(entity.getMetadata());
        if(entityId == null || !entityId.equals(entity.getRepresentation().getId())){
            return false;
        }
        return true;
    }
    /**
     * Sets the parsed default state to the metadata if no other one is already
     * present and that wraps the entity as locally managed entity.
     * @param entity the entity
     * @param defaultState the default state used if no one is yet defined for
     * this entity
     * @return the wrapped entity
     */
    public static ManagedEntity init(Entity entity, ManagedEntityState defaultState){
        Reference stateUri = entity.getMetadata().getFirstReference(STATE);
        if(stateUri == null ||
                !ManagedEntityState.isState(stateUri.getReference())){
            entity.getMetadata().setReference(STATE, defaultState.getUri());
        }
        String entityId = ModelUtils.getAboutRepresentation(entity.getMetadata());
        if(entityId == null){
            entity.getMetadata().setReference(
                RdfResourceEnum.aboutRepresentation.getUri(), 
                entity.getRepresentation().getId());
        } else if(!entityId.equals(entity.getRepresentation().getId())){
            //the metadata are about a different Entity ->
            throw new IllegalArgumentException(String.format(
                "The Metadata of the parsed Entity are not about the Entity ("+
                "entity: %s | metadataId: %s | metadataAbout: %s)",
                entity.getRepresentation().getId(),entity.getMetadata().getId(),
                entityId));
        }//else the ID value is OK
        return new ManagedEntity(entity,false);
    }

    /**
     * Adds a description in the default language to the Symbol
     * @param description the description
     */
    public final void addDescription(String description) {
        metadata.addNaturalText(DESCRIPTION, description,defaultLanguage);
    }

    /**
     * Adds a description in the parsed language to the Symbol
     * @param description the description
     * @param lanugage the language. <code>null</code> indicates to use no language tag
     */
    public final void addDescription(String description, String lanugage) {
        metadata.addNaturalText(DESCRIPTION, description,lanugage);
    }

    /**
     * Adds the symbol with the parsed ID as a predecessor
     * @param predecessor the id of the predecessors
     */
    public final void addPredecessor(String predecessor) {
        metadata.addReference(PREDECESSOR, predecessor);
    }

    /**
     * Adds the symbol with the parsed ID as a successor
     * @param successor the id of the successor
     */
    public final void addSuccessor(String successor) {
        metadata.addReference(SUCCESSOR, successor);

    }

    /**
     * Getter for the descriptions of this symbol in the default language.
     * @return The descriptions or an empty collection.
     */
    public final Iterator<Text> getDescriptions() {
        return metadata.getText(DESCRIPTION);
    }

    /**
     * Getter for the short description as defined for the parsed language.
     * @param lang The language. Parse <code>null</code> for values without language tags
     * @return The description or <code>null</code> if no description is defined
     * for the parsed language.
     */
    public final Iterator<Text> getDescriptions(String lang) {
        return metadata.get(DESCRIPTION, lang);
    }

    /**
     * The label of this Symbol in the default language
     * @return the label
     */
    public final String getLabel() {
        String label = getLabel(defaultLanguage);
        if(label == null){ //no label for the default language
            //search labels in other languages
            Text altLabel = metadata.getFirst(LABEL, ALT_LABEL_LANGUAGES);
            if(altLabel == null){
                Iterator<Text> labels = metadata.getText(LABEL);
                if(labels.hasNext()){
                    altLabel = labels.next();
                }
            }
            return altLabel!=null?altLabel.getText():null;
        } else {
            return label;
        }
    }

    /**
     * The preferred label of this Symbol in the given language or
     * <code>null</code> if no label for this language is defined
     * TODO: how to handle internationalisation.
     * @param lang the language
     * @return The preferred label of this Symbol in the given language or
     * <code>null</code> if no label for this language is defined
     */
    public final String getLabel(String lang) {
        Text label = metadata.getFirst(LABEL, lang);
        return label!=null?label.getText():null;
    }

    /**
     * Getter for the ID's of the symbols defined as predecessors of this one.
     * @return The id's of the symbols defined as predecessors of this one or an
     * empty list if there are no predecessors are defined.
     */
    public final Iterator<String> getPredecessors() {
        return new ToStringIterator(metadata.get(PREDECESSOR));
    }

    /**
     * Getter for the state of this symbol
     * @return the state
     */
    public final ManagedEntityState getState() {
        Reference stateUri = metadata.getFirstReference(STATE);
        if(stateUri != null){
            if(ManagedEntityState.isState(stateUri.getReference())){
                return ManagedEntityState.getState(stateUri.getReference());
            } else {
                log.warn("Value {} for field {} is not a valied SymbolState! -> return null",
                    stateUri,STATE);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Getter for the ID's of the symbols defined as successors of this one.
     * @return The id's of the symbols defined as successors of this one or an
     * empty list if there are no successors are defined.
     */
    public final Iterator<String> getSuccessors() {
        return new ToStringIterator(metadata.get(SUCCESSOR));
    }

    /**
     * Returns if this Symbols does have any predecessors
     * @return Returns <code>true</code> if predecessors are defined for this
     * symbol; otherwise <code>false</code>.
     */
    public final boolean isPredecessors() {
        return getPredecessors().hasNext();
    }

    /**
     * Returns if this Symbols does have any successors
     * @return Returns <code>true</code> if successors are defined for this
     * symbol; otherwise <code>false</code>.
     */
    public final boolean isSuccessor() {
        return getSuccessors().hasNext();
    }

    /**
     * Removes the description in the default language from the Symbol
     * @param description the description to remove
     */
    public final void removeDescription(String description) {
        metadata.removeNaturalText(DESCRIPTION,description,defaultLanguage);
    }

    /**
     * Removes the description in the parsed language from the Symbol
     * @param description the description to remove
     * @param language the language. <code>null</code> indicates to use no language tag
     */
    public final void removeDescription(String description, String language) {
        metadata.removeNaturalText(DESCRIPTION,description,language);
    }

    /**
     * Removes the symbol with the parsed ID as a predecessor
     * @param predecessor the id of the predecessor to remove
     */
    public final void removePredecessor(String predecessor) {
        metadata.removeReference(PREDECESSOR, predecessor);
    }

    /**
     * Removes the symbol with the parsed ID as a successor
     * @param successor the id of the successor to remove
     */
    public final void removeSuccessor(String successor) {
        metadata.removeReference(SUCCESSOR, successor);
    }

    /**
     * Setter for the Label in the default Language
     * @param label
     */
    public final void setLabel(String label) {
        metadata.setNaturalText(LABEL, label, defaultLanguage);
    }

    /**
     * Setter for a label of a specific language
     * @param label the label
     * @param language the language. <code>null</code> indicates to use no language tag
     */
    public final void setLabel(String label, String language) {
        metadata.setNaturalText(LABEL, label, language);
    }

    /**
     * Setter for the state of the Symbol
     * @param state the new state
     * @throws IllegalArgumentException if the parsed state is <code>null</code>
     */
    public final void setState(ManagedEntityState state) throws IllegalArgumentException {
        if(state != null){
            metadata.setReference(STATE, state.getUri());
        } else {
            throw new IllegalArgumentException("SymbolState can not be set to NULL!");
        }
    }
    /**
     * Getter for the default language used for {@link #getLabel()}
     * @return the preferred language used for {@link #getLabel()}
     */
    public final String getDefaultLanguage() {
        return defaultLanguage;
    }
    /**
     * Setter for the default language used for {@link #getLabel()} (
     * <code>null</code> is supported)
     * @param defaultLanguage the preferred language used for {@link #getLabel()} 
     */
    public final void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
//    /**
//     * Setter for the ID of the Metadata for this managed entity.
//     * @param id the id of the metadata of this entity
//     */
//    public final void setMetadataId(String id){
//        wrappedEntity.getRepresentation().setReference(
//            RdfResourceEnum.hasMetadata.getUri(), id);
//    }
//    /**
//     * Getter for the ID of the Metadata for this managed entity.
//     * @return the id of the metadata of entity
//     */
//    public final String getMetadataId(){
//        Reference ref = wrappedEntity.getRepresentation().getFirstReference(
//            RdfResourceEnum.hasMetadata.getUri());
//        return ref == null ? null : ref.getReference();
//    }
}
