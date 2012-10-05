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

/**
 * An event that encompasses a change in the state of a KReS session.
 * 
 * @author alexdma
 * 
 */
public class SessionEvent {

    public enum OperationType {
        ACTIVATE,
        CLOSE,
        CREATE,
        DEACTIVATE,
        KILL,
        STORE
    }

    /**
     * The session affected by this event.
     */
    private Session affectedSession;

    private OperationType operationType;

    /**
     * Creates a new instance of SessionEvent.
     * 
     * @param session
     *            the KReS session affected by this event
     */
    public SessionEvent(Session session, OperationType operationType) {
        if (operationType == null) throw new IllegalArgumentException(
                "No operation type specified for this session event.");
        if (session == null) throw new IllegalArgumentException(
                "No session specified for this session event.");
        this.operationType = operationType;
        this.affectedSession = session;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    /**
     * Returns the KReS session affected by this event.
     * 
     * @return the affected KReS session
     */
    public Session getSession() {
        return affectedSession;
    }

}
