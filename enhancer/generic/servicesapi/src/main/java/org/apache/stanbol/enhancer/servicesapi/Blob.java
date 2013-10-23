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
package org.apache.stanbol.enhancer.servicesapi;

import java.io.InputStream;
import java.util.Map;

/**
 * represents a finite sequence of bytes associated to a mime-type
 */
public interface Blob {

	/**
	 * Getter for the mime-type of the content. The returned string MUST only
	 * contain the "{type}/{sub-type}". No wildcards MUST BE used.
	 * @return the mime-type of his blog
	 */
	String getMimeType();
	
	/**
	 * Getter for the data of this blob in form of an {@link InputStream}.
	 * Multiple calls need to return multiple instances of InputStreams
	 * @return a stream of the data of this blog
	 */
	InputStream getStream();
	/**
	 * Additional parameters parsed with the mime-type. Typically the 'charset'
	 * used to encode text is parsed as a parameter.
	 * @return read only map with additional parameter for the used mime-type.
	 */
    Map<String,String> getParameter();
	/**
	 * The size of the Content in bytes or a negative value if not known
	 */
    long getContentLength();
}
