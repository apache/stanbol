package org.apache.stanbol.rules.manager.atoms;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.stanbol.rules.base.api.RuleAtom;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.vocabulary.XSD;


public class GreaterThanAtomTest extends AtomTest {

	private Object variable1;
	private Object variable2;
	
	private Object literal1;
	private Object literal2;
	
	
	private Object typedLiteral1;
	private Object typedLiteral2;
	
	@Before
	public void setup() {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		this.factory = manager.getOWLDataFactory();
		
		variable1 = new VariableAtom(URI.create("http://kres.iks-project.eu/ontology/meta/variables#x"), false);
		variable2 = new VariableAtom(URI.create("http://kres.iks-project.eu/ontology/meta/variables#y"), false);
		
		
		literal1 = "some text";
		literal2 = "some other text";
		
		try {
			typedLiteral1 = new TypedLiteralAtom(3.0, new ResourceAtom(new URI(XSD.xdouble.getURI())));
			typedLiteral2 = new TypedLiteralAtom(5.0, new ResourceAtom(new URI(XSD.xdouble.getURI())));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testValidAtomWithVariableArguments() {
		
		RuleAtom ruleAtom = new GreaterThanAtom(variable1, variable2);
		
		execTest(ruleAtom);
		
	}
	
	
	@Test
	public void testValidAtomWithLiteralArguments() {
		
		RuleAtom ruleAtom = new GreaterThanAtom(literal1, literal2);
		
		execTest(ruleAtom);
	}
	
	
	@Test
	public void testValidAtomWithTypedLiteralArguments() {
		
		RuleAtom ruleAtom = new GreaterThanAtom(typedLiteral1, typedLiteral2);
		
		execTest(ruleAtom);
	}
	
	
}
