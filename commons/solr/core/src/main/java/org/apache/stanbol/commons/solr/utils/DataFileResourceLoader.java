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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;

/**
 * SolrResourceLoader that supports loading resources via the Apache Stanbol
 * {@link DataFileProvider}<p>
 * This does NOT implement the {@link #newInstance(String, Class)} method.
 * Calls will throw an {@link UnsupportedOperationException}.
 * Users that need to also load classes should combine this implementation with
 * the {@link StanbolResourceLoader} that supports instantiation of classes via
 * the parsed ClassLoader.
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,policy=ConfigurationPolicy.OPTIONAL)
@Service(value={ResourceLoader.class, DataFileResourceLoader.class})
public class DataFileResourceLoader implements ResourceLoader {

    @Reference
    private DataFileProvider dfp;
    
    /**
     * Default constructure used by OSGI
     */
    public DataFileResourceLoader(){}
    
    /**
     * Constructs a {@link DataFileResourceLoader} using the parsed 
     * {@link DataFileProvider}.
     * @param dfp the {@link DataFileProvider}
     * @throws IllegalArgumentException if the parsed {@link DataFileProvider} is
     * <code>null</code>
     */
    public DataFileResourceLoader(DataFileProvider dfp){
        if(dfp == null){
            throw new IllegalArgumentException("The parsed DataFileProvider MUST NOT be NULL!");
        }
        this.dfp = dfp;
    }
    
    @Override
    public InputStream openResource(String resource) throws IOException {
        return dfp.getInputStream(null, resource, null);
    }

    public List<String> getLines(String resource) throws IOException {
        List<String> lines = new ArrayList<String>();
        LineIterator it = IOUtils.lineIterator(openResource(resource), "UTF-8");
        while(it.hasNext()){
            String line = it.nextLine();
            if(line != null && !line.isEmpty() && line.charAt(0) != '#'){
                lines.add(line);
            }
        }
        return lines;
    }
    /**
     * Not implemented!
     * @throws UnsupportedOperationException on every call to this mehtod
     * @see StanbolResourceLoader#newInstance(String, Class)
     */
    @Override
    public <T> T newInstance(String cname, Class<T> expectedType) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Loading of ClassFiles is not supported");
    }

    @Override
    public <T> Class<? extends T> findClass(String cname, Class<T> expectedType) {
        throw new UnsupportedOperationException("Loading of ClassFiles is not supported");
    }

}
