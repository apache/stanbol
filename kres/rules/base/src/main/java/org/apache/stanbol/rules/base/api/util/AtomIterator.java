package org.apache.stanbol.rules.base.api.util;

import java.util.Iterator;

import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleAtom;


public class AtomIterator implements Iterator<RuleAtom>{
	
	private int currentIndex;
	private int listSize;
	private RuleAtom[] kReSRuleAtoms;
	
	public AtomIterator(AtomList atomList) {
		this.listSize = atomList.size();
		this.kReSRuleAtoms = new RuleAtom[listSize];
		this.kReSRuleAtoms = atomList.toArray(this.kReSRuleAtoms);
		this.currentIndex = 0;
	}

	public boolean hasNext() {
		if(currentIndex<listSize){
			return true;
		}
		else{
			return false;
		}
	}

	public RuleAtom next() {
		RuleAtom atom = kReSRuleAtoms[currentIndex];
		currentIndex++;
		return atom;
	}

	public void remove() {
		
	}

}
