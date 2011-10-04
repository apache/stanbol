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

package org.apache.stanbol.contenthub.servicesapi.search.processor;

import java.util.List;

import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;

/**
 * The processor interface executes the search operation by running allowed search engines and passing
 * {@link SearchContext} to each of them.
 * 
 * @author cihan
 */
public interface SearchProcessor {

    /**
     * Retrieves the available engines in the Stanbol environment. If a {@link SearchEngine} is actively
     * running in the OSGi environment of Stanbol, then it is available.
     * 
     * @return A lisrt of {@link SearchEngine}s.
     */
    List<SearchEngine> listEngines();

    /**
     * Processes the semantic search. It passes the {@link SearchContext} to every {@link SearchEngine}. Each
     * {@link SearchEngine} manipulates the {@link SearchContext} one by one.
     * 
     * @param context
     *            Initial {@link SearchContext}. This context includes {@link QueryKeyword}s only.
     */
    void processQuery(SearchContext context);
}
