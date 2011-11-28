/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.rules.manager.changes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.semanticweb.owlapi.model.IRI;

import com.hp.hpl.jena.rdf.model.Model;


/**
 * The RecipeImpl is a concrete implementation of the Recipe interface.
 * A Recipe is a collection identified by an URI of rules. Each rules of the recipe is also identified by an URI.
 * Rules are expressed both in SWRL and in KReS rules syntax.
 * 
 * @author andrea.nuzzolese
 *
 */

public class RecipeImpl extends Observable implements Recipe {

	
	
	
	private IRI recipeID;
	private String recipeDescription;
	private RuleList kReSRuleList;
	
	
	
	
	/**
	 * 
	 * Create a new {@code RecipeImpl} from a set of rule expressed in KReS rule syntax.
	 * 
	 * 
	 * @param recipeID
	 * @param recipeDescription
	 * @param kReSRuleList
	 */
	public RecipeImpl(IRI recipeID, String recipeDescription, RuleList kReSRuleList) {
		this.recipeID = recipeID;
		this.recipeDescription = recipeDescription;
		this.kReSRuleList = kReSRuleList;
	}
	
	
	public RuleList getkReSRuleList() {
		return kReSRuleList;
	}
	
	public IRI getRecipeID() {
		return recipeID;
	}
	
	public String getRecipeDescription() {
		return recipeDescription;
	}
	
	public Model getRecipeAsRDFModel() {
		
		return null;
	}

	public Rule getRule(String ruleURI) {
		//return new SWRLToKReSRule(ruleModel).parse(ruleURI);
		return null;
	}


	private String getNSPrefixString(Model model){
		Map<String, String> nsPrefix = model.getNsPrefixMap();
		Set<String> prefixSet = nsPrefix.keySet();
		Iterator<String> it = prefixSet.iterator();
		
		String sparqlPrefix = "";
		
		
		while(it.hasNext()){
			String prefix = it.next();
			try {
				String uri = nsPrefix.get(prefix);
				uri = uri.replace("\\", "/");
				sparqlPrefix += "PREFIX "+prefix+": <"+(new URI(uri).toString())+">"+System.getProperty("line.separator");
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		return sparqlPrefix;
	}

	
	public String[] toSPARQL() {
		String[] sparqlStrings = new String[kReSRuleList.size()];
		int i=0;
		for(Rule kReSRule : kReSRuleList){
			sparqlStrings[i] = kReSRule.toSPARQL();
			i++;
		}
		return sparqlStrings;
	}
	
	
	@Override
	public String getRulesInKReSSyntax(){
		String kReSSyntax = "";
		
		boolean firstLoop = true;
		for(Rule kReSRule : kReSRuleList){
			if(!firstLoop){
				kReSSyntax += " . ";
			}
			else{
				firstLoop = false;
			}
			kReSSyntax += kReSRule.toKReSSyntax();
		}
		
		return kReSSyntax;
	}


	@Override
	public void addKReSRule(Rule kReSRule) {
		if(kReSRuleList == null){
			kReSRuleList = new RuleList();
		}
		kReSRuleList.add(kReSRule);
		
	}


	@Override
	public List<com.hp.hpl.jena.reasoner.rulesys.Rule> toJenaRules() {
		List<com.hp.hpl.jena.reasoner.rulesys.Rule> jenaRules = new ArrayList<com.hp.hpl.jena.reasoner.rulesys.Rule>();
		
		for(Rule rule : kReSRuleList){
			jenaRules.add(rule.toJenaRule());
		}
		
		return jenaRules;
	}
}
