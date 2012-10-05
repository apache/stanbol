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
