package org.apache.stanbol.explanation.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.explanation.api.Configuration;
import org.apache.stanbol.explanation.api.Explainable;
import org.apache.stanbol.explanation.api.Explanation;
import org.apache.stanbol.explanation.api.ExplanationGenerator;
import org.apache.stanbol.explanation.api.ExplanationTypes;
import org.apache.stanbol.explanation.api.KnowledgeItem;
import org.apache.stanbol.ontologymanager.ontonet.api.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.BlankOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true)
@Service(ExplanationGenerator.class)
public class ExplanationGeneratorImpl implements ExplanationGenerator {

    private static final String _EXPLANATION_SCHEMA = "http://ontologydesignpatterns.org/schemas/explanationschema.owl";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Configuration config;

    @Reference
    private ONManager onm;

    @Reference
    private DataFileProvider dataFileProvider;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ExplanationGeneratorImpl instances do need to be
     * configured! YOU NEED TO USE {@link #ExplanationGeneratorImpl(ONManager, DataFileProvider, Dictionary)}
     * or its overloads, to parse the configuration and then initialise the rule store if running outside an
     * OSGI environment.
     */
    public ExplanationGeneratorImpl() {
        super();
    }

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param onManager
     * @param config
     * @param dataFileProvider
     * @param configuration
     */
    public ExplanationGeneratorImpl(Configuration config,
                                    DataFileProvider dataFileProvider,
                                    Dictionary<String,Object> configuration) {
        this();
        this.config = config;
        this.onm = config.getOntologyNetworkManager();
        this.dataFileProvider = dataFileProvider;
        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + ExplanationGeneratorImpl.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        // OntoNetSimulator sim = new OntoNetSimulator();
        // Create and register the scope for explanations.
        String scopeid = config.getScopeID();
        OntologyScope scope = null;
        try {
            OntologyInputSource coreSrc;
            try {
                coreSrc = new RootOntologyIRISource(IRI.create(_EXPLANATION_SCHEMA));
            } catch (OWLOntologyCreationException e) {
                coreSrc = new BlankOntologySource();
            }
            scope = onm.getOntologyScopeFactory().createOntologyScope(scopeid, coreSrc);
            onm.getScopeRegistry().registerScope(scope, true);
        } catch (DuplicateIDException e) {
            log.warn("Cannot create scope {}. A scope with this ID is already registered.", scopeid);
            scope = onm.getScopeRegistry().getScope(scopeid);
        }
        if (scope != null) {
            try {
                if (dataFileProvider != null) {
                    InputStream is = dataFileProvider.getInputStream(null, "organizationalhierarchy.owl",
                        null);
                }
            } catch (IOException ex) {
                log.warn("FAiled to get file");
            }
        }

        log.debug("Explanation Generator activated.");

    }

    @Override
    public Explanation createExplanation(Explainable<?> item,
                                         ExplanationTypes type,
                                         Set<? extends OWLAxiom> grounds) {

        Explanation result = new ExplanationImpl(item, type);

        switch (type) {
            case KNOWLEDGE_OBJECT_SYNOPSIS:
                if (!(item instanceof KnowledgeItem)) break;
                KnowledgeItem ki = (KnowledgeItem) item;
                break;
            case UI_FEEDBACK_JUSTIFICATION:
                break;
        }

        return result;
    }

    /**
     * Deactivation of the ONManagerImpl resets all its resources.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + ExplanationGeneratorImpl.class + " deactivate with context " + context);
    }

}
