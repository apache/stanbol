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
package org.apache.stanbol.factstore.web.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.factstore.web.FactStoreWebFragment;

public class BaseFactStoreResource extends BaseStanbolResource {

    @Override
    @SuppressWarnings("unchecked")
    public List<ScriptResource> getRegisteredScriptResources() {
        if (servletContext != null) {
            List<ScriptResource> scriptResources = (List<ScriptResource>) servletContext
                    .getAttribute(SCRIPT_RESOURCES);

            List<ScriptResource> fragmentsScriptResources = new ArrayList<ScriptResource>();
            for (ScriptResource scriptResource : scriptResources) {
                if (scriptResource.getFragmentName().equals("home") ||
                    scriptResource.getFragmentName().equals(FactStoreWebFragment.NAME)) {
                    fragmentsScriptResources.add(scriptResource);
                }
            }
            return fragmentsScriptResources;
        } else {
            return Collections.emptyList();
        }
    }
    
}
