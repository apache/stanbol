package org.apache.stanbol.rules.base.api.util;


import java.util.Collection;
import java.util.Iterator;

import org.apache.stanbol.rules.base.api.KReSRuleAtom;
import org.apache.stanbol.rules.base.api.util.AtomIterator;


public class AtomList implements Collection<KReSRuleAtom> {

	private KReSRuleAtom[] kReSRuleAtoms;
	
	public AtomList() {
		
	}

	public boolean add(KReSRuleAtom kReSRuleAtom) {
		if(kReSRuleAtoms == null){
			kReSRuleAtoms = new KReSRuleAtom[1];
			kReSRuleAtoms[0] = kReSRuleAtom;
		}
		else{
			KReSRuleAtom[] semionRulesCopy = new KReSRuleAtom[kReSRuleAtoms.length+1];
			System.arraycopy(kReSRuleAtoms, 0, semionRulesCopy, 0, kReSRuleAtoms.length);
			semionRulesCopy[semionRulesCopy.length-1] = kReSRuleAtom;
			kReSRuleAtoms = semionRulesCopy;
		}
		return true;
	}
	
	public boolean addToHead(KReSRuleAtom kReSRuleAtom) {
		if(kReSRuleAtoms == null){
			kReSRuleAtoms = new KReSRuleAtom[1];
			kReSRuleAtoms[0] = kReSRuleAtom;
		}
		else{
			KReSRuleAtom[] semionRulesCopy = new KReSRuleAtom[kReSRuleAtoms.length+1];
			System.arraycopy(kReSRuleAtoms, 0, semionRulesCopy, 1, kReSRuleAtoms.length);
			semionRulesCopy[0] = kReSRuleAtom;
			kReSRuleAtoms = semionRulesCopy;
		}
		return true;
	}

	public boolean addAll(Collection<? extends KReSRuleAtom> c) {
		
		KReSRuleAtom[] collectionOfSemionRules = new KReSRuleAtom[c.size()];
		collectionOfSemionRules = c.toArray(collectionOfSemionRules);
		
		if(kReSRuleAtoms == null){
			kReSRuleAtoms = collectionOfSemionRules;
		}
		else{
			KReSRuleAtom[] semionRulesCopy = new KReSRuleAtom[kReSRuleAtoms.length+collectionOfSemionRules.length];
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
		for(KReSRuleAtom semionRule : kReSRuleAtoms){
			if(semionRule.equals(o)){
				return true;
			}
		}
		return false;
	}

	public boolean containsAll(Collection<?> c) {
		
		for(Object o : c){
			for(KReSRuleAtom semionRule : kReSRuleAtoms){
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

	public Iterator<KReSRuleAtom> iterator() {
		return new AtomIterator(this);
	}

	public boolean remove(Object o) {
		boolean removed = false;
		for(int i=0; i<kReSRuleAtoms.length && !removed; i++){
			KReSRuleAtom semionRule = kReSRuleAtoms[i];
			if(semionRule.equals(o)){
				KReSRuleAtom[] semionRulesCopy = new KReSRuleAtom[kReSRuleAtoms.length-1];
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
					KReSRuleAtom semionRule = kReSRuleAtoms[i];
					if(semionRule.equals(o)){
						KReSRuleAtom[] semionRulesCopy = new KReSRuleAtom[kReSRuleAtoms.length-1];
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
		KReSRuleAtom[] semionRulesCopy = null;
		KReSRuleAtom[] semionRulesTMP = null;
		for(Object o : c){
			if(o instanceof KReSRuleAtom){
				if(contains(o)){
					if(semionRulesCopy == null){
						semionRulesCopy = new KReSRuleAtom[1];
						semionRulesCopy[0] = (KReSRuleAtom) o;
					}
					else{
						semionRulesTMP = new KReSRuleAtom[semionRulesCopy.length+1];
						System.arraycopy(semionRulesCopy, 0, semionRulesTMP, 0, semionRulesCopy.length);
						semionRulesTMP[semionRulesTMP.length-1] = (KReSRuleAtom) o;
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

