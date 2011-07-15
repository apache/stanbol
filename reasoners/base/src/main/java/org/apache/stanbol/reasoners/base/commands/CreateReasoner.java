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

import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.owllink.OWLlinkHTTPXMLReasonerFactory;
import org.semanticweb.owlapi.owllink.OWLlinkReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

/**
 * This class create an OWLReasoner. The used reasoner is HermiT but alternatively can be used an online reasoner.
 * 
 */
public class CreateReasoner {

    private OWLReasoner reasoner;

   /**
     * To create an HermiT OWLReasoner object with the input ontology.
     *
     * @param owl {An OWLOntology object where to perform the inferences.}
     */
    public CreateReasoner(OWLOntology owl){
        //ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        OWLReasonerConfiguration config = new SimpleConfiguration();
        ReasonerFactory risfactory = new ReasonerFactory();
        this.reasoner = risfactory.createReasoner(owl,config);
    }

   /**
     * To create an HermiT OWLReasoner object with the input ontology.
     *
     * @param owl {An OWLOntology object where to perform the inferences.}
     * @param owlrc {An OWLReasonerConfiguration object contains particular configuration to set the resaoner.}
     */
    public CreateReasoner(OWLOntology owl,OWLReasonerConfiguration owlrc){
        ReasonerFactory risfactory = new ReasonerFactory();
        this.reasoner = risfactory.createReasoner(owl,owlrc);
    }


   /**
     * To create an OWLReasoner object by using online reasoner with the input ontology.
     *
     * @param owl {An OWLOntology object where to perform the inferences.}
     * @param reasonerurl {The url of the reasoner server end-point.}
     */
    public CreateReasoner(OWLOntology owl,URL reasonerurl){
        OWLlinkReasonerConfiguration reasonerConfiguration = new OWLlinkReasonerConfiguration(reasonerurl);
        OWLlinkHTTPXMLReasonerFactory factory = new OWLlinkHTTPXMLReasonerFactory();
        this.reasoner = factory.createNonBufferingReasoner(owl, reasonerConfiguration);
    }

   /**
     * To get the created resoner.
     *
     * @return {An OWLReasoner object with the ontology inside.}
     */
    public OWLReasoner getReasoner(){
        return this.reasoner;
    }


}
