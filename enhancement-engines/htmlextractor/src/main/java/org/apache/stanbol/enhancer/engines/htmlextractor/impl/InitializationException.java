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
package org.apache.stanbol.enhancer.engines.htmlextractor.impl;

/**
 * <code>InitializationException</code> is thrown when an initialization step
 * fails.
 *
 * @author Joerg Steffen, DFKI
 * @version $Id: InitializationException.java 1068358 2011-02-08 12:58:11Z bdelacretaz $
 */
public class InitializationException extends Exception {

    /**
     * This creates a new instance of <code>InitializationException</code> with
     * null as its detail message. The cause is not initialized.
     */
    public InitializationException() {
        super();
    }

    /**
     * This creates a new instance of <code>InitializationException</code> with
     * the given detail message. The cause is not initialized.
     *
     * @param message
     *            a <code>String</code> with the detail message
     */
    public InitializationException(String message) {
        super(message);
    }

    /**
     * This creates a new instance of <code>InitializationException</code> with
     * the specified cause and a detail message of (cause==null ? null :
     * cause.toString()) (which typically contains the class and detail message
     * of cause).
     *
     * @param cause
     *            a <code>Throwable</code> with the cause of the exception
     *            (which is saved for later retrieval by the {@link #getCause()}
     *            method). (A <tt>null</tt> value is permitted, and indicates
     *            that the cause is nonexistent or unknown.)
     */
    public InitializationException(Throwable cause) {

        super(cause);
    }

    /**
     * This creates a new instance of <code>InitializationException</code> with
     * the given detail message and the given cause.
     *
     * @param message
     *            a <code>String</code> with the detail message
     * @param cause
     *            a <code>Throwable</code> with the cause of the exception
     *            (which is saved for later retrieval by the {@link #getCause()}
     *            method). (A <tt>null</tt> value is permitted, and indicates
     *            that the cause is nonexistent or unknown.)
     */
    public InitializationException(String message, Throwable cause) {

        super(message, cause);
    }

}
