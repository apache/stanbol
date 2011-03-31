package org.apache.stanbol.reasoners.base.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.owl.trasformation.JenaToOwlConvert;
import org.apache.stanbol.reasoners.base.api.ConsistentRefactorer;
import org.apache.stanbol.reasoners.base.api.InconcistencyException;
import org.apache.stanbol.reasoners.base.api.Reasoner;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.apache.stanbol.rules.refactor.api.util.URIGenerator;
import org.apache.stanbol.rules.refactor.impl.RefactorerImpl;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * Default implementation of a {@link ConsistentRefactorer}.
 * 
 * @author alessandro
 * 
 */
@Component(immediate = true, metatype = true)
@Service(ConsistentRefactorer.class)
public class ConsistentRefactorerImpl extends RefactorerImpl implements ConsistentRefactorer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    Reasoner kReSReasoner;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ConsistentRefactorerImpl instances do need to be
     * configured! YOU NEED TO USE
     * {@link #ConsistentRefactorerImpl(WeightedTcProvider, Serializer, TcManager, ONManager, SemionManager, RuleStore, Reasoner, Dictionary)}
     * or its overloads, to parse the configuration and then initialise the rule store if running outside an
     * OSGI environment.
     */
    public ConsistentRefactorerImpl() {

    }

    /**
     * Basic constructor to be used if outside of an OSGi environment. Invokes default constructor.
     * 
     * @param weightedTcProvider
     * @param serializer
     * @param tcManager
     * @param onManager
     * @param semionManager
     * @param ruleStore
     * @param kReSReasoner
     * @param configuration
     */
    public ConsistentRefactorerImpl(WeightedTcProvider weightedTcProvider,
                                    Serializer serializer,
                                    TcManager tcManager,
                                    ONManager onManager,
                                    RuleStore ruleStore,
                                    Reasoner kReSReasoner,
                                    Dictionary<String,Object> configuration) {

        super(weightedTcProvider, serializer, tcManager, onManager, ruleStore, configuration);
        this.kReSReasoner = kReSReasoner;
        activate(configuration);
    }

    @Override
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        // Nothing different to do wrt the superclass (may change if new parameters are given).
        super.activate(context);
    }

    @Override
    public void consistentOntologyRefactoring(IRI refactoredOntologyIRI, IRI datasetURI, IRI recipeIRI) throws RefactoringException,
                                                                                                       NoSuchRecipeException,
                                                                                                       InconcistencyException {

        OWLOntology refactoredOntology = null;

        ClerezzaOntologyStorage ontologyStorage = onManager.getOntologyStore();

        Recipe recipe;
        try {
            recipe = ruleStore.getRecipe(recipeIRI);

            RuleList kReSRuleList = recipe.getkReSRuleList();

            OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

            String fingerPrint = "";
            for (Rule kReSRule : kReSRuleList) {
                String sparql = kReSRule.toSPARQL();

                OWLOntology refactoredDataSet = ontologyStorage
                        .sparqlConstruct(sparql, datasetURI.toString());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    ontologyManager.saveOntology(refactoredDataSet, new RDFXMLOntologyFormat(), out);
                    if (refactoredOntologyIRI == null) {
                        ByteArrayOutputStream fpOut = new ByteArrayOutputStream();
                        fingerPrint += URIGenerator.createID("", fpOut.toByteArray());
                    }

                } catch (OWLOntologyStorageException e) {
                    log.error(
                        "Failed to store refactored ontology in memory. Consistency checking cannot be performed.",
                        e);
                }

                ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

                try {
                    ontologyManager.loadOntologyFromOntologyDocument(in);
                } catch (OWLOntologyCreationException e) {
                    log.error(
                        "Failed to reload refactored ontology. Consistency checking cannot be performed.", e);
                }

            }

            if (refactoredOntologyIRI == null) {
                refactoredOntologyIRI = IRI.create(URIGenerator.createID("urn://", fingerPrint.getBytes()));
            }
            OWLOntologyMerger merger = new OWLOntologyMerger(ontologyManager);

            try {

                refactoredOntology = merger.createMergedOntology(ontologyManager, refactoredOntologyIRI);

                if (!kReSReasoner.consistencyCheck(kReSReasoner.getReasoner(refactoredOntology))) {
                    throw new InconcistencyException(
                            "Semion Refactorer : the refactored data set seems to be inconsistent");
                } else {
                    ontologyStorage.store(refactoredOntology);
                }
            } catch (OWLOntologyCreationException e) {
                log.error("Failed to merge refactored ontology. Consistency checking cannot be performed.", e);
            }

        } catch (NoSuchRecipeException e1) {
            log.error("SemionRefactorer : No Such recipe in the KReS Rule Store", e1);
            throw e1;
        }

        if (refactoredOntology == null) {
            throw new RefactoringException();
        }

    }

    @Override
    public OWLOntology consistentOntologyRefactoring(OWLOntology inputOntology, IRI recipeIRI) throws RefactoringException,
                                                                                              NoSuchRecipeException,
                                                                                              InconcistencyException {

        OWLOntology refactoredOntology = null;

        JenaToOwlConvert jenaToOwlConvert = new JenaToOwlConvert();

        OntModel ontModel = jenaToOwlConvert.ModelOwlToJenaConvert(inputOntology, "RDF/XML");

        Recipe recipe;
        try {
            recipe = ruleStore.getRecipe(recipeIRI);

            RuleList kReSRuleList = recipe.getkReSRuleList();

            OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

            for (Rule kReSRule : kReSRuleList) {
                String sparql = kReSRule.toSPARQL();

                Query sparqlQuery = QueryFactory.create(sparql);
                QueryExecution qexec = QueryExecutionFactory.create(sparqlQuery, ontModel);
                Model refactoredModel = qexec.execConstruct();

                OWLOntology refactoredDataSet = jenaToOwlConvert.ModelJenaToOwlConvert(refactoredModel,
                    "RDF/XML");

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    ontologyManager.saveOntology(refactoredDataSet, new RDFXMLOntologyFormat(), out);
                } catch (OWLOntologyStorageException e) {
                    log.error(
                        "Failed to store refactored ontology in memory. Consistency checking cannot be performed.",
                        e);
                }

                ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

                try {
                    ontologyManager.loadOntologyFromOntologyDocument(in);
                } catch (OWLOntologyCreationException e) {
                    log.error(
                        "Failed to reload refactored ontology. Consistency checking cannot be performed.", e);
                }

            }

            OWLOntologyMerger merger = new OWLOntologyMerger(ontologyManager);

            try {
                IRI defaultOntologyIRI = IRI.create("http://kres.iksproject.eu/semion/autoGeneratedOntology");
                refactoredOntology = merger.createMergedOntology(ontologyManager, defaultOntologyIRI);

                if (!kReSReasoner.consistencyCheck(kReSReasoner.getReasoner(refactoredOntology))) {
                    throw new InconcistencyException(
                            "Semion Refactorer : the refactored data set seems to be inconsistent");
                }

            } catch (OWLOntologyCreationException e) {
                log.error("Failed to merge refactored ontology. Consistency checking cannot be performed.", e);
            }

        } catch (NoSuchRecipeException e1) {
            log.error("SemionRefactorer : No Such recipe in the KReS Rule Store", e1);
            throw e1;
        }

        if (refactoredOntology == null) {
            throw new RefactoringException();
        } else {
            return refactoredOntology;
        }
    }

    @Override
    @Deactivate
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
        // Here we also need to unset the reasoner.
        this.kReSReasoner = null;
    }

}
