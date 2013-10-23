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
package org.apache.stanbol.entityhub.servicesapi.mapping;

import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
/**
 * Intended to define the configuration of the fieldMapper.
 *
 * @author Rupert Westenthaler
 * @deprecated unsure - Currently the functionality of this service is part of
 * the {@link EntityhubConfiguration} and the {@link SiteConfiguration} interfaces. 
 * Access Methods for the {@link FieldMapper} are defined by the 
 * {@link Entityhub} and the {@link Site} interfaces
 */
@Deprecated
public interface FieldMapperConfig {
    /**
     * The property used to configure the default mappings used by all
     * {@link Site} instances active within the Entityhub
     */
    String DEFAULT_MAPPINGS = "org.apache.stanbol.entityhub.mapping.default";
    /**
     * The Property used to configure mappings that are only used for
     * representation of a specific Site.
     */
    String SITE_MAPPINGS = "org.apache.stanbol.entityhub.mapping.site";

}
