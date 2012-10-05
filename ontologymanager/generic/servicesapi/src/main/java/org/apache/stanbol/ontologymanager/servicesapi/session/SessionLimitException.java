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
 * Thrown whenever there is an attempt to exceed the maximum allowed number of active sessions.
 * 
 * @author alexdma
 * 
 */
public class SessionLimitException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -7717192393787765332L;

    private int limit;

    public SessionLimitException(int limit) {
        this.limit = limit;
    }

    public SessionLimitException(int limit, String message) {
        super(message);
        this.limit = limit;
    }

    public SessionLimitException(int limit, Throwable cause) {
        this(limit);
        initCause(cause);
    }

    public int getSessionLimit() {
        return limit;
    }

}
