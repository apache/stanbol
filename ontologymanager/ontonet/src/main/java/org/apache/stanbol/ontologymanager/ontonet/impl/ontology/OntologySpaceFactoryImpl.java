package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceListener;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that generates default implementations of the three types of ontology scope.
 * 
 * @author alessandro
 * 
 */
public class OntologySpaceFactoryImpl implements OntologySpaceFactory {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected ScopeRegistry registry;

    /*
     * The ClerezzaOntologyStorage (local to OntoNet) has been changed with PersistenceStore (general from
     * Stanbol)
     */
    protected ClerezzaOntologyStorage storage;
    
    protected OWLOntologyManagerFactoryImpl mgrFactory;

    public OntologySpaceFactoryImpl(ScopeRegistry registry, ClerezzaOntologyStorage storage, OWLOntologyManagerFactoryImpl mgrFactory) {
        this.registry = registry;
        this.storage = storage;
        this.mgrFactory = mgrFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory#createCoreOntologySpace
     * (org.semanticweb.owlapi.model.IRI,
     * org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource)
     */
    @Override
    public CoreOntologySpace createCoreOntologySpace(IRI scopeID, OntologyInputSource coreSource) {
        CoreOntologySpace s = new CoreOntologySpaceImpl(scopeID, storage,mgrFactory.createOntologyManager(true));
        configureSpace(s, scopeID, coreSource);
        return s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory#createCustomOntologySpace
     * (org.semanticweb.owlapi.model.IRI,
     * org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource)
     */
    @Override
    public CustomOntologySpace createCustomOntologySpace(IRI scopeID, OntologyInputSource customSource) {
        CustomOntologySpace s = new CustomOntologySpaceImpl(scopeID, storage,mgrFactory.createOntologyManager(true));
        configureSpace(s, scopeID, customSource);
        return s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory#createSessionOntologySpace
     * (org.semanticweb.owlapi.model.IRI)
     */
    @Override
    public SessionOntologySpace createSessionOntologySpace(IRI scopeID) {
        SessionOntologySpace s = new SessionOntologySpaceImpl(scopeID, storage,mgrFactory.createOntologyManager(true));
        // s.setUp();
        return s;
    }

    /**
     * Utility method for configuring ontology spaces after creating them.
     * 
     * @param s
     * @param scopeID
     * @param rootSource
     */
    private void configureSpace(OntologySpace s, IRI scopeID, OntologyInputSource rootSource) {
        // FIXME: ensure that this is not null
        OntologyScope parentScope = registry.getScope(scopeID);

        if (parentScope != null && parentScope instanceof OntologySpaceListener) s
                .addOntologySpaceListener((OntologySpaceListener) parentScope);
        // Set the supplied ontology's parent as the root for this space.
        try {
            s.setTopOntology(rootSource, true);
        } catch (UnmodifiableOntologySpaceException e) {
            log.error("Ontology space " + s.getID() + " was found locked at creation time!", e);
        }
        // s.setUp();
    }

}
