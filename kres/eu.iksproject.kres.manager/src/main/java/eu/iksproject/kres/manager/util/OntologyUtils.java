package eu.iksproject.kres.manager.util;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.io.RootOntologySource;

/**
 * A set of static utility methods for managing ontologies in KReS.
 * 
 * @author alessandro
 * 
 */
public class OntologyUtils {

	// /**
	// * Creates an ontology with the specified IRI and only the import
	// statements
	// * for linking to all the ontologies in the subtrees set. Useful for
	// running
	// * reasoners on an ontology set, as reasoners are instantiated on a single
	// * ontology.
	// *
	// * @param rootIri
	// * @param subtrees
	// * @return
	// */
	// public static OWLOntology buildImportTree(IRI rootIri,
	// Set<OWLOntology> subtrees, OWLOntologyManager mgr) {
	// OWLOntology root = null;
	// try {
	// root = rootIri != null ? mgr.createOntology(rootIri) : mgr
	// .createOntology();
	// } catch (OWLOntologyAlreadyExistsException e) {
	// root = mgr.getOntology(rootIri);
	// } catch (OWLOntologyCreationException e) {
	// e.printStackTrace();
	// return root;
	// }
	// return buildImportTree(root, subtrees, mgr);
	// }

	public static OWLOntology appendOntology(OntologyInputSource parentSrc,
			OntologyInputSource childSrc, OWLOntologyManager ontologyManager) {
		return appendOntology(parentSrc, childSrc, ontologyManager, null);
	}

	public static OWLOntology appendOntology(OntologyInputSource parentSrc,
			OntologyInputSource childSrc) {
		return appendOntology(parentSrc, childSrc, null, null);
	}

	public static OWLOntology appendOntology(OntologyInputSource parentSrc,
			OntologyInputSource childSrc, IRI rewritePrefix) {
		return appendOntology(parentSrc, childSrc, null, rewritePrefix);
	}

	/**
	 * This method appends one ontology (the child) to another (the parent) by
	 * proceeding as follows. If a physical URI can be obtained from the child
	 * source, an import statement using that physical URI will be added to the
	 * parent ontology, otherwise all the axioms from the child ontology will be
	 * copied to the parent. <br>
	 * Note: the ontology manager will not load additional ontologies.
	 * 
	 * @param parentSrc
	 *            must exist!
	 * @param childSrc
	 * @param ontologyManager
	 *            can be null (e.g. when one does not want changes to be
	 *            immediately reflected in their ontology manager), in which
	 *            case a temporary ontology manager will be used.
	 * @param rewritePrefix
	 *            . if not null, import statements will be generated in the form
	 *            <tt>rewritePrefix/child_ontology_logical_IRI</tt>. It can be
	 *            used for relocating the ontology document file elsewhere.
	 * @return the parent with the appended child
	 */
	public static OWLOntology appendOntology(OntologyInputSource parentSrc,
			OntologyInputSource childSrc, OWLOntologyManager ontologyManager,
			IRI rewritePrefix) {

		if (ontologyManager == null)
			ontologyManager = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = ontologyManager.getOWLDataFactory();
		OWLOntology oParent = parentSrc.getRootOntology();
		OWLOntology oChild = childSrc.getRootOntology();

		// Named ontology with a provided absolute prefix. Use name and prefix
		// for creating an new import statement.
		OWLOntology child = childSrc.getRootOntology();
		if (!child.isAnonymous() && rewritePrefix != null
		/* && rewritePrefix.isAbsolute() */) {
			IRI impIri = IRI.create(rewritePrefix + "/"
					+ child.getOntologyID().getOntologyIRI());
			OWLImportsDeclaration imp = factory
					.getOWLImportsDeclaration(impIri);
			ontologyManager.applyChange(new AddImport(oParent, imp));
		}
		// Anonymous, with physicalIRI. A plain import statement is added.
		else if (childSrc.hasPhysicalIRI()) {
			OWLImportsDeclaration imp = factory
					.getOWLImportsDeclaration(childSrc.getPhysicalIRI());
			ontologyManager.applyChange(new AddImport(oParent, imp));
		}

		// Anonymous and no physical IRI (e.g. in memory). Copy all axioms and
		// import statements.
		else {
			ontologyManager.addAxioms(oParent, oChild.getAxioms());
			for (OWLImportsDeclaration imp : oChild.getImportsDeclarations())
				ontologyManager.applyChange(new AddImport(oParent, factory
						.getOWLImportsDeclaration(imp.getIRI())));
		}
		return oParent;
	}

	public static OWLOntology buildImportTree(OntologyInputSource rootSrc,
			Set<OWLOntology> subtrees) {

		return buildImportTree(rootSrc.getRootOntology(), subtrees, OWLManager
				.createOWLOntologyManager());

	}

	/**
	 * 
	 * @param rootSrc
	 * @param subtrees
	 * @param mgr
	 * @return
	 */
	public static OWLOntology buildImportTree(OntologyInputSource rootSrc,
			Set<OWLOntology> subtrees, OWLOntologyManager mgr) {

		return buildImportTree(rootSrc.getRootOntology(), subtrees, mgr);

	}

