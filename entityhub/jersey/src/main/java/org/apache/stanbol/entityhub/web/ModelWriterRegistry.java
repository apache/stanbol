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

package org.apache.stanbol.entityhub.web;

import java.util.Collection;

import javax.ws.rs.core.MediaType;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.osgi.framework.ServiceReference;

/**
 * Tracks all registered {@link ModelWriter} services and provides an API for
 * accessing those based on the requested {@link MediaType} and native type.<p>
 * See 
 * <a href="https://issues.apache.org/jira/browse/STANBOL-1237">STANBOL-1237</a>
 * for details.
 * @author Rupert Westenthaler
 *
 */
public interface ModelWriterRegistry {

    /**
     * Getter for a sorted list of References to {@link ModelWriter} that can
     * serialise Representations to the parsed {@link MediaType}. If a
     * nativeType of the Representation is given {@link ModelWriter} for that
     * specific type will be preferred.
     * @param mediaType The {@link MediaType}. Wildcards are supported
     * @param nativeType optionally the native type of the {@link Representation}
     * @return A sorted collection of references to compatible {@link ModelWriter}.
     * Use {@link #getService()} to receive the actual service. However note that
     * such calls may return <code>null</code> if the service was deactivated in
     * the meantime.
     */
    public Collection<ServiceReference> getModelWriters(MediaType mediaType, 
        Class<? extends Representation> nativeType);

    /**
     * Getter for the {@link ModelWriter} service for the parsed ServiceReference
     * @param ref the {@link ServiceReference}
     * @return The {@link ModelWriter} or <code>null<code> if the referenced
     * service was deactivated.
     */
    public ModelWriter getService(ServiceReference ref);
    
    /**
     * Checks if the parsed mediaType is writeable
     * @param mediaType the mediaType
     * @param nativeType optionally the native type of the {@link Representation}
     * @return the state
     */
    public boolean isWriteable(MediaType mediaType, Class<? extends Representation> nativeType);
    
}
