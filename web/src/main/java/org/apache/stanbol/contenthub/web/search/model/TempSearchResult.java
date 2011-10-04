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

package org.apache.stanbol.contenthub.web.search.model;

import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.web.utils.JSONUtils;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.pacaci
 * @author cihan
 * 
 */
// TODO Will be deleted and replaced by SearchContext
public class TempSearchResult {
    private static final Logger logger = LoggerFactory.getLogger(TempSearchResult.class);

    private SearchContext context;

    public TempSearchResult(SearchContext context) {
        this.context = context;
    }

    public SearchContext getContext() {
        return context;
    }

    public String getConstraints() {
        try {
            return JSONUtils.convertToString(context.getConstraints());
        } catch (JSONException e) {
            logger.error("Cannot convert from constraints map to JSON String", e);
        }
        return null;
    }
}
