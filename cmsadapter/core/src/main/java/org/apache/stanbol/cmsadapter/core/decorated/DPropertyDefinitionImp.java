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

import java.math.BigInteger;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.AnnotationType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;

public class DPropertyDefinitionImp implements DPropertyDefinition {

    private PropertyDefinition instance;

    public DPropertyDefinitionImp(PropertyDefinition instance) {
        this.instance = instance;
    }

    @Override
    public String getUniqueRef() {
        return instance.getUniqueRef();
    }

    @Override
    public String getName() {
        return instance.getLocalname();
    }

    @Override
    public String getNamespace() {
        return instance.getNamespace();
    }

    @Override
    public AnnotationType getAnnotations() {
        return instance.getAnnotation();
    }

    @Override
    public BigInteger getCardinality() {
        return instance.getCardinality();
    }

    @Override
    public PropType getPropertyType() {
        return instance.getPropertyType();
    }

    @Override
    public List<String> getValueConstraints() {
        return instance.getValueConstraint();
    }

    @Override
    public PropertyDefinition getInstance() {
        return instance;
    }

}
