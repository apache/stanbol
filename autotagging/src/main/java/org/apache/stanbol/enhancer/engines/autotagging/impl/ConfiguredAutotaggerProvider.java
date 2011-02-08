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
package org.apache.stanbol.enhancer.engines.autotagging.impl;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.commons.io.FileUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.store.FSDirectory;
import org.apache.stanbol.autotagging.Autotagger;
import org.apache.stanbol.autotagging.jena.ModelIndexer;
import org.apache.stanbol.enhancer.engines.autotagging.AutotaggerProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple OSGi component to look up a configured autotagger instance.
 */
@Component(immediate = true, metatype = true)
@Service
public class ConfiguredAutotaggerProvider implements AutotaggerProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(value = "")
    public static final String LUCENE_INDEX_PATH = "org.apache.stanbol.enhancer.engines.autotagging.indexPath";

    private Autotagger autotagger;

    protected BundleContext bundleContext;

    public Autotagger getAutotagger() {
        return autotagger;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @SuppressWarnings("unchecked")
    public void activate(ComponentContext ce) throws IOException {
        bundleContext = ce.getBundleContext();
        Dictionary<String, String> properties = ce.getProperties();
        String directoryPath = properties.get(LUCENE_INDEX_PATH);
        if (directoryPath == null || directoryPath.trim().length() == 0) {
            //TODO: replace naming with stanbol enhancer
            File dataFolder = bundleContext.getDataFile("enhancer-engines-autotagging");
            if (!dataFolder.isDirectory()) {
                FileUtils.deleteQuietly(dataFolder);
                dataFolder.mkdirs();
            }
            directoryPath = ModelIndexer.buildDefaultIndex(dataFolder, false).getAbsolutePath();
        }
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            throw new IOException("Lucene index directory not found: "
                    + directory.getAbsolutePath());
        }
        log.info("Loading the autotagging index from {}", directoryPath);
        autotagger = new Autotagger(FSDirectory.open(directory));
    }

    public void deactivate(ComponentContext ce) {
        autotagger = null;
    }
}
