package org.apache.stanbol.rules.base.api.util;

import java.util.Iterator;

import org.apache.stanbol.rules.base.api.Rule;


public class RuleIterator implements Iterator<Rule> {

	private int currentIndex;
	private int listSize;
	private Rule[] semionRules;
	
	public RuleIterator(RuleList semionRuleList) {
		this.listSize = semionRuleList.size();
		this.semionRules = new Rule[listSize];
		this.semionRules = semionRuleList.toArray(this.semionRules);
		this.currentIndex = 0;
		
	}
	
	public boolean hasNext() {
		if(currentIndex<(listSize)){
			return true;
		}
		else{
			return false;
		}
	}

	public Rule next() {
		Rule semionRule = semionRules[currentIndex];
		currentIndex++;
		return semionRule;
	}

	public void remove() {
		
	}

}
