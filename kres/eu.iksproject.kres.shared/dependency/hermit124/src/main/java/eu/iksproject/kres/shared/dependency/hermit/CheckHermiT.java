/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.shared.dependency.hermit;

import org.semanticweb.HermiT.Reasoner.ReasonerFactory;

/**
 *
 * @author elvio
 */
public class CheckHermiT {

    private boolean hermit;

    public CheckHermiT(){

    //Check for hermit
    try{
        ReasonerFactory risfactory = new ReasonerFactory();
        this.hermit = risfactory.getReasonerName().equals("HermiT");
    }catch(Exception e){
        e.printStackTrace();
        this.hermit = false;
    }
    }

    public boolean getCkHermiT(){
        boolean ok = this.hermit;
        return ok;
    }

}
