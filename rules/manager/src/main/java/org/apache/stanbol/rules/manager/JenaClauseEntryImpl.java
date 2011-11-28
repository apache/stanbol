package org.apache.stanbol.rules.manager;

import org.apache.stanbol.rules.base.api.JenaClauseEntry;
import org.apache.stanbol.rules.base.api.JenaVariableMap;

import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;

public class JenaClauseEntryImpl implements JenaClauseEntry {

	private ClauseEntry clauseEntry;
	private JenaVariableMap variableMap;
	
	public JenaClauseEntryImpl(ClauseEntry clauseEntry, JenaVariableMap variableMap) {
		this.clauseEntry = clauseEntry;
		this.variableMap = variableMap;
	}

	@Override
	public ClauseEntry getClauseEntry() {
		return clauseEntry;
	}

	@Override
	public JenaVariableMap getJenaVariableMap() {
		return variableMap;
	}
	
}