	/**
	 * Non-recursively adds import statements to the root ontology so that it is
	 * directly linked to all the ontologies in the subtrees set.
	 * 
	 * @param root
	 *            the ontology to which import subtrees should be appended. If
	 *            null, a runtime exception will be thrown.
	 * @param subtrees
	 *            the set of target ontologies for import statements. These can
	 *            in turn be importing other ontologies, hence the
	 *            &quot;subtree&quot; notation. A single statement will be added
	 *            for each member of this set.
	 * @return the same input ontology as defined in <code>root</code>, but with
	 *         the added import statements.
	 */
	public static OWLOntology buildImportTree(OWLOntology root,
			Set<OWLOntology> subtrees) {

		return buildImportTree(root, subtrees, OWLManager
				.createOWLOntologyManager());

	}

	/**
	 * Non-recursively adds import statements to the root ontology so that it is
	 * directly linked to all the ontologies in the subtrees set.
	 * 
	 * @param parent
	 *            the ontology to which import subtrees should be appended. If
	 *            null, a runtime exception will be thrown.
	 * @param subtrees
	 *            the set of target ontologies for import statements. These can
	 *            in turn be importing other ontologies, hence the
	 *            &quot;subtree&quot; notation. A single statement will be added
	 *            for each member of this set.
	 * @param mgr
	 *            the OWL ontology manager to use for constructing the import
	 *            tree. If null, an internal one will be used instead, otherwise
	 *            an existing ontology manager can be used e.g. for extracting
	 *            import statements from its IRI mappers or known ontologies.
	 *            Note that the supplied manager will <i>never</i> try to load
	 *            any ontologies, even when they are unknown.
	 * @return the same input ontology as defined in <code>root</code>, but with
	 *         the added import statements.
	 */
	public static OWLOntology buildImportTree(OWLOntology parent,
			Set<OWLOntology> subtrees, OWLOntologyManager mgr) {

		if (parent == null)
			throw new NullPointerException(
					"Cannot append import trees to a nonexistent ontology.");

		// If no manager was supplied, use a temporary one.
		if (mgr == null)
			mgr = OWLManager.createOWLOntologyManager();
		OWLDataFactory owlFactory = mgr.getOWLDataFactory();
		List<OWLOntologyChange> changes = new LinkedList<OWLOntologyChange>();
		Logger logger = ONManager.get().log;

		for (OWLOntology o : subtrees) {

			IRI importIri = null;
			try {
				/*
				 * First query the manager, as it could know the physical
				 * location of anonymous ontologies, if previously loaded or
				 * IRI-mapped.
				 */
				importIri = mgr.getOntologyDocumentIRI(o);
			} catch (UnknownOWLOntologyException ex) {
				/*
				 * Otherwise, ask the ontology itself (the location of an
				 * anonymous ontology may have been known at creation/loading
				 * time, even if another manager built it.)
				 */
				importIri = o.getOntologyID().getDefaultDocumentIRI();
			} catch (Exception ex) {
				logger.error(
						"KReS :: Exception caught during tree building. Skipping import of ontology "
								+ o.getOntologyID(), ex);
			} finally {
				/*
				 * It is still possible that an imported ontology is anonymous
				 * but has no physical document IRI (for example, because it was
				 * only generated in-memory but not stored). In this case it is
				 * necessary (and generally safe) to copy all its axioms and
				 * import statements to the parent ontology, or else it is lost.
				 */
				if (o.isAnonymous() && importIri == null) {
					logger
							.warn("KReS :: [NONFATAL] Anonymous import target "
									+ o.getOntologyID()
									+ " not mapped to physical IRI. Will add extracted axioms to parent ontology.");
					for (OWLImportsDeclaration im : o.getImportsDeclarations())
						changes.add(new AddImport(parent, im));
					for (OWLAxiom im : o.getAxioms())
						changes.add(new AddAxiom(parent, im));
				} else if (importIri != null) {
					// An anonymous ontology can still be imported if it has a
					// valid document IRI.
					changes.add(new AddImport(parent, owlFactory
							.getOWLImportsDeclaration(importIri)));
				}
			}

		} // End subtrees cycle.

		// All possible error causes should have been dealt with by now, but we
		// apply the changes one by one, just in case.
		for (OWLOntologyChange im : changes)
			try {
				mgr.applyChange(im);
			} catch (Exception ex) {
				logger
						.error(
								"KReS :: Exception caught during tree building. Skipping import",
								ex);
				continue;
			}
		// mgr.applyChanges(changes);

		return parent;
	}

	public static OWLOntology buildImportTree(Set<OWLOntology> subtrees)
			throws OWLOntologyCreationException {

		return buildImportTree(subtrees, OWLManager.createOWLOntologyManager());

	}

	public static OWLOntology buildImportTree(Set<OWLOntology> subtrees,
			OWLOntologyManager mgr) throws OWLOntologyCreationException {

		return buildImportTree(new RootOntologySource(mgr.createOntology()),
				subtrees, mgr);

	}

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
