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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.apache.stanbol.commons.web.base.processor.FreemarkerViewProcessor;
import org.apache.stanbol.commons.web.base.writers.GraphWriter;
import org.apache.stanbol.commons.web.base.writers.JenaModelWriter;
import org.apache.stanbol.commons.web.base.writers.ResultSetWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

/**
 * Define the list of available resources and providers to be used by the Stanbol JAX-RS Endpoint.
 */
public class JerseyEndpointApplication extends Application {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(JerseyEndpointApplication.class);

    protected final Set<Class<?>> contributedClasses = new HashSet<Class<?>>();

    protected final Set<Object> contributedSingletons = new HashSet<Object>();

    protected List<TemplateLoader> templateLoaders = new ArrayList<TemplateLoader>();

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        // resources contributed buy other bundles
        classes.addAll(contributedClasses);

        // message body writers, hard-coded for now
        classes.add(GraphWriter.class);
        classes.add(JenaModelWriter.class);
        classes.add(ResultSetWriter.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.addAll(contributedSingletons);

        TemplateLoader[] loadersArray = templateLoaders.toArray(new TemplateLoader[templateLoaders.size()]);
        MultiTemplateLoader templateLoader = new MultiTemplateLoader(loadersArray);
        singletons.add(new FreemarkerViewProcessor(templateLoader));
        return singletons;
    }

    public void contributeClasses(Set<Class<?>> classes) {
        contributedClasses.addAll(classes);
    }

    public void contributeSingletons(Set<Object> singletons) {
        contributedSingletons.addAll(singletons);
    }

    public void contributeTemplateLoader(TemplateLoader templateLoader) {
        if (templateLoader != null) {
            this.templateLoaders.add(templateLoader);
        }
    }
}
