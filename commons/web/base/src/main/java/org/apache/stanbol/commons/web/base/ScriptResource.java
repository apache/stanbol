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
package org.apache.stanbol.commons.web.base;

/**
 * Simple data transfer object for scripts (typically javascript) to be contributed by WebFragment for
 * inclusion to the HTML head of the pages by the NavigationMixin abstract JAX-RS resource.
 */
@Deprecated
public class ScriptResource implements Comparable<ScriptResource> {

    private final String type;

    private final String relativePath;

    private final WebFragment fragment;
    
    protected final int order;

    public ScriptResource(String type, String relativePath, WebFragment fragment, int order) {
        this.type = type;
        this.relativePath = relativePath;
        this.fragment = fragment;
        this.order = order;
    }

    public String getType() {
        return type;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getFragmentName() {
        return fragment.getName();
    }

    @Override
    public int compareTo(ScriptResource o) {
        return order - o.order;
    }

}
