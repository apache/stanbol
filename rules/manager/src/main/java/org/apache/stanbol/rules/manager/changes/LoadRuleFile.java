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

package org.apache.stanbol.rules.manager.changes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import org.apache.stanbol.rules.base.api.RuleStore;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will store a set of rules and recipes, describe in a file, to the KReSRuleStore used as input.<br/>
 * The file contains all the rules and recipes to be added to the KReSRuleStore. The rows are the following format:<br/>
 *  - All the rows that start with # will be ignored<br/>
 *  - All the rows that start with $ identify a class either a Recipe or a KReSRule<br/>
 *  - All the rows that start with @ identify a name<br/>
 *  - All the rows that start with * identify a comment<br/>
 * <br/>
 * An example of file is:<br/>
 * #This is a comment to the file<br/>
 * #These are the rules that I want to insert in the system.<br/>
 * $KReSRule<br/>
 *  &#64;MyRuleA<br/>
 * *My comment to the rule A<br/>
 * MyRuleABody -> MyRuleAHead<br/>
 *  &#64;MyRuleB<br/>
 * *My comment to the rule B<br/>
 * MyRuleBBody -> MyRuleBHead<br/>
 *  &#64;MyRuleC<br/>
 * *My comment to the rule C<br/>
 * MyRuleCBody -> MyRuleCHead<br/>
 *<br/>
 * #This is a recipe<br/>
 * $Recipe<br/>
 *  &#64;MyRecipe<br/>
 * *My comment to the recipe<br/>
 * MyRuleC<br/>
 * MyRuleB<br/>
 * MyRuleA<br/>
 *<br/>
 * N.B. The KReSRuleStore object used as input is not changed and to get the new modified KReSRuleStore there is the method getStore().
 *
 */
public class LoadRuleFile {
    
   private Logger log = LoggerFactory.getLogger(getClass());

   private OWLOntology owlmodel;
   private OWLClass ontocls;
   private OWLNamedIndividual ontoind;
   private OWLClassAssertionAxiom classAssertion;
   private OWLDataPropertyAssertionAxiom dataPropAssertion;
   private OWLObjectPropertyAssertionAxiom objectPropAssertion;
   private RuleStore storeaux;
   private String owlIDrmi;

