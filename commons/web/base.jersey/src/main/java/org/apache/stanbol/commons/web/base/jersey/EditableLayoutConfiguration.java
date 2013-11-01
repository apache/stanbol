/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.commons.web.base.jersey;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.resource.LayoutConfiguration;

/**
 * This class is here for supporting legacy templates
 *
 * @deprecated this sets global css-links and script imports, they should be set
 * specifically in templates
 */
@Component
@Service({LayoutConfiguration.class, EditableLayoutConfiguration.class})
public class EditableLayoutConfiguration implements LayoutConfiguration {

    private List<NavigationLink> navigationLinks;
    private String rootUrl;
    
    public static final String SYSTEM_CONSOLE = "system/console";
    private String staticResourcesRootUrl;
    private List<LinkResource> linkResources;
    private List<ScriptResource> scriptResources;

    
    @Override
    public List<NavigationLink> getNavigationLinks() {
        return navigationLinks;
    }
    
    
    @Override
    public String getRootUrl() {
        return rootUrl;
    }
    
    void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    @Override
    public String getStaticResourcesRootUrl() {
        return staticResourcesRootUrl;
    }

    @Override
    public List<LinkResource> getRegisteredLinkResources() {
        return linkResources;
    }

    @Override
    public List<ScriptResource> getRegisteredScriptResources() {
        return scriptResources;
    }

    void setStaticResourcesRootUrl(String staticResourcesRootUrl) {
        this.staticResourcesRootUrl = staticResourcesRootUrl;
    }

    void setLinkResources(List<LinkResource> linkResources) {
        this.linkResources = linkResources;
    }

    void setScriptResources(List<ScriptResource> scriptResources) {
        this.scriptResources = scriptResources;
    }

    void setNavigationsLinks(List<NavigationLink> navigationLinks) {
        this.navigationLinks = navigationLinks;
    }
}
