/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.reasoners.base.commands;

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import eu.iksproject.kres.shared.transformation.JenaToOwlConvert;

/**
 *
 * @author elvio
 */
public final class KReSRunRules {


    private OWLOntology targetontology;
    private OWLReasoner reasoner;
    private OWLOntologyManager owlmanager;
    private OWLOntology originalowl;

    /**
     * To create a list of imported ontlogy to be added as import declarations
     *
     * @param inowl {Input ontology where to get the import declarations}
     * @return {A list of declarations}
     */
    private List<OWLOntologyChange> createImportList(OWLOntology inowl,OWLOntology toadd){

        Iterator<OWLOntology> importedonto = inowl.getDirectImports().iterator();
        List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
        OWLDataFactory auxfactory = inowl.getOWLOntologyManager().getOWLDataFactory();

        while(importedonto.hasNext()){
            OWLOntology auxonto = importedonto.next();
            additions.add(new AddImport(toadd,auxfactory.getOWLImportsDeclaration(auxonto.getOWLOntologyManager().getOntologyDocumentIRI(auxonto))));
        }

        if(additions.size()==0){
            Iterator<OWLImportsDeclaration> importedontob = inowl.getImportsDeclarations().iterator();
            additions = new LinkedList<OWLOntologyChange>();
            auxfactory = inowl.getOWLOntologyManager().getOWLDataFactory();

            while(importedontob.hasNext()){
                OWLImportsDeclaration  auxontob = importedontob.next();
                additions.add(new AddImport(toadd,auxontob));
            }
        }

        return additions;
    }

