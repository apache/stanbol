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
package org.apache.stanbol.cmsadapter.servicesapi.helper;

import org.apache.clerezza.rdf.core.UriRef;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * This class contains necessary {@link Resource}s and {@link Property}ies that are used in the scope of CMS
 * Adapter component.
 * 
 * @author suat
 * 
 */
public class CMSAdapterVocabulary {
    private static final String PATH_DELIMITER = "/";

    public static final String DEFAULT_NS_URI = "http://www.apache.org/stanbol";

    public static final String CMS_ADAPTER_VOCABULARY_PREFIX = "cms";
    public static final String CMS_ADAPTER_VOCABULARY_URI = DEFAULT_NS_URI + PATH_DELIMITER
                                                            + CMS_ADAPTER_VOCABULARY_PREFIX;

    /*
     * Property to represent the path of the CMS item
     */
    public static final String CMSAD_PATH_PROP_NAME = "path";
    public static final Property CMSAD_PATH_PROP = property(CMS_ADAPTER_VOCABULARY_URI, CMSAD_PATH_PROP_NAME);

    /*
     * Property to keep mapping between resource name and its unique reference
     */
    private static final String CMSAD_RESOURCE_REF_PROP_NAME = "resourceUniqueRef";
    public static final Property CMSAD_RESOURCE_REF_PROP = property(CMS_ADAPTER_VOCABULARY_URI,
        CMSAD_RESOURCE_REF_PROP_NAME);

    /*
     * Property to keep source object type definition of a datatype property or object property
     */
    private static final String CMSAD_PROPERTY_SOURCE_OBJECT_PROP_NAME = "sourceObject";
    public static final Property CMSAD_PROPERTY_SOURCE_OBJECT_PROP = property(CMS_ADAPTER_VOCABULARY_URI,
        CMSAD_PROPERTY_SOURCE_OBJECT_PROP_NAME);

    /**
     * Property to access metadata of a specific cms object.
     */
    private static final String CMSAD_PROPERTY_CONTENT_ITEM_REF_NAME = "contentItemRef";
    public static final Property CMSAD_PROPERTY_CONTENT_ITEM_REF = property(CMS_ADAPTER_VOCABULARY_URI,
        CMSAD_PROPERTY_CONTENT_ITEM_REF_NAME);

    /*
     * Properties to store connection info in the ontology
     */
    // connection info resource
    private static final String CONNECTION_INFO_RES_NAME = "connectionInfo";
    public static final Resource CONNECTION_INFO_RES = resource(CMS_ADAPTER_VOCABULARY_URI,
        CONNECTION_INFO_RES_NAME);

    // workspace property
    private static final String CONNECTION_WORKSPACE_PROP_NAME = "workspace";
    public static final Property CONNECTION_WORKSPACE_PROP = property(CMS_ADAPTER_VOCABULARY_URI,
        CONNECTION_WORKSPACE_PROP_NAME);

    // username property
    private static final String CONNECTION_USERNAME_PROP_NAME = "username";
    public static final Property CONNECTION_USERNAME_PROP = property(CMS_ADAPTER_VOCABULARY_URI,
        CONNECTION_USERNAME_PROP_NAME);

    // password property
    private static final String CONNECTION_PASSWORD_PROP_NAME = "password";
    public static final Property CONNECTION_PASSWORD_PROP = property(CMS_ADAPTER_VOCABULARY_URI,
        CONNECTION_PASSWORD_PROP_NAME);

    // workspace url property
    private static final String CONNECTION_WORKSPACE_URL_PROP_NAME = "workspaceURL";
    public static final Property CONNECTION_WORKSPACE_URL_PROP = property(CMS_ADAPTER_VOCABULARY_URI,
        CONNECTION_WORKSPACE_URL_PROP_NAME);

    // connection type property
    private static final String CONNECTION_TYPE_PROP_NAME = "connectionType";
    public static final Property CONNECTION_TYPE_PROP = property(CMS_ADAPTER_VOCABULARY_URI,
        CONNECTION_TYPE_PROP_NAME);

    /*
     * Properties to store bridge definitions
     */
    // bridge definitions resource
    private static final String BRIDGE_DEFINITIONS_RES_NAME = "bridgeDefinitions";
    public static final Resource BRIDGE_DEFINITIONS_RES = resource(CMS_ADAPTER_VOCABULARY_URI,
        BRIDGE_DEFINITIONS_RES_NAME);

    // property to keep bridge definitions
    private static final String BRIDGE_DEFINITIONS_CONTENT_PROP_NAME = "content";
    public static final Property BRIDGE_DEFINITIONS_CONTENT_PROP = property(CMS_ADAPTER_VOCABULARY_URI,
        BRIDGE_DEFINITIONS_CONTENT_PROP_NAME);

    private static Property property(String URI, String local) {
        URI = OntologyResourceHelper.addResourceDelimiter(URI);
        return ResourceFactory.createProperty(URI, local);
    }

    private static Resource resource(String URI, String local) {
        URI = OntologyResourceHelper.addResourceDelimiter(URI);
        return ResourceFactory.createResource(URI + local);
    }

    /*
     * CMS Vocabulary Annotations:
     * 
     * Below UriRef instances are used while annotating external RDF data. They form a standard vocabulary to
     * consisting of content repository elements. Through this standard vocabulary, annotated RDF is reflected
     * into the content repository.
     */
    /**
     * Represent the RDF type of CMS Object in the content management system
     */
    public static final UriRef CMS_OBJECT = new UriRef(CMS_ADAPTER_VOCABULARY_URI + "#CMSObject");

    /**
     * Represents a reference to name of a CMS Object
     */
    public static final UriRef CMS_OBJECT_NAME = new UriRef(CMS_ADAPTER_VOCABULARY_URI + "#name");

    /**
     * Represents a reference to path of a CMS Object
     */
    public static final UriRef CMS_OBJECT_PATH = new UriRef(CMS_ADAPTER_VOCABULARY_URI + "#path");

    /**
     * Represents a reference to parent of a CMS Object
     */
    public static final UriRef CMS_OBJECT_PARENT_REF = new UriRef(CMS_ADAPTER_VOCABULARY_URI + "#parentRef");

    /**
     * Represents a reference to URI representing a CMS Object. It is used to identify CMS Objects in CMS as
     * in the original RDF.
     */
    public static final UriRef CMS_OBJECT_HAS_URI = new UriRef(CMS_ADAPTER_VOCABULARY_URI + "#hasURI");
    
    /*
     * JCR Specific URI references
     */
    /**
     * The predicate representing the primary type of JCR content repository objects
     */
    public static final UriRef JCR_PRIMARY_TYPE = new UriRef(NamespaceEnum.jcr + "primaryType");
    
    /**
     * The predicate representing mixin types of JCR content repository objects
     */
    public static final UriRef JCR_MIXIN_TYPES = new UriRef(NamespaceEnum.cmis + "mixinTypes");
    
    /*
     * CMIS Specific URI references
     */
    /**
     * The predicate representing the base type of CMIS content repository objects
     */
    public static final UriRef CMIS_BASE_TYPE_ID = new UriRef(NamespaceEnum.cmis + "baseTypeId");
}
