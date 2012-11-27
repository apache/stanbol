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

import org.junit.Test;

/**
 * Default MultiThreadedTest tool. Supports the use of System properties to
 * configure the test. See the <a href="http://stanbol.apache.org/docs/trunk/utils/enhancerstresstest">
 * Stanbol Enhancer Stress Test Utility</a> documentation for details
 * @author Rupert Westenthaler
 *
 */
public final class MultiThreadedTest extends MultiThreadedTestBase {
    
    public MultiThreadedTest(){
        super();
    }

    @Test
    public void testMultipleParallelRequests() throws Exception {
        performTest(TestSettings.fromSystemProperties());
    }
    
}
