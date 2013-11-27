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
package org.apache.stanbol.enhancer.engines.dereference;

/**
 * Define configuration parameters for Dereference engines
 * @author Rupert Westenthaler
 *
 */
public interface DereferenceConstants {
    /**
     * Property used to configure the fields that should be dereferenced.<p>
     * DereferenceEngines need to support a list of URIs but may also support more
     * complex syntax (such as the Entityhub FiedMapping). However parsing a
     * list of properties URIs MUST BE still valid.<p>
     * Support for Namespace prefixes via the Stanbol Namespace Prefix Service
     * is optional. If unknown prefixes are used or prefixes are not supported
     * the Engine is expected to throw a 
     * {@link org.osgi.service.cm.ConfigurationException} during activation
     */
    String DEREFERENCE_ENTITIES_FIELDS = "enhancer.engines.dereference.fields";
    /**
     * Property used to configure LDPath statements. Those are applied using
     * each referenced Entity as Context.<p>
     * DereferenceEngines that can not support LDPath are expected to throw a
     * {@link org.osgi.service.cm.ConfigurationException} if values are set
     * for this property.
     */
    String DEREFERENCE_ENTITIES_LDPATH = "enhancer.engines.dereference.ldpath";

}
