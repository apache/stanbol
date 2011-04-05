package org.apache.stanbol.rules.manager.parse;

import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.KB;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * 
 * @author andrea.nuzzolese
 *
 */
public class RuleParserTest {

	private static String kReSRule;
	
	@BeforeClass
	public static void setup() {
		kReSRule = "ProvaParent = <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#> . " +
		"rule1[ has(ProvaParent:hasParent, ?x, ?y) . has(ProvaParent:hasBrother, ?y, ?z) -> " +
		"has(ProvaParent:hasUncle, ?x, ?z) ]";
	}
	
	@Test
	public void testParser(){
		try{
			KB kReSKB = RuleParserImpl.parse(kReSRule);
			if(kReSKB != null){
				RuleList kReSRuleList = kReSKB.getkReSRuleList();
				if(kReSRuleList != null){
					for(Rule kReSRule : kReSRuleList){
						System.out.println("RULE : "+kReSRule.toString());
					}
				}
				System.out.println("RULE LIST IS NULL");
			}
			else{
				System.out.println("KB IS NULL");
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
