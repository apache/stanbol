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

package org.apache.stanbol.contenthub.servicesapi.search.execution;

import java.util.List;

/**
 * Each {@link ClassResource} is linked with a {@link Keyword} regardless of this resource is extracted by
 * class subsumption relation or keyword based text search. This interface provides the function to access the
 * keywords which cause the resource to exist in the {@link SearchContext}.
 * 
 * @author cihan
 * 
 */
public interface KeywordRelated extends Scored {

    /**
     * Retrieves the related {@link Keyword}s.
     * 
     * @return @{link Keyword}s which cause this resource to occur in the search result by means of the
     *         {@link SearchContext}.
     */

    List<Keyword> getRelatedKeywords();
}
