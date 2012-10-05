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

import java.util.ArrayList;
import java.util.List;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides user authentication given the user credentials.
 *
 * This service considers all enabled {@link AuthenticationChecker} services to 
 * authenticate a user. If multiple AuthenticationCheckers are present, 
 * only one needs to positively authenticate the user for the authentication
 * process to succeed.
 *
 * @author daniel
 */
@Component
@Service(AuthenticationService.class)
@Reference(name = "restrictionElement",
cardinality = ReferenceCardinality.MANDATORY_MULTIPLE,
policy = ReferencePolicy.DYNAMIC,
referenceInterface = AuthenticationChecker.class)
public class AuthenticationService {
	private final static Logger logger = 
			LoggerFactory.getLogger(AuthenticationCheckerImpl.class);

	private List<AuthenticationChecker> authenticationCheckers =
			new ArrayList<AuthenticationChecker>();


	/**
	 * Authenticates a user given its user name and password credentials.
	 *
	 * @param userName
	 *		The name of the user to authenticate. The name uniquely identifies
	 *		the user.
	 * @param password	
	 *		The password used to authenticate the user identified by the user
	 *		name.
	 * @return	true is the user has been authenticated, false if the user can
	 *			not be authenticated
	 * @throws NoSuchAgent	if no user could be found for the provided user name
	 */
	public boolean authenticateUser(String userName, String password)
			throws NoSuchAgent {

		boolean userNameExists = false;
		for(AuthenticationChecker checker : authenticationCheckers) {
			try {
				if(checker.authenticate(userName, password)) {
					return true;
				}
				userNameExists = true;
			} catch (NoSuchAgent ex) {
				continue;
			}
		}

		if(!userNameExists) {
			logger.info("No service could unsuccessfully authenticate user {}. Reason: user does not exist", userName);
			throw new NoSuchAgent();
		}
		return false;
	}

	/**
	 * Called when new {@link AuthenticationChecker} services are registered in
	 * the OSGi environment.
	 *
	 * @param service	the AuthenticationChecker
	 */
	protected void bindAuthenticationChecker(AuthenticationChecker service) {
		authenticationCheckers.add(service);
	}

	/**
	 * Called when {@link AuthenticationChecker} services are unregistered
	 * in the OSGi environment.
	 *
	 * @param service	the AuthenticationChecker
	 */
	protected void unbindAuthenticationChecker(AuthenticationChecker service) {
		authenticationCheckers.remove(service);
	}
}
