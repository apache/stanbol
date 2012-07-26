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
package org.apache.stanbol.entityhub.servicesapi.yard;

public enum CacheingLevel {
    /**
     * This indicated that a document in the cache confirms to the specification
     * as stored within the cache. This configuration is stored within the cache
     * and only be changed for an empty cache.<p>
     */
    base,
    /**
     * If a Document is updated in the cache, than there may be more information
     * be stored as defined by the initial creation of a cache. <p> This level indicates
     * that a document includes all the field defined for {@link #base} but
     * also some additional information.
     */
    special
}
