package eu.iksproject.kres.reasoners;

import java.util.Iterator;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.reasoners.KReSReasoner;

@Component(immediate = true, metatype = true)
@Service(KReSReasoner.class)
public class KReSReasonerImpl implements KReSReasoner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public KReSReasonerImpl() {
		
	}
	
	public OWLReasoner getReasoner(OWLOntology ontology){
		KReSCreateReasoner kReSCreateReasoner = new KReSCreateReasoner(ontology);
		return kReSCreateReasoner.getReasoner();
	}
	
	public boolean consistencyCheck(OWLReasoner owlReasoner){
		KReSRunReasoner kReSRunReasoner = new KReSRunReasoner(owlReasoner);
		return kReSRunReasoner.isConsistent();
	}
	
	protected void activate(ComponentContext context){
		
		log.info("Activated KReS Reasoning Services");
	}
	
	protected void deactivate(ComponentContext context){
		log.info("Dectivated KReS Reasoning Services");
	}
	
	@Override
	public OWLOntology runRules(OWLOntology ontology, OWLOntology ruleOntology){

        KReSRunRules kReSRunRules = new KReSRunRules(ruleOntology, ontology);
        return kReSRunRules.runRulesReasoner();
 
	}
	
}
