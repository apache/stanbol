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
package org.apache.stanbol.entityhub.indexing.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceImporter;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceLoader;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceState;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceLoaderTest {
    /**
     * mvn copies the resources in "src/test/resources" to target/test-classes
     */
    private static final String TEST_CONFIGS_ROOT = 
        FilenameUtils.separatorsToSystem("/target/test-classes/resourceLoaderTest/");
    private static final String TEST_FOLDER_NAME = 
        FilenameUtils.separatorsToSystem("testFolder/");
    protected static Logger log = LoggerFactory.getLogger(ResourceLoaderTest.class);
    private static String rootDir;

        public static class DummyResourceImporter implements ResourceImporter {

        Collection<String> expectedNames;
        DummyResourceImporter(Collection<String> expectedResource){
            this.expectedNames = new HashSet<String>();
            for(String resource : expectedResource){
                //this works only if there are not two files with the same name
                //so add an assertion to check for that
                String name = FilenameUtils.getName(resource);
                assertFalse("This Test requires that there are no files with the same name!",
                    expectedNames.contains(name));
                this.expectedNames.add(name);
            }
        }
        @Override
        public ResourceState importResource(InputStream is, String resourceName) throws IOException {
            assertNotNull(is);
            assertNotNull(resourceName);
            assertFalse(resourceName.isEmpty());
            assertTrue("resourceName '"+resourceName+"' not expected",
                expectedNames.remove(resourceName));
            IOUtils.closeQuietly(is);
            log.debug("Import RDFTerm {}",resourceName);
            if(resourceName.startsWith("ignore")){
                return ResourceState.IGNORED;
            } else if(resourceName.startsWith("error")){
                throw new IOException("To test an Error");
            } else {
                return ResourceState.LOADED;
            }
        }
        public void checkAllProcessed(){
            assertTrue(expectedNames.isEmpty());
        }
        
    }
    
    @BeforeClass
    public static void init(){
        String baseDir = System.getProperty("basedir");
        if(baseDir == null){
            baseDir = System.getProperty("user.dir");
        }
        rootDir = baseDir+TEST_CONFIGS_ROOT;
    }

    @Test
    public void testSingleFile(){
        DummyResourceImporter importer = new DummyResourceImporter(
            Arrays.asList(rootDir+"singleFileTest.txt"));
        ResourceLoader loader = new ResourceLoader(importer, false, false);
        loader.addResource(new File(rootDir,"singleFileTest.txt"));
        assertEquals(new HashSet<String>(Arrays.asList(rootDir+"singleFileTest.txt")), 
            loader.getResources(ResourceState.REGISTERED));
        assertTrue(loader.getResources(ResourceState.ERROR).isEmpty());
        assertTrue(loader.getResources(ResourceState.LOADED).isEmpty());
        assertTrue(loader.getResources(ResourceState.IGNORED).isEmpty());
        loader.loadResources();
        assertEquals(new HashSet<String>(Arrays.asList(rootDir+"singleFileTest.txt")),
            loader.getResources(ResourceState.LOADED));
        assertTrue(loader.getResources(ResourceState.REGISTERED).isEmpty());
        assertTrue(loader.getResources(ResourceState.IGNORED).isEmpty());
        assertTrue(loader.getResources(ResourceState.ERROR).isEmpty());
        importer.checkAllProcessed();
        
    }
    
    @Test
    public void testFailOnError(){
    	assertTrue(isFailedOnError(true));
    	assertFalse(isFailedOnError(false));	
    }
    
    @Test
    public void testFolderWithoutProcessingArchives(){
        String folder = rootDir+TEST_FOLDER_NAME;
        Collection<String> expectedFolderResources = new HashSet<String>(Arrays.asList(
            folder+"archiveInFolder.zip",
            folder+"archiveWithIgnore.zip",
            folder+"archiveWithError.zip",
            folder+"errorFileInFolder.txt",
            folder+"fileInFolder.txt",
            folder+"ignoreFileInFolder.txt",
            folder+"otherFileInFolder.txt"));
        DummyResourceImporter importer = new DummyResourceImporter(
            expectedFolderResources);
        ResourceLoader loader = new ResourceLoader(importer, false, false);
        loader.addResource(new File(rootDir,"testFolder"));
        assertEquals(expectedFolderResources, loader.getResources(ResourceState.REGISTERED));
        assertTrue(loader.getResources(ResourceState.ERROR).isEmpty());
        assertTrue(loader.getResources(ResourceState.LOADED).isEmpty());
        assertTrue(loader.getResources(ResourceState.IGNORED).isEmpty());
        loader.loadResources();
        assertEquals(new HashSet<String>(Arrays.asList(
            folder+"archiveInFolder.zip", folder+"fileInFolder.txt",
            folder+"otherFileInFolder.txt",folder+"archiveWithIgnore.zip",
            folder+"archiveWithError.zip")), 
            loader.getResources(ResourceState.LOADED));
        assertTrue(loader.getResources(ResourceState.REGISTERED).isEmpty());
        assertEquals(new HashSet<String>(Arrays.asList(
            folder+"errorFileInFolder.txt")), 
            loader.getResources(ResourceState.ERROR));
        assertEquals(new HashSet<String>(Arrays.asList(
            folder+"ignoreFileInFolder.txt")), 
            loader.getResources(ResourceState.IGNORED));
    }
    @Test
    public void testFolderWithProcessingArchives(){
        String folder = rootDir+TEST_FOLDER_NAME;
        Collection<String> expectedResources = new HashSet<String>(Arrays.asList(
            folder+"archiveInFolder.zip",
            folder+"archiveWithIgnore.zip",
            folder+"archiveWithError.zip",
            folder+"errorFileInFolder.txt",
            folder+"fileInFolder.txt",
            folder+"ignoreFileInFolder.txt",
            folder+"otherFileInFolder.txt"));
        //the resourceNames send to the importer are now different because the
        //archives are processed and the entries are sent to the  ResourceImporter
        Collection<String> expectedResourceNames = Arrays.asList(
            "fileInArchive.txt", //part of archiveInFolder.zip
            "otherFileInArchive.txt", //part of archiveInFolder.zip
            "ignoreFileInArchive.txt", //part of archiveWithIgnore.zip
            "errorFileInArchive.txt", //part of archiveWithError.zip
            "errorFileInFolder.txt",
            "fileInFolder.txt",
            "ignoreFileInFolder.txt",
            "otherFileInFolder.txt");
        DummyResourceImporter importer = new DummyResourceImporter(
            expectedResourceNames);
        ResourceLoader loader = new ResourceLoader(importer, true, false);
        loader.addResource(new File(rootDir,TEST_FOLDER_NAME));

        assertEquals(expectedResources, loader.getResources(ResourceState.REGISTERED));
        assertTrue(loader.getResources(ResourceState.ERROR).isEmpty());
        assertTrue(loader.getResources(ResourceState.LOADED).isEmpty());
        assertTrue(loader.getResources(ResourceState.IGNORED).isEmpty());
        loader.loadResources();
        assertEquals(new HashSet<String>(Arrays.asList(
            folder+"archiveInFolder.zip", 
            folder+"archiveWithIgnore.zip", //ignored files in archives are OK
            folder+"fileInFolder.txt",folder+"otherFileInFolder.txt")), 
            loader.getResources(ResourceState.LOADED));
        assertTrue(loader.getResources(ResourceState.REGISTERED).isEmpty());
        assertEquals(new HashSet<String>(Arrays.asList(
            folder+"errorFileInFolder.txt",
            folder+"archiveWithError.zip")), //archive with errors MUST be ERROR
            loader.getResources(ResourceState.ERROR));
        assertEquals(new HashSet<String>(Arrays.asList(
            folder+"ignoreFileInFolder.txt")), 
            loader.getResources(ResourceState.IGNORED));
    }
    
    private boolean isFailedOnError(boolean failOnError){
		String folder = rootDir + TEST_FOLDER_NAME;
		boolean failed = false;
		DummyResourceImporter importer = new DummyResourceImporter(
				Arrays.asList(folder + "errorFileInFolder.txt"));
		ResourceLoader loader = new ResourceLoader(importer, false, failOnError);
		loader.addResource(new File(folder, "errorFileInFolder.txt"));
		try {
			loader.loadResources();
		} catch (IllegalStateException ex) {
			failed = true;
		}
		return failed;
    }
}
