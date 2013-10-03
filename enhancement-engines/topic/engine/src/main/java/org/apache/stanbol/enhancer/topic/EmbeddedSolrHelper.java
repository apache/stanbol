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
package org.apache.stanbol.enhancer.topic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.xml.sax.SAXException;

/**
 * Helper class to build an embedded Solr server with a configuration files looked-up from the classpath. This
 * helper is both useful in real service usage (for Cross Validation) and in tests.
 */
public class EmbeddedSolrHelper {

    /**
     * Create a single core Solr server with it's own folder hierarchy.
     */
    public static EmbeddedSolrServer makeEmbeddedSolrServer(File rootFolder,
                                                            String solrServerId,
                                                            String coreId,
                                                            String configName) throws IOException,
                                                                              ParserConfigurationException,
                                                                              SAXException {
        File solrFolder = new File(rootFolder, solrServerId);
        FileUtils.deleteQuietly(solrFolder);
        solrFolder.mkdir();

        // solr conf file
        File solrFile = new File(solrFolder, "solr.xml");
        InputStream is = EmbeddedSolrHelper.class.getResourceAsStream("/solr.xml");
        if (is == null) {
            throw new IllegalArgumentException("missing test solr.xml file");
        }
        IOUtils.copy(is, new FileOutputStream(solrFile));

        // solr conf folder with schema
        File solrCoreFolder = new File(solrFolder, coreId);
        solrCoreFolder.mkdir();
        File solrConfFolder = new File(solrCoreFolder, "conf");
        solrConfFolder.mkdir();
        File schemaFile = new File(solrConfFolder, "schema.xml");
        is = EmbeddedSolrHelper.class.getResourceAsStream("/" + configName + "/conf/schema.xml");
        if (is == null) {
            throw new IllegalArgumentException("missing test schema.xml file");
        }
        IOUtils.copy(is, new FileOutputStream(schemaFile));

        File solrConfigFile = new File(solrConfFolder, "solrconfig.xml");
        is = EmbeddedSolrHelper.class.getResourceAsStream("/" + configName + "/conf/solrconfig.xml");
        if (is == null) {
            throw new IllegalArgumentException("missing test solrconfig.xml file");
        }
        IOUtils.copy(is, new FileOutputStream(solrConfigFile));

        // create the embedded server
        SolrResourceLoader loader = new SolrResourceLoader(solrFolder.getAbsolutePath());
        CoreContainer coreContainer = new CoreContainer(loader);
        //NOTE: with Solr 4.4 we need to call coreContainer.load() otherwise we
        //would be affected by the issue stated at 
        //http://mail-archives.apache.org/mod_mbox/lucene-solr-user/201301.mbox/%3CB7B8B36F1A0BE24F842758C318E56E925EB334%40EXCHDB2.na1.ad.group%3E
        //while this was introduced with 4.1 this only affects this code with 4.4
        //as with an API change the methods implicitly calling load() where
        //removed.
        coreContainer.load();
        CoreDescriptor coreDescriptor = new CoreDescriptor(coreContainer, coreId,
                solrCoreFolder.getAbsolutePath());
        SolrCore core = coreContainer.create(coreDescriptor);
//        coreContainer.createAndLoad(solrHome, configFile)load();
        coreContainer.register(coreId, core, true);
        return new EmbeddedSolrServer(coreContainer, coreId);
    }

}
