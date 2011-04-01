package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.RuleAtom;

public abstract class SPARQLConstructAtomAbstract implements RuleAtom {

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
