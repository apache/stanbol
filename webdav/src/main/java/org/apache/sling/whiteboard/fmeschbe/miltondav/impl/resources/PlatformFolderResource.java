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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class PlatformFolderResource extends SlingCollectionResource {

    PlatformFolderResource(final Resource slingResource) {
        super(slingResource);
    }

    public CollectionResource createCollection(String newName)
            throws NotAuthorizedException, ConflictException {
        File child = new File(getFile(), newName);
        if (child.mkdir()) {
            return (CollectionResource) SlingResourceFactory.createResource(getSlingResource().getChild(
                newName));
        }

        throw new ConflictException(this);
    }

    public com.bradmcevoy.http.Resource createNew(String newName,
            InputStream inputStream, Long length, String contentType)
            throws IOException, ConflictException {
        File file = getSlingResource().adaptTo(File.class);
        File child = new File(file, newName);
        if (child.exists()) {
            child.delete();
        }

        FileOutputStream out = new FileOutputStream(child);
        IOUtils.copy(inputStream, out);
        IOUtils.closeQuietly(out);

        return SlingResourceFactory.createResource(getSlingResource().getChild(
            newName));
    }

    public void copyTo(CollectionResource toCollection, String name) {
        // throw new ConflictException(this);
    }

    public void delete() throws NotAuthorizedException, ConflictException,
            BadRequestException {
        if (!getFile().delete()) {
            throw new ConflictException(this);
        }
    }

    public void moveTo(CollectionResource rDest, String name)
            throws ConflictException {
        if (rDest instanceof PlatformFolderResource) {
            // can only move to another platform folder location
            File dest = ((PlatformFolderResource) rDest).getFile();
            dest = new File(dest, name);
            if (getFile().renameTo(dest)) {
                // over and out;
                return;
            }
        }

        // cannot move due to existing target or not platform folder
        throw new ConflictException(this);
    }

}
