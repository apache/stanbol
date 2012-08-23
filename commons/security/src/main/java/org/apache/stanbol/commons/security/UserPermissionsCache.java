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
package org.apache.stanbol.commons.security;

import java.security.PermissionCollection;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches <code>PermissonCollection</code>S of users
 * 
 * @author mir
 */
public class UserPermissionsCache {
	/**
	 * Stores the mapping between a <code>Principal</code> representing the user
	 * and his/her <code>PermissionCollection</code>.
	 */
	private Map<Principal, PermissionCollection> userPermissionsMap 
				= new HashMap<Principal, PermissionCollection>();

	/**
	 * Return the cached <code>PermissionCollection</code> containing the users
	 * <code>Permission</code>S.
	 * 
	 * @param user
	 * @return
	 */
	public PermissionCollection getCachedUserPermissions(Principal user) {
		return userPermissionsMap.get(user);
	}

	/**
	 * Caches the <code>PermissionCollection</code> for the specified user
	 * 
	 * @param user
	 * @param permissions
	 */
	public void cacheUserPermissions(Principal user,
			PermissionCollection permissions) {
		userPermissionsMap.put(user, permissions);
	}

	/**
	 * Clears the cache.
	 */
	public void clear() {
		userPermissionsMap.clear();
	}
}
