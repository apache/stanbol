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
package org.apache.stanbol.cmsadapter.cmis.repository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.RelationshipType;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

/**
 * This class converts objects from {@linkplain org.apache.chemistry.opencmis.client.api} to objects from
 * {@linkplain org.apache.stanbol.cmsadapter.servicesapi.model.web}
 * 
 * @author cihan
 * 
 */
public class CMISModelMapper {

    private static final SimpleDateFormat XML_DATETIME_FORMAT = new SimpleDateFormat("yyyy-mm-dd HH:mm:ssZ");

    public static CMSObject getCMSObject(CmisObject object) {
        BaseTypeId baseTypeId = object.getBaseTypeId();
        if (baseTypeId.equals(BaseTypeId.CMIS_DOCUMENT)) {
            return getCMSObjectFromDocument((Document) object);
        } else if (baseTypeId.equals(BaseTypeId.CMIS_FOLDER)) {
            return getCMSObjectFromFolder((Folder) object);
        } else {
            throw new IllegalArgumentException("Expected cmis:document or cmis:folder, found "
                                               + object.getBaseTypeId().name());
        }
    }

    private static CMSObject getCMSObjectFromDocument(Document document) {
        CMSObject cmsObject = new CMSObject();
        cmsObject.setUniqueRef(document.getId());
        String[] names = separateNamespace(document.getName());
        cmsObject.setLocalname(names[0]);
        cmsObject.setNamespace(names[1]);
        // TODO Handle Documents with multiple paths

        cmsObject.setPath(document.getParents().get(0).getPath() + "/" + document.getName());
        cmsObject.setObjectTypeRef(document.getType().getId());
        if (document.getParents().size() > 0) {
            // TODO Handle documents with multiple parents
            cmsObject.setParentRef(document.getParents().get(0).getId());
        }
        return cmsObject;

    }

    private static CMSObject getCMSObjectFromFolder(Folder folder) {
        CMSObject cmsObject = new CMSObject();
        cmsObject.setUniqueRef(folder.getId());
        String[] names = separateNamespace(folder.getName());
        cmsObject.setLocalname(names[0]);
        cmsObject.setNamespace(names[1]);
        // TODO Handle Folders with multiple paths
        cmsObject.setPath(folder.getPath());
        cmsObject.setObjectTypeRef(folder.getType().getId());
        if (folder.getParents().size() > 0) {
            // TODO Handle folders with multiple parents
            cmsObject.setParentRef(folder.getParents().get(0).getId());
        }
        return cmsObject;
    }

    public static ObjectTypeDefinition getObjectTypeDefinition(ObjectType typeDef) {
        ObjectTypeDefinition otd = new ObjectTypeDefinition();
        otd.setLocalname(typeDef.getLocalName());
        otd.setNamespace(typeDef.getLocalNamespace());
        otd.setUniqueRef(typeDef.getId());
        // Resolve super types recursively
        ObjectType parentType = typeDef.getParentType();
        while (parentType != null) {
            otd.getParentRef().add(parentType.getId());
            parentType = parentType.getParentType();
        }
        return otd;

    }

    public static Property getProperty(CMSObject node,
                                       org.apache.chemistry.opencmis.client.api.Property<?> cmisProperty) {
        Property property = new Property();
        property.setLocalname(cmisProperty.getLocalName());
        // property.setType(convertPropertyType(cmisProperty.getType()));
        property.setContainerObjectRef(node.getUniqueRef());
        property.setSourceObjectTypeRef(node.getObjectTypeRef());
        property.setPropertyDefinition(getPropertyDefinition(cmisProperty.getDefinition()));
        if (cmisProperty.getValues() != null) {
            for (Object value : cmisProperty.getValues()) {
                if (value == null) {
                    continue;
                }
                if (cmisProperty.getType() == PropertyType.DATETIME) {
                    Calendar cal = (Calendar) value;
                    property.getValue().add(XML_DATETIME_FORMAT.format(cal.getTime()));
                } else {
                    property.getValue().add(value.toString());
                }
            }
        }
        return property;
    }

    public static PropertyDefinition getPropertyDefinition(org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition<?> type) {
        PropertyDefinition propDef = new PropertyDefinition();
        propDef.setUniqueRef(type.getId());
        String[] names = separateNamespace(type.getLocalName());
        propDef.setLocalname(names[0]);
        propDef.setNamespace(names[1]);
        propDef.setRequired(type.isRequired());
        // TODO Set cardinality
        propDef.setPropertyType(convertPropertyType(type.getPropertyType()));
        return propDef;
    }

    public static PropertyDefinition getRelationshipType(RelationshipType relType) {
        PropertyDefinition propDef = new PropertyDefinition();
        propDef.setUniqueRef(relType.getId());
        propDef.setLocalname(relType.getLocalName());
        propDef.setNamespace(relType.getLocalNamespace());
        // TODO Set cardinality
        propDef.setPropertyType(PropType.REFERENCE);
        return propDef;
    }

    private static PropType convertPropertyType(PropertyType type) {
        switch (type) {
            case BOOLEAN:
                return PropType.BOOLEAN;
            case DATETIME:
                return PropType.DATE;
            case DECIMAL:
                return PropType.DECIMAL;
            case HTML:
                return PropType.HTML;
            case ID:
                return PropType.REFERENCE;
            case INTEGER:
                return PropType.INTEGER;
            case STRING:
                return PropType.STRING;
            case URI:
                return PropType.URI;
            default:
                return null;
        }
    }

    public static List<CMSObject> convertCMISObjects(List<CmisObject> cmisObjects) {
        List<CMSObject> cmsObjects = new ArrayList<CMSObject>(cmisObjects.size());
        for (CmisObject cmisObject : cmisObjects) {
            cmsObjects.add(getCMSObject(cmisObject));
        }
        return cmsObjects;
    }

    public static void fillProperties(CmisObject cmisOBject, CMSObject node) {
        for (org.apache.chemistry.opencmis.client.api.Property<?> property : cmisOBject.getProperties()) {
            node.getProperty().add(getProperty(node, property));
        }
    }

    public static void fillPropertyDefinitions(ObjectTypeDefinition instance, ObjectType objectType) {
        Map<String,org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition<?>> definitions = objectType
                .getPropertyDefinitions();
        if (definitions != null) {
            for (org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition<?> propDef : definitions
                    .values()) {
                instance.getPropertyDefinition().add(getPropertyDefinition(propDef));
            }
        }
    }

    public static void fillChildObjectTypeDefinitions(ObjectTypeDefinition instance, ObjectType type) {
        Iterator<ObjectType> children = type.getChildren().iterator();
        while (children.hasNext()) {
            ObjectType child = children.next();
            instance.getObjectTypeDefinition().add(getObjectTypeDefinition(child));
        }
    }

    private static String[] separateNamespace(String name) {
        String[] parts = name.split(":", 2);
        if (parts.length != 2) {
            return new String[] {name, ""};
        } else {
            return new String[] {parts[1], parts[0]};
        }
    }
}
