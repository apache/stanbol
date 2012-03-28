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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.ettrema.http.fs.LockManager;
import com.ettrema.http.fs.SimpleLockManager;

public class SlingResource implements PropFindableResource, GetableResource,
        LockableResource {

    private static final LockManager lockManager = new SimpleLockManager();

    private final Resource slingResource;

    protected SlingResource(final Resource slingResource) {
        this.slingResource = slingResource;
    }

    protected Resource getSlingResource() {
        return slingResource;
    }

    protected Node getNode() {
        return slingResource.adaptTo(Node.class);
    }

    protected File getFile() {
        return slingResource.adaptTo(File.class);
    }

    protected String getAbsPath(final SlingResource parent, final String child) {
        String parentPath = parent.getSlingResource().getPath();
        if (!parentPath.endsWith("/")) {
            parentPath += "/";
        }
        return parentPath + child;
    }

    public Date getModifiedDate() {
        return new Date(
            slingResource.getResourceMetadata().getModificationTime());
    }

    public Date getCreateDate() {
        return new Date(slingResource.getResourceMetadata().getCreationTime());
    }

    public String getName() {
        return slingResource.getName();
    }

    public String getUniqueId() {
        return getName();
    }

    public String checkRedirect(Request request) {
        return null;
    }

    public Object authenticate(String user, String password) {
        // should return the request's slingResource resolver ??
        return null;
    }

    public boolean authorise(Request request, Method method, Auth auth) {
        return true;
    }

    public String getRealm() {
        return "<notused>";
    }

    // ---------- GetableResource

    public Long getContentLength() {
        Object length = getSlingResource().getResourceMetadata().get(
            ResourceMetadata.CONTENT_LENGTH);
        return (length instanceof Long) ? (Long) length : null;
    }

    public String getContentType(String accepts) {
        // TODO: ensure not null
        return getSlingResource().getResourceMetadata().getContentType();
    }

    public Long getMaxAgeSeconds(Auth auth) {
        // TODO: no caching for now ... might reconsider
        return null;
    }

    public void sendContent(OutputStream out, Range range,
            Map<String, String> params, String contentType) throws IOException {
        // just spool the content, ignore are arguments (for now)
        InputStream ins = getSlingResource().adaptTo(InputStream.class);
        if (ins != null) {
            try {
                IOUtils.copy(ins, out);
            } finally {
                IOUtils.closeQuietly(ins);
            }
        }
    }

    // ---------- LockableResource

    public LockResult lock(LockTimeout timeout, LockInfo lockInfo)
            throws NotAuthorizedException {
        return lockManager.lock(timeout, lockInfo, this);
    }

    public LockResult refreshLock(String token) throws NotAuthorizedException {
        return lockManager.refresh(token, this);
    }

    public void unlock(String tokenId) throws NotAuthorizedException {
        lockManager.unlock(tokenId, this);
    }

    public LockToken getCurrentLock() {
        return lockManager.getCurrentToken(this);
    }

}
