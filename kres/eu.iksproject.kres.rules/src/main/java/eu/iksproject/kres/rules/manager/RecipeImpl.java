package eu.iksproject.kres.rules.manager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import org.apache.stanbol.rules.base.api.KReSRule;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.util.KReSRuleList;
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
	private KReSRuleList kReSRuleList;
	
	
	
	
	/**
	 * 
	 * Create a new {@code RecipeImpl} from a set of rule expressed in KReS rule syntax.
	 * 
	 * 
	 * @param recipeID
	 * @param recipeDescription
	 * @param kReSRuleList
	 */
	public RecipeImpl(IRI recipeID, String recipeDescription, KReSRuleList kReSRuleList) {
		this.recipeID = recipeID;
		this.recipeDescription = recipeDescription;
		this.kReSRuleList = kReSRuleList;
	}
	
	
	public KReSRuleList getkReSRuleList() {
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

	public KReSRule getRule(String ruleURI) {
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
		for(KReSRule kReSRule : kReSRuleList){
			sparqlStrings[i] = kReSRule.toSPARQL();
			i++;
		}
		return sparqlStrings;
	}
	
	
	@Override
	public String getRulesInKReSSyntax(){
		String kReSSyntax = "";
		
		boolean firstLoop = true;
		for(KReSRule kReSRule : kReSRuleList){
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
	public void addKReSRule(KReSRule kReSRule) {
		if(kReSRuleList == null){
			kReSRuleList = new KReSRuleList();
		}
		kReSRuleList.add(kReSRule);
		
	}
}
