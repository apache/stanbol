/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.intcheck.reasoners;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.reasoners.base.commands.RunRules;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 *
 * @author elvio
 */
@Path("/classify/demo")
public class ClassifyDemo {

    private RuleStore kresRuleStore;

    protected ONManager onm;
    protected ClerezzaOntologyStorage storage;

    private Logger log = LoggerFactory.getLogger(getClass());
     
     public ClassifyDemo(){
    	 
     }

    /**
    * To get the RuleStoreImpl where are stored the rules and the recipes
    *
    * @param servletContext
    *            {To get the context where the REST service is running.}
    */
    public ClassifyDemo(@Context ServletContext servletContext){
        this.kresRuleStore = (RuleStoreImpl) servletContext.getAttribute(RuleStore.class.getName());
        this.onm = (ONManager) servletContext.getAttribute(ONManager.class.getName());

        if (onm == null) {
        log.warn("No KReSONManager in servlet context. Instantiating manually...");
        onm = new ONManagerImpl(new TcManager(), null, new Hashtable<String, Object>());
        }
        
        this.storage = onm.getOntologyStore();
        if (storage == null) {
        log.warn("No OntologyStorage in servlet context. Instantiating manually...");
        storage = new ClerezzaOntologyStorage(new TcManager(),null);
        }
        
        if (kresRuleStore == null) {
                    log.warn("No KReSRuleStore with stored rules and recipes found in servlet context. Instantiating manually with default values...");
                    this.kresRuleStore = new RuleStoreImpl(onm,new Hashtable<String, Object>(), "");
                    log.debug("PATH TO OWL FILE LOADED: "+ kresRuleStore.getFilePath());
        }
    } 

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)   
    @Produces(value = {KRFormat.RDF_XML, KRFormat.TURTLE, KRFormat.OWL_XML, KRFormat.FUNCTIONAL_OWL,KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response ontologyClassify(@FormParam(value="scope") String scope,
                                @FormParam(value="rule") String rule,
                                @Context UriInfo uriInfo, 
                                @Context HttpHeaders headers,
                                @Context ServletContext servletContext){
        
       //System.out.println(":::::: START");  
       final OWLOntologyManager man = OWLManager.createOWLOntologyManager();
       final OWLDataFactory factory = OWLManager.getOWLDataFactory();
       
        try {
            
            OWLOntology ontology = man.loadOntologyFromOntologyDocument(IRI.create(scope+"/all"));
            
            //System.out.println(":::::: RECUPERATO LO SCOPE COME ONTOLOGIA "+ontology.getAxiomCount()); 
            if(rule != null){
                    
                    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                    OWLOntology ontologyRule = manager.loadOntologyFromOntologyDocument(IRI.create(rule));
                    OWLOntology ruleOntology = manager.createOntology();
                    
                    String ind = rule.substring(rule.lastIndexOf("http"),rule.length());
                    OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(ind));
                    OWLDataProperty bodyhead = factory.getOWLDataProperty(IRI.create("http://kres.iks-project.eu/ontology/meta/rmi.owl#hasBodyAndHead"));
                    Set<OWLLiteral> literal = ontoind.getDataPropertyValues(bodyhead, ontologyRule);
                    KB kReSKB = RuleParserImpl.parse(literal.iterator().next().getLiteral());
                    //System.out.println(":::::: RECUPERO LA REGOLA IN KB"); 
                    RuleList ruleList = kReSKB.getkReSRuleList();
                    Iterator<Rule> ruleiter = ruleList.iterator();
                    while(ruleiter.hasNext()){
                            Rule myrule = ruleiter.next();
                            SWRLRule swrlRule = myrule.toSWRL(factory);
                            manager.applyChange(new AddAxiom(ruleOntology, swrlRule));
                    }
                    //System.out.println(":::::: CREO le swrlRule e le metto in ruleOntology "+ruleOntology.getAxiomCount()); 
                    //Faccio eseguire il reasoner e ritorno il risultato.    
                    if(ruleOntology!=null){
                        //System.out.println("::: PRIMA DEL REASONER :::");
                        RunRules reasoner = new RunRules(ruleOntology,ontology);
                        //System.out.println("AVVIO REASONER");
                        OWLOntology reasonerresult = reasoner.runRulesReasoner();
                        //System.out.println("REASONER FINITO");
                        return Response.ok(reasonerresult).build();
                    }
                    else{
                        return Response.status(204).build();
                    }     
            }else{
                return Response.status(204).build();
            }
            
            

        } catch (OWLOntologyCreationException ex) {
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    public static void main(String[] args){
    	OWLOntologyManager man = OWLManager.createOWLOntologyManager();
    	
		OWLOntology ont;
		
		
		try {
			ont = man.createOntology();
			OWLOntology ontology = man.loadOntology(IRI.create("http://dbpedia.org/resource/Sony_Music_Entertainment"));
			Set<OWLAxiom> axioms = ontology.getAxioms();
			for(OWLAxiom axiom : axioms){
				
				if(!axiom.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)){
					man.addAxiom(ont, axiom);
				}
			}
			
			
			try {
				FileOutputStream fos = new FileOutputStream(new File("/Users/mac/Desktop/Sony_Music_Entertainment.rdf"));
				man.saveOntology(ont, fos);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
