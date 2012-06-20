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
package org.apache.stanbol.ontologymanager.ontonet.api.scope;

/**
 * An ontology space that wraps the components that can be customized by CMS developers, IKS customizers and
 * the like. The custom ontology space becomes read-only after bootstrapping (i.e. after a call to
 * <code>setUp()</code>).
 * 
 * The ontologies in a custom space typically depend on those in the core space. However, a custom space does
 * <i>not</i> know which is the core space, it only imports its ontologies. The core-custom-session
 * relationship between spaces is a scope is handled by external objects.
 * 
 * @author alexdma
 * 
 */
public interface CustomOntologySpace extends OntologySpace {

}
