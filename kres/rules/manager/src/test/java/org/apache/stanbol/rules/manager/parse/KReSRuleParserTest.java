package org.apache.stanbol.rules.manager.parse;

import org.apache.stanbol.rules.base.api.KReSRule;
import org.apache.stanbol.rules.base.api.util.KReSRuleList;
import org.apache.stanbol.rules.manager.KReSKB;
import org.apache.stanbol.rules.manager.parse.KReSRuleParser;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * 
 * @author andrea.nuzzolese
 *
 */
public class KReSRuleParserTest {

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
			KReSKB kReSKB = KReSRuleParser.parse(kReSRule);
			if(kReSKB != null){
				KReSRuleList kReSRuleList = kReSKB.getkReSRuleList();
				if(kReSRuleList != null){
					for(KReSRule kReSRule : kReSRuleList){
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
