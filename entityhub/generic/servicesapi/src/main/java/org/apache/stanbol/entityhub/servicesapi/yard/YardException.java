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
package org.apache.stanbol.entityhub.servicesapi.yard;

import org.apache.stanbol.entityhub.servicesapi.EntityhubException;

/**
 * Used to indicate an error while performing an operation on a yard
 * @author Rupert Westenthaler
 *
 */
public class YardException extends EntityhubException {

    /**
     * The default serial version UIR
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message and a cause
     * @param reason the message describing the reason for the error
     * @param cause the parent
     */
    public YardException(String reason, Throwable cause) {
        super(reason, cause);
    }

    /**
     * Creates an exception with a message and a cause
     * @param reason the message describing the reason for the error
     */
    public YardException(String reason) {
        super(reason);
    }

}
