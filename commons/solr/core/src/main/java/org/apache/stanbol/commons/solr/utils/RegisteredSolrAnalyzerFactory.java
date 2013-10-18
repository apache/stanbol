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
package org.apache.stanbol.commons.solr.utils;

import java.util.ServiceLoader;

import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.stanbol.commons.solr.SolrServerAdapter;

/**
 * Helper Interface used to register Solr {@link CharFilterFactory},
 * {@link TokenizerFactory} and {@link TokenFilterFactory} implementations as
 * OSGI services. This is required as the {@link SolrResourceLoader} does not
 * work within OSGI because it uses functionality similar to
 * {@link ServiceLoader} to locate and load implementations.<p>
 * Registration is implemented by the {@link AbstractAnalyzerFactoryActivator}
 * sub-classes contained in all o.a.stanbol.commons.solr.* modules that do
 * provide Solr Analyzers. Registered services are consumed by the
 * OsgiSolrResourceLoader. NOTE that the {@link SolrServerAdapter} replaces the
 * default {@link SolrResourceLoader} with the OSGI one.
 * @author Rupert Westenthaler
 *
 */
public class RegisteredSolrAnalyzerFactory<T extends AbstractAnalysisFactory> {

    private final String name;
    private final Class<T> factoryType;
    private final Class<? extends T> factoryImpl;
 
    public RegisteredSolrAnalyzerFactory(String name, Class<T> factoryType, Class<? extends T> factoryImpl) {
        this.name = name;
        this.factoryType = factoryType;
        this.factoryImpl = factoryImpl;
    }

    public String getName() {
        return name;
    }
    
    public Class<T> getFactoryType(){
        return factoryType;
    }
    
    public Class<? extends T> getFactoryClass() {
        return factoryImpl;
    }
    
}
