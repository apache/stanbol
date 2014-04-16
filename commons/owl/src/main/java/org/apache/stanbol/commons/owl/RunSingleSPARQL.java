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

package org.apache.stanbol.commons.owl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLOntology;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;

import org.apache.stanbol.commons.owl.transformation.JenaToOwlConvert;

/**
 *
 * @author elvio
 */
public class RunSingleSPARQL {

    private OWLOntology owlmodel;
    private OntModel jenamodel;
    private Map<String,String> sparqlprefix;

   /**
     * Constructor to create an OWLOntology object where run the SPARQL query.
     *
     * @param owl {The OWLOntology to be querying.}
     */
    public RunSingleSPARQL(OWLOntology owl){
        this.owlmodel = owl;

        try{
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        this.jenamodel = j2o.ModelOwlToJenaConvert(owlmodel,"RDF/XML");
        Iterator<String> iter = jenamodel.getNsPrefixMap().keySet().iterator();
        Map<String, String> map = jenamodel.getNsPrefixMap();
        this.sparqlprefix = map;
   
        while(iter.hasNext()){
            String k = iter.next();
            
            if(!k.isEmpty()){            
                this.sparqlprefix.put(k,"<"+map.get(k)+">");
            }else{
                String base = map.get(k);
                this.sparqlprefix.put("base", "<"+base+">");
                this.sparqlprefix.remove(k);
            }
        }
    
        }catch (Exception e){
            e.printStackTrace();
        }
        
    }

   /**
     * Constructor to create an OWLOntology object where run the SPARQL query.
     *
     * @param owl {The OWLOntology to be querying}
     * @param prefix {The map where the keys are the prefix label and the value the IRI of the prefix on the form: http://www.w3.org/2000/01/rdf-schema#.}
     */
    public RunSingleSPARQL(OWLOntology owl, Map<String,String> prefix){
        this.owlmodel = owl;
        JenaToOwlConvert j2o = new JenaToOwlConvert();
        this.jenamodel = j2o.ModelOwlToJenaConvert(owlmodel,"RDF/XML");
        Iterator<String> keys = prefix.keySet().iterator();
        this.sparqlprefix = new HashMap();
        while(keys.hasNext()){
            String key = keys.next();
            String pre = prefix.get(key);
       
            if(pre.contains("<"))
              this.sparqlprefix.put(key, pre);
            else
              this.sparqlprefix.put(key,"<"+pre+">");
        }
    }

   /**
     * To get the prefix mapping
     *
     * @return {Return a prefix mapping.}
     */
    public Map<String,String> getSPARQLprefix(){
        return this.sparqlprefix;
    }

   /**
     * To add a new prefix.
     *
     * @param label {The prefix label}
     * @param prefix {The new prefix to be added in the form: http://www.w3.org/2000/01/rdf-schema#}.
     * @return {A boolean that is true if process finish without errors.}
     */
    public boolean addSPARQLprefix(String label,String prefix){

        boolean ok = false;

        if(!sparqlprefix.containsKey(label)){
            
            if(!prefix.contains("<"))
              sparqlprefix.put(label, "<"+prefix+">");
            else
              sparqlprefix.put(label, prefix);

        if(sparqlprefix.containsKey(label))
            if(sparqlprefix.get(label).contains(prefix))
                ok = true;

        }else{
            System.err.println("The prefix with "+label+" already exists. Prefix: "+sparqlprefix.get(label));
            ok = false;
        }
        return ok;

    }

   /**
     * To remove a prefix with a particular label from the prefix mapping.
     *
     * @param label {The label of the prefix to be removed.}
     * @return {A boolean that is true if process finish without errors.}
     */
    public boolean removeSPARQLprefix(String label){
        boolean ok = false;

        if(sparqlprefix.containsKey(label)){

            sparqlprefix.remove(label);
            if(!sparqlprefix.containsKey(label))
                ok = true;
            else
                ok = false;

        }else{
            System.err.println("No prefix with name "+label);
            ok = false;
        }

        return ok;
    }

   /**
    * To create a SPARQL QueryExecution Object
    *
    * @param query {The query string without the declaration of the prefixes.}
    * @return {Return a QueryExecution Object.}
    */
   public QueryExecution createSPARQLQueryExecutionFactory(String query){

        if(!sparqlprefix.isEmpty()){

            Iterator<String> keys = sparqlprefix.keySet().iterator();
            String prefix ="";
            while(keys.hasNext()){
                String key = keys.next();
                prefix = prefix+"PREFIX "+key+": "+sparqlprefix.get(key)+"\n";
            }
            query = prefix+query;

            try{
                return QueryExecutionFactory.create(query,jenamodel);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }else{
            System.err.println("There is not prefix defined in sparqlprefix.");
            return null;
        }

    }

}
