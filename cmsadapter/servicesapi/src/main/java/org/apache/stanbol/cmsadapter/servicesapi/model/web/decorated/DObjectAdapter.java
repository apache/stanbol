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
package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

/**
 * Adapter Class for converting CMS model objects to Decorated CMS objects.
 * The former group has no access to the original CMS repository they are extracted from.
 * The latter can be configured to access to the repository if needed.
 * @author cihan
 *
 */
public interface DObjectAdapter {

    
    DObject wrapAsDObject(CMSObject node);

    DObjectType wrapAsDObjectType(ObjectTypeDefinition definition);

    DPropertyDefinition wrapAsDPropertyDefinition(PropertyDefinition propertyDefinition);

    DProperty wrapAsDProperty(Property property);

    /**
     * Session object to connect Remote CMS repository.
     * @return
     */
    Object getSession();

    /**
     * Mode must be set before fetching an object from CMS. Otherwise inconsistent/erroneous behavior can be
     * faced.
     * 
     * @param mode
     */
    void setMode(AdapterMode mode);

    AdapterMode getMode();

}
