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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.ontologies.OSGI;
import org.apache.clerezza.rdf.ontologies.PERMISSION;
import org.apache.clerezza.rdf.ontologies.SIOC;

/**
 * Provides utility methods to extract infomation for the permission assignment.
 * 
 * @author clemens
 */
class PermissionDefinitions {

	private Graph systemGraph;

	PermissionDefinitions(Graph systeGraph) {
		this.systemGraph = systeGraph;
	}

	/**
	 * Returns the permissions of a specified location.
	 * I.e. the permissions of all permission assignments matching <code>location</code>.
	 * 
	 * @param location	the location of a bundle
	 * @return an array with <code>PermissionInfo</code> elements
	 */
	PermissionInfo[] retrievePermissions(String location) {
		List<PermissionInfo> permInfoList = new ArrayList<PermissionInfo>();

		Iterator<Triple> ownerTriples =
				systemGraph.filter(new IRI(location), OSGI.owner, null);

		if (ownerTriples.hasNext()) {
			BlankNodeOrIRI user = (BlankNodeOrIRI) ownerTriples.next().getObject();
			lookForPermissions(user, permInfoList);
		}

		if (permInfoList.isEmpty()) {
			return null;
		}
		return permInfoList.toArray(new PermissionInfo[permInfoList.size()]);
	}

	/**
	 * Look for all permissions of a role and add them to a list.
	 * And if the role has another role, then execute this function recursively,
	 * until all permissions are found.
	 * 
	 * @param role	a <code>BlankNodeOrIRI</code> which is either a user or a role
	 * @param permInfoList	a list with all the added permissions of this bundle
	 */
	private void lookForPermissions(BlankNodeOrIRI role, List<PermissionInfo> permInfoList) {
		Iterator<Triple> permissionTriples =
				systemGraph.filter(role, PERMISSION.hasPermission, null);

		while (permissionTriples.hasNext()) {

			BlankNodeOrIRI permission = (BlankNodeOrIRI) permissionTriples.next().getObject();

			Iterator<Triple> javaPermissionTriples =
					systemGraph.filter(permission, PERMISSION.javaPermissionEntry, null);

			while (javaPermissionTriples.hasNext()) {

				Triple t = javaPermissionTriples.next();
				Literal permEntry = (Literal) t.getObject();

				permInfoList.add(new PermissionInfo(permEntry.getLexicalForm()));
			}
		}

		Iterator<Triple> roleTriples =
				systemGraph.filter(role, SIOC.has_function, null);

		while (roleTriples.hasNext()) {
			BlankNodeOrIRI anotherRole = (BlankNodeOrIRI) roleTriples.next().getObject();
			this.lookForPermissions(anotherRole, permInfoList);
		}
	}
}
