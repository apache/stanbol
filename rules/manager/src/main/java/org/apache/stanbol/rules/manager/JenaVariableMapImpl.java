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
package org.apache.stanbol.rules.manager;

import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.rules.base.api.JenaVariableMap;

public class JenaVariableMapImpl implements JenaVariableMap {

	private Map<String, Integer> variableMap;
	
	public JenaVariableMapImpl() {
		variableMap = new HashMap<String, Integer>();
	}
	
	public JenaVariableMapImpl(Map<String, Integer> variableMap) {
		this.variableMap = variableMap;
	}
	
	public int getVariableIndex(String ruleVariable){
		Integer index = variableMap.get(ruleVariable);
		if(index == null){
			index = variableMap.size();
			
			variableMap.put(ruleVariable, index);
			
			
		}
		
		return index;
	}
	
}
