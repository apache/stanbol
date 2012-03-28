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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.ConflictException;

public abstract class SlingCollectionResource extends SlingResource implements
        CollectionResource, FolderResource, PutableResource {

    public SlingCollectionResource(
            org.apache.sling.api.resource.Resource slingResource) {
        super(slingResource);
    }

    public Resource child(String childName) {
        return SlingResourceFactory.createResource(getSlingResource().getChild(
            childName));
    }

    public List<? extends Resource> getChildren() {
        Iterator<org.apache.sling.api.resource.Resource> children = getSlingResource().listChildren();
        List<Resource> childList = new ArrayList<Resource>();
        while (children.hasNext()) {
            childList.add(SlingResourceFactory.createResource(children.next()));
        }
        return childList;
    }

    // ---------- PutableResource

    public Resource createNew(String newName, InputStream inputStream,
            Long length, String contentType) throws IOException,
            ConflictException {
        File file = getFile();
        if (file != null) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(new File(file, newName));
                IOUtils.copyLarge(inputStream, fos);
            } finally {
                IOUtils.closeQuietly(fos);
            }
        }

        Node node = getNode();
        if (node == null) {
            try {
                final Session s = getSlingResource().getResourceResolver().adaptTo(
                    Session.class);
                final String path = getSlingResource().getPath();
                if (s.itemExists(path)) {
                    node = (Node) s.getItem(path);
                } else {
                    String[] segs = path.split("/");
                    node = s.getRootNode();
                    for (String seg : segs) {
                        if (node.hasProperty(seg)) {
                            // yikes, bail out, fail
                            throw new ConflictException(this);
                        } else if (node.hasNode(seg)) {
                            node = node.getNode(seg);
                        } else {
                            node = node.addNode(seg, "sling:Folder");
                        }
                    }
                }
            } catch (ClassCastException cce) {
                // item is a property not a node
                throw new IOException("Cannot create child for " + newName);
            } catch (RepositoryException re) {
                // general repository problem
                throw new IOException("Cannot create child for " + newName
                    + " (" + re + ")");
            }
        }

        try {
            Node fileNode = node.addNode(newName, "nt:file");
            Node content = fileNode.addNode("jcr:content", "nt:unstructured");
            content.setProperty("jcr:data", inputStream);
            content.setProperty("jcr:lastModified", Calendar.getInstance());
            content.setProperty("jcr:mimeType", contentType);
            node.getSession().save();
        } catch (RepositoryException re) {
            throw new IOException(re.toString());
        } finally {
            try {
                node.getSession().refresh(false);
            } catch (RepositoryException re) {
            }
        }

        org.apache.sling.api.resource.Resource slingResource = getSlingResource().getChild(
            newName);
        return SlingResourceFactory.createResource(slingResource);
    }
}
