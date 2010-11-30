package eu.iksproject.kres.rules;

import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;

import eu.iksproject.kres.api.rules.KReSRule;
import eu.iksproject.kres.api.rules.util.KReSRuleList;
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
		kReSRuleList.add(kReSRule);
	}
	
	public KReSRuleList getkReSRuleList() {
		return kReSRuleList;
	}
	
}