   /**
     * To clone ontology with all its axioms and imports declaration
     *
     * @param inowl {The onotlogy to be cloned}
     * @return {An ontology with the same characteristics}
     */
    private void cloneOntology(OWLOntology inowl){

        //Clone the targetontology
        try {
            this.originalowl = OWLManager.createOWLOntologyManager().createOntology(inowl.getOntologyID().getOntologyIRI());
            OWLOntologyManager manager = this.originalowl.getOWLOntologyManager();
            //Add axioms
            manager.addAxioms(this.originalowl,inowl.getAxioms());
            //Add import declaration
            List<OWLOntologyChange> additions = createImportList(inowl,originalowl);
            if(additions.size()>0)
                manager.applyChanges(additions);
        } catch (OWLOntologyCreationException ex) {
            Logger.getLogger(KReSRunRules.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

   /**
     * Constructor where inputs are the OWLOntology models contains the rules and the target ontology where to perform the reasoning (HermiT).
     *
     * @param SWRLruleOntology {The OWLOntology contains the SWRL rules.}
     * @param targetOntology {The OWLOntology model where to perform the SWRL rule reasoner.}
     */
    public KReSRunRules(OWLOntology SWRLruleOntology, OWLOntology targetOntology){  

        cloneOntology(targetOntology);
        this.targetontology = targetOntology;
        this.owlmanager = originalowl.getOWLOntologyManager();

        //Add SWRL to the model
        owlmanager.addAxioms(originalowl,SWRLruleOntology.getAxioms());
        List<OWLOntologyChange> additions = createImportList(SWRLruleOntology,originalowl);
        if(!additions.isEmpty())
            owlmanager.applyChanges(additions);

        //Create the reasoner
        this.reasoner = (new KReSCreateReasoner(originalowl)).getReasoner();

        //Prepare the reasoner
        this.reasoner.prepareReasoner();

    }

    /**
     * Constructor where the inputs are the OWLOntology models contains the rules, the target ontology where to perform the reasoning and the url of reasoner server end-point.
     *
     * @param SWRLruleOntology {The OWLOntology contains the SWRL rules.}
     * @param targetOntology {The OWLOntology model where to perform the SWRL rule reasoner.}
     * @param reasonerurl {The url of reasoner server end-point.}
     */
    public KReSRunRules(OWLOntology SWRLruleOntology, OWLOntology targetOntology, URL reasonerurl){

        cloneOntology(targetOntology);
        this.targetontology = targetOntology;
        this.owlmanager = originalowl.getOWLOntologyManager();

        //Add SWRL to the model
        owlmanager.addAxioms(originalowl,SWRLruleOntology.getAxioms());
        List<OWLOntologyChange> additions = createImportList(SWRLruleOntology,originalowl);
        if(!additions.isEmpty())
            owlmanager.applyChanges(additions);

        //Create the reasoner
        this.reasoner = (new KReSCreateReasoner(originalowl,reasonerurl)).getReasoner();

        //Prepare the reasoner
        this.reasoner.prepareReasoner();
    }

   /**
     * Construct where the inputs are the: Model of type jena.rdf.model.Model that contains the SWRL rules and a target OWLOntology where to perform the reasoning.
     *
     * @param SWRLruleOntology {The Jena Model contains the SWRL rules.}
     * @param targetOntology {The OWLOntology model where to perform the SWRL rule reasoner.}
     */
    public KReSRunRules(Model SWRLruleOntology, OWLOntology targetOntology){

        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel jenamodel = ModelFactory.createOntologyModel();
        jenamodel.add(SWRLruleOntology);
        OWLOntology swrlowlmodel = j2o.ModelJenaToOwlConvert(jenamodel, "RDF/XML");
        cloneOntology(targetOntology);
        this.targetontology = targetOntology;
        this.owlmanager = originalowl.getOWLOntologyManager();

        //Add SWRL to the model
        owlmanager.addAxioms(originalowl,swrlowlmodel.getAxioms());
        List<OWLOntologyChange> additions = createImportList(swrlowlmodel,originalowl);
        if(!additions.isEmpty())
            owlmanager.applyChanges(additions);

        //Create the reasoner
        this.reasoner = (new KReSCreateReasoner(originalowl)).getReasoner();

        //Prepare the reasoner
        this.reasoner.prepareReasoner();

    }

    /**
     * Construct where the inputs are the: Model of type jena.rdf.model.Model that contains the SWRL rules and a target OWLOntology where to perform the reasoning.
     *
     * @param SWRLruleOntology {The Jena Model contains the SWRL rules.}
     * @param targetOntology {The OWLOntology model where to perform the SWRL rule reasoner.}
     * @param reasonerurl {The url of the the reasoner server end-point.}
     */
    public KReSRunRules(Model SWRLruleOntology, OWLOntology targetOntology, URL reasonerurl){
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel jenamodel = ModelFactory.createOntologyModel();
        jenamodel.add(SWRLruleOntology);
        OWLOntology swrlowlmodel = j2o.ModelJenaToOwlConvert(jenamodel, "RDF/XML");

        cloneOntology(targetOntology);
        this.targetontology = targetOntology;
        this.owlmanager = originalowl.getOWLOntologyManager();

        //Add SWRL to the model
        owlmanager.addAxioms(originalowl,swrlowlmodel.getAxioms());
        List<OWLOntologyChange> additions = createImportList(swrlowlmodel,originalowl);
        if(!additions.isEmpty())
            owlmanager.applyChanges(additions);
        
        //Create the reasoner
        this.reasoner = (new KReSCreateReasoner(originalowl,reasonerurl)).getReasoner();
        //Prepare the reasoner
        this.reasoner.prepareReasoner();

    }
    

   /**
     * This method will run the reasoner and then save the inferred axioms with old axioms in a new ontology
     *
     * @param newmodel {The OWLOntology model where to save the inference.}
     * @return {An OWLOntology object contains the ontology and the inferred axioms.}
     */
    public OWLOntology runRulesReasoner(OWLOntology newmodel){

            InferredOntologyGenerator iogpellet  =new InferredOntologyGenerator(reasoner);
            iogpellet.fillOntology(owlmanager, newmodel);
            List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();

            Set<OWLAxiom> setx = newmodel.getAxioms();
            Iterator<OWLAxiom> iter = setx.iterator();
            while(iter.hasNext()){
                OWLAxiom axiom = iter.next();
                if(axiom.toString().contains("Equivalent")){
                 newmodel.getOWLOntologyManager().removeAxiom(newmodel,axiom);
                 }
            }

            additions = createImportList(targetontology,newmodel);
            if(!additions.isEmpty())
                newmodel.getOWLOntologyManager().applyChanges(additions);

            return newmodel;
    }

   /**
     * This method will run the reasoner and then save the inferred axion in the  OWLOntology
     *
     * @return {An OWLOntology object contains the ontology and the inferred axioms.}
     */
    public OWLOntology runRulesReasoner(){

            //Create inferred axiom
            InferredOntologyGenerator ioghermit  =new InferredOntologyGenerator(reasoner);
            ioghermit.fillOntology(owlmanager, targetontology);
            Set<OWLAxiom> setx = targetontology.getAxioms();
            Iterator<OWLAxiom> iter = setx.iterator();
            while(iter.hasNext()){
                OWLAxiom axiom = iter.next();
                if(axiom.toString().contains("Equivalent")){
                 targetontology.getOWLOntologyManager().removeAxiom(targetontology,axiom);
                 }
            }

//            OWLDataProperty noprop = newmodel.getOWLOntologyManager().getOWLDataFactory().getOWLDataProperty(IRI.create("http://www.w3.org/2002/07/owl#topDataProperty"));
//            OWLEquivalentDataPropertiesAxiom nopropax = newmodel.getOWLOntologyManager().getOWLDataFactory().getOWLEquivalentDataPropertiesAxiom(noprop);
//            newmodel.getOWLOntologyManager().removeAxiom(newmodel, nopropax);          
            
            return targetontology;
     
    }

}
