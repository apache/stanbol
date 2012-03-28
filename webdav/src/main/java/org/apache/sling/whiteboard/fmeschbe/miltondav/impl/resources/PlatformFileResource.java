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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.ReplaceableResource;
import com.bradmcevoy.http.exceptions.ConflictException;

public class PlatformFileResource extends SlingFileResource implements
        ReplaceableResource {

    PlatformFileResource(final Resource slingResource) {
        super(slingResource);
    }

    public void copyTo(CollectionResource toCollection, String name) {
        if (toCollection instanceof PlatformFolderResource) {
            // can only move to another platform folder location
            File dest = ((PlatformFolderResource) toCollection).getFile();
            dest = new File(dest, name);
            InputStream ins = null;
            OutputStream out = null;
            try {
                ins = new FileInputStream(getFile());
                out = new FileOutputStream(dest);
                IOUtils.copy(ins, out);
                return;
            } catch (IOException ioe) {
                // TODO: log
            } finally {
                IOUtils.closeQuietly(ins);
                IOUtils.closeQuietly(out);
            }
        }

        // throw new ConflictException(this);
    }

    public void delete() throws ConflictException {
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

    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws ConflictException {
        throw new ConflictException(this);
    }

    // ---------- ReplaceableResource

    public void replaceContent(InputStream in, Long length) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(getFile());
            IOUtils.copyLarge(in, fos);
        } catch (IOException ignore) {
            // TODO: should actually fail here !
        } finally {
            IOUtils.closeQuietly(fos);
        }
        return;
    }

}
