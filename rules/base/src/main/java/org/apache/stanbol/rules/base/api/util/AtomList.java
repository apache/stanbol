package org.apache.stanbol.rules.base.api.util;


import java.util.Collection;
import java.util.Iterator;

import org.apache.stanbol.rules.base.api.RuleAtom;


public class AtomList implements Collection<RuleAtom> {

	private RuleAtom[] kReSRuleAtoms;
	
	public AtomList() {
		
	}

	public boolean add(RuleAtom kReSRuleAtom) {
		if(kReSRuleAtoms == null){
			kReSRuleAtoms = new RuleAtom[1];
			kReSRuleAtoms[0] = kReSRuleAtom;
		}
		else{
			RuleAtom[] semionRulesCopy = new RuleAtom[kReSRuleAtoms.length+1];
			System.arraycopy(kReSRuleAtoms, 0, semionRulesCopy, 0, kReSRuleAtoms.length);
			semionRulesCopy[semionRulesCopy.length-1] = kReSRuleAtom;
			kReSRuleAtoms = semionRulesCopy;
		}
		return true;
	}
	
	public boolean addToHead(RuleAtom kReSRuleAtom) {
		if(kReSRuleAtoms == null){
			kReSRuleAtoms = new RuleAtom[1];
			kReSRuleAtoms[0] = kReSRuleAtom;
		}
		else{
			RuleAtom[] semionRulesCopy = new RuleAtom[kReSRuleAtoms.length+1];
			System.arraycopy(kReSRuleAtoms, 0, semionRulesCopy, 1, kReSRuleAtoms.length);
			semionRulesCopy[0] = kReSRuleAtom;
			kReSRuleAtoms = semionRulesCopy;
		}
		return true;
	}

	public boolean addAll(Collection<? extends RuleAtom> c) {
		
		RuleAtom[] collectionOfSemionRules = new RuleAtom[c.size()];
		collectionOfSemionRules = c.toArray(collectionOfSemionRules);
		
		if(kReSRuleAtoms == null){
			kReSRuleAtoms = collectionOfSemionRules;
		}
		else{
			RuleAtom[] semionRulesCopy = new RuleAtom[kReSRuleAtoms.length+collectionOfSemionRules.length];
			System.arraycopy(kReSRuleAtoms, 0, semionRulesCopy, 0, kReSRuleAtoms.length);
			System.arraycopy(collectionOfSemionRules, 0, semionRulesCopy, kReSRuleAtoms.length, collectionOfSemionRules.length);
			kReSRuleAtoms = semionRulesCopy;
		}
		return true;
	}

	public void clear() {
		// TODO Auto-generated method stub
		
	}

	public boolean contains(Object o) {
		for(RuleAtom semionRule : kReSRuleAtoms){
			if(semionRule.equals(o)){
				return true;
			}
		}
		return false;
	}

	public boolean containsAll(Collection<?> c) {
		
		for(Object o : c){
			for(RuleAtom semionRule : kReSRuleAtoms){
				if(!semionRule.equals(o)){
					return false;
				}
				else{
					break;
				}
			}
		}
		return true;
	}

	public boolean isEmpty() {
		if(kReSRuleAtoms == null || (kReSRuleAtoms.length == 1 && kReSRuleAtoms[0] == null)){
			return true;
		}
		else{
			return false;
		}
	}

	public Iterator<RuleAtom> iterator() {
		return new AtomIterator(this);
	}

	public boolean remove(Object o) {
		boolean removed = false;
		for(int i=0; i<kReSRuleAtoms.length && !removed; i++){
			RuleAtom semionRule = kReSRuleAtoms[i];
			if(semionRule.equals(o)){
				RuleAtom[] semionRulesCopy = new RuleAtom[kReSRuleAtoms.length-1];
				System.arraycopy(kReSRuleAtoms, 0, semionRulesCopy, 0, i);
				System.arraycopy(kReSRuleAtoms, i+1, semionRulesCopy, 0, semionRulesCopy.length-i);
				kReSRuleAtoms = semionRulesCopy;
				removed = true;
			}
		}
		return removed;
	}

	public boolean removeAll(Collection<?> c) {
		if(contains(c)){
			for(Object o : c){
				boolean removed = false;
				for(int i=0; i<kReSRuleAtoms.length && !removed; i++){
					RuleAtom semionRule = kReSRuleAtoms[i];
					if(semionRule.equals(o)){
						RuleAtom[] semionRulesCopy = new RuleAtom[kReSRuleAtoms.length-1];
						System.arraycopy(kReSRuleAtoms, 0, semionRulesCopy, 0, i);
						System.arraycopy(kReSRuleAtoms, i+1, semionRulesCopy, 0, semionRulesCopy.length-i);
						kReSRuleAtoms = semionRulesCopy;
						removed = true;
					}
				}
			}
			return true;
		}
		else{
			return false;
		}
	}

	public boolean retainAll(Collection<?> c) {
		RuleAtom[] semionRulesCopy = null;
		RuleAtom[] semionRulesTMP = null;
		for(Object o : c){
			if(o instanceof RuleAtom){
				if(contains(o)){
					if(semionRulesCopy == null){
						semionRulesCopy = new RuleAtom[1];
						semionRulesCopy[0] = (RuleAtom) o;
					}
					else{
						semionRulesTMP = new RuleAtom[semionRulesCopy.length+1];
						System.arraycopy(semionRulesCopy, 0, semionRulesTMP, 0, semionRulesCopy.length);
						semionRulesTMP[semionRulesTMP.length-1] = (RuleAtom) o;
						semionRulesCopy = semionRulesTMP;
					}
				}
			}
		}
		kReSRuleAtoms = semionRulesCopy;
		return true;
	}

	public int size() {
		return kReSRuleAtoms.length;
	}

	public Object[] toArray() {
		return kReSRuleAtoms;
	}

	public <T> T[] toArray(T[] a) {
		return (T[]) kReSRuleAtoms;
	}
	
}

