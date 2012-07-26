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

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Abstract super class for all Wrappers over Entities managed by the Entityhub. 
 * Provides also getter and setter for some general purpose metadata.
 * @author Rupert Westenthaler
 */
public abstract class EntityWrapper {
    
    protected final Entity wrappedEntity;
    public EntityWrapper(Entity entity) {
        if(entity == null){
            throw new IllegalArgumentException("The parsed Entity MUST NOT be NULL");
        }
        this.wrappedEntity = entity;
    }
    /**
     * Getter for the wrapped Entity
     * @return the wrapped entity
     */
    public final Entity getWrappedEntity() {
        return wrappedEntity;
    }
    
    /**
     * Setter for the creation date of this mapping.<p>
     * Note this is store in the metadata.
     * @param date the date
     */
    public final void setCreated(Date date) {
        if(date != null){
            wrappedEntity.getMetadata().set(NamespaceEnum.dcTerms+"created", date);
        }
    }
    /**
     * Getter for the creation date of this mapping
     * TODO: decide if that should be stored in the data or the metadata
     * @return the creation date. 
     */
    public final Date getCreated() {
        return wrappedEntity.getMetadata().getFirst(NamespaceEnum.dcTerms+"created", Date.class);
    }
    /**
     * Setter for the modified date (replaces existing values)
     * @param date the new date
     */
    public void setModified(Date date) {
        if(date != null){
            wrappedEntity.getMetadata().set(NamespaceEnum.dcTerms+"modified", date);
        }
    }
    /**
     * Getter for the last modified date
     * @return the date of the last modification
     */
    public final Date getModified() {
        return wrappedEntity.getMetadata().getFirst(NamespaceEnum.dcTerms+"modified", Date.class);
    }

    /**
     * Adds a link to a contributor (e.g. a site where some information where
     * imported)
     * @param reference the contributor
     */
    public final void addContributorLink(String reference) {
        if(reference != null && !reference.isEmpty()){
            wrappedEntity.getMetadata().addReference(NamespaceEnum.dcTerms+"contributor", reference);
        }
    }
    /**
     * Removes a reference to a contributor
     * @param reference the contributor
     */
    public final void removeContributorLink(String reference) {
        wrappedEntity.getMetadata().removeReference(NamespaceEnum.dcTerms+"contributor", reference);
    }
    /**
     * Adds a name to a contributor (e.g. a site where some information where
     * imported)
     * @param name the contributor
     */
    public final void addContributorName(String name) {
        if(name != null && !name.isEmpty()){
            wrappedEntity.getMetadata().addNaturalText(NamespaceEnum.dcTerms+"contributor", name);
        }
    }
    /**
     * Removes a contributor
     * @param name the contributor
     */
    public final void removeContributorName(String name) {
        wrappedEntity.getMetadata().removeNaturalText(NamespaceEnum.dcTerms+"contributor", name);
    }
    /**
     * Adds a reference to the creator to the entity (metadata)
     * @param reference the creator
     */
    public final void addCreatorLink(String reference) {
        if(reference != null && !reference.isEmpty()){
            wrappedEntity.getMetadata().addReference(NamespaceEnum.dcTerms+"creator", reference);
        }
    }
    /**
     * Removes a link to the creator
     * @param reference the creator
     */
    public final void removeCreatorLink(String reference) {
        wrappedEntity.getMetadata().removeReference(NamespaceEnum.dcTerms+"creator", reference);
    }

    /**
     * Adds a name to the creator to the entity (metadata)
     * @param name the creator
     */
    public final void addCreatorName(String name) {
        if(name != null && !name.isEmpty()){
            wrappedEntity.getMetadata().addNaturalText(NamespaceEnum.dcTerms+"creator", name);
        }
    }

    /**
     * Removes a creator
     * @param name the creator
     */
    public final void removeCreatorName(String name) {
        wrappedEntity.getMetadata().removeNaturalText(NamespaceEnum.dcTerms+"creator", name);
    }

    /**
     * Adds an attribution to the metadata of the entity
     * @param text the attribution
     * @param lang the language of the attribution (optional)
     */
    public final void addAttributionText(String text,String lang){
        if(text != null && !text.isEmpty()){
            wrappedEntity.getMetadata().addNaturalText(NamespaceEnum.cc+"attributionName", text,lang);
            wrappedEntity.getMetadata().addReference(NamespaceEnum.rdf+"type", NamespaceEnum.cc+"Work");
        }
    }
    /**
     * Adds an link to the attribution to the metadata of the entity
     * @param reference the link to the attribution
     */
    public final void addAttributionLink(String reference){
        if(reference != null && !reference.isEmpty()){
            wrappedEntity.getMetadata().addReference(NamespaceEnum.cc+"attributionURL", reference);
            wrappedEntity.getMetadata().addReference(NamespaceEnum.rdf+"type", NamespaceEnum.cc+"Work");
        }
    }
    /**
     * Removes all Attributions form the metadata fo the entity
     */
    public final void removeAttributions(){
        wrappedEntity.getMetadata().removeAll(NamespaceEnum.cc+"attributionURL");
        wrappedEntity.getMetadata().removeAll(NamespaceEnum.cc+"attributionName");
        checkForCcWork();
    }
    /**
     * Adds a reference to the license to the metadata of the entity
     * @param reference the license
     */
    public final void addLicenseUrl(String reference){
        if(reference != null && !reference.isEmpty()){
            wrappedEntity.getMetadata().addReference(NamespaceEnum.cc+"license", reference);
            wrappedEntity.getMetadata().addReference(NamespaceEnum.rdf+"type", NamespaceEnum.cc+"Work");
        }
    }
//    public final void setLicense(String name,String text,String lang){
//        if(name != null && text != null){
//            //add type, relation to the entity and the license information
//            wrappedEntity.getMetadata().addNaturalText("", name, lang);
//        } else {
//            throw new IllegalArgumentException("Both the license name and the text MUST NOT be NULL!");
//        }
//    }
    /**
     * Removes the license from the metadata of the entity
     * @param reference the license
     */
    public final void removeLicense(String reference){
        wrappedEntity.getMetadata().removeReference(NamespaceEnum.cc+"license", reference);
        checkForCcWork();
    }
    /**
     * checks if the cc:Work type can be removed
     */
    private void checkForCcWork(){
        if(wrappedEntity.getMetadata().getFirst(NamespaceEnum.cc+"license")==null && 
                wrappedEntity.getMetadata().getFirst(NamespaceEnum.cc+"attributionName")==null &&
                wrappedEntity.getMetadata().getFirst(NamespaceEnum.cc+"attributionURL")==null){
            wrappedEntity.getMetadata().removeReference(NamespaceEnum.rdf+"type", NamespaceEnum.cc+"Work");
        }
    }
}
