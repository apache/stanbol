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
package org.apache.stanbol.ontologymanager.servicesapi.scope;

/**
 * Thrown whenever an operation on a scope that has not been registered is thrown.
 * 
 * @author alexdma
 * 
 */
public class NoSuchScopeException extends RuntimeException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 6339531579406287445L;

    private String scopeID = null;

    public NoSuchScopeException(String scopeID) {
        this.scopeID = scopeID;
    }

    public String getScopeId() {
        return scopeID;
    }

}
