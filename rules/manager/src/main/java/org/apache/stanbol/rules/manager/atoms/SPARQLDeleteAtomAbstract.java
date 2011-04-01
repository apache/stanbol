package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.RuleAtom;

public abstract class SPARQLDeleteAtomAbstract implements RuleAtom {
	
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
