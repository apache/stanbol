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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.common.SolrException;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.utils.RegisteredSolrAnalyzerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OsgiResourceLoaderUtil {

   /**
    * Restrict instantiation
    */
    private OsgiResourceLoaderUtil() {}

   /*
        * static members form the SolrResourceLoader that are not visible to this
        * class
        */
    static final String project = "solr";
    static final String base = "org.apache" + "." + project;
    private static final Pattern legacyAnalysisPattern = Pattern.compile("((\\Q" + base
            + ".analysis.\\E)|(\\Q" + project
            + ".\\E))([\\p{L}_$][\\p{L}\\p{N}_$]+?)(TokenFilter|Filter|Tokenizer|CharFilter)Factory");

    private static Logger log = LoggerFactory.getLogger(OsgiResourceLoaderUtil.class);
    
    /**
     * Finds classes by using {@link RegisteredSolrAnalyzerFactory} with a 
     * filter over {@link SolrConstants#PROPERTY_ANALYZER_FACTORY_NAME}.
     * @param <T> This is the type parameter
     * @param bc the {@link BundleContext} used for the search
     * @param cname the cname as parsed to {@link SolrResourceLoader#findClass(String, Class, String...)}
     * @param expectedType the expected type as parsed to {@link SolrResourceLoader#findClass(String, Class, String...)}
     * @param subpackages the subpackages as parsed to {@link SolrResourceLoader#findClass(String, Class, String...)}
     * @return the class
     */
    public static  <T> Class<? extends T> findOsgiClass(BundleContext bc, String cname, Class<T> expectedType, String... subpackages){
        Class<? extends T> clazz = null;
        RuntimeException parentEx = null;
        final Matcher m = legacyAnalysisPattern.matcher(cname);
        if (m.matches()) {
            final String name = m.group(4);
            log.trace("Trying to load class from analysis SPI using name='{}'", name);
            ServiceReference[] referenced;
            String filter;
            try {
                filter = String.format("(%s=%s)", PROPERTY_ANALYZER_FACTORY_NAME, name.toLowerCase(Locale.ROOT));
                referenced = bc.getServiceReferences(RegisteredSolrAnalyzerFactory.class.getName(), filter);
            } catch (InvalidSyntaxException e) {
                throw new IllegalStateException("Unable to create Filter for Service with name '" + name
                        + "'!", e);
            }
            if (referenced != null && referenced.length > 0) {
                Object service = bc.getService(referenced[0]);
                if (service instanceof RegisteredSolrAnalyzerFactory) {
                    //TODO: we could check the type here
                    clazz = ((RegisteredSolrAnalyzerFactory)service).getFactoryClass();
                    //we do not use a service so immediately unget it
                    bc.ungetService(referenced[0]);
                    return clazz;
                }
            } else {
                parentEx = new SolrException(SolrException.ErrorCode.SERVER_ERROR, 
                    "Error loading Class '" + cname + "' via OSGI service Registry by using filter '"
                    + filter + "'!", parentEx);
            }
        }
        if(parentEx != null) {
            throw parentEx;
        } else {
            throw new SolrException( SolrException.ErrorCode.SERVER_ERROR, "Error loading class '" + cname + "'");
        }
    }
    
}
