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
package org.apache.stanbol.enhancer.servicesapi.helper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.enhancer.servicesapi.Blob;

/**
 * A ContentItem retrieving its content and MediaType by dereferencing a given 
 * URI. After the content is loaded from the remote server it is cached 
 * {@link InMemoryBlob in-memory}.
 * 
 * After construction the <code>metadata</code> graph is empty.
 *
 */
/*
 * The current implementation keeps the content in memory after the first connection 
 * to the remote server. 
 */
public class WebContentItem extends ContentItemImpl {
	
	/**
	 * Creates an instance for a given URL and uses a {@link SimpleMGraph} to
	 * store metadata in memory.
	 * 
	 * @param url the dereferenceable URI
	 */
    public WebContentItem(URL url) {
        this(url,null);
    }
    /**
     * Creates an instance for a given URL and an existing {@link MGraph} to
     * store the metadata.
     * @param url the dereferenceable URI
     * @param metadata the {@link MGraph} to store the metadata
     */
	public WebContentItem(URL url, MGraph metadata) {
		super(new UriRef(url.toString()), new UrlBlob(url),
		    metadata == null ? new IndexedMGraph() : metadata);
	}
	
	/**
	 * Blob implementation that dereferences the parsed URL on the first
	 * access to the Blob. The downloaded content is stored within an
	 * {@link InMemoryBlob}
	 *
	 */
	private static class UrlBlob implements Blob {

	    private Blob dereferenced;
        private final URL url;
        protected UrlBlob(URL url){
            this.url = url;
        }
	    
        @Override
        public String getMimeType() {
            if(dereferenced == null){
                dereference();
            }
            return dereferenced.getMimeType();
        }

        @Override
        public InputStream getStream() {
            if(dereferenced == null){
                dereference();
            }
            return dereferenced.getStream();
        }

        @Override
        public Map<String,String> getParameter() {
            if(dereferenced == null){
                dereference();
            }
            return dereferenced.getParameter();
        }

        @Override
        public long getContentLength() {
            if(dereferenced == null){
                dereference();
            }
            return dereferenced.getContentLength();
        }
        
        private synchronized void dereference() {
            //checking again in the synchronized section
            if (dereferenced == null) {
                URLConnection uc;
                try {
                    uc = url.openConnection();
                    InputStream in = uc.getInputStream();
                    String mimeType = uc.getContentType();
                    if (mimeType == null) {
                        mimeType = "application/octet-stream";
                    }
                    dereferenced = new InMemoryBlob(in, mimeType);
                } catch (IOException e) {
                    throw new RuntimeException("Exception derefereing URI "+url, e);
                }
            }	 
        }
	}
}
