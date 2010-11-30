/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.shared.dependency.owllink;

import org.semanticweb.owlapi.owllink.OWLlinkHTTPXMLReasonerFactory;

/**
 *
 * @author elvio
 */
public class CheckLink {

    private boolean link;

    public CheckLink(){

    //Check for hermit
    try{
        OWLlinkHTTPXMLReasonerFactory factory = new OWLlinkHTTPXMLReasonerFactory();
        
        this.link = factory.getReasonerName().contains("OWLlink");
    }catch(Exception e){
        e.printStackTrace();
        this.link = false;
    }
    }

    public boolean getCkLink(){
        boolean ok = this.link;
        return ok;
    }

}
