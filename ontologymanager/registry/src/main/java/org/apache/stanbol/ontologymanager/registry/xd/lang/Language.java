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
/**
 * 
 */
package org.apache.stanbol.ontologymanager.registry.xd.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Enrico Daga
 * 
 */
public enum Language {
    EN("en"), IT("it"), FR("fr"), DE("de"), ES("es");
    private String value = "";

    Language(String s) {
	this.value = s;
    }

    public String getValue() {
	return this.value;
    }

    @Override
    public String toString() {
	return this.value;
    }

    public String toValue() {
	return this.value;
    }

    public static String[] allValues() {
	List<String> str = new ArrayList<String>();
	for (Language l : Language.values()) {
	    str.add(l.getValue());
	}
	return str.toArray(new String[str.size()]);
    }

    public static Language getInstance(String xx) {
	return Language.valueOf(xx.toUpperCase());
    }

    public static Language getDefault() {
	return EN;
    }
    
}
