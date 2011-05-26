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
package org.apache.stanbol.entityhub.core.model;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * Implementation of the Entity interface that also enforces the required 
 * relations between {@link Entity#getRepresentation() representation} and 
 * {@link Entity#getMetadata() metadata}.
 * 
 * @author Rupert Westenthaler
 *
 */
public class EntityImpl implements Entity{

    //private static final Logger log = LoggerFactory.getLogger(EntityImpl.class);
    
    private final Representation representation;
    private final String site;
    private final Representation metadata;

    /**
     * Creates a new Entity for the parsed site, representation and metadata.<p>
     * This Constructor checks for {@link RdfResourceEnum#hasMetadata} relations
     * in the {@link Entity#getRepresentation() representation} as well as
     * {@link RdfResourceEnum#aboutRepresentation} relations in the 
     * {@link Entity#getMetadata() metadata}. <p>
     * If such are present, the IDs of the parsed representation and metadata
     * are validated against those. If they are not present this values are
     * initialised based on the IDs of the parsed representation and metadata.<p>
     * In case <code>null</code> is parsed as metadata, than a new
     * representation is created with the default ID (the ID of the represnetation
     * <code>+ ".meta"</code>.
     * @param siteId the site (MUST NOT be <code>null</code> nor empty)
     * @param representation the representation(MUST NOT be <code>null</code> nor empty)
     * @param metadata the metadata (MAY BE <code>null</code>)
     * @throws IllegalArgumentException In case <ul>
     * <li> <code>null</code> is parsed for any parameter other than the metadata
     * <li> the siteId is empty
     * <li> the id of the representation is not the same as the aboutness of the
     *      metadata
     * <li> the id of the metadata is not the same as the id referenced by the
     *      representation
     * </ul>
     */
    public EntityImpl(String siteId,Representation representation,Representation metadata) throws IllegalArgumentException {
        super();
        if(representation == null){
            throw new IllegalArgumentException("NULL value ist not allowed for the Representation");
        }
        if(siteId == null || siteId.isEmpty()){
            throw new IllegalStateException("Parsed SiteId MUST NOT be NULL nor empty!");
        }
        this.site = siteId;
        this.representation = representation;
        Reference representationRef;
        if(metadata != null){
            if(representation.getId().equals(metadata.getId())){
                throw new IllegalArgumentException("The ID of the Representation and " +
                "the Metadata MUST NOT BE the same!");
            }
            representationRef = metadata.getFirstReference(
                RdfResourceEnum.aboutRepresentation.getUri());
            if(representationRef != null && !representationRef.getReference().equals(
                representation.getId())){
                throw new IllegalArgumentException(String.format(
                    "The parsed Metadata are not about the representation of the" +
                    "Entity to be created (metadata aboutness=%s, representation=%s).",
                    representationRef.getReference(),representation.getId()));
            }
            this.metadata = metadata;
        } else { //init new metadata
            //This is typically used if no metadata are persisted for an entity
            //(e.g. for entities of a remote site).
            //However the created and initialised metadata can be persisted
            //afterwards.
            //The usage of a specific Representation implementation here is not
            //an issue because Yards need to support storage of any
            //Representation implementation!
            String metadataId = representation.getId()+".meta";
            this.metadata = InMemoryValueFactory.getInstance().createRepresentation(metadataId);
            representationRef = null;
        }
        //init the link from the metadata to the representation
        if(representationRef == null){
            this.metadata.setReference(RdfResourceEnum.aboutRepresentation.getUri(), representation.getId());
        }
        //add the rdf:type for Metadata
        this.metadata.addReference(NamespaceEnum.rdf+"type", RdfResourceEnum.Metadata.getUri());
    }
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(", id=");
        sb.append(getId());
        sb.append(", site=");
        sb.append(getSite());
        sb.append(", representation=");
        sb.append(getRepresentation());
        sb.append(", metadata=");
        sb.append(getMetadata());
        return sb.toString();
    }
    
    @Override
    public final String getSite() {
        return site;
    }

    @Override
    public final String getId() {
        return representation.getId();
    }

    @Override
    public final Representation getRepresentation() {
        return representation;
    }
    @Override
    public final Representation getMetadata(){
        return metadata;
    }
    @Override
    public int hashCode() {
        return representation.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof Entity && 
            representation.equals(((Entity)o).getRepresentation()) &&
            site.equals(((Entity)o).getSite()) &&
            metadata.equals(((Entity)o).getMetadata());
    }
    
}
