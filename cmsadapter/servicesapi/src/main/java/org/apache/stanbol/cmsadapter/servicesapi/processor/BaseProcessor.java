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
package org.apache.stanbol.cmsadapter.servicesapi.processor;

/**
 * Base class for which can be extended by any {@link Processor} implementation. This class contains common
 * functions that can be used in {@link Processor} implementations.
 * 
 */
public class BaseProcessor {
    /**
     * Detects whether the path of a CMS object specified in <i>path</i> parameter is included in the
     * <i>query</i> parameter.
     * 
     * @param path
     * @param query
     * @return
     */
    protected boolean matches(String path, String query) {
        if (path != null) {
            if (query.endsWith("%")) {
                return path.startsWith(query.substring(0, query.length() - 1))
                       || path.contentEquals(query.substring(0, query.length() - 2));
            } else {
                return path.equals(query);
            }
        } else {
            return false;
        }
    }
}
