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
package org.apache.stanbol.contenthub.it;

import static org.junit.Assert.assertTrue;

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitTest extends StanbolTestBase {

    Logger log = LoggerFactory.getLogger(UnitTest.class);

    @Test
    public void triggerContenthubUnitTests() throws Exception {
        RequestExecutor reqExec = executor.execute(builder.buildPostRequest("/system/sling/junit/.json"));
        String content = reqExec.getContent();
        log.info(content);
        assertTrue("see the above logs", !content.contains("failure"));
    }
}
