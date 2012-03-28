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

import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.ReplaceableResource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class JcrFileResource extends SlingFileResource implements
        ReplaceableResource {

    JcrFileResource(final Resource slingResource) {
        super(slingResource);
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

    public String processForm(Map<String, String> parameters,
            Map<String, FileItem> files) throws BadRequestException,
            NotAuthorizedException, ConflictException {
        // TODO Auto-generated method stub
        return null;
    }

    // ---------- ReplaceableResource

    public void replaceContent(InputStream in, Long length) {
        Node node = getNode();
        try {
            Node content = node.getNode("jcr:content");
            content.setProperty("jcr:data", in);
            content.setProperty("jcr:lastModified", Calendar.getInstance());
            node.getSession().save();
        } catch (RepositoryException re) {
        } finally {
            try {
                node.getSession().refresh(false);
            } catch (RepositoryException re) {
            }
        }
    }

}
