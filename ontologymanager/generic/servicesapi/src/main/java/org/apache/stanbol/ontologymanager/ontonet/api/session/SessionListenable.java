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
 * An object that can fire or propagate changes in {@link Session} objects.
 * 
 * @deprecated Packages, class names etc. containing "ontonet" in any capitalization are being phased out.
 *             Please switch to
 *             {@link org.apache.stanbol.ontologymanager.servicesapi.session.SessionListenable} as soon as
 *             possible.
 * 
 * @see org.apache.stanbol.ontologymanager.servicesapi.session.SessionListenable
 * 
 * @author alexdma
 * 
 */
public interface SessionListenable extends
        org.apache.stanbol.ontologymanager.servicesapi.session.SessionListenable {

}
