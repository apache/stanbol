/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.launchpad;

import java.io.File;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.apache.sling.launchpad.base.shared.SharedConstants.SLING_HOME;

public class Main {

    public static final String DEFAULT_STANBOL_HOME = "stanbol";
    /**
     * If this argument is set Stanbol is started without a securitymanager
     */
    public static final String NOSECURITYARG = "-no-security";
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        String home = System.getProperties().getProperty(SLING_HOME);
        if(home == null){
            home = new File(DEFAULT_STANBOL_HOME).getAbsolutePath();
            System.setProperty(SLING_HOME, home);
        } //else do not override user configured values
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        if (argsList.contains(NOSECURITYARG)) {
        	argsList.remove(NOSECURITYARG);
        } else {
        	args = argsList.toArray(new String[argsList.size()]);
	        Policy.setPolicy(new Policy() {
				@Override
				public PermissionCollection getPermissions(ProtectionDomain domain) {
					PermissionCollection result = new Permissions();
					result.add(new AllPermission());
					return result;
				}
			});
	        System.setSecurityManager(new SecurityManager());
        }
        //now use the standard Apache Sling launcher to do the job
        org.apache.sling.launchpad.app.Main.main(args);
    }

}
