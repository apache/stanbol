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
package org.apache.stanbol.entityhub.servicesapi.site;

public interface ManagedSiteConfiguration extends SiteConfiguration {
    
    
    /**
     * The key used for the configuration of the id for the yard used as to
     * manage the Entity data.
     */
    String YARD_ID = "org.apache.stanbol.entityhub.site.yardId";
    
    /**
     * The name of the Yard used by the {@link ManagedSite} to store the
     * Entity data. A {@link ManagedSite} will only be available if the
     * {@link org.apache.stanbol.entityhub.servicesapi.yard.Yard} 
     * with this ID is also available os OSGI service.
     * @return the ID of the Yard used to store entity data of this managed site.
     */
    String getYardId();

}
