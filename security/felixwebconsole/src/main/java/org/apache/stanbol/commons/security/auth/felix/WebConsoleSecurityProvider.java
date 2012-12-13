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
package org.apache.stanbol.commons.security.auth.felix;

import java.security.AccessController;
import java.security.AllPermission;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.WebConsoleSecurityProvider2;


/**
 * Provides a Felix WebConsole Security provider that relies on Stanbol Security
 */
@Component
@Service(org.apache.felix.webconsole.WebConsoleSecurityProvider.class)
public class WebConsoleSecurityProvider implements WebConsoleSecurityProvider2 {

	@Override
	public Object authenticate(String username, String password) {
		// this method should not be called 
		return null;
	}

	@Override
	public boolean authorize(Object user, String role) {
		// TODO permission checking
		return false;
	}

	@Override
	public boolean authenticate(HttpServletRequest request,
			HttpServletResponse response) {
		// surprisingly this works even when stanbol is started without -s for securitymanager
		//TODO check for some more concrete permission
		AccessController.checkPermission(new AllPermission());
		return true;
	}

}
