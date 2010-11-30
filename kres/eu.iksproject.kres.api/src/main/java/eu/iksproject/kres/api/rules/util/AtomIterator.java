package eu.iksproject.kres.api.rules.util;

import java.util.Iterator;

import eu.iksproject.kres.api.rules.KReSRule;
import eu.iksproject.kres.api.rules.KReSRuleAtom;

public class AtomIterator implements Iterator<KReSRuleAtom>{
	
	private int currentIndex;
	private int listSize;
	private KReSRuleAtom[] kReSRuleAtoms;
	
	public AtomIterator(AtomList atomList) {
		this.listSize = atomList.size();
		this.kReSRuleAtoms = new KReSRuleAtom[listSize];
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

	public KReSRuleAtom next() {
		KReSRuleAtom atom = kReSRuleAtoms[currentIndex];
		currentIndex++;
		return atom;
	}

	public void remove() {
		
	}

}
