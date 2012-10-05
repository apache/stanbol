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
package org.apache.stanbol.commons.security.auth;

import java.security.Permission;

/*@PermissionInfo(value="Authentication Checker Permission", description=" Grants permission " +
	"to use the AuthenticationChecker service which checks if a provided username " +
	"and password matches a username and password stored in the system graph")*/
public class CheckAuthenticationPermission extends Permission {

	public CheckAuthenticationPermission() {
		super(null);
	}
	
	public CheckAuthenticationPermission(String name, String actions) {
		super(null);
	}
	
	@Override
	public boolean implies(Permission permission) {
		if (permission instanceof CheckAuthenticationPermission) {
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof CheckAuthenticationPermission) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String getActions() {
		return "authenticate";
	}

}
