/*****************************************************************************
 * Source code metadata
 *
 * Author    ijd
 * Package   Jena2 Tests
 * Created   Nov 12, 2007
 * Filename  DbAwareDocumentManager.java
 *
 * (c) Copyright 2007 Hewlett-Packard Development Company, LP
 *****************************************************************************/

// Package
///////////////
package org.apache.stanbol.ontologymanager.store.jena;

// Imports
///////////////
import java.util.List;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.ModelReader;

/**
 * <p>
 * An extension to the standard document manager that holds a reference to the {@link ModelMaker} for a Jena
 * database model. That maker is used as a preferential source of stored models, indexed by URI.
 * </p>
 * 
 * @author Ian Dickinson, HP Labs (<a href="mailto:Ian.Dickinson@hp.com">email</a>)
 */
public class DbAwareDocumentManager extends OntDocumentManager {
    // Constants
    // ////////////////////////////////

    // Static variables
    // ////////////////////////////////

    @SuppressWarnings(value = "unused")
    private static Logger log = Logger.getLogger(DbAwareDocumentManager.class);

    // Instance variables
    // ////////////////////////////////

    private ModelMaker m_maker;

    // Constructors
    // ////////////////////////////////

    public DbAwareDocumentManager(ModelMaker maker) {
        m_maker = maker;
    }

    // External signature methods
    // ////////////////////////////////

    @Override
    protected void loadImport(OntModel model, String importURI, List readQueue) {
        if (m_processImports) {
            log.info("OntDocumentManager loading " + importURI);

            // add this model to occurs check list
            model.addLoadedImport(importURI);

            // check first to see if we have already loaded this model into the db
            Model in = checkExistingDbImport(importURI);

            // otherwise, try to find it
            if (in == null) {
                in = fetchPossiblyCachedImportModel(model, importURI);
            }

            // we trap the case of importing ourself (which may happen via an indirect imports chain)
            if (in != model) {
                // queue the imports from the input model on the end of the read queue
                queueImports(in, readQueue, model.getProfile());

                // add to the imports union graph, but don't do the rebind yet
                model.addSubModel(in, false);

                // we also cache the model if we haven't seen it before (and caching is on)
                addModel(importURI, in);
            }
        }
    }

    /**
     * Given a URI of an ontology, check to see whether that URL is already loaded into the database. If so,
     * use that model.
     * 
     * @param importURI
     *            The URI of an ontology to import
     * @return The existing DB model for that URI, or null
     */
    protected Model checkExistingDbImport(String importURI) {
        if (m_maker.hasModel(importURI)) {
            return m_maker.getModel(importURI);
        } else {
            return null;
        }
    }

    // Internal implementation methods
    // ////////////////////////////////

    /*
     * The following two methods have been copied by OntDocumentManager since they're marked private. This is
     * a bug (they should be protected), and will be fixed in the next Jena release.
     */

    /**
     * if we have a cached version get that, otherwise load from the URI but don't do the imports closure
     * 
     * @param model
     * @param importURI
     * @return
     */
    protected Model fetchPossiblyCachedImportModel(OntModel model, String importURI) {
        Model in = getModel(importURI);

        // if not cached, we must load it from source
        if (in == null) {
            in = fetchLoadedImportModel(model.getSpecification(), importURI);
        }
        return in;
    }

    /**
     * @param spec
     * @param importURI
     * @return
     */
    protected Model fetchLoadedImportModel(OntModelSpec spec, String importURI) {
        ModelMaker maker = spec.getImportModelMaker();
        if (maker.hasModel(importURI)) {
            Model m = maker.getModel(importURI);
            if (!m.isClosed()) {
                return m;
            } else {
                // we don't want to hang on to closed models
                maker.removeModel(importURI);
            }
        }

        // otherwise, we use the model maker to get the model anew
        Model m = spec.getImportModelGetter().getModel(importURI, new ModelReader() {
            public Model readModel(Model toRead, String URL) {
                read(toRead, URL, true);
                return toRead;
            }
        });

        return m;
    }

    // ==============================================================================
    // Inner class definitions
    // ==============================================================================

}
