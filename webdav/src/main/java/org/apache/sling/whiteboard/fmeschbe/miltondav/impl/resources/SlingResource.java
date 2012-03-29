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
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class SlingResource implements PropFindableResource, GetableResource {
	 
	private static final String FILENAME = "scratchpad.txt";
	private static final String MESSAGE = "Hello world";
 
	public static String getFilename() {
		return FILENAME;
	}
 
	public String getUniqueId() {
		return getFilename();
	}
 

	public String getName() {
		return getFilename();
	}
 
	public Object authenticate(String user, String password) {
		return "anonymous";
	}
 
	public boolean authorise(Request request, Method method, Auth auth) {
		return true;
	}
 
	public String getRealm() {
		return null;
	}
 
	public Date getCreateDate() {
		return new Date();
	}
 
	public Date getModifiedDate() {
		return new Date();
	}
 

	public String checkRedirect(Request request) {
		return null;
	}
 

	public Long getMaxAgeSeconds(Auth auth) {
		return null;
	}
 
	public String getContentType(String accepts) {
		return "text/plain";
	}
 
	public Long getContentLength() {
		return Long.valueOf(MESSAGE.length());
	}
 
	public void sendContent(OutputStream out, Range range, Map params, String contentType) throws IOException, NotAuthorizedException, BadRequestException {
		out.write(MESSAGE.getBytes());
	}
}