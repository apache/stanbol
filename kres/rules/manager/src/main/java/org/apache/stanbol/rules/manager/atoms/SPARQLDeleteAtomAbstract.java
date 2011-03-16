package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.KReSRuleAtom;

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
