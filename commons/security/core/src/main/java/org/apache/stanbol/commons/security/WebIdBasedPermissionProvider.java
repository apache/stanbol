/*
 *  Copyright 2010 reto.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.apache.stanbol.commons.security;

import java.util.Collection;
import org.apache.clerezza.commons.rdf.IRI;

/**
 * Services implementing this interface provide additional permissions
 * to users with a Web-Id.
 * 
 * Typically this is used to assign permissions to roaming users.
 * 
 * @author reto
 */
public interface WebIdBasedPermissionProvider {
	/**
	 * This methods returns string descriptions of the permissions to be granted
	 * to the user with a specified Web-Id. The permissions are described 
	 * using the conventional format '("ClassName" "name" "actions")'.
	 *
	 * @param webId the uri identifying the user (aka Web-Id)
	 * @return the string descriptions of the permissions
	 */
	Collection<String> getPermissions(IRI webId);
}
