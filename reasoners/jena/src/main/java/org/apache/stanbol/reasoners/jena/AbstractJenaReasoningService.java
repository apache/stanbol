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
package org.apache.stanbol.reasoners.jena;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.reasoners.jena.filters.PropertyFilter;
import org.apache.stanbol.reasoners.servicesapi.InconsistentInputException;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceException;
import org.apache.stanbol.reasoners.servicesapi.UnsupportedTaskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.InfGraph;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.reasoner.ValidityReport.Report;
import com.hp.hpl.jena.reasoner.rulesys.FBRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Abstract implementation of the {@see JenaReasoningService} interface
 */
public abstract class AbstractJenaReasoningService implements JenaReasoningService {
    private Reasoner reasoner;
    private static final Logger log = LoggerFactory.getLogger(AbstractJenaReasoningService.class);

    /**
     * This constructor sets the given reasoner instance as the default shared one.
     * 
     * @param reasoner
     */
    protected AbstractJenaReasoningService(Reasoner reasoner) {
        this.reasoner = reasoner;
    }

    /**
     * Gets the Jena reasoner instance (to be used by subclasses)
     * 
     * @return
     */
    protected Reasoner getReasoner() {
        return reasoner;
    }

    /**
     * Generic method to perform inferences
     */
    @Override
    public InfModel run(Model data) {
        log.debug(" run(Model data)");
        InfModel im = ModelFactory.createInfModel(this.reasoner, data);
        im.prepare();
        return im;
    }

    /**
     * This method performs inferences creating a new specialized reasoner, which extends the capabilities of
     * the default one and adds the given rule set.
     * 
     * @param data
     * @param rules
     * @return
     */
    @Override
    public InfModel run(Model data, List<Rule> rules) {
        log.debug(" run(Model data, List<Rule> rules)");
        InfGraph inferredGraph = customReasoner(rules).bind(data.getGraph());
        return ModelFactory.createInfModel(inferredGraph);
    }

    /**
     * This method provides the default implementation for executing one of the default tasks.
     * 
     * TODO: Add support for the filtered parameter on task 'classify'; TODO: The task 'classify' should also
     * return rdfs:subClassOf statements.
     */
    @Override
    public Set<Statement> runTask(String taskID,
                                  Model data,
                                  List<Rule> rules,
                                  boolean filtered,
                                  Map<String,List<String>> parameters) throws UnsupportedTaskException,
                                                           ReasoningServiceException,
                                                           InconsistentInputException {
        log.debug(" runTask(String taskID,Model data,List<Rule> rules,boolean filtered,Map<String,List<String>> parameters)");
        if (taskID.equals(ReasoningService.Tasks.CLASSIFY)) {
            if (rules != null) {
                return classify(data, rules);
            } else {
                return classify(data);
            }
        } else if (taskID.equals(ReasoningService.Tasks.ENRICH)) {
            if (rules != null) {
                return enrich(data, rules, filtered);
            } else {
                return enrich(data, filtered);
            }
        } else throw new UnsupportedTaskException();
    }

    /**
     * This method provides the default implementation for executing one of the default tasks with no
     * additional arguments.
     * 
     * TODO: Add support for the filtered parameter on task 'classify'; TODO: The task 'classify' should also
     * return rdfs:subClassOf statements.
     */
    @Override
    public Set<Statement> runTask(String taskID, Model data) throws UnsupportedTaskException,
                                                            ReasoningServiceException,
                                                            InconsistentInputException {
        log.debug(" runTask(String taskID, Model data)");
        if (taskID.equals(ReasoningService.Tasks.CLASSIFY)) {
            return classify(data);
        } else if (taskID.equals(ReasoningService.Tasks.ENRICH)) {
            return enrich(data);
        } else throw new UnsupportedTaskException();
    }

    /**
     * This method is called to build a custom reasoner to be used with a given rule set. Subclasses may want
     * to specialize the default behavior, which simply merge the standard rule set with the given list
     * 
     * Note: to customized the default reasoner instance, we need to create a new instance, to avoid to keep
     * the configuration changes in the standard shared instance. This is not much efficient, because we
     * create a new reasoner for each call which includes a specified rule set.
     * 
     * In the future, we may want to implement a way to deploy customized shared reasoning services, based on
     * a specific recipe.
     * 
     * @param rules
     * @return
     */
    protected Reasoner customReasoner(List<Rule> customRules) {
        log.debug(" customReasoner(List<Rule> customRules)");
        List<Rule> standardRules = ((FBRuleReasoner) this.reasoner).getRules();
        standardRules.addAll(customRules);
        return new GenericRuleReasoner(standardRules);
    }

    /**
     * Default implementation for task {@see ReasoningService.Tasks.CLASSIFY}. Classification: 1) Perform
     * reasoning 2) Returns only rdf:type statements.
     * 
     * This is a default implementation of task {@see ReasoningService.Tasks.CLASSIFY}. Subclasses may want to
     * change it.
     * 
     * TODO: This method should also return rdfs:subClassOf statements
     * 
     * @param data
     * @return
     */
    protected Set<Statement> classify(Model data) {
        log.debug(" classify(Model data)");
        return run(data).listStatements().filterKeep(new PropertyFilter(RDF.type)).toSet();
    }

    /**
     * 
     * Classification: 1) Perform reasoning on a reasoner customized with the given rule set 2) Returns only
     * rdf:type statements
     * 
     * This is a default implementation of task {@see ReasoningService.Tasks.CLASSIFY}. Subclasses may want to
     * change it.
     * 
     * TODO: This method should also return rdfs:subClassOf statements
     * 
     * @param data
     * @param rules
     * @return
     */
    protected Set<Statement> classify(Model data, List<Rule> rules) {
        log.debug(" classify(Model data, List<Rule> rules)");
        return run(data, rules).listStatements().filterKeep(new PropertyFilter(RDF.type)).toSet();
    }

