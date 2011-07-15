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


import java.util.ArrayList;


public class RegistryLibrary extends AbstractRegistryItem {
	
	private ArrayList<AbstractRegistryItem> children;

	public RegistryLibrary(String name) {
		super(name);
		children = new ArrayList<AbstractRegistryItem>();
	}

	public void addChild(AbstractRegistryItem child) {
		children.add(child);
		child.setParent(this);
	}

	public void removeChild(RegistryItem child) {
		children.remove(child);
		child.setParent(null);
	}

	public void removeChildren(){
		children = new ArrayList<AbstractRegistryItem>();
	}
	public RegistryItem[] getChildren() {
		return children
				.toArray(new AbstractRegistryItem[children.size()]);
	}

	public boolean hasChildren() {
		return children.size() > 0;
	}

	@Override
	public boolean isLibrary() {
		return true;
	}

	@Override
	public boolean isOntology() {
		return false;
	}

}
