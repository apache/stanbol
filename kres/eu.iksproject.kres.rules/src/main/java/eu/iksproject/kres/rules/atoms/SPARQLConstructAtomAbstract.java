package eu.iksproject.kres.rules.atoms;

import org.apache.stanbol.rules.base.api.KReSRuleAtom;

public abstract class SPARQLConstructAtomAbstract implements KReSRuleAtom {

	@Override
	public boolean isSPARQLConstruct() {
		return true;
	}
	
	@Override
	public boolean isSPARQLDelete() {
		return false;
	}
	
	@Override
	public boolean isSPARQLDeleteData() {
		return false;
	}
}
