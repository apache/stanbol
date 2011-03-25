package org.apache.stanbol.rules.manager;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

public class RuleContainer {

	private Hashtable<String, String> semionRules;
	
	public RuleContainer() {
		semionRules = new Hashtable<String, String>();
	}
	
	public void addSemionRule(String ruleName, String rule){
		semionRules.put(ruleName, rule);
	}
	
	public String getSemionRule(String ruleName){
		return semionRules.get(ruleName);
	}
	
	public String removeSemionRule(String ruleName){
		return semionRules.remove(ruleName);
	}
	
	public Set<String> listSemionRuleNames(){
		return semionRules.keySet();
	}
	
	public Collection<String> listSemionRules(){
		return semionRules.values();
	}
	
	public int size(){
		return semionRules.size();
	}
	
}
