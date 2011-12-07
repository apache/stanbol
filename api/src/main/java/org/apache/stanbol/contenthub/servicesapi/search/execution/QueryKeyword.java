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
import java.util.Map;

import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;

/**
 * The interface to represent a single query keyword and its related resources. This is a regular
 * {@link Keyword} in addition with the related keywords. Related keywords are attached to the
 * {@link QueryKeyword} as the {@link SearchEngine}s execute.
 * 
 * @author cihan
 * 
 */
public interface QueryKeyword extends Keyword {

    /**
     * If any {@link SearchEngine} attaches a {@link Keyword} to this {@link QueryKeyword}, this function
     * returns a list of them.
     * 
     * @return A list of {@link Keyword}.
     */
    Map<String, List<Keyword>> getRelatedKeywords();

    /**
     * Adds a {@link Keyword} as related with this {@link QueryKeyword}. A {@link SearchEngine}
     * 
     * @param keyword
     *            The {@link Keyword} to be added as a related keyword to this {@link QueryKeyword}.
     */
    void addRelatedKeyword(Keyword keyword);
}
