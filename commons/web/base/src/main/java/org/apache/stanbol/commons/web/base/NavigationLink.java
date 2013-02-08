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
package org.apache.stanbol.commons.web.base;

/**
 * Data transfer object to define an entry in the main navigation menu.
 */
public class NavigationLink implements Comparable<NavigationLink> {

    private final String label;

    private final String path;

    private final int order;

	private final String htmlDescription;

    public NavigationLink(String path, String label, int order) {
        this.path = path;
        this.label = label;
        this.order = order;
        htmlDescription = null;
    }

    public NavigationLink(String path, String label, String htmlDescription, int order) {
    	this.path = path;
        this.label = label;
        this.order = order;
        this.htmlDescription = htmlDescription;
	}

	@Override
    public int compareTo(NavigationLink other) {
        return order - other.order;
    }

    public String getLabel() {
        return label;
    }

    public String getPath() {
        return path;
    }


    public int getOrder() {
        return order;
    }
    
    /**
     * @return An English language description of the linked resource 
     */
    public String getHtmlDescription() {
    	return htmlDescription;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NavigationLink other = (NavigationLink) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

    
}
