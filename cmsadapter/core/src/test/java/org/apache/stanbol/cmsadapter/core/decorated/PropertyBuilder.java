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
package org.apache.stanbol.cmsadapter.core.decorated;

import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.LOCAL_NAME;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

public class PropertyBuilder {
    private static ObjectFactory of = new ObjectFactory();
    private Property instance = of.createProperty();

    public PropertyBuilder(String prefix) {
        instance.setLocalname(prefix + LOCAL_NAME);
    }

    public PropertyBuilder propertyDefinition(PropertyDefinition propDef) {
        instance.setPropertyDefinition(propDef);
        return this;
    }

    public PropertyBuilder containerObjectRef(CMSObject obj) {
        instance.setContainerObjectRef(obj.getUniqueRef());
        return this;
    }

    public Property build() {
        return instance;
    }

}
