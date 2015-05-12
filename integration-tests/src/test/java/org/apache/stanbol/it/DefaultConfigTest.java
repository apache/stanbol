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

/** Verify that the example config of STANBOL-110 is present */ 
public class DefaultConfigTest extends StanbolTestBase {
    
    @Override
    protected String getCredentials() {
        return "admin:admin";
    }

    @Test
    public void testDefaultConfig() throws Exception {
        // AFAIK there's no way to get the config in machine
        // format from the webconsole, so we just grep the 
        // text config output
        final String path = "/system/console/config/configuration-status-20110304-1743+0100.txt";
        executor.execute(
                builder.buildGetRequest(path)
        )
        .assertStatus(200)
        .assertContentRegexp(
                "PID.*org.apache.stanbol.examples.ExampleBootstrapConfig",
                "anotherValue.*This is AnotherValue.",
                "message.*This test config should be loaded at startup",
                "org.apache.stanbol.examples.ExampleBootstrapConfig.*launchpad:resources/config/org.apache.stanbol.examples.ExampleBootstrapConfig.cfg"
        );
    }
}

