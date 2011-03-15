package eu.iksproject.kres.rules.atoms;

import org.apache.stanbol.rules.base.api.KReSRuleAtom;

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
