package org.apache.stanbol.reasoners.base.impl;

import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.reasoners.base.api.Reasoner;
import org.apache.stanbol.reasoners.base.commands.CreateReasoner;
import org.apache.stanbol.reasoners.base.commands.RunReasoner;
import org.apache.stanbol.reasoners.base.commands.RunRules;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true)
@Service(Reasoner.class)
public class ReasonerImpl implements Reasoner {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * This default constructor is <b>only</b> intended to be used by the OSGI
	 * environment with Service Component Runtime support.
	 * <p>
	 * DO NOT USE to manually create instances - the KReSRuleStore instances do
	 * need to be configured! YOU NEED TO USE
	 * {@link #ReasonerImpl(Dictionary)} or its overloads, to parse the
	 * configuration and then initialise the rule store if running outside a
	 * OSGI environment.
	 */
	public ReasonerImpl() {
	}
		
	/**
	 * Basic constructor to be used if outside of an OSGi environment. Invokes
	 * default constructor.
	 * 
	 */
	public ReasonerImpl(Dictionary<String, Object> configuration) {
		// Always call activate(Dictionary) in the constructor as a golden rule.
		activate(configuration);
	}
	// org.osgi.service.component.ComponentContext;
	@SuppressWarnings("unchecked")
	@Activate
	protected void activate(ComponentContext context) {
		log.info("in " + ReasonerImpl.class + " activate with context "
				+ context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		activate((Dictionary<String, Object>) context.getProperties());
	}

	protected void activate(Dictionary<String, Object> configuration) {

	}
	
	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.reasoners.Reasoner#consistencyCheck(org.semanticweb.owlapi.reasoner.OWLReasoner)
	 */
	public boolean consistencyCheck(OWLReasoner owlReasoner){
		RunReasoner kReSRunReasoner = new RunReasoner(owlReasoner);
		return kReSRunReasoner.isConsistent();
	}
	
	@Deactivate
	protected void deactivate(ComponentContext context) {
		log.info("in " + ReasonerImpl.class + " deactivate with context "
				+ context);
	}
	
	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.reasoners.Reasoner#getReasoner(org.semanticweb.owlapi.model.OWLOntology)
	 */
	public OWLReasoner getReasoner(OWLOntology ontology) {
		CreateReasoner kReSCreateReasoner = new CreateReasoner(ontology);
		return kReSCreateReasoner.getReasoner();
	}
	
	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.reasoners.Reasoner#runRules(org.semanticweb.owlapi.model.OWLOntology, org.semanticweb.owlapi.model.OWLOntology)
	 */
	@Override
	public OWLOntology runRules(OWLOntology ontology, OWLOntology ruleOntology){
        RunRules kReSRunRules = new RunRules(ruleOntology, ontology);
        return kReSRunRules.runRulesReasoner();
	}
	
}
