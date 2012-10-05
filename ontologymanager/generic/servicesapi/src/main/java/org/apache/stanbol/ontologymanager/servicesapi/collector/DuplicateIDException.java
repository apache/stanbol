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
package org.apache.stanbol.ontologymanager.servicesapi.collector;

/**
 * Indicates an attempt to illegally create a resource by assigning it an IRI that already identifies another
 * known resource. This exception typically results in the new resource not being created at all.<br>
 * <br>
 * This exception can be subclassed for managing specific resource type-based behaviour (e.g. scopes, spaces
 * or sessions).
 * 
 * @author alexdma
 * 
 */
public class DuplicateIDException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 802959772682427494L;

    /**
     * The duplicate identifier
     */
    protected String dupe;

    /**
     * Returns the IRI that identifies the existing resource. This can be use to obtain the resource itself by
     * passing it onto appropriate managers.
     * 
     * @return the duplicate identifier
     */
    public String getDuplicateID() {
        return dupe;
    }

    /**
     * Creates a new instance of DuplicateIDException.
     * 
     * @param dupe
     *            the duplicate ID.
     */
    public DuplicateIDException(String dupe) {
        this.dupe = dupe;
    }

    /**
     * Creates a new instance of DuplicateIDException.
     * 
     * @param dupe
     *            the duplicate ID.
     * @param message
     *            the detail message.
     */
    public DuplicateIDException(String dupe, String message) {
        super(message);
        this.dupe = dupe;
    }

    /**
     * Creates a new instance of DuplicateIDException.
     * 
     * @param dupe
     *            the duplicate ID.
     * @param cause
     *            the throwable that caused this exception to be thrown.
     */
    public DuplicateIDException(String dupe, Throwable cause) {
        this(dupe);
        initCause(cause);
    }

}
