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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.security.auth.Subject;

import org.apache.stanbol.commons.security.auth.PrincipalImpl;

/**
 * Utility methods for retrieving user information.
 *
 *
 * @author mir, tio
 */
public final class UserUtil {

   /**
    * Restrict instantiation
    */
   private UserUtil() {}

   /**
	 *
	 * @return the name of user which is associated to the 
	 * <code>AccessControlContext</code> of the current thread.
	 */
	public static String getCurrentUserName() {		 
		return getUserName(AccessController.getContext());
	}

	/**
	 *
	 * @return the subject which is associated to the
	 * <code>AccessControlContext</code> of the current thread.
	 */
	public static Subject getCurrentSubject() {
		return getSubject(AccessController.getContext());
	}


	/**
	 * Returns the name of the user associtated with the specified 
	 * <code>AccessControlContext</code>.
	 * 
	 * @param context
	 * @return the username of the current user or null if no
	 * user name is associated with the provided <code>AccessControlContext</code>.
	 */
	public static String getUserName(final AccessControlContext context) {
		Subject subject = getSubject(context);
		if (subject == null) return null;
        Set<Principal> principals = subject.getPrincipals();
        if (principals==null) return null;
        Iterator<Principal> iter = principals.iterator();
		String name = null;
		if (iter.hasNext()) {
				name = iter.next().getName();
		}
		return name;
	}

	/**
	 * Returns the name of the user associtated with the specified
	 * <code>AccessControlContext</code>.
	 *
	 * @param context
	 * @return the username of the current user or null if no
	 * user name is associated with the provided <code>AccessControlContext</code>.
	 */
	public static Subject getSubject(final AccessControlContext context) {
		Subject subject;
		try {
			subject = AccessController.doPrivileged(new PrivilegedExceptionAction<Subject>() {

				@Override
				public Subject run() throws Exception {
					return Subject.getSubject(context);
				}
			});
		} catch (PrivilegedActionException ex) {
			Exception cause = (Exception)ex.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
		return subject;
	}

	public static final Principal ANONYMOUS = new PrincipalImpl("anonymous");

	public static Subject createSubject(String userName) {
		return new Subject(true,
			Collections.singleton(new PrincipalImpl(userName)), Collections.EMPTY_SET,
			Collections.EMPTY_SET);
	}
}
