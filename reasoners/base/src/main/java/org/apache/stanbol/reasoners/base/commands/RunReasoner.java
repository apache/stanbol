/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.reasoners.base.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;

/**
 *
 * @author elvio
 */
public class RunReasoner {

    private OWLOntology owlmodel;
    private OWLReasoner loadreasoner;
    private OWLOntologyManager owlmanager;

   /**
     * This create an object where to perform reasoner tasks as consistency check, classification and general inference.
     *
     * @param reasoner {The OWLReasoner object containing the ontology to be inferred.}
     */
    public RunReasoner(OWLReasoner reasoner){

        this.owlmodel = reasoner.getRootOntology();
        this.owlmanager = owlmodel.getOWLOntologyManager();

        //Create the reasoner
        this.loadreasoner = reasoner;
        //Prepare the reasoner
        //this.loadreasoner.prepareReasoner();

    }

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
     * This method will check the consistence of the ontology.
     *
     * @return {A boolean that is true if the ontology is consistence otherwise the value is false.}
     */
    public boolean isConsistent(){
        boolean ok = false;

        ok = loadreasoner.isConsistent();

        return(ok);
    }


   /**
     * This method will get class and sub-class to classify an individual.
     *
     * @param inferredAxiomsOntology {It is an object of type OWLOntology where to put the inferred axioms.}
     * @return {This is an object of type OWLOntology that contains the inferred Axioms.}
     */
    public OWLOntology runClassifyInference(OWLOntology inferredAxiomsOntology){

        List<InferredAxiomGenerator<? extends OWLAxiom>> generators=new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
        generators.add(new InferredSubClassAxiomGenerator());
        generators.add(new InferredClassAssertionAxiomGenerator());

        InferredOntologyGenerator ioghermit  =new InferredOntologyGenerator(loadreasoner,generators);
        ioghermit.fillOntology(owlmanager, inferredAxiomsOntology);
            
            return inferredAxiomsOntology;

    }

    /**
     * This method will get class and sub-class to classify an individual.
     *
     * @return {Return an ontology with the new inference axioms inside the ontology specified in the KReSCreateReasoner object.}
     */
    public OWLOntology runClassifyInference(){

        List<InferredAxiomGenerator<? extends OWLAxiom>> generators=new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
        generators.add(new InferredSubClassAxiomGenerator());
        generators.add(new InferredClassAssertionAxiomGenerator());

        InferredOntologyGenerator ioghermit  =new InferredOntologyGenerator(loadreasoner,generators);
        ioghermit.fillOntology(owlmanager, owlmodel);

        return owlmodel;

    }

   /**
    * This method will perform a general inference with the object properties specified in the ontology given in the KReSCreateREasoner.
    *
    * @return {Return an ontology with the new inference axioms inside the ontology specified in the KReSCreateReasoner object.}
    */
    public OWLOntology runGeneralInference(){

        InferredOntologyGenerator ioghermit  =new InferredOntologyGenerator(loadreasoner);
        ioghermit.fillOntology(owlmanager, owlmodel);
        
          Set<OWLAxiom> setx = owlmodel.getAxioms();
          Iterator<OWLAxiom> iter = setx.iterator();
          
            while(iter.hasNext()){
                OWLAxiom axiom = iter.next();
                if(axiom.toString().contains("Equivalent")){
                 owlmodel.getOWLOntologyManager().removeAxiom(owlmodel,axiom);
                }
            }

          return owlmodel;
        
    }

   /**
     * This method will perform a general inference with the object properties specified in the ontology and will insert the inferences in a new ontology.
     *
     * @param newmodel {An OWLOntology objject where to insert the inferences.}
     * @return {This is an object of type OWLOntology that contains the inferred Axioms.}
     */
    public OWLOntology runGeneralInference(OWLOntology newmodel){

        InferredOntologyGenerator ioghermit  =new InferredOntologyGenerator(loadreasoner);

        ioghermit.fillOntology(owlmanager,newmodel);
        List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();

        additions = createImportList(owlmodel,newmodel);

        if(!additions.isEmpty())
            newmodel.getOWLOntologyManager().applyChanges(additions);

        Set<OWLAxiom> setx = newmodel.getAxioms();
        Iterator<OWLAxiom> iter = setx.iterator();

        while(iter.hasNext()){
                OWLAxiom axiom = iter.next();
                if(axiom.toString().contains("Equivalent")){
                 newmodel.getOWLOntologyManager().removeAxiom(newmodel,axiom);
                 }
        }

        return newmodel;
    }
    

}
