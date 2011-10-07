package org.apache.stanbol.reasoners.jena;

import java.util.List;

import org.apache.stanbol.reasoners.servicesapi.ReasoningService;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * Interface for a Jena based reasoning services
 */
public interface JenaReasoningService extends ReasoningService<Model,Rule,Statement> {

    /**
     * Runs the reasoner over the given input data
     * 
     * @param data
     * @return
     */
    public abstract InfModel run(Model data);

    /**
     * Run the reasoner over the given data and rules
     * 
     * @param data
     * @param rules
     * @return
     */
    public abstract InfModel run(Model data, List<Rule> rules);
}