    /**
     * Enriching: 1) Perform reasoning 2) Returns all the statements (filtered = false) or only inferred ones
     * (filtered = true)
     * 
     * This is a default implementation of task {@see ReasoningService.Tasks.ENRICH}. Subclasses may want to
     * change it.
     * 
     * @param data
     * @param rules
     * @return
     */
    protected Set<Statement> enrich(Model data, boolean filtered) {
        log.debug(" enrich(Model data, boolean filtered)");
        // Since the input model is modified by the reasoner,
        // We keep the original list to prune the data after, if necessary
        if(filtered){
            Set<Statement> original = new HashSet<Statement>();
            original.addAll(data.listStatements().toSet());
            log.debug(" original statements are: {}",original.size());
            InfModel i = run(data);
            Set<Statement> inferred = i.listStatements().toSet();
            log.debug(" inferred statements are: {}",inferred.size());
            return prune(original, inferred);
        }else{
            return run(data).listStatements().toSet();
        }
    }

    /**
     * Removes the statements in the first set from the second set
     * 
     * @param input
     * @param statements
     * @return
     */
    protected final Set<Statement> prune(Set<Statement> first, Set<Statement> second) {
        log.debug(" prune(Set<Statement> first[{}], Set<Statement> second[{}])",first.size(),second.size());
        Set<Statement> remove = new HashSet<Statement>();
        for (Statement s : second) {
            if (first.contains(s)) {
                remove.add(s);
            }
        }
        log.debug(" ---- removing {} statements from {}",first.size(),second.size());
        second.removeAll(remove);
        return second;
    }

    /**
     * Enriching: 1) Perform reasoning 2) Returns the inferred statements only. This is the same as
     * enrich(data, true)
     * 
     * This is a default implementation of task {@see ReasoningService.Tasks.ENRICH}. Subclasses may want to
     * change it.
     * 
     * @param data
     * @return
     */
    public Set<Statement> enrich(Model data) {
        log.debug(" enrich(Model data)");
        return enrich(data, true);
    }

    /**
     * Enriching: 1) Perform reasoning on a reasoner customized with the given rule set 2) Returns all the
     * statements (filtered = false) or only inferred ones (filtered = true)
     * 
     * This is a default implementation of task {@see ReasoningService.Tasks.ENRICH} when a set of rules is
     * given. Subclasses may want to change it.
     * 
     * @param data
     * @param rules
     * @param filtered
     * @return
     */
    protected Set<Statement> enrich(Model data, List<Rule> rules, boolean filtered) {
        log.debug(" enrich(Model data, List<Rule> rules, boolean filtered)");
        // Since the input model is modified by the reasoner,
        // We keep the original list to prune the data after, if necessary
        if(filtered){
            Set<Statement> original = new HashSet<Statement>();
            original.addAll(data.listStatements().toSet());
            log.debug(" original statements are: {}",original.size());
            InfModel i = run(data, rules);
            Set<Statement> inferred = i.listStatements().toSet();
            log.debug(" inferred statements are: {}",inferred.size());
            return prune(original, inferred);
        }else{
            return run(data, rules).listStatements().toSet();
        }
    }

    /**
     * Enriching: 1) Perform reasoning on a reasoner customized with the given rule set 2) Returns the
     * inferred statements only. This is the same as enrich(data, rules, true)
     * 
     * This is a default implementation of task {@see ReasoningService.Tasks.ENRICH} when a set of rules is
     * given. Subclasses may want to change it.
     * 
     * @param data
     * @param rules
     * @return
     */
    protected Set<Statement> enrich(Model data, List<Rule> rules) {
        log.debug(" enrich(Model data, List<Rule> rules)");
        return enrich(data, rules, true);
    }

    /**
     * Consistency check: whether this RDF is consistent or not
     * 
     * @param data
     * @return
     */
    @Override
    public boolean isConsistent(Model data) {
        log.debug(" isConsistent(Model data)");
        return isConsistent(run(data).validate());
    }

    /**
     * Consistency check: whether this RDF is consistent or not
     * 
     * We decide to apply a strict meaning of consistency. The alternative would be to use isValid() method,
     * which tolerates classes that can't be instantiated
     * 
     * @param data
     * @param rules
     * @return
     */
    @Override
    public boolean isConsistent(Model data, List<Rule> rules) {
        log.debug(" isConsistent(Model data, List<Rule> rules)");
        return isConsistent(run(data, rules).validate());
    }

    /**
     * This internal method implements the logic of consistency.
     * 
     * We decide to apply a strict meaning of consistency. The alternative would be to use isValid() method,
     * which tolerates classes that can't be instantiated.
     * 
     * Subclasses may want to change this behavior.
     * 
     * @param report
     * @return
     */
    protected boolean isConsistent(ValidityReport report) {
        log.debug(" isConsistent(ValidityReport report)");
        if(log.isDebugEnabled()){
        Iterator<Report> it = report.getReports();
            while (it.hasNext()) {
                log.debug("Report: {}", it.next());
            }
        }
        return report.isClean();
    }

    @Override
    public Class<Model> getModelType() {
        return Model.class;
    }

    @Override
    public Class<Rule> getRuleType() {
        return Rule.class;
    }

    @Override
    public Class<Statement> getStatementType() {
        return Statement.class;
    }

    /**
     * Subclasses may want to extend this
     */
    @Override
    public List<String> getSupportedTasks() {
        return ReasoningService.Tasks.DEFAULT_TASKS;
    }

    /**
     * Subclasses may need to change this
     */
    public boolean supportsTask(String taskID) {
        return getSupportedTasks().contains(taskID);
    };
}
