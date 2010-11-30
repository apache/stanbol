package eu.iksproject.kres.api.rules.util;

import java.util.Iterator;

import eu.iksproject.kres.api.rules.KReSRule;

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
