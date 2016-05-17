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

import java.security.AccessController;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.security.PasswordUtil;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.rdf.ontologies.PERMISSION;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.clerezza.platform.config.SystemConfig;
import org.apache.clerezza.rdf.ontologies.PLATFORM;

/**
 * A service that checks if a provided username and password matches a
 * username and password stored in the system graph
 *
 * @author mir
 */
@Component
@Service(value=AuthenticationChecker.class)
public class AuthenticationCheckerImpl implements AuthenticationChecker {

	private final static Logger logger = LoggerFactory.getLogger(AuthenticationCheckerImpl.class);

	@Reference(target=SystemConfig.SYSTEM_GRAPH_FILTER)
	private Graph systemGraph;

	/**
	 * Checks if the provided username and password matches a username and
	 * password stored in the system graph
	 *
	 * @param userName
	 * @param password
	 * @return true if the password matched, false otherwise
	 * @throws org.apache.stanbol.commons.security.auth.NoSuchAgent
	 */
	@Override
	public boolean authenticate(String userName, String password) throws NoSuchAgent
	{
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			AccessController.checkPermission(new CheckAuthenticationPermission());
		}
		BlankNodeOrIRI agent = getAgentFromGraph(userName);
		String storedPassword = getPasswordOfAgent(agent);
		if (storedPassword.equals(PasswordUtil.convertPassword(password))) {
			logger.debug("user {} successfully authenticated", userName);
			return true;
		} else {
			logger.debug("unsuccessful authentication attempt as user {}", userName);
			return false;
		}
	}

	private BlankNodeOrIRI getAgentFromGraph(String userName) throws NoSuchAgent {
		BlankNodeOrIRI agent;
		Lock l = systemGraph.getLock().readLock();
		l.lock();
		try {
			Iterator<Triple> agents = systemGraph.filter(null, PLATFORM.userName, new PlainLiteralImpl(userName));
			if (agents.hasNext()) {
				agent = agents.next().getSubject();
			} else {
				logger.debug("unsuccessful authentication attempt as non-existent user {}", userName);
				throw new NoSuchAgent();
			}
		} finally {
			l.unlock();
		}
		return agent;
	}

	private String getPasswordOfAgent(BlankNodeOrIRI agent) {
		String storedPassword = "";
		Lock l = systemGraph.getLock().readLock();
		l.lock();
		try {
			Iterator<Triple> agentPassword = systemGraph.filter(agent, PERMISSION.passwordSha1, null);
			if (agentPassword.hasNext()) {
				storedPassword = ((Literal) agentPassword.next().getObject()).getLexicalForm();
			}
		} finally {
			l.unlock();
		}
		return storedPassword;
	}
}
