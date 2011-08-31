package org.apache.stanbol.reasoners.hermit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;


public class TestUtils {

	public static void debug(OWLOntology ont,Logger log) {
		// For debug only
		if (log.isDebugEnabled()) {
			// We show all axioms in this ontology
			log.debug("OntologyID: {}", ont.getOntologyID());
			log.debug("Imports:");
			// Imports
			for (OWLOntology o : ont.getOWLOntologyManager().getImports(ont)) {
				log.debug(" - {}", o);
			}
			log.debug("Axioms:");
			for (OWLAxiom a : ont.getAxioms())
				log.debug(" - {}", a);
		}
	}

	public static void debug(Set<? extends OWLAxiom> ax,Logger log) {
		if (log.isDebugEnabled()) {
			log.debug("Axioms: ");
			for (OWLAxiom a : ax) {
				log.debug(" - {}", a);
			}
		}
	}
	/**
	 * This is for monitoring hermit with datatype properties.
	 * 
	 * @param ont
	 */
    public static void checkProperties(OWLOntology ont,Logger log){
        // When throw inconsistent exception = false and ignoreUnsupportedDatatypes=true
        //- Datatypes which are not builtIn break the reasoner
        //- Looks like rdf:PlainLiteral is not supported by Hermit, even if it is marked as BuiltIn datatype by OWLApi
        // This incoherence generates an unexpected error!
        //
        Map<OWLDataProperty,Set<OWLDatatype>> properties = new HashMap<OWLDataProperty,Set<OWLDatatype>>();
        Set<OWLAxiom> remove = new HashSet<OWLAxiom>();
        for(OWLAxiom a :ont.getLogicalAxioms()){
            if(a instanceof OWLDataPropertyAssertionAxiom){
                OWLDataPropertyAssertionAxiom aa = (OWLDataPropertyAssertionAxiom) a;
                for (OWLDataProperty p : aa.getDataPropertiesInSignature()) {
                    if (!properties.keySet().contains(p)) {
                        properties.put(p, new HashSet<OWLDatatype>());
                    }
                    for (OWLDatatype dt : aa.getDatatypesInSignature()){
                        properties.get(p).add(dt);
                        
                    }
                }
            }
        }
        log.info("Data properties : ");
        for(Entry<OWLDataProperty,Set<OWLDatatype>> p: properties.entrySet()){
            log.info(" - {} ",p.getKey());
            for(OWLDatatype d : p.getValue()){
                log.info(" ---> {} [{}]",d,d.isBuiltIn());
            }
        }
        log.info("Data property axioms removed:");
        for(OWLAxiom d : remove){
            log.info(" removed ---> {} ",d.getDataPropertiesInSignature());
        }
    }
}
