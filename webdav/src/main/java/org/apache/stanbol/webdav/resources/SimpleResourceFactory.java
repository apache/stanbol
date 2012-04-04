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
package org.apache.stanbol.webdav.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class SimpleResourceFactory implements ResourceFactory {

	final static Logger log = LoggerFactory.getLogger(SimpleResourceFactory.class); 
	
	private CollectionResource rootResource;
	

	public SimpleResourceFactory(CollectionResource rootResource) {
		this.rootResource = rootResource;
	}

	public Resource getResource(String host, String strPath) throws NotAuthorizedException, BadRequestException {
		Path path = Path.path(strPath);
		return getResource(host, path);
	}

	public Resource getResource(String host, Path path) throws NotAuthorizedException, BadRequestException {	
		// STRIP PRECEEDING PATH
		// TODO make this depend on what the dav servlet is actually configured
		// to
		path = path.getStripFirst();
		return getResourceFromStrippedPath(host, path);
	}
	private Resource getResourceFromStrippedPath(String host, Path path) throws NotAuthorizedException, BadRequestException {
		log.info("Getting resource {}", path);
		if (path.isRoot()) {
			return rootResource;
		} else {
			CollectionResource parent = (CollectionResource)getResourceFromStrippedPath(host, 
					path.getParent());
			return parent.child(path.getName());
		}
	}

}