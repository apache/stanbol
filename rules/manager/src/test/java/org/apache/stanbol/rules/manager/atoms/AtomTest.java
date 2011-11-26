package org.apache.stanbol.rules.manager.atoms;

import static org.junit.Assert.fail;

import org.apache.stanbol.rules.base.api.RuleAtom;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SWRLAtom;

public abstract class AtomTest {
	
	
	protected OWLDataFactory factory;
	
	@Before
	public void init(){
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
	}

	@Test
	public abstract void testValidAtomWithVariableArguments();
	
	
	@Test
	public abstract void testValidAtomWithLiteralArguments();
	
	
	@Test
	public abstract void testValidAtomWithTypedLiteralArguments();
	
	protected void execTest(RuleAtom ruleAtom){
		String stanbolSyntax = ruleAtom.toKReSSyntax();
		if(stanbolSyntax == null){
			fail(GreaterThanAtom.class.getCanonicalName() + " does not produce any rule in Stanbo syntax.");
		}
		
		String sparql = ruleAtom.toSPARQL().getObject();		
		if(sparql == null){
			fail(GreaterThanAtom.class.getCanonicalName() + " does not produce any rule as SPARQL CONSTRUCT.");
		}
		
		SWRLAtom swrlAtom = ruleAtom.toSWRL(factory);
		
		if(swrlAtom == null){
			fail(GreaterThanAtom.class.getCanonicalName() + " does not produce any rule in SWRL.");
		}
	}
	
}
