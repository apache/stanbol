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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
/**
 * The ModelWirter extension point allows to have native serialiszation support
 * for specific Entityhub model implementation. <p>
 * See 
 * <a href="https://issues.apache.org/jira/browse/STANBOL-1237">STANBOL-1237</a>
 * for details.
 * @author Rupert Westenthaler
 *
 */
public interface ModelWriter {
    
    /**
     * The default charset used by the Entityhub is <code>UTF-8</code>
     */
    String DEFAULT_CHARSET = "UTF-8";
    /**
     * The default MediaType is {@link MediaType#APPLICATION_JSON} with the
     * {@link #DEFAULT_CHARSET}.
     */
    MediaType DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE.withCharset(DEFAULT_CHARSET);
    /**
     * Class of the native type of this ModelWriter
     * @return the native type
     */
    public Class<? extends Representation> getNativeType();
    /**
     * The list of supported MediaTypes in the order of preference
     * @return the supported mediaTypes
     */
    public List<MediaType> supportedMediaTypes();
    
    /**
     * Getter for the best fitting {@link MediaType} for the parsed one. This
     * Method is intended to let the ModelWriter choose the best fitting type
     * in case the parsed {@link MediaType} uses a wildcard type or sub-type. 
     * @param mediaType a wildcard mediaType
     * @return the selected mediaType or <code>null<code> if none was found.
     */
    public MediaType getBestMediaType(MediaType mediaType);
    /**
     * Writes the parsed {@link Representation} to the stream
     * @param rep the {@link Representation}
     * @param out the stream
     * @param mediaType the {@link MediaType}
     * @throws WebApplicationException If the parsed Representation can not be
     * serialized for some reasons
     * @throws IOException on any Error while writing to the stream
     */
    public void write(Representation rep, OutputStream out, MediaType mediaType) throws WebApplicationException, IOException;
    /**
     * Writes the parsed {@link Entity} to the stream
     * @param entity the {@link Entity} to serialize
     * @param out the stream
     * @param mediaType the {@link MediaType}
     * @throws WebApplicationException If the parsed Entity can not be
     * serialized for some reasons
     * @throws IOException on any Error while writing to the stream
     */
    public void write(Entity entity, OutputStream out, MediaType mediaType) throws WebApplicationException, IOException;
    /**
     * Writes {@link QueryResultList} to the stream
     * @param result the {@link QueryResultList} containing {@link String},
     * {@link Entity} or {@link Representation} instances. Also empty results
     * lists need to be supported.
     * @param out the stream
     * @param mediaType the {@link MediaType}
     * @throws WebApplicationException If the parsed query result list can not be
     * serialized for some reasons
     * @throws IOException on any Error while writing to the stream
     */
    public void write(QueryResultList<?> result, OutputStream out, MediaType mediaType) throws WebApplicationException, IOException;

}
