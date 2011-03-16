package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.KReSRuleAtom;

public abstract class NumericFunctionAtom implements KReSRuleAtom {

	
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
		return false;
	}
}
