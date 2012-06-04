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
package org.apache.stanbol.enhancer.engines.celi;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public interface CeliConstants {
    
    /**
     * Property used to provide the license key for the CELI service to all the
     * CELI engines.<p>
     * License keys are read from:<ol>
     * <li> {@link ComponentContext#getProperties()} - engine configuration: 
     * This can be used to configure a specific keys for single Engine
     * <li> {@link BundleContext#getProperty(String)} - system configuration:
     * This can be used to configure the key within the "sling.properties" file
     * or as a system property when starting the Stanbol instance.
     * </ol>
     * <b>Note</b><ul>
     * <li> License keys configures like that will be used by all CELI engines 
     * that do not provide there own key.
     * <li> If the License key is configured via a System property it can be
     * also accessed by other components.
     * </ul>
     */
    String CELI_LICENSE = "celi.license";

}
