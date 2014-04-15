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
package org.apache.stanbol.ontologymanager.servicesapi.util;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.sources.owlapi.OntologySpaceSource;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of static utility methods for managing ontologies.
 * 
 * @author alexdma
 * 
 */
public final class OntologyImportUtils {

    /**
     * Restrict instantiation
     */
    private OntologyImportUtils() {}

    private static final Logger logger = LoggerFactory.getLogger(OntologyImportUtils.class);

    public static OWLOntology buildImportTree(OntologyInputSource<OWLOntology> rootSrc,
                                              Set<OWLOntology> subtrees) {
        return buildImportTree(rootSrc.getRootOntology(), subtrees, OWLManager.createOWLOntologyManager());
    }

    /**
     * 
     * @param rootSrc
     * @param subtrees
     * @param mgr
     * @return
     */
    public static OWLOntology buildImportTree(OntologyInputSource<OWLOntology> rootSrc,
                                              Set<OWLOntology> subtrees,
                                              OWLOntologyManager mgr) {

        if (rootSrc instanceof OntologySpaceSource) {
            OntologySpace spc = ((OntologySpaceSource) rootSrc).asOntologySpace();
            for (OWLOntology o : subtrees)
                try {
                    spc.addOntology(new RootOntologySource(o));
                } catch (UnmodifiableOntologyCollectorException e) {
                    logger.error("Cannot add ontology {} to unmodifiable space {}", o, spc);
                    continue;
                }
        }

        return buildImportTree(rootSrc.getRootOntology(), subtrees, mgr);

    }

    /**
     * Non-recursively adds import statements to the root ontology so that it is directly linked to all the
     * ontologies in the subtrees set.
     * 
     * @param root
     *            the ontology to which import subtrees should be appended. If null, a runtime exception will
     *            be thrown.
     * @param subtrees
     *            the set of target ontologies for import statements. These can in turn be importing other
     *            ontologies, hence the &quot;subtree&quot; notation. A single statement will be added for
     *            each member of this set.
     * @return the same input ontology as defined in <code>root</code>, but with the added import statements.
     */
    public static OWLOntology buildImportTree(OWLOntology root, Set<OWLOntology> subtrees) {
        return buildImportTree(root, subtrees, OWLManager.createOWLOntologyManager());
    }

    /**
     * Non-recursively adds import statements to the root ontology so that it is directly linked to all the
     * ontologies in the subtrees set.
     * 
     * @param parent
     *            the ontology to which import subtrees should be appended. If null, a runtime exception will
     *            be thrown.
     * @param subtrees
     *            the set of target ontologies for import statements. These can in turn be importing other
     *            ontologies, hence the &quot;subtree&quot; notation. A single statement will be added for
     *            each member of this set.
     * @param mgr
     *            the OWL ontology manager to use for constructing the import tree. If null, an internal one
     *            will be used instead, otherwise an existing ontology manager can be used e.g. for extracting
     *            import statements from its IRI mappers or known ontologies. Note that the supplied manager
     *            will <i>never</i> try to load any ontologies, even when they are unknown.
     * @return the same input ontology as defined in <code>root</code>, but with the added import statements.
     */
    public static OWLOntology buildImportTree(OWLOntology parent,
                                              Set<OWLOntology> subtrees,
                                              OWLOntologyManager mgr) {

        if (parent == null) throw new NullPointerException(
                "Cannot append import trees to a nonexistent ontology.");

        // If no manager was supplied, use a temporary one.
        if (mgr == null) mgr = OWLManager.createOWLOntologyManager();
        OWLDataFactory owlFactory = mgr.getOWLDataFactory();
        List<OWLOntologyChange> changes = new LinkedList<OWLOntologyChange>();

        for (OWLOntology o : subtrees) {

            IRI importIri = null;
            try {
                /*
                 * First query the manager, as it could know the physical location of anonymous ontologies, if
                 * previously loaded or IRI-mapped.
                 */
                importIri = mgr.getOntologyDocumentIRI(o);
            } catch (UnknownOWLOntologyException ex) {
                /*
                 * Otherwise, ask the ontology itself (the location of an anonymous ontology may have been
                 * known at creation/loading time, even if another manager built it.)
                 */
                importIri = o.getOntologyID().getDefaultDocumentIRI();
            } catch (Exception ex) {
                logger.error(
                    "Exception caught during tree building. Skipping import of ontology " + o.getOntologyID(),
                    ex);
            } finally {
                /*
                 * It is still possible that an imported ontology is anonymous but has no physical document
                 * IRI (for example, because it was only generated in-memory but not stored). In this case it
                 * is necessary (and generally safe) to copy all its axioms and import statements to the
                 * parent ontology, or else it is lost.
                 */
                if (o.isAnonymous() && importIri == null) {
                    logger.warn("Anonymous import target "
                                + o.getOntologyID()
                                + " not mapped to physical IRI. Will add extracted axioms to parent ontology.");
                    for (OWLImportsDeclaration im : o.getImportsDeclarations())
                        changes.add(new AddImport(parent, im));
                    for (OWLAxiom im : o.getAxioms())
                        changes.add(new AddAxiom(parent, im));
                } else if (importIri != null) {
                    // An anonymous ontology can still be imported if it has a
                    // valid document IRI.
                    changes.add(new AddImport(parent, owlFactory.getOWLImportsDeclaration(importIri)));
                }
            }

        } // End subtrees cycle.

        // All possible error causes should have been dealt with by now, but we
        // apply the changes one by one, just in case.
        for (OWLOntologyChange im : changes)
            try {
                mgr.applyChange(im);
            } catch (Exception ex) {
                logger.error("KReS :: Exception caught during tree building. Skipping import", ex);
                continue;
            }
        // mgr.applyChanges(changes);

        return parent;
    }

    public static OWLOntology buildImportTree(Set<OWLOntology> subtrees) throws OWLOntologyCreationException {
        return buildImportTree(subtrees, OWLManager.createOWLOntologyManager());
    }

    public static OWLOntology buildImportTree(Set<OWLOntology> subtrees, OWLOntologyManager mgr) throws OWLOntologyCreationException {
        return buildImportTree(new RootOntologySource(mgr.createOntology()), subtrees, mgr);
    }

    @Deprecated
    public static void printOntology(OWLOntology o, PrintStream printer) {

        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        StringDocumentTarget tgt = new StringDocumentTarget();
        try {
            mgr.saveOntology(o, new RDFXMLOntologyFormat(), tgt);
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace(printer);
        }
        printer.println(tgt.toString());

    }

}
