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
package org.apache.stanbol.commons.authentication.basic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.AccessControlException;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.security.UserUtil;
import org.apache.stanbol.commons.security.auth.AuthenticationService;
import org.apache.stanbol.commons.security.auth.LoginException;
import org.apache.stanbol.commons.security.auth.NoSuchAgent;
import org.apache.stanbol.commons.security.auth.PrincipalImpl;
import org.apache.stanbol.commons.security.auth.WeightedAuthenticationMethod;
import org.osgi.service.component.ComponentContext;
import org.wymiwyg.commons.util.Base64;


@Component
@Service(WeightedAuthenticationMethod.class)
@Property(name = "weight", intValue = 10)
public class BasicAuthentication implements WeightedAuthenticationMethod {

	/**
	 *	weight of the authentication method
	 */
	private int weight = 10;
	
	@Reference
	AuthenticationService authenticationService;

	public void activate(ComponentContext componentContext) {
		weight = (Integer) componentContext.getProperties().get("weight");
	}

	@Override
	public boolean authenticate(HttpServletRequest request, Subject subject)
			throws LoginException, ServletException {
		String authorization = request.getHeader("Authorization");
		if (authorization != null) {
			String authBase64 = authorization.substring(authorization.indexOf(' ') + 1);
			String[] credentials = new String(Base64.decode(authBase64)).split(":");
			if (credentials.length == 0) {
				return false;
			}
			String userName = credentials[0];
			String password;
			if (credentials.length > 1) {
				password = credentials[1];
			} else {
				password = "";
			}
			try {
				if (authenticationService.authenticateUser(userName, password)) {
					subject.getPrincipals().remove(UserUtil.ANONYMOUS);
					subject.getPrincipals().add(new PrincipalImpl(userName));
					return true;
				} else {
					throw new LoginException(LoginException.PASSWORD_NOT_MATCHING);
				}
			} catch (NoSuchAgent ex) {
				throw new LoginException(LoginException.USER_NOT_EXISTING);
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean writeLoginResponse(HttpServletRequest request,
			HttpServletResponse response, Throwable cause) throws ServletException, IOException {
		if (cause == null || cause instanceof AccessControlException) {
			setUnauthorizedResponse(response,
					"<html><body>unauthorized</body></html>");
			return true;
		}
		if (cause instanceof LoginException) {
			LoginException loginException = (LoginException) cause;
			String type = loginException.getType();
			if (type.equals(LoginException.PASSWORD_NOT_MATCHING)) {
				setUnauthorizedResponse(response,
						"<html><body>Username and password do not match</body></html>");
				return true;
			}
			if (type.equals(LoginException.USER_NOT_EXISTING)) {
				setUnauthorizedResponse(response,
						"<html><body>User does not exist</body></html>");
				return true;
			}
		}
		return false;
	}

	private void setUnauthorizedResponse(final HttpServletResponse response, String message)
			throws ServletException, IOException {
		response.setStatus(401);
		response.addHeader("WWW-Authenticate",
				"Basic realm=\"Apache Stanbol authentication needed\"");
		final java.io.InputStream pipedIn = new ByteArrayInputStream(message.getBytes());
		response.setHeader("Content-Length", ""+message.getBytes().length);
                response.getOutputStream().write(message.getBytes());
	}

	@Override
	public int getWeight() {
		return weight;
	}
	


}
