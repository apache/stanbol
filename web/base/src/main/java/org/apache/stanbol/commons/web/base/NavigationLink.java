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

    public final String label;

    public final String path;

    public final String descriptionTemplate;

    public final int order;

    public NavigationLink(String path, String label, int order) {
        this.path = path;
        this.label = label;
        this.descriptionTemplate = null;
        this.order = order;
    }

    public NavigationLink(String path, String label, String descriptionTemplate, int order) {
        this.path = path;
        this.label = label;
        this.descriptionTemplate = descriptionTemplate;
        this.order = order;
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

    public String getDescriptionTemplate() {
        return descriptionTemplate;
    }

    public int getOrder() {
        return order;
    }

    public boolean getHasDescriptionTemplate() {
        return descriptionTemplate != null;
    }
    
}
