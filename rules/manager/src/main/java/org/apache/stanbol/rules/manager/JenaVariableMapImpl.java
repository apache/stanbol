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
