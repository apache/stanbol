package eu.iksproject.kres.rules;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

import org.apache.stanbol.rules.base.api.KReSRule;
import org.apache.stanbol.rules.base.api.util.KReSRuleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
/**
 * 
 * FIXME
 * Missing description
 *
 */
public class KReSKB {


	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private Hashtable<String, String> prefixes;
	
	/**
	 * FIXME Why is this here?
	 */
	private Hashtable<String, OntModel> ontologies;
	
	private KReSRuleList kReSRuleList;
	
	
	public KReSKB() {
		log.debug("Setting up a KReSKB");
		prefixes = new Hashtable<String, String>();
		prefixes.put("var", "http://kres.iks-project.eu/ontology/meta/variables#");
		kReSRuleList = new KReSRuleList();
	}
	
	public void addPrefix(String prefixString, String prefixURI){
		prefixes.put(prefixString, prefixURI);
	}
	
	
	public String getPrefixURI(String prefixString){
		return prefixes.get(prefixString);
	}
	
	public void addRule(KReSRule kReSRule){
		System.out.println("Adding rule to Head");
		kReSRuleList.add(kReSRule);
	}
	
	public KReSRuleList getkReSRuleList() {
		return kReSRuleList;
	}
	
	public String toSPARQL(){
		String sparql = null;
		if(kReSRuleList != null){
			boolean firstIteration = true;
			for(KReSRule kReSRule : kReSRuleList){
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
		for(KReSRule kReSRule : kReSRuleList){
			
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
		for(KReSRule kReSRule : kReSRuleList){
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
