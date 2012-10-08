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
package org.apache.stanbol.ontologymanager.ontonet.api.session;

/**
 * Manages session objects via CRUD-like operations. A <code>SessionManager</code> maintains in-memory storage
 * of sessions, creates new ones and either destroys or stores existing ones persistently. All sessions are
 * managed via unique identifiers of the <code>org.semanticweb.owlapi.model.IRI</code> type.<br>
 * <br>
 * NOTE: implementations should either be synchronized, or document whenever they are not.
 * 
 * @deprecated Packages, class names etc. containing "ontonet" in any capitalization are being phased out.
 *             Please switch to {@link org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager}
 *             as soon as possible.
 * 
 * @see org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager
 * 
 * @author alexdma
 * 
 */
public interface SessionManager extends org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager {

}
