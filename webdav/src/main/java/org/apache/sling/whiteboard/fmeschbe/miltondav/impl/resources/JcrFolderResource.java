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

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class JcrFolderResource extends SlingCollectionResource {

    JcrFolderResource(final Resource slingResource) {
        super(slingResource);
    }

    public CollectionResource createCollection(String newName)
            throws NotAuthorizedException, ConflictException {
        try {
            Node folder = getNode().addNode(newName, "sling:Folder");
            folder.getSession().save();
            return (CollectionResource) SlingResourceFactory.createResource(getSlingResource().getChild(
                newName));
        } catch (AccessDeniedException ade) {
            throw new NotAuthorizedException(this);
        } catch (RepositoryException re) {
            throw new ConflictException(this);
        }
    }

    public com.bradmcevoy.http.Resource createNew(String newName,
            InputStream inputStream, Long length, String contentType)
            throws IOException, ConflictException {
        try {
            Node parent = getNode();
            if (parent.hasNode(newName)) {
                parent.getNode(newName).remove();
            }

            Node file = getNode().addNode(newName, "nt:file");
            Node content = file.addNode("jcr:content", "nt:resource");

            content.setProperty("jcr:data", inputStream);
            content.setProperty("jcr:lastModified", System.currentTimeMillis());
            content.setProperty("jcr:mimeType", contentType);

            file.getSession().save();

            return SlingResourceFactory.createResource(getSlingResource().getChild(
                newName));
        } catch (RepositoryException re) {
            throw new ConflictException(this);
        }
    }

    public void copyTo(CollectionResource toCollection, String name) {
        if (toCollection instanceof JcrFolderResource) {
            // can only move to another platform folder location
            final String srcAbsPath = getSlingResource().getPath();
            final String destAbsPath = getAbsPath(
                (JcrFolderResource) toCollection, name);
            try {
                getNode().getSession().getWorkspace().copy(srcAbsPath,
                    destAbsPath);
                return;
            } catch (RepositoryException re) {
                // TODO : log
            }
        }

        // cannot move or error during move
        // throw new ConflictException(this);
        // TODO: log
    }

    public void delete() throws NotAuthorizedException, ConflictException,
            BadRequestException {
        try {
            Node node = getNode();
            Session s = node.getSession();
            node.remove();
            s.save();
            return;
        } catch (RepositoryException re) {
            // TODO: log
        }
        throw new ConflictException(this);
    }

    public void moveTo(CollectionResource rDest, String name)
            throws ConflictException {
        if (rDest instanceof JcrFolderResource) {
            // can only move to another platform folder location
            final String srcAbsPath = getSlingResource().getPath();
            final String destAbsPath = getAbsPath((JcrFolderResource) rDest,
                name);
            try {
                getNode().getSession().getWorkspace().move(srcAbsPath,
                    destAbsPath);
                return;
            } catch (RepositoryException re) {
                // TODO : log
            }
        }

        // cannot move or error during move
        throw new ConflictException(this);
    }
}
