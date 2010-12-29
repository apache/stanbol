/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.rules.manager;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.rules.RuleStore;
import eu.iksproject.kres.api.rules.SetRuleStore;

/**
 *
 * @author elvio
 */
//@Component(immediate = true, metatype = true)
@Service(SetRuleStore.class)
public class KReSSetRuleStore implements SetRuleStore{

	private static Logger log = LoggerFactory.getLogger(KReSSetRuleStore.class);
    private String file;
    private KReSLoadRuleFile load;
    private OWLOntology ont;
    private SetRuleStore setRuleStore;
	private RuleStore store;

   /**
	 * Private constructor used only for not repeating some code on other
	 * constructors. For this purpose, this does NOT call the
	 * <code>activate()</code> method, therefore should ONLY be invoked if
	 * <code>activate()</code> is invoked afterwards.
	 * 
	 * @param onm
	 *            the KReS ontology manager to be used by the rule store.
     */
	private KReSSetRuleStore(KReSONManager onm) {
		// Since the configuration dictionary is empty, default values will be
		// used instead.
		this.store = new KReSRuleStore(onm, new Hashtable<String, Object>());
    }

   /**
	 * To directly set the store with an ontology already contains rules and
	 * recipe
	 * 
     * @param ontology
     * @throws OWLOntologyCreationException
     */
	public KReSSetRuleStore(KReSONManager onm, File ontology) {
		this(onm);
        try{
			this.ont = OWLManager.createOWLOntologyManager()
					.loadOntologyFromOntologyDocument(ontology);
            this.store.setStore(ont);
        }catch(OWLOntologyCreationException e){
			log.error(e.getMessage(), e);
        }
		// Won't do anything in this case, but is invoked as a good practice.
		activate(new Hashtable<String, Object>());
    }

   /**
	 * To directly set the store with a file contains the rules and the recipe
	 * 
	 * @param filepath
	 *            the string containing the file path.
     */
	public KReSSetRuleStore(KReSONManager onm, String filepath) {
		this(onm);
		this.file = filepath;
		this.load = new KReSLoadRuleFile(file, store);
		// Won't do anything in this case, but is invoked as a good practice.
		activate(new Hashtable<String, Object>());
    }

	/**
	 * Used to configure an instance within an OSGi container.
	 */
	@SuppressWarnings("unchecked")
	@Activate
    protected void activate(ComponentContext context,String filepath){
		log.info("in " + KReSSetRuleStore.class + " activate with context "
				+ context);
		if (context == null) {
			throw new IllegalStateException("No valid" + ComponentContext.class
					+ " parsed in activate!");
		}
		// Won't do anything in this case, but is invoked as a good practice.
		activate((Dictionary<String, Object>) context.getProperties());
	}

	/**
	 * Internally used to configure an instance (within and without an OSGi
	 * container.
	 * 
	 * @param configuration
	 */
	protected void activate(Dictionary<String, Object> configuration) {
		// There is no configuration for this component, so do nothing.
    }

	/**
	 * Deactivation of the KreSRuleStore resets all its resources.
	 */
	@Deactivate
    protected void deactivate(ComponentContext context){
		log.info("in " + KReSSetRuleStore.class + " deactivate with context "
				+ context);
		file = null;
		store = null;
		load = null;
		ont = null;
		setRuleStore = null;
    }

	/**
	 * To get the rule store;
	 * 
	 * @return {A RuleStore object.}
	 */
	public RuleStore returnStore() {
		return this.load.getStore();
}

}
