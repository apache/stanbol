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
package org.apache.stanbol.ontologymanager.ontonet.api.registry.models;

import java.net.URL;



public abstract class AbstractRegistryItem implements RegistryItem {
	private URL url;
	private String name;
	private RegistryLibrary parent;

	public AbstractRegistryItem(String name) {
		setName(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#setURL(java.net.URL)
	 */
	public void setURL(URL url) {
		this.url = url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#getName()
	 */
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#getURL()
	 */
	public URL getURL() {
		return this.url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.stlab.xd.registry.models.RegistryItem#setParent(org.stlab.xd.registry
	 * .models.RegistryLibrary)
	 */
	public void setParent(RegistryLibrary parent) {
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#getParent()
	 */
	public RegistryLibrary getParent() {
		return parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#setName(java.lang.String)
	 */
	public void setName(String string) {
		this.name = string;
	}
}
