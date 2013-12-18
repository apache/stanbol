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
package org.apache.stanbol.commons.solr.impl;

import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_ANALYZER_FACTORY_NAME;

import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.stanbol.commons.solr.SolrServerAdapter;
import org.apache.stanbol.commons.solr.utils.AbstractAnalyzerFactoryActivator;
import org.apache.stanbol.commons.solr.utils.RegisteredSolrAnalyzerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Overrides the {@link SolrResourceLoader#findClass(String, Class, String...)}
 * method to look for {@link TokenFilterFactory}, {@link CharFilterFactory} and
 * {@link TokenizerFactory} registered as OSGI services.<p>
 * This is because Solr 4 uses SPI ("META-INF/services" files) to lookup
 * those factories and this does not work across bundles in OSGI.<p>
 * This {@link SolrResourceLoader} variant is intended to be used together
 * with Bundle-Activators based on the {@link AbstractAnalyzerFactoryActivator}.
 * <p> The {@link SolrServerAdapter} does use this class as {@link SolrResourceLoader}
 * when creating {@link SolrCore}s.
 * 
 * @author Rupert Westenthaler
 *
 */
public class OsgiSolrResourceLoader extends SolrResourceLoader {

    /*
     * static members form the parent implementation that are not visible to subclasses in a different package
     */
    static final String project = "solr";
    static final String base = "org.apache" + "." + project;
    private static final Pattern legacyAnalysisPattern = Pattern.compile("((\\Q" + base
            + ".analysis.\\E)|(\\Q" + project
            + ".\\E))([\\p{L}_$][\\p{L}\\p{N}_$]+?)(TokenFilter|Filter|Tokenizer|CharFilter)Factory");

    protected final BundleContext bc;

    public OsgiSolrResourceLoader(BundleContext bc, String instanceDir) {
        super(instanceDir, OsgiSolrResourceLoader.class.getClassLoader());
        this.bc = bc;
    }

    public OsgiSolrResourceLoader(BundleContext bc, String instanceDir, ClassLoader parent) {
        super(instanceDir, parent);
        this.bc = bc;
    }

    public OsgiSolrResourceLoader(BundleContext bc, String instanceDir, ClassLoader parent,
            Properties coreProperties) {
        super(instanceDir, parent, coreProperties);
        this.bc = bc;
    }

    @Override
    public <T> Class<? extends T> findClass(String cname, Class<T> expectedType, String... subpackages) {
        Class<? extends T> clazz = null;
        RuntimeException parentEx = null;
        try {
            clazz = super.findClass(cname, expectedType, subpackages);
        } catch (RuntimeException e) {
            parentEx = e;
        }
        if (clazz != null) {
            return clazz;
        } else {
            try {
                //try to load via the OSGI service factory
                return OsgiResourceLoaderUtil.findOsgiClass(bc, cname, expectedType, subpackages);
            } catch (SolrException e) {
                //prefer to throw the first exception
                if(parentEx != null){
                    throw parentEx;
                } else {
                    throw e;
                }
            }
        }
    }

}
