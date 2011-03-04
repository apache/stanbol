package eu.iksproject.kres.rules.atoms;

import eu.iksproject.kres.api.rules.KReSRuleAtom;

public abstract class SPARQLDeleteAtomAbstract implements KReSRuleAtom {
	
	@Override
	public boolean isSPARQLConstruct() {
		return false;
	}
	
	@Override
	public boolean isSPARQLDelete() {
		return true;
	}
	
	@Override
	public boolean isSPARQLDeleteData() {
		return false;
	}
}
