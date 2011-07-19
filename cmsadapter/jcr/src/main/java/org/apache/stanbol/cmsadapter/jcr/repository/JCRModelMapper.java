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
package org.apache.stanbol.cmsadapter.jcr.repository;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRModelMapper {
    private static Logger logger = LoggerFactory.getLogger(JCRModelMapper.class);

    /**
     * Gets a {@link Node} and transforms it to {@link CMSObject}
     * 
     * @param jcrNode
     *            JCR node to be transformed
     * @return transformed {@link CMSObject}
     * @throws RepositoryException
     */
    public static CMSObject getCMSObject(Node jcrNode) throws RepositoryException {
        CMSObject cmsObject = new CMSObject();
        cmsObject.setUniqueRef(jcrNode.getIdentifier());
        cmsObject.setLocalname(jcrNode.getName());
        cmsObject.setPath(jcrNode.getPath());
        cmsObject.setObjectTypeRef(jcrNode.getPrimaryNodeType().getName());
        try {
            // TODO For a quick fix parent ref is used for parent path
            cmsObject.setParentRef(jcrNode.getParent().getIdentifier());
        } catch (ItemNotFoundException e) {
            logger.info("Item has no parent. Can not set parent ref");
        } catch (AccessDeniedException e1) {
            logger.info("Item does not have permission to access parent. Can not set parent ref");
        }

        return cmsObject;
    }

    /**
     * Gets a {@link NodeType} and transforms it to {@link ObjectTypeDefinition}
     * 
     * @param jcrNodeType
     *            JCR node type to be transformed
     * @return transformed {@link ObjectTypeDefinition}
     */
    public static ObjectTypeDefinition getObjectTypeDefinition(NodeType jcrNodeType) {
        ObjectTypeDefinition objectTypeDefinition = new ObjectTypeDefinition();
        objectTypeDefinition.setLocalname(jcrNodeType.getName());
        objectTypeDefinition.setUniqueRef(jcrNodeType.getName());
        for (NodeType superType : jcrNodeType.getSupertypes()) {
            objectTypeDefinition.getParentRef().add(superType.getName());
        }

        return objectTypeDefinition;
    }

    /**
     * Gets a {@link Property} and transforms it to
     * {@link org.apache.stanbol.cmsadapter.servicesapi.model.web.Property}
     * 
     * @param jcrProperty
     *            JCR property to be transformed
     * @return transformed {@link org.apache.stanbol.cmsadapter.servicesapi.model.web.Property}
     * @throws RepositoryException
     */
    public static org.apache.stanbol.cmsadapter.servicesapi.model.web.Property getProperty(Property jcrProperty) throws RepositoryException {
        org.apache.stanbol.cmsadapter.servicesapi.model.web.Property property = new org.apache.stanbol.cmsadapter.servicesapi.model.web.Property();
        String sourceObjectType = jcrProperty.getDefinition().getDeclaringNodeType().getName();
        String localName = jcrProperty.getName();
        property.setSourceObjectTypeRef(sourceObjectType);
        property.setLocalname(localName);
        property.setPropertyDefinition(getPropertyDefinition(jcrProperty.getDefinition()));
        property.setContainerObjectRef(jcrProperty.getParent().getIdentifier());

        // TODO Consider value types
        if (jcrProperty.isMultiple()) {
            for (Value value : jcrProperty.getValues()) {
                property.getValue().add(value.toString());
            }
        } else {
            property.getValue().add(jcrProperty.getValue().toString());
        }
        return property;
    }

    /**
     * Gets a {@link PropertyDefinition} and transforms it to
     * {@link org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition}
     * 
     * @param jcrPropertyDefinition
     *            JCR property definition to be transformed
     * @return transformed {@link org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition}
     */
    public static org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition getPropertyDefinition(PropertyDefinition jcrPropertyDefinition) {
        org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition propertyDefinition = new org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition();
        propertyDefinition.setLocalname(jcrPropertyDefinition.getName());
        propertyDefinition.setPropertyType(mapPropertyType(jcrPropertyDefinition.getRequiredType()));
        propertyDefinition.setRequired(jcrPropertyDefinition.isMandatory());
        propertyDefinition.setSourceObjectTypeRef(jcrPropertyDefinition.getDeclaringNodeType().getName());
        propertyDefinition.setUniqueRef(getPropertyName(jcrPropertyDefinition));

        String[] valueConstraints = jcrPropertyDefinition.getValueConstraints();
        for (String constraint : valueConstraints) {
            propertyDefinition.getValueConstraint().add(constraint);
        }
        return propertyDefinition;
    }

    /**
     * Gets a JCR property type as integer value and transforms it to {@link PropType}
     * 
     * @param jcrPropType
     *            JCR property to be transformed
     * @return transformed {@link PropType}
     * @throws IllegalStateException
     */
    private static PropType mapPropertyType(int jcrPropType) throws IllegalStateException {
        String upperCasePropType = PropertyType.nameFromValue(jcrPropType).toUpperCase();
        return PropType.fromValue(upperCasePropType);
    }

    private static String getPropertyName(String declaringNodeTypeRef, String localName) {
        return declaringNodeTypeRef + "/" + localName;
    }

    private static String getPropertyName(PropertyDefinition jcrPropertyDefinition) {
        return getPropertyName(jcrPropertyDefinition.getDeclaringNodeType().getName(),
            jcrPropertyDefinition.getName());
    }

    public static void fillCMSObjectChildren(CMSObject cmsObject, Node jcrNode) throws RepositoryException {
        NodeIterator nit = jcrNode.getNodes();
        while (nit.hasNext()) {
            Node childNode = nit.nextNode();
            CMSObject child = getCMSObject(childNode);
            cmsObject.getChildren().add(child);
        }
    }

    public static void fillProperties(CMSObject node, Node jcrNode) throws RepositoryException {
        PropertyIterator pit = jcrNode.getProperties();
        while (pit.hasNext()) {
            Property jcrProperty = pit.nextProperty();
            node.getProperty().add(getProperty(jcrProperty));
        }

    }

    public static void fillChildObjectDefinitions(ObjectTypeDefinition instance, NodeType nodeType) {
        NodeDefinition[] childNodeDefinitions = nodeType.getChildNodeDefinitions();
        for (NodeDefinition childNodeDefinition : childNodeDefinitions) {
            instance.getObjectTypeDefinition().add(
                getObjectTypeDefinition(childNodeDefinition.getDeclaringNodeType()));
        }
    }

    public static void fillPropertyDefinition(org.apache.stanbol.cmsadapter.servicesapi.model.web.Property instance,
                                              NodeType sourceObjectType) throws RepositoryException {
        for (javax.jcr.nodetype.PropertyDefinition propDef : sourceObjectType.getPropertyDefinitions()) {
            if (instance.getLocalname().contentEquals(propDef.getName())) {
                instance.setPropertyDefinition(getPropertyDefinition(propDef));
                break;
            }
        }
    }

    public static void fillPropertyDefinitions(ObjectTypeDefinition instance, NodeType nodeType) {
        for (PropertyDefinition propertyDefinition : nodeType.getPropertyDefinitions()) {
            instance.getPropertyDefinition().add(getPropertyDefinition(propertyDefinition));
        }
    }
}
