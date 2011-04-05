package org.apache.stanbol.rules.base.api.util;


import java.util.Collection;
import java.util.Iterator;

import org.apache.stanbol.rules.base.api.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RuleList implements Collection<Rule> {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private Rule[] kReSRules;

	public RuleList() {

	}

	
	public boolean add(Rule semionRule) {
		if (kReSRules == null) {
			kReSRules = new Rule[1];
			kReSRules[0] = semionRule;
		} else {
			Rule[] semionRulesCopy = new Rule[kReSRules.length + 1];
			System
					.arraycopy(kReSRules, 0, semionRulesCopy, 0,
							kReSRules.length);
			semionRulesCopy[semionRulesCopy.length - 1] = semionRule;
			kReSRules = semionRulesCopy;
		}
		log.debug("Added rule " + semionRule, this);
		System.out.println("Added rule " + semionRule);
		return true;
	}
	
	public boolean addToHead(Rule semionRule) {
		if (kReSRules == null) {
			kReSRules = new Rule[1];
			kReSRules[0] = semionRule;
		} else {
			Rule[] semionRulesCopy = new Rule[kReSRules.length + 1];
			System
					.arraycopy(kReSRules, 0, semionRulesCopy, 1,
							kReSRules.length);
			semionRulesCopy[0] = semionRule;
			kReSRules = semionRulesCopy;
		}
		log.debug("Added to HEAD rule " + semionRule, this);
		System.out.println("Added to HEAD rule " + semionRule);
		return true;
	}

	public boolean addAll(Collection<? extends Rule> c) {

		Rule[] collectionOfSemionRules = new Rule[c.size()];
		collectionOfSemionRules = c.toArray(collectionOfSemionRules);

		if (kReSRules == null) {
			kReSRules = collectionOfSemionRules;
		} else {
			Rule[] semionRulesCopy = new Rule[kReSRules.length
					+ collectionOfSemionRules.length];
			System
					.arraycopy(kReSRules, 0, semionRulesCopy, 0,
							kReSRules.length);
			System.arraycopy(collectionOfSemionRules, 0, semionRulesCopy,
					kReSRules.length, collectionOfSemionRules.length);
			kReSRules = semionRulesCopy;
		}

		log.debug("Added all rules : " + c, this);
		return true;
	}

	/**
	 * To clear the collection
	 */
	public void clear() {
		this.kReSRules = null;
	}

	public boolean contains(Object o) {
		for (Rule semionRule : kReSRules) {
			if (semionRule.equals(o)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsAll(Collection<?> c) {

		for (Object o : c) {
			for (Rule semionRule : kReSRules) {
				if (!semionRule.equals(o)) {
					return false;
				} else {
					break;
				}
			}
		}
		return true;
	}

	public boolean isEmpty() {
		if (kReSRules == null
				|| (kReSRules.length == 1 && kReSRules[0] == null)) {
			return true;
		} else {
			return false;
		}
	}

	public Iterator<Rule> iterator() {
		return new RuleIterator(this);
	}

	public boolean remove(Object o) {
		boolean removed = false;
		for (int i = 0; i < kReSRules.length && !removed; i++) {
			Rule semionRule = kReSRules[i];
			if (semionRule.equals(o)) {
				Rule[] semionRulesCopy = new Rule[kReSRules.length - 1];
				System.arraycopy(kReSRules, 0, semionRulesCopy, 0, i);
				System.arraycopy(kReSRules, i + 1, semionRulesCopy, 0,
						semionRulesCopy.length - i);
				kReSRules = semionRulesCopy;
				removed = true;
			}
		}
		return removed;
	}

	public boolean removeAll(Collection<?> c) {
		if (contains(c)) {
			for (Object o : c) {
				boolean removed = false;
				for (int i = 0; i < kReSRules.length && !removed; i++) {
					Rule semionRule = kReSRules[i];
					if (semionRule.equals(o)) {
						Rule[] semionRulesCopy = new Rule[kReSRules.length - 1];
						System.arraycopy(kReSRules, 0, semionRulesCopy, 0, i);
						System.arraycopy(kReSRules, i + 1, semionRulesCopy, 0,
								semionRulesCopy.length - i);
						kReSRules = semionRulesCopy;
						removed = true;
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean retainAll(Collection<?> c) {
		Rule[] semionRulesCopy = null;
		Rule[] semionRulesTMP = null;
		for (Object o : c) {
			if (o instanceof Rule) {
				if (contains(o)) {
					if (semionRulesCopy == null) {
						semionRulesCopy = new Rule[1];
						semionRulesCopy[0] = (Rule) o;
					} else {
						semionRulesTMP = new Rule[semionRulesCopy.length + 1];
						System.arraycopy(semionRulesCopy, 0, semionRulesTMP, 0,
								semionRulesCopy.length);
						semionRulesTMP[semionRulesTMP.length - 1] = (Rule) o;
						semionRulesCopy = semionRulesTMP;
					}
				}
			}
		}
		kReSRules = semionRulesCopy;
		return true;
	}

	public int size() {
		return kReSRules.length;
	}

	public Object[] toArray() {
		return kReSRules;
	}

	public <T> T[] toArray(T[] a) {
		return (T[]) kReSRules;
	}

}
