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
package org.apache.stanbol.entityhub.servicesapi.model.rdf;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;

public enum RdfResourceEnum {
    /**
     * The representation concept
     */
    Representation,
    /**
     * The Entity concept
     */
    Entity,
    /**
     * The site that defines/manages an {@link RdfResourceEnum#Entity}
     */
    site,
    /**
     * The ranking of the entity by this site in the value range of [0..1]
     * A sign with the rank 1 would be (one of) the most important entities
     * managed by this Site. A sign with rank 0 has no relevance. Even that this
     * is still within the value range one could wonder why this site does
     * even manage a representation about that entity.
     */
    entityRank,
    /**
     * The rdf:type used for Metadata. Note that foaf:Document is used as Type 
     * for the Metadata
     */
    Metadata(NamespaceEnum.foaf,"Document"),
    /**
     * relation used to link from the 
     * {@link org.apache.stanbol.entityhub.servicesapi.model.Entity#getMetadata() metadata}
     * to the {@link org.apache.stanbol.entityhub.servicesapi.model.Entity#getRepresentation() representation}
     * of an {@link org.apache.stanbol.entityhub.servicesapi.model.Entity}.<p>
     * Note that this uses a property in the entityhub namespace to ensure that
     * no other (external) Information does accidently use the same property.
     */
    aboutRepresentation(null,"about"),
//    /**
//     * The representation about an Entity (domain=Entity, range=Representation).
//     */
//    representation,
    /**
     * The label of an Entity
     */
    label,
    /**
     * The description of an Entity
     */
    description,
    /**
     * Predecessors of an Entity
     */
    predecessor,
    /**
     * Successors of an Entity
     */
    successor,
    /**
     * The property used for the state of locally managed Entities
     */
    hasState,
    /**
     * The Concept used to type instances of EntityStates
     */
    EntityState,
    /**
     * The Individual representing the active state of a locally managed Entity
     */
    entityStateActive(null,"entityState-active"),
    /**
     * The Individual representing the depreciated state of a locally managed Entity
     */
    entityStateDepreciated(null,"entityState-depreciated"),
    /**
     * The Individual representing the proposed state of a locally managed Entity
     */
    entityStateProposed(null,"entityState-proposed"),
    /**
     * The Individual representing the removed state of a locally managed Entity
     */
    entityStateRemoved(null,"entityState-removed"),
    /**
     * Property used to refer to mapped entities. This directly links the 
     * mapped entity. To get the mapping information one needs to lookup the
     * Entity mapping for the source and target of this relation.<p>
     */
    mappedTo,
    /**
     * A directed mapping between two entities that holds additional 
     * information about the mapping
     */
    EntityMapping,
    /**
     * Property used to reference the source of the mapping 
     */
    mappingSource,
    /**
     * Property used to refer to the target of the mapping
     */
    mappingTarget,
    /**
     * The property used for the state of the MappedEntity
     */
    hasMappingState,
    /**
     * The expires date of a representation
     */
    expires,
    /**
     * The Concept used to type instances of mapping states
     */
    MappingState,
    /**
     * The Individual representing the confirmed state of MappedEntities
     */
    mappingStateConfirmed(null,"mappingState-confirmed"),
    /**
     * The Individual representing the expired state of MappedEntities
     */
    mappingStateExpired(null,"mappingState-expired"),
    /**
     * The Individual representing the proposed state of MappedEntities
     */
    mappingStateProposed(null,"mappingState-proposed"),
    /**
     * The Individual representing the rejected state of MappedEntities
     */
    mappingStateRejected(null,"mappingState-rejected"),
    /**
     * The Individual representing the result set of an field query
     */
    QueryResultSet(NamespaceEnum.entityhubQuery),
    /**
     * The property used to link from the {@link #QueryResultSet} to the
     * {@link org.apache.stanbol.entityhub.servicesapi.model.Representation} nodes.
     */
    queryResult(NamespaceEnum.entityhubQuery),
    /**
     * Property used to link the Literal with the executed Query to the
     * {@link #QueryResultSet}
     */
    query(NamespaceEnum.entityhubQuery),
    /**
     * The score of the result in respect to the parsed query.
     */
    resultScore(NamespaceEnum.entityhubQuery,"score"),
    /**
     * The id of the site the result was found
     */
    resultSite(NamespaceEnum.entityhubQuery),
    /**
     * The data type URI for the {@link org.apache.stanbol.entityhub.servicesapi.model.Reference}
     * interface. Used  entityhub-model:ref
     */
    ReferenceDataType(null,"ref"),
    /**
     * The data type URI for the {@link org.apache.stanbol.entityhub.servicesapi.model.Text}
     * interface. Uses entityhub-model:text
     */
    TextDataType(null,"text"),
    /*
     * Metadata for Entities
     */
    /**
     * Tells if an returned Entity represents an locally cached version
     */
    isChached,
//    /**
//     * Full text search field. This can be used in {@link FieldQuery} to 
//     * indicate that a constraint should be applied to the full text field.
//     * {@link Yard} implementations will need to specially treat this field
//     * (e.g. in SPARQL one need to use a variable instead if this URI)
//     * @see SpecialFieldEnum#fullText
//     */
//    fullTextField(null,"fullText"),
//    /**
//     * Field that contains all {@link Reference}s of an {@link Entity}. {@link Yard}
//     * implementation will need to treat this field specially. (e.g. in
//     * SPARQL one needs to use a variable as property instead of this URI). 
//     * @see SpecialFieldEnum#references
//     */
//    referencesField(null,"references")
    ;
    private String uri;
    /**
     * Initialise a new property by using the parse URI. If <code>null</code> is
     * parsed, the URI is generated by using the Entityhub model namespace (
     * {@link NamespaceEnum#entityhub}).
     * @param uri the uri of the element
     */
    RdfResourceEnum(String uri) {
        if(uri == null){
            this.uri = NamespaceEnum.entityhub+name();
        }
        this.uri = uri;
    }
    /**
     * Initialise a new property with the namespace and the {@link #name()} as
     * local name.
     * @param ns the namespace of the property or <code>null</code> to use the
     * default namespace
     */
    RdfResourceEnum(NamespaceEnum ns){
        this(ns,null);
    }
    /**
     * Initialise a new property with the parsed namespace and local name.
     * @param ns the namespace of the property or <code>null</code> to use the
     * default namespace
     * @param localName the local name of the property or <code>null</code> to
     * use the {@link #name()} as local name.
     */
    RdfResourceEnum(NamespaceEnum ns, String localName){
        String uri;
        if(ns == null){
            uri = NamespaceEnum.entityhub.getNamespace();
        } else {
            uri = ns.getNamespace();
        }
        if(localName == null){
            uri = uri+name();
        } else {
            uri = uri+localName;
        }
        this.uri = uri;
    }
    /**
     * Initialise a new property with {@link NamespaceEnum#entityhub}) as namespace
     * and the {@link #name()} as local name.
     */
    RdfResourceEnum(){
        this(null,null);
    }
    /**
     * Getter for the Unicode character of the URI
     * @return
     */
    public String getUri(){
        return uri;
    }
    @Override
    public String toString() {
        return uri;
    }

}
