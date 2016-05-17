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
package org.apache.stanbol.enhancer.servicesapi.rdf;

import org.apache.clerezza.commons.rdf.IRI;

public final class Enhancer {

    /**
     * Restrict instantiation
     */
    private Enhancer() {}

    public static final IRI CONTENT_ITEM = new IRI(NamespaceEnum.enhancer+"ContentItem");
    public static final IRI ENHANCEMENT_ENGINE = new IRI(NamespaceEnum.enhancer+"EnhancementEngine");
    public static final IRI ENHANCEMENT_CHAIN = new IRI(NamespaceEnum.enhancer+"EnhancementChain");
    public static final IRI ENHANCER = new IRI(NamespaceEnum.enhancer+"Enhancer");
    public static final IRI HAS_ENGINE = new IRI(NamespaceEnum.enhancer+"hasEngine");
    public static final IRI HAS_CHAIN = new IRI(NamespaceEnum.enhancer+"hasChain");
    public static final IRI HAS_DEFAULT_CHAIN = new IRI(NamespaceEnum.enhancer+"hasDefaultChain");
    
}
