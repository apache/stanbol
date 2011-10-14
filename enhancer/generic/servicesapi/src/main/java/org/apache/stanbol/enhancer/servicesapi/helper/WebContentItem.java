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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

/**
 * A ContentItem retrieving its content and MediaType by dereferencing a given URI.
 * 
 * After construction the <code>metadata</code> graph is empty.
 *
 */
/*
 * The current implementation keeps the content in memory after the firts connection 
 * to the remote server. 
 */
public class WebContentItem implements ContentItem {
	
	private final MGraph metadata = new SimpleMGraph();
	private final URL url;
	private boolean dereferenced = false;
	private byte[] data;
	private String mimeType;
	
	/**
	 * Creates an instance for a given URL
	 * 
	 * @param url the dereferenceable URI
	 */
	public WebContentItem(URL url) {
		this.url = url;
	}

	@Override
	public String getId() {
		return url.toString();
	}

	@Override
	public InputStream getStream() {
		if (!dereferenced) {
			dereference();
		}
		return new ByteArrayInputStream(data);
	}

	@Override
	public String getMimeType() {
		if (!dereferenced) {
			dereference();
		}
		return mimeType;
	}

	@Override
	public MGraph getMetadata() {
		return metadata;
	}
	
	private synchronized void dereference() {
		//checking again in the synchronized section
		if (!dereferenced) {
			URLConnection uc;
			try {
				uc = url.openConnection();
				data = IOUtils.toByteArray(uc.getInputStream());
	            mimeType = uc.getContentType();
	            if (mimeType == null) {
	                mimeType = "application/octet-stream";
	            } else {
	                // Keep only first part of content-types like text/plain ; charset=UTF-8
	                mimeType = mimeType.split(";")[0].trim();
	            }
	            dereferenced = true;
			} catch (IOException e) {
				throw new RuntimeException("Exception derefereing URI "+url, e);
			}
		}
	}
}
