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
package org.apache.stanbol.commons.web.base.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;

/**
 * Mixin class to provide the controller method for the navigation template.
 * 
 * TODO: make the list of menu items dynamically contributed by WebFragments from the OSGi runtime.
 */
//not itself a component but subclasses must be
//according to http://felix.apache.org/documentation/subprojects/apache-felix-maven-scr-plugin.html
//"With the annotations the super class is required to have the Component annotation."
@Component(componentAbstract = true)
public class BaseStanbolResource extends TemplateLayoutConfiguration {
       
   
    @Reference
    private LayoutConfiguration layoutConfiguration;

    @Context
    protected UriInfo uriInfo;

    protected LayoutConfiguration getLayoutConfiguration() {
        return layoutConfiguration;
    }
    
    protected UriInfo getUriInfo() {
        return uriInfo;
    }
    
    /**
     * Subclasses extend this object  to provide 
     * a data model for rendering with a Viewable Object
     */
    public abstract class ResultData extends TemplateLayoutConfiguration {

        @Override
        protected LayoutConfiguration getLayoutConfiguration() {
            return BaseStanbolResource.this.getLayoutConfiguration();
        }

        @Override
        protected UriInfo getUriInfo() {
            return BaseStanbolResource.this.getUriInfo();
        }


    }


}
