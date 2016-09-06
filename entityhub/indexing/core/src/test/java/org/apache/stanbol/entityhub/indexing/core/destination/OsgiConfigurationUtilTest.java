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

package org.apache.stanbol.entityhub.indexing.core.destination;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import static org.junit.Assert.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.stanbol.entityhub.indexing.core.IndexerFactory;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiConfigurationUtilTest {
	
	private static final Logger log = LoggerFactory.getLogger(OsgiConfigurationUtilTest.class);
    private static final String CONFIG_ROOT = 
        FilenameUtils.separatorsToSystem("testOsgiConfiguration/");

    private static IndexerFactory factory;
    /**
     * mvn copies the resources in "src/test/resources" to target/test-classes.
     * This folder is than used as classpath.<p>
     * "/target/test-files/" does not exist, but is created by the
     * {@link IndexingConfig}.
     */
    private static final String TEST_ROOT = 
        FilenameUtils.separatorsToSystem("/target/test-files");
    private static String  userDir;
    private static String testRoot;
    /**
     * The methods resets the "user.dir" system property
     */
    @BeforeClass
    public static void initTestRootFolder(){
        String baseDir = System.getProperty("basedir");
        if(baseDir == null){
            baseDir = System.getProperty("user.dir");
        }
        //store the current user.dir
        userDir = System.getProperty("user.dir");
        testRoot = baseDir+TEST_ROOT;
        log.info("ConfigTest Root : "+testRoot);
        //set the user.dir to the testRoot (needed to test loading of missing
        //configurations via classpath
        //store the current user.dir and reset it after the tests
        System.setProperty("user.dir", testRoot);
    }
    
	@Test
	public void testOsgiConfigurationGeneration() throws URISyntaxException, IOException{
		String name = CONFIG_ROOT+"bundlebuild";
		
		//copy test destination folder to mock-up the destination "config" folder creation operation
		String destConfFolder = name + "/indexing/destination/" + "config";
		File destConfSource = new File( this.getClass().getResource("/" + destConfFolder).toURI());
		File destConfTarget = new File(testRoot + "/" + destConfFolder);
		FileUtils.copyDirectory( destConfSource, destConfTarget);
		
		File distFolder = new File(testRoot+'/'+name+"/indexing/dist");
		File bundle = new File(distFolder,"org.apache.stanbol.data.site.simple-1.0.0.jar");
		
        IndexingConfig config = new IndexingConfig(name,name){};
        
        OsgiConfigurationUtil.createBundle(config);
        
        assertTrue("Configuration bundle '"+bundle+"' was not created!", bundle.isFile());
        
        //if choose to get .createBundle throwing exceptions, this test may be better :
//        try{
//        	OsgiConfigurationUtil.createBundle(config);
//        }catch(Exception npe){
//        	fail("NPE fired when try to build the osgi configuration");
//        }
        
	}
}
