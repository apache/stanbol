/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.reasoners.base.commands;

import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.HashSet;

import org.apache.stanbol.owl.transformation.JenaToOwlConvert;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author elvio
 */
public final class RunRules {


    private OWLOntology swrlontology;
    private OWLOntology targetontology;
    private OWLOntology workontology;
    private OWLReasoner reasoner;
    private OWLOntologyManager owlmanager;

   /**
     * Constructor where inputs are the OWLOntology models contains the rules and the target ontology where to perform the reasoning (HermiT).
     *
     * @param SWRLruleOntology {The OWLOntology contains the SWRL rules.}
     * @param targetOntology {The OWLOntology model where to perform the SWRL rule reasoner.}
     */
    public RunRules(OWLOntology SWRLruleOntology, OWLOntology targetOntology){

//        cloneOntology(targetOntology);
        this.swrlontology = SWRLruleOntology;
        this.targetontology = targetOntology;
        this.owlmanager = OWLManager.createOWLOntologyManager();
        
        final Set<OWLOntology> ontologies = new HashSet();

        OWLOntologySetProvider provider = new OWLOntologySetProvider() {

            @Override
            public Set<OWLOntology> getOntologies() {
                ontologies.add(targetontology);
                ontologies.add(swrlontology);
                return ontologies;
            }
            
        };
       
        OWLOntologyMerger merger = new OWLOntologyMerger(provider);
        
        try {
            this.workontology = merger.createMergedOntology(owlmanager, targetOntology.getOntologyID().getOntologyIRI());
    
        } catch (OWLOntologyCreationException ex) {
            LoggerFactory.getLogger(RunRules.class).error("Problem to create mergedontology",ex);
        }

        //Create the reasoner
        this.reasoner = (new CreateReasoner(workontology)).getReasoner();

        //Prepare the reasoner
        //this.reasoner.prepareReasoner();
  

    }

    /**
     * Constructor where the inputs are the OWLOntology models contains the rules, the target ontology where to perform the reasoning and the url of reasoner server end-point.
     *
     * @param SWRLruleOntology {The OWLOntology contains the SWRL rules.}
     * @param targetOntology {The OWLOntology model where to perform the SWRL rule reasoner.}
     * @param reasonerurl {The url of reasoner server end-point.}
     */
    public RunRules(OWLOntology SWRLruleOntology, OWLOntology targetOntology, URL reasonerurl){

        this.targetontology = targetOntology;
        this.swrlontology = SWRLruleOntology;
        
        final Set<OWLOntology> ontologies = new HashSet();

        OWLOntologySetProvider provider = new OWLOntologySetProvider() {

            @Override
            public Set<OWLOntology> getOntologies() {
                ontologies.add(targetontology);
                ontologies.add(swrlontology);
                return ontologies;
            }
            
        };
        
        OWLOntologyMerger merger = new OWLOntologyMerger(provider);
        try {
            this.workontology = merger.createMergedOntology(owlmanager, targetOntology.getOntologyID().getOntologyIRI());
        } catch (OWLOntologyCreationException ex) {
            LoggerFactory.getLogger(RunRules.class).error("Problem to create mergedontology",ex);
        }

        //Create the reasoner
        this.reasoner = (new CreateReasoner(workontology,reasonerurl)).getReasoner();

        //Prepare the reasoner
        //this.reasoner.prepareReasoner();
    }

   /**
     * Construct where the inputs are the: Model of type jena.rdf.model.Model that contains the SWRL rules and a target OWLOntology where to perform the reasoning.
     *
     * @param SWRLruleOntology {The Jena Model contains the SWRL rules.}
     * @param targetOntology {The OWLOntology model where to perform the SWRL rule reasoner.}
     */
    public RunRules(Model SWRLruleOntology, OWLOntology targetOntology){

        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel jenamodel = ModelFactory.createOntologyModel();
        jenamodel.add(SWRLruleOntology);
        OWLOntology swrlowlmodel = j2o.ModelJenaToOwlConvert(jenamodel, "RDF/XML");
        
        this.targetontology = targetOntology;
        this.swrlontology = swrlowlmodel;
        this.targetontology = targetOntology;
        this.owlmanager = OWLManager.createOWLOntologyManager();
        
        final Set<OWLOntology> ontologies = new HashSet();

        OWLOntologySetProvider provider = new OWLOntologySetProvider() {

            @Override
            public Set<OWLOntology> getOntologies() {
                ontologies.add(targetontology);
                ontologies.add(swrlontology);
                return ontologies;
            }
            
        };
        
        OWLOntologyMerger merger = new OWLOntologyMerger(provider);
        try {
            this.workontology = merger.createMergedOntology(owlmanager, targetOntology.getOntologyID().getOntologyIRI());
        } catch (OWLOntologyCreationException ex) {
            LoggerFactory.getLogger(RunRules.class).error("Problem to create mergedontology",ex);
        }

        //Create the reasoner
        this.reasoner = (new CreateReasoner(workontology)).getReasoner();

        //Prepare the reasoner
        //this.reasoner.prepareReasoner();

    }

