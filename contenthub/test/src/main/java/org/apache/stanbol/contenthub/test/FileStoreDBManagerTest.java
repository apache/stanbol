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
package org.apache.stanbol.contenthub.test.store.file;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.store.file.FileStoreDBManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SlingAnnotationsTestRunner.class)
public class FileStoreDBManagerTest {
    private static final Logger log = LoggerFactory.getLogger(FileStoreDBManagerTest.class);

    @TestReference
    FileStoreDBManager dbManager;
    
    @Test
    public void dbManagerTest() {
        assertNotNull("Expecting FileStoreDBManager to be injected by Sling test runner", dbManager);
    }

    @Test
    public void testConnection() throws StoreException {
        Connection connection = dbManager.getConnection();
        assertTrue("Null connection", connection != null);
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Failed to close test connection");
            }
        }
    }

    @Test
    public void testTables() throws StoreException {
        assertTrue("recently_enhanced_content_items does not exist", dbManager.existsTable("recently_enhanced_content_items"));
        assertTrue("content_item_revisions does not exist", dbManager.existsTable("content_item_revisions"));
    }
}
