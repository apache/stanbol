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
package org.apache.stanbol.entityhub.servicesapi;

import java.util.Collection;

import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

/**
 * Provides the Configuration needed by the {@link Entityhub}.<p>
 * @author Rupert Westenthaler
 *
 */
public interface EntityhubConfiguration {
    /**
     * The key used to configure the ID of the entity hub
     */
    String ID = "org.apache.stanbol.entityhub.id";
    /**
     * The ID of the Entityhub. This ID is used as origin (sign site) for all symbols
     * and mapped entities created by the Entityhub
     * @return the ID of the Entityhub
     */
    String getID();
    /**
     * The property used to configure the prefix used for {@link Symbol}s and
     * {@link EntityMapping}s created by the Entityhub
     */
    String PREFIX = "org.apache.stanbol.entityhub.prefix";
    /**
     * Getter for the Prefix to be used for all {@link Symbol}s and {@link EntityMapping}s
     * created by the {@link Entityhub}
     * @return The prefix for {@link Symbol}s and {@link EntityMapping}s
     */
    String getEntityhubPrefix();
    /**
     * The key used to configure the name of the entity hub
     */
    String NAME = "org.apache.stanbol.entityhub.name";
    /**
     * The human readable name of this entity hub instance. Typically used as label
     * in addition/instead of the ID.
     * @return the Name (or the ID in case no name is defined)
     */
    String getName();
    /**
     * The key used to configure the description of the entity hub
     */
    String DESCRIPTION = "org.apache.stanbol.entityhub.description";
    /**
     * The human readable description to provide some background information about
     * this entity hub instance.
     * @return the description or <code>null</code> if none is defined/configured.
     */
    String getDescription();
    /**
     * The property used to configure the id of the {@link Yard} used to store
     * the data of the {@link Entityhub}
     */
    String ENTITYHUB_YARD_ID = "org.apache.stanbol.entityhub.yard.entityhubYardId";
    /**
     * The default ID for the {@link Yard} used for the {@link Entityhub}
     */
    String DEFAULT_ENTITYHUB_YARD_ID = "entityhubYard";
    /**
     * This is the ID of the {@link Yard} used by the {@link Entityhub} to store 
     * its data
     * @return the entity hub yard id
     */
    String getEntityhubYardId();
    /**
     * The property used to configure the field mappings for the {@link Entityhub}
     */
    String FIELD_MAPPINGS = "org.apache.stanbol.entityhub.mapping.entityhub";
    /**
     * Getter for the FieldMapping configuration of the {@link Entityhub}. 
     * These Mappings are used for every {@link Site} of the 
     * {@link Entityhub}.<br>
     * Note that {@link FieldMapping#parseFieldMapping(String)} is used to
     * parsed the values returned by this Method
     * @return the configured mappings for the {@link Entityhub}
     */
    Collection<String> getFieldMappingConfig();
    /**
     * The property used to configure the initial state for new {@link EntityMapping}s
     */
    String DEFAULT_MAPPING_STATE = "org.apache.stanbol.entityhub.defaultMappingState";
    /**
     * The initial (default) state for new {@link EntityMapping}s
     * @return the default state for new {@link EntityMapping}s
     */
    MappingState getDefaultMappingState();
    /**
     * The property used to configure the initial state for new {@link Symbol}s
     */
    String DEFAULT_SYMBOL_STATE = "org.apache.stanbol.entityhub.defaultSymbolState";
    /**
     * The initial (default) state for new {@link Symbol}s
     * @return the default state for new {@link Symbol}s
     */
    ManagedEntityState getDefaultManagedEntityState();

}
