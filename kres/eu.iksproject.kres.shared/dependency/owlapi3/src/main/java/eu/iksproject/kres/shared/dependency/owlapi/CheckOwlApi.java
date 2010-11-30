/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.shared.dependency.owlapi;

import org.semanticweb.owlapi.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 *
 * @author elvio
 */
public class CheckOwlApi {

private boolean owlapi;

    public CheckOwlApi(){

    //Check for owl model;
    try{
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology owlmodel = manager.createOntology();
        this.owlapi = owlmodel.isEmpty();
    }catch(Exception e){
        e.printStackTrace();
        this.owlapi = false;
    }
    }

    public boolean getCkOwl(){
        boolean ok = this.owlapi;
        return ok;
    }

}
