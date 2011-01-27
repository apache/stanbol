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
package org.apache.stanbol.enhancer.it;

import org.apache.stanbol.commons.testing.it.JarExecutor;
import org.junit.BeforeClass;
import org.junit.Test;

/** Demonstrates how to start the runnable jar, with this and a mechanism
 *  to check when the server is ready we should be ready to start writing
 *  integration tests that talk to the server via http.
 */
public class DummyTest {

    @BeforeClass
    public static void startRunnableJar() throws Exception {
        JarExecutor.getInstance(System.getProperties()).start();
    }

    @Test
    public void testNothing() throws Exception {
        // TODO check that server is started and test it...
        final long delay = 10000;
        System.out.println(getClass().getName()
                + " - this test does nothing for now, just waits "
                + delay + " msec to let you check that the server is starting ...");
        Thread.sleep(delay);
        System.out.println(getClass().getName() + " - done waiting");
    }
}
