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
package org.apache.stanbol.cmsadapter.servicesapi.processor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service(value = TypeLifterManager.class)
public class TypeLifterManager {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, referenceInterface = TypeLifter.class, policy = ReferencePolicy.DYNAMIC, bind = "bindTypeLifter", unbind = "unbindTypeLifter")
    private List<TypeLifter> typeLifters = new CopyOnWriteArrayList<TypeLifter>();

    private static final Logger logger = LoggerFactory.getLogger(TypeLifterManager.class);

    public TypeLifter getRepositoryAccessor(String connectionType) {
        for (TypeLifter typeLifter : typeLifters) {
            if (typeLifter.canLift(connectionType)) {
                return typeLifter;
            }
        }

        logger.warn("No suitable type lifter implementation for connection type: {} ", connectionType);
        return null;
    }

    protected void bindTypeLifter(TypeLifter typeLifter) {
        typeLifters.add(typeLifter);
    }

    protected void unbindTypeLifter(TypeLifter typeLifter) {
        typeLifters.remove(typeLifter);
    }
}