   /**
     * This class reads an ad hoc file contains all the rules and recipes:<br/>
     *  - All the rows that start with # will be ignored<br/>
     *  - All the rows that start with $ identify a class or a Recipe or a KReSRule<br/>
     *  - All the rows that start with @ identify a name<br/>
     *  - All the rows that start with * identify a comment
     *
     * @param filepath {Path of the file contains thr rules and the recipes}
     * @param store {The KReSRuleStore where the rules will be storage}
     */
    public LoadRuleFile(String filepath, RuleStore store){
        this.storeaux = store;
        this.owlIDrmi = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";
        
        try{
            OWLOntology owlaux = OWLManager.createOWLOntologyManager().createOntology(store.getOntology().getOntologyID());
            this.owlmodel = owlaux;
            owlmodel.getOWLOntologyManager().addAxioms(owlmodel, storeaux.getOntology().getAxioms());
        }catch(OWLOntologyCreationException e){
            e.printStackTrace();
        }

        OWLOntologyManager owlmanager = owlmodel.getOWLOntologyManager();
        OWLDataFactory factory = owlmanager.getOWLDataFactory();

        String ID = owlmodel.getOntologyID().toString().replace("<","").replace(">","")+"#";
        String input ="";
        String cls ="";
        String descrp ="";
        String name="";
        String text="";
        Vector<String> seq = new Vector();

       try{
       File file = new File(filepath.trim());
       BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF8"));

       while ((input = br.readLine())!=null){
        try{
           input = input.trim();

           if((!input.startsWith("#"))&(!input.isEmpty())){

               //Verify the type of owl class
               if(input.startsWith("$")){
                   //Add the last Recipe property
                   if(seq.size()>0){
                        OWLDataProperty dataprop = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasSequence"));
                        if(owlmodel.containsIndividualInSignature(ontoind.getIRI())){
                            dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,seq.toString().replace("[","").replace("]",""));
                            owlmanager.addAxiom(owlmodel,dataPropAssertion);
                        }else{
                            log.error("There isn't the istance to which you are trying to add the property. Pleas check that "+ontoind.getIRI()+" exists.");
                            this.owlmodel = null;
                        break;
                        }
                   }
                   seq.clear();
                   ontoind = null;
                   cls = input.replace("$","").replace(" ","");

                   if(owlmodel.containsClassInSignature(IRI.create(owlIDrmi+cls))){
                        ontocls = factory.getOWLClass(IRI.create(owlIDrmi+cls));
                   }else{
                       log.error("The file contains a wrong class name. Pleas check the name: "+IRI.create(owlIDrmi+cls));
                       this.owlmodel = null;
                       break;
                   }
               }

               //Get the instance name and create the istance
               if(input.startsWith("@")){
                   name = input.replace("@", "").replace(" ","");

                   if(!owlmodel.containsIndividualInSignature(IRI.create(ID+name))){
                        ontoind = factory.getOWLNamedIndividual(IRI.create(ID+name));
                        classAssertion = factory.getOWLClassAssertionAxiom(ontocls,ontoind);
                        owlmanager.addAxiom(owlmodel, classAssertion);
                   }else{
                      log.error("The file contains a repeated istance name. The istance is already inside the ontology. Pleas check the name: "+input);
                      this.owlmodel = null;
                      break;
                   }
               }

               //Add an eventually description of the rule or recipe
               if((input.startsWith("*")&&(ontoind!=null))){
                   descrp = input.replace("*", "").trim();
                   OWLDataProperty dataprop = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasDescription"));
                   if(owlmodel.containsIndividualInSignature(ontoind.getIRI())){
                        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind, descrp);
                        owlmanager.addAxiom(owlmodel, dataPropAssertion);
                   }else{
                      log.error("There isn't the istance to which you are trying to add the property. Pleas check that "+ontoind.getIRI()+" exists.");
                      this.owlmodel = null;
                       break;
                   }
               }

               //Add the rule
               if(ontocls.getIRI().toString().equals(owlIDrmi+"KReSRule")&&(ontoind!=null)&&(!input.startsWith("*")&&(!input.startsWith("@"))&&(!input.startsWith("$")))){
                   text =input.trim();
                   OWLDataProperty dataprop = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasBodyAndHead"));

                   if(owlmodel.containsIndividualInSignature(ontoind.getIRI())){
                        dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind, text);
                        owlmanager.addAxiom(owlmodel,dataPropAssertion);
                   }else{
                      log.error("There isn't the istance to which you are trying to add the property. Pleas check that "+ontoind.getIRI()+" exists.");
                      this.owlmodel = null;
                       break;
                   }
               }

             //Add the recipe with its rule set.
             if(ontocls.getIRI().toString().equals(owlIDrmi+"Recipe")&&(ontoind!=null)&&(!input.startsWith("*"))&&(!input.startsWith("@"))&&(!input.startsWith("$"))){
               text =input.trim();
               if(owlmodel.containsIndividualInSignature(IRI.create(ID+text))&&(!IRI.create(ID+text).equals(ontoind.getIRI()))){
                   seq.add(ID+text);
                   if(owlmodel.containsIndividualInSignature(ontoind.getIRI())){
                        OWLObjectProperty objprop = factory.getOWLObjectProperty(IRI.create(owlIDrmi+"hasRule"));
                        OWLNamedIndividual ruleind = factory.getOWLNamedIndividual(IRI.create(ID+text));
                        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop,ontoind, ruleind);
                        owlmanager.addAxiom(owlmodel,objectPropAssertion);
                        
                        //Add the first rule to the Recipe
                        if(seq.size()==1){
                           objprop = factory.getOWLObjectProperty(IRI.create(owlIDrmi+"startWith"));
                           ruleind = factory.getOWLNamedIndividual(IRI.create(ID+text));
                           objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop,ontoind, ruleind);
                           owlmanager.addAxiom(owlmodel,objectPropAssertion);

                        //Add the sequential rules
                        }else if(seq.size()>1){
                           objprop = factory.getOWLObjectProperty(IRI.create("http://www.ontologydesignpatterns.org/cp/owl/sequence.owl#directlyPrecedes"));
                           OWLNamedIndividual ruleindp = factory.getOWLNamedIndividual(IRI.create(seq.get(seq.size()-2)));
                           OWLNamedIndividual ruleindf = factory.getOWLNamedIndividual(IRI.create(seq.lastElement()));
                           objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop,ruleindp, ruleindf);
                           owlmanager.addAxiom(owlmodel,objectPropAssertion);

                           //Add the last rule to the recipe
                           objprop = factory.getOWLObjectProperty(IRI.create(owlIDrmi+"endWith"));
                           objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(objprop,ontoind, ruleindf);
                           owlmanager.addAxiom(owlmodel, objectPropAssertion);
                           //Remove the previous endWith
                           if(seq.size()>2){
                                OWLObjectPropertyAssertionAxiom remove = factory.getOWLObjectPropertyAssertionAxiom(objprop,ontoind, ruleindp);
                                owlmanager.removeAxiom(owlmodel,remove);
                           }
                        }

                   }else{
                      log.error("There isn't the istance to which you are trying to add the property. Pleas check that "+ontoind.getIRI()+" and/or "+ID+text+" are already inside the ontology.");
                      this.owlmodel = null;
                      break;
                   }
              }
             }
          }
        }catch(Exception g){
            g.printStackTrace();
            this.owlmodel = null;
            break;
        }
      }

       //Add the last sequence
       if(seq.size()>0){
         OWLDataProperty dataprop = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasSequence"));
         if(owlmodel.containsIndividualInSignature(ontoind.getIRI())){
               dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(dataprop, ontoind,seq.toString().replace("[","").replace("]",""));
               owlmanager.addAxiom(owlmodel,dataPropAssertion);
         }else{
               log.error("There isn't the istance to which you are trying to add the property. Pleas check that "+ontoind.getIRI()+" exists.");
               this.owlmodel = null;
             }
       }

      if(owlmodel!=null)
           storeaux.setStore(owlmodel);

     }catch (UnsupportedEncodingException uee){
         uee.printStackTrace();
         this.owlmodel = null;
     }catch (FileNotFoundException fnfe){
         fnfe.printStackTrace();
         this.owlmodel = null;
     }catch (IOException ioe){
         ioe.printStackTrace();
         this.owlmodel = null;
     }catch (Exception e){
         e.printStackTrace();
         this.owlmodel = null;
     }
 }

   /**
     * Get the KReSRuleStore filled with rules and recipes
    *
     * @return {A KReSRuleStore object with the stored rules and recipes.}
     */
     public RuleStore getStore(){
         return this.storeaux;
     }

}