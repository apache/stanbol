package org.apache.stanbol.rules.base.api;

import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;

public interface JenaClauseEntry {

	public ClauseEntry getClauseEntry();
	
	public JenaVariableMap getJenaVariableMap();
	
}
