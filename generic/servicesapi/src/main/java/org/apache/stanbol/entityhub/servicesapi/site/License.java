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
package org.apache.stanbol.entityhub.servicesapi.site;

public final class License {
    
    /**
     * Constructs an License. At least one of the three parameters MUST NOT be
     * <code>null</code> otherwise an {@link IllegalArgumentException} is 
     * thrown
     * @param name the name of the license
     * @param url the link to the page about the license
     * @param text the natural language text defining the license
     * @throws IllegalArgumentException if all three parameters are 
     * <code>null</code> or empty strings
     */
    public License(String name,String url,String text) throws IllegalArgumentException {
        super();
        if((name == null || name.isEmpty()) && 
                (url == null || url.isEmpty()) &&
                (text == null || text.isEmpty())){
            throw new IllegalArgumentException("One of name, url and text MUST NOT be NULL nor emtpy");
        }
        this.name = name;
        this.url = url;
        this.text = text;
    }
    
    private final String name;
    private final String url;
    private final String text;
    
    /**
     * Getter for the name of the License
     * @return
     */
    public final String getName() {
        return name;
    }
    /**
     * Getter for the Url of the License
     * @return
     */
    public final String getUrl() {
        return url;
    }
    /**
     * Getter for the text of the License
     * @return
     */
    public final String getText() {
        return text;
    }
    
    
    

}
