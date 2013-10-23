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
package org.apache.stanbol.enhancer.contentitem.file;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.test.ContentItemFactoryTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class CustomDirFileContentItemFactoryTest extends ContentItemFactoryTest {

    private static File customDir;
    private static ContentItemFactory factory;
    
    @BeforeClass
    public static void init(){
        String prefix = System.getProperty("basedir",".");
        File targetDir = new File(prefix,"target");
        customDir = new File(targetDir,"fileContentItem");
    }
    
    @Override
    protected ContentItemFactory createContentItemFactory() throws IOException {
        if(factory == null){
            factory = new FileContentItemFactory(customDir);
        }
        return factory;
    }

    /**
     * Tests that the specified directory is actually used!
     */
    @Test
    public void testCustomDir() throws IOException {
        assertTrue("The custom dir '"+customDir+"'MUST exist",
            customDir.exists());
        assertTrue("The custom dir '"+customDir+"'MUST be an directory",
            customDir.isDirectory());
        int numFiles = customDir.list().length;
        Blob blob = contentItemFactory.createBlob(new StringSource("ensure a file exist"));
        assertNotNull(blob);
        Assert.assertEquals("Creating a new Blob has not increased the " +
        		"number of files by one!",numFiles, customDir.list().length-1);
    }
}