    /**
     * Construct where the inputs are the: Model of type jena.rdf.model.Model that contains the SWRL rules and a target OWLOntology where to perform the reasoning.
     *
     * @param SWRLruleOntology {The Jena Model contains the SWRL rules.}
     * @param targetOntology {The OWLOntology model where to perform the SWRL rule reasoner.}
     * @param reasonerurl {The url of the the reasoner server end-point.}
     */
    public RunRules(Model SWRLruleOntology, OWLOntology targetOntology, URL reasonerurl){
        
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        OntModel jenamodel = ModelFactory.createOntologyModel();
        jenamodel.add(SWRLruleOntology);
        OWLOntology swrlowlmodel = j2o.ModelJenaToOwlConvert(jenamodel, "RDF/XML");
        
        this.targetontology = targetOntology;
        this.swrlontology = swrlowlmodel;
        this.targetontology = targetOntology;
        
        final Set<OWLOntology> ontologies = new HashSet();

        OWLOntologySetProvider provider = new OWLOntologySetProvider() {

            @Override
            public Set<OWLOntology> getOntologies() {
                ontologies.add(targetontology);
                ontologies.add(swrlontology);
                return ontologies;
            }
            
        };
        
        OWLOntologyMerger merger = new OWLOntologyMerger(provider);
        try {
            this.workontology = merger.createMergedOntology(owlmanager, targetOntology.getOntologyID().getOntologyIRI());
        } catch (OWLOntologyCreationException ex) {
            LoggerFactory.getLogger(RunRules.class).error("Problem to create mergedontology",ex);
        }
        
        //Create the reasoner
        this.reasoner = (new CreateReasoner(workontology,reasonerurl)).getReasoner();
        //Prepare the reasoner
        //this.reasoner.prepareReasoner();

    }
    

   /**
     * This method will run the reasoner and then save the inferred axioms with old axioms in a new ontology
     *
     * @param newmodel {The OWLOntology model where to save the inference.}
     * @return {An OWLOntology object contains the ontology and the inferred axioms.}
     */
    public OWLOntology runRulesReasoner(final OWLOntology newmodel){

        try {
            InferredOntologyGenerator iogpellet  =new InferredOntologyGenerator(reasoner);
            iogpellet.fillOntology(newmodel.getOWLOntologyManager(), newmodel);

            Set<OWLAxiom> setx = newmodel.getAxioms();
            Iterator<OWLAxiom> iter = setx.iterator();
            while(iter.hasNext()){
                OWLAxiom axiom = iter.next();
                if(axiom.toString().contains("Equivalent")){
                 newmodel.getOWLOntologyManager().removeAxiom(newmodel,axiom);
                 }
            }
            
            final Set<OWLOntology> ontologies = new HashSet();

            OWLOntologySetProvider provider = new OWLOntologySetProvider() {

                @Override
                public Set<OWLOntology> getOntologies() {
                    ontologies.add(targetontology);
                    ontologies.add(newmodel);
                    return ontologies;
                }

            };

            OWLOntologyMerger merger = new OWLOntologyMerger(provider);
            OWLOntology ontologyout = merger.createMergedOntology(OWLManager.createOWLOntologyManager(), targetontology.getOntologyID().getOntologyIRI());
            return ontologyout;
            
        } catch (OWLOntologyCreationException ex) {
                LoggerFactory.getLogger(RunRules.class).error("Problem to create out ontology",ex);
                return null;
            }
        
    }

   /**
     * This method will run the reasoner and then save the inferred axion in the  OWLOntology
     *
     * @return {An OWLOntology object contains the ontology and the inferred axioms.}
     */
    public OWLOntology runRulesReasoner(){

            //Create inferred axiom
            InferredOntologyGenerator ioghermit  =new InferredOntologyGenerator(reasoner);
            ioghermit.fillOntology(targetontology.getOWLOntologyManager(), targetontology);
            
            Set<OWLAxiom> setx = targetontology.getAxioms();
            Iterator<OWLAxiom> iter = setx.iterator();
            while(iter.hasNext()){
                OWLAxiom axiom = iter.next();
                if(axiom.toString().contains("Equivalent")){
                 targetontology.getOWLOntologyManager().removeAxiom(targetontology,axiom);
                 }
            }         
            
            return targetontology;
     
    }

}
