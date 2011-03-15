package org.apache.stanbol.rules.base.api.util;

import java.util.Iterator;

import org.apache.stanbol.rules.base.api.KReSRule;


public class KReSRuleIterator implements Iterator<KReSRule> {

	private int currentIndex;
	private int listSize;
	private KReSRule[] semionRules;
	
	public KReSRuleIterator(KReSRuleList semionRuleList) {
		this.listSize = semionRuleList.size();
		this.semionRules = new KReSRule[listSize];
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

	public KReSRule next() {
		KReSRule semionRule = semionRules[currentIndex];
		currentIndex++;
		return semionRule;
	}

	public void remove() {
		
	}

}
