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

import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.osgi.service.component.ComponentContext;

/**
 * A component with no required dependency ensuring that the UserAwarePolicy is 
 * activated if available or otherwise a restrictive default policy is set
 * 
 * @author reto
 */
@Component
@Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY,
name = "userAwarePolicy",
policy = ReferencePolicy.DYNAMIC,
referenceInterface = UserAwarePolicy.class)
public class SecurityActivator {

	private Policy originalPolicy;
	private Policy fallBackPolicy = new Policy() {

		@Override
		public PermissionCollection getPermissions(final ProtectionDomain domain) {

			PermissionCollection result;

			Principal[] principals = domain.getPrincipals();
			if (principals.length > 0) {
				result = new Permissions();
			} else {
				result = originalPolicy.getPermissions(domain);
			}
			return result;
		}
	};

	protected void activate(final ComponentContext context) throws Exception {
		originalPolicy = Policy.getPolicy();
	}

	protected void deactivate(final ComponentContext context) throws Exception {
		Policy.setPolicy(originalPolicy);
	}

	protected void bindUserAwarePolicy(UserAwarePolicy userAwarePolicy) {
		Policy.setPolicy(userAwarePolicy);
	}

	protected void unbindUserAwarePolicy(UserAwarePolicy userAwarePolicy) {
		Policy.setPolicy(fallBackPolicy);
	}
}
