/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.whiteboard.fmeschbe.miltondav.impl.resources;

import java.util.Map;

import org.apache.sling.api.resource.Resource;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class ReadOnlyFileResource extends SlingFileResource {

    ReadOnlyFileResource(final Resource slingResource) {
        super(slingResource);
    }

    public void copyTo(CollectionResource toCollection, String name) {
        //TODO: throw new ConflictException(this);
    }

    public void delete() throws NotAuthorizedException, ConflictException,
            BadRequestException {
        throw new ConflictException(this);
    }

    public void moveTo(CollectionResource rDest, String name)
            throws ConflictException {
        throw new ConflictException(this);
    }

    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException, ConflictException {
        throw new ConflictException(this);
    }

}
