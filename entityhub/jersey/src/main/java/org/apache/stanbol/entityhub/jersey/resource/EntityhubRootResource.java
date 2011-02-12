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
package org.apache.stanbol.entityhub.jersey.resource;

import javax.ws.rs.Path;

import com.sun.jersey.api.view.ImplicitProduces;

/**
 * Root JAX-RS resource. The HTML view is implicitly rendered by a freemarker
 * template to be found in the META-INF/templates folder.
 */
@Path("/")
@ImplicitProduces("text/html")
public class EntityhubRootResource extends NavigationMixin {

    // TODO: add here some controllers to provide some stats on the usage of the
    // FISE instances: np of content items in the store, nb of registered
    // engines, nb of extracted enhancements, ...

    // Also disable some of the features in the HTML view if the store, sparql
    // engine, engines are not registered.

}
