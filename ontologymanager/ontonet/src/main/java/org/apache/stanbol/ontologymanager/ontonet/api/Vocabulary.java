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
package org.apache.stanbol.ontologymanager.ontonet.api;

import org.apache.clerezza.rdf.core.UriRef;

public class Vocabulary {

    /**
     * The default namespace for the Stanbol OntoNet metadata vocabulary
     */
    public static final String _NS_ONTONET = "http://stanbol.apache.org/ontology/meta/ontonet#";

    /**
     * This namespace is used for representing Stanbol resources internally. It should applied to all portable
     * resources that might be moved from one host to another, e.g. scopes and sessions.<br/>
     * <br>
     * This namespace MUST NOT be used for identifying resources in the outside world, e.g. RESTful services:
     * it MUST be converted to the public namespace before exporting.
     */
    public static final String _NS_STANBOL_INTERNAL = "http://stanbol.apache.org/ontology/.internal/";

    public static final UriRef HAS_SPACE_CORE = new UriRef(_NS_ONTONET + "hasCoreSpace");

    public static final UriRef HAS_SPACE_CUSTOM = new UriRef(_NS_ONTONET + "hasCustomSpace");

    public static final UriRef HAS_STATUS = new UriRef(_NS_ONTONET + "hasStatus");

    public static final UriRef IS_MANAGED_BY = new UriRef(_NS_ONTONET + "isManagedBy");

    public static final UriRef IS_MANAGED_BY_CORE = new UriRef(_NS_ONTONET + "isManagedByCore");

    public static final UriRef IS_MANAGED_BY_CUSTOM = new UriRef(_NS_ONTONET + "isManagedByCustom");

    public static final UriRef IS_SPACE_CORE_OF = new UriRef(_NS_ONTONET + "isCoreSpaceOf");

    public static final UriRef IS_SPACE_CUSTOM_OF = new UriRef(_NS_ONTONET + "isCustomSpaceOf");

    public static final UriRef MANAGES = new UriRef(_NS_ONTONET + "manages");

    public static final UriRef MANAGES_IN_CORE = new UriRef(_NS_ONTONET + "managesInCore");

    public static final UriRef MANAGES_IN_CUSTOM = new UriRef(_NS_ONTONET + "managesInCustom");

    public static final UriRef SCOPE = new UriRef(_NS_ONTONET + "Scope");

    public static final UriRef SESSION = new UriRef(_NS_ONTONET + "Session");

    public static final UriRef SPACE = new UriRef(_NS_ONTONET + "Space");

    public static final UriRef STATUS = new UriRef(_NS_ONTONET + "Status");

    public static final UriRef STATUS_ACTIVE = new UriRef(_NS_ONTONET + "Status.ACTIVE");

    public static final UriRef STATUS_INACTIVE = new UriRef(_NS_ONTONET + "Status.INACTIVE");

}
