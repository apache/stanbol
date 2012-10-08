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
package org.apache.stanbol.ontologymanager.ontonet.api.collector;

@Deprecated
public class DuplicateIDException extends
        org.apache.stanbol.ontologymanager.servicesapi.collector.DuplicateIDException {

    /**
     * 
     */
    private static final long serialVersionUID = -7598213876617720105L;

    /**
     * Creates a new instance of DuplicateIDException.
     * 
     * @param dupe
     *            the duplicate ID.
     */
    public DuplicateIDException(String dupe) {
        super(dupe);
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
        super(dupe, message);
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
        super(dupe, cause);
    }

}
