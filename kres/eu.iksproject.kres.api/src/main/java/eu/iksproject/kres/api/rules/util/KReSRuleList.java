package eu.iksproject.kres.api.rules.util;

import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.rules.KReSRule;

public class KReSRuleList implements Collection<KReSRule> {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private KReSRule[] kReSRules;

	public KReSRuleList() {

	}

	public boolean add(KReSRule semionRule) {
		if (kReSRules == null) {
			kReSRules = new KReSRule[1];
			kReSRules[0] = semionRule;
		} else {
			KReSRule[] semionRulesCopy = new KReSRule[kReSRules.length + 1];
			System
					.arraycopy(kReSRules, 0, semionRulesCopy, 0,
							kReSRules.length);
			semionRulesCopy[semionRulesCopy.length - 1] = semionRule;
			kReSRules = semionRulesCopy;
		}
		log.debug("Added rule " + semionRule, this);
		return true;
	}

	public boolean addAll(Collection<? extends KReSRule> c) {

		KReSRule[] collectionOfSemionRules = new KReSRule[c.size()];
		collectionOfSemionRules = c.toArray(collectionOfSemionRules);

		if (kReSRules == null) {
			kReSRules = collectionOfSemionRules;
		} else {
			KReSRule[] semionRulesCopy = new KReSRule[kReSRules.length
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
		for (KReSRule semionRule : kReSRules) {
			if (semionRule.equals(o)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsAll(Collection<?> c) {

		for (Object o : c) {
			for (KReSRule semionRule : kReSRules) {
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

	public Iterator<KReSRule> iterator() {
		return new KReSRuleIterator(this);
	}

	public boolean remove(Object o) {
		boolean removed = false;
		for (int i = 0; i < kReSRules.length && !removed; i++) {
			KReSRule semionRule = kReSRules[i];
			if (semionRule.equals(o)) {
				KReSRule[] semionRulesCopy = new KReSRule[kReSRules.length - 1];
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
					KReSRule semionRule = kReSRules[i];
					if (semionRule.equals(o)) {
						KReSRule[] semionRulesCopy = new KReSRule[kReSRules.length - 1];
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
		KReSRule[] semionRulesCopy = null;
		KReSRule[] semionRulesTMP = null;
		for (Object o : c) {
			if (o instanceof KReSRule) {
				if (contains(o)) {
					if (semionRulesCopy == null) {
						semionRulesCopy = new KReSRule[1];
						semionRulesCopy[0] = (KReSRule) o;
					} else {
						semionRulesTMP = new KReSRule[semionRulesCopy.length + 1];
						System.arraycopy(semionRulesCopy, 0, semionRulesTMP, 0,
								semionRulesCopy.length);
						semionRulesTMP[semionRulesTMP.length - 1] = (KReSRule) o;
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
