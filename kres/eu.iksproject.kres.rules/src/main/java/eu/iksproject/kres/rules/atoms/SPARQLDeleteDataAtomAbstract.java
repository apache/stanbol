package eu.iksproject.kres.rules.atoms;

import eu.iksproject.kres.api.rules.KReSRuleAtom;

public abstract class SPARQLDeleteDataAtomAbstract implements KReSRuleAtom {

	
	@Override
	public boolean isSPARQLConstruct() {
		return false;
	}
	
	@Override
	public boolean isSPARQLDelete() {
		return false;
	}
	
	@Override
	public boolean isSPARQLDeleteData() {
		return true;
	}
}
