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
package org.apache.stanbol.ontologymanager.servicesapi.session;

import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;

/**
 * Objects that want to listen to events affecting sessions should implement this interface and add themselves
 * as listener to a manager.
 * 
 * @author alexdma
 * 
 */
public interface SessionListener {

    /**
     * Called whenever a scope is appended to a session.
     * 
     * @param session
     *            the affected session
     * @param scopeId
     *            the identifier of the scope that was attached.
     */
    void scopeAppended(Session session, String scopeId);

    /**
     * Called whenever a scope is detached from a session.
     * 
     * @param session
     *            the affected session
     * @param scopeId
     *            the identifier of the scope that was attached. Note that the corresponding
     *            {@link Scope} could be null if detachment occurred as a consequence of a scope
     *            deletion.
     * */
    void scopeDetached(Session session, String scopeId);

    /**
     * Called whenever an event affecting a session is fired. This method encompasses all and only the event
     * where it only interesting to know the affected session.
     * 
     * @param event
     *            the session event.
     */
    void sessionChanged(SessionEvent event);

}
