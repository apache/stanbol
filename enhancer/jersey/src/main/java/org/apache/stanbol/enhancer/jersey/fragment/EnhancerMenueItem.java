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

package org.apache.stanbol.enhancer.jersey.fragment;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

/**
 * The menue item for the Stanbol Enhancer component
 */
@Component
@Service(value=NavigationLink.class)
public class EnhancerMenueItem extends NavigationLink {

	private static final String htmlDescription = 
			"This is a <strong>stateless interface</strong> to allow clients to submit"+
			"content to <strong>analyze</strong> by the <code>EnhancementEngine</code>s"+
			"and get the resulting <strong>RDF enhancements</strong> at once without"+
			"storing anything on the server-side.";

	public EnhancerMenueItem() {
		super("enhancer", "/enhancer", htmlDescription, 10);
	}
	
}
