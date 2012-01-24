/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.it;

import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.Test;

/** Test access to the OSGi console */
public class OsgiConsoleTest extends StanbolTestBase {
    
    @Test
    public void testDefaultConsolePaths() throws Exception {
        final String [] subpaths = {
                "bundles",
                "components",
                "configMgr",
                "config",
                "licenses",
                "logs",
                "memoryusage",
                "services",
                "shell",
                "stanbol_datafileprovider",
                "osgi-installer",
                "slinglog"
        };
        
        for(String subpath : subpaths) {
            final String path = "/system/console/" + subpath;
            executor.execute(
                    builder.buildGetRequest(path)
                    .withCredentials("admin", "admin")
            ).assertStatus(200);
        }
    }
}
