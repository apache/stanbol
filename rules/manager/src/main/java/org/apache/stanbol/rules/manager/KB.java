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
package org.apache.stanbol.rules.manager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
/**
 * 
 * FIXME
 * Missing description
 *
 */
public class KB {


	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private Hashtable<String, String> prefixes;
	
	/**
	 * FIXME Why is this here?
	 */
	private Hashtable<String, OntModel> ontologies;
	
	private RuleList kReSRuleList;
	
	
	public KB() {
		log.debug("Setting up a KReSKB");
		prefixes = new Hashtable<String, String>();
		prefixes.put("var", "http://kres.iks-project.eu/ontology/meta/variables#");
		kReSRuleList = new RuleList();
	}
	
	public void addPrefix(String prefixString, String prefixURI){
		prefixes.put(prefixString, prefixURI);
	}
	
	
	public String getPrefixURI(String prefixString){
		return prefixes.get(prefixString);
	}
	
	public void addRule(Rule kReSRule){
		kReSRuleList.add(kReSRule);
	}
	
	public RuleList getkReSRuleList() {
		return kReSRuleList;
	}
	
	public String toSPARQL(){
		String sparql = null;
		if(kReSRuleList != null){
			boolean firstIteration = true;
			for(Rule kReSRule : kReSRuleList){
				if(firstIteration){
					firstIteration = false;
				}
				else{
					sparql += " . ";
				}
				sparql += kReSRule.toSPARQL();
			}
		}
		
		return sparql;
	}
	
	public void write(OutputStream outputStream) throws IOException{
		boolean firstIt = true;
		for(Rule kReSRule : kReSRuleList){
			
			String rule;
			
			if(firstIt){
				rule = kReSRule.toKReSSyntax();
				
				firstIt = false;
			}
			else{
				rule = " . " + System.getProperty("line.separator") + kReSRule.toKReSSyntax();
			}
			outputStream.write(rule.getBytes());
		}
		outputStream.close();
	}
	
	public void write(FileWriter fileWriter) throws IOException{
		boolean write = true;
		for(Rule kReSRule : kReSRuleList){
			if(write){
				fileWriter.write(kReSRule.toKReSSyntax());
				write = false;
			}
			else{
				fileWriter.write(" . " + System.getProperty("line.separator") + kReSRule.toKReSSyntax());
			}
		}
		fileWriter.close();
	}
	
}
