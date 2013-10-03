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
package org.apache.stanbol.enhancer.servicesapi;

/**
 * Enhancement Engine should throw this exception when the provided Content Item
 * does not match there declared expectation (i.e. a malformed JPEG file).
 *
 * @author ogrisel
 */
public class InvalidContentException extends EngineException {

    private static final long serialVersionUID = 1L;

    public InvalidContentException(String message) {
        super(message);
    }

    public InvalidContentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidContentException(EnhancementEngine ee, ContentItem ci,
            Throwable cause) {
        super(String.format("'%s' failed to process invalid content item '%s'"
                + " with type '%s': %s", ee.getClass().getSimpleName(),
                ci.getUri().getUnicodeString(), ci.getMimeType(), cause.getMessage()), cause);
    }

}
