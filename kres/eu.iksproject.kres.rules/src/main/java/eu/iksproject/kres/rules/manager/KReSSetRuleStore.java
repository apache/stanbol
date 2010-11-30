/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.rules.manager;

import eu.iksproject.kres.api.rules.RuleStore;
import eu.iksproject.kres.api.rules.SetRuleStore;
import java.io.File;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author elvio
 */
//@Component(immediate = true, metatype = true)
@Service(SetRuleStore.class)
public class KReSSetRuleStore implements SetRuleStore{

    private String file;
    private RuleStore store;
    private KReSLoadRuleFile load;
    private OWLOntology ont;
    private SetRuleStore setRuleStore;
    private static Logger log = LoggerFactory.getLogger(KReSSetRuleStore.class);

   /**
     * To directly set the store with a file contains the rules and the recipe
     * @param filepath {The string contains the file path.}
     */
    public KReSSetRuleStore(String filepath){

        this.file = filepath;
        this.store = new KReSRuleStore();
        this.load = new KReSLoadRuleFile(file, store);

    }

   /**
     * To directly set the store with an ontology alredy contains rules and recipe
     * @param ontology
     * @throws OWLOntologyCreationException
     */
    public KReSSetRuleStore(File ontology){

        this.store = new KReSRuleStore();

        try{
            this.ont = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontology);
            this.store.setStore(ont);
        }catch(OWLOntologyCreationException e){
            e.printStackTrace();
        }

    }

   /**
     * To get the rule store;
     * @return {A RuleStore object.}
     */
    public RuleStore returnStore(){
        return this.load.getStore();
    }

    protected void activate(ComponentContext context,String filepath){
		log.info("Activated KReS Set Rule Store");
    }

    protected void deactivate(ComponentContext context){
		log.info("Deactivated KReS Set Rule Store");
    }

}

