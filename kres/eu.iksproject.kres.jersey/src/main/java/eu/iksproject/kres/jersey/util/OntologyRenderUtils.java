package eu.iksproject.kres.jersey.util;

import java.util.ArrayList;
import java.util.List;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import eu.iksproject.kres.api.format.KReSFormat;
import eu.iksproject.kres.api.manager.KReSONManager;

public class OntologyRenderUtils {

	/**
	 * TODO : make a writer for this.
	 * 
	 * @param ont
	 * @param format
	 * @return
	 * @throws OWLOntologyStorageException
	 */
	public static String renderOntology(OWLOntology ont,
			OWLOntologyFormat format, String rewritePrefix, KReSONManager onm)
			throws OWLOntologyStorageException {
		OWLOntologyManager tmpmgr = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = tmpmgr.getOWLDataFactory();

		// Now the hack
		OWLOntology o2 = null;
		OWLOntology copy = null;
		OWLOntologyManager origMgr = ont.getOWLOntologyManager();
		try {
			List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
			copy = tmpmgr.createOntology(ont.getOntologyID());
			for (OWLAxiom ax : ont.getAxioms()) {
				changes.add(new AddAxiom(copy, ax));
			}
			for (OWLImportsDeclaration imp : ont.getImportsDeclarations()) {
				OWLOntology oi = origMgr.getImportedOntology(imp);
				if (oi == null)
					oi = onm.getOwlCacheManager().getImportedOntology(imp);
				String impiri = "";
				if (rewritePrefix != null)
					impiri += rewritePrefix + "/";
				if (oi == null)
					// Proprio non riesci a ottenerla questa ontologia? Rinuncia
					continue;
				if (oi.isAnonymous())
					impiri = imp.getIRI().toString();
				else
					impiri += oi.getOntologyID().getOntologyIRI();
				OWLImportsDeclaration im = df.getOWLImportsDeclaration(IRI
						.create(impiri));
				changes.add(new AddImport(copy, im));
			}
			tmpmgr.applyChanges(changes);
		} catch (OWLOntologyCreationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (copy != null)
			o2 = copy;
		else
			o2 = ont;

		StringDocumentTarget tgt = new StringDocumentTarget();
		tmpmgr.saveOntology(o2, format, tgt);
		return tgt.toString();
	}

	public static String renderOntology(OWLOntology ont, String format,
			String rewritePrefix, KReSONManager onm)
			throws OWLOntologyStorageException {
		OWLOntologyManager tmpmgr = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = tmpmgr.getOWLDataFactory();
		StringDocumentTarget tgt = new StringDocumentTarget();

		// Now the hack
		OWLOntology o2 = null;
		OWLOntology copy = null;
		OWLOntologyManager origMgr = ont.getOWLOntologyManager();
		try {
			List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
			copy = tmpmgr.createOntology(ont.getOntologyID());
			for (OWLAxiom ax : ont.getAxioms()) {
				changes.add(new AddAxiom(copy, ax));
			}
			for (OWLImportsDeclaration imp : ont.getImportsDeclarations()) {
				OWLOntology oi = origMgr.getImportedOntology(imp);
				if (oi == null)
					oi = onm.getOwlCacheManager().getImportedOntology(imp);
				String impiri = "";
				if (rewritePrefix != null)
					impiri += rewritePrefix + "/";
				if (oi == null)
					// Proprio non riesci a ottenerla questa ontologia? Rinuncia
					continue;
				if (oi.isAnonymous())
					impiri = imp.getIRI().toString();
				else
					impiri += oi.getOntologyID().getOntologyIRI();
				OWLImportsDeclaration im = df.getOWLImportsDeclaration(IRI
						.create(impiri));
				changes.add(new AddImport(copy, im));
			}
			tmpmgr.applyChanges(changes);
		} catch (OWLOntologyCreationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (copy != null)
			o2 = copy;
		else
			o2 = ont;

		if (format.equals(KReSFormat.RDF_XML)) {
			try {
				tmpmgr.saveOntology(o2, new RDFXMLOntologyFormat(), tgt);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (format.equals(KReSFormat.OWL_XML)) {
			try {
				tmpmgr.saveOntology(o2, new OWLXMLOntologyFormat(), tgt);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (format.equals(KReSFormat.MANCHESTER_OWL)) {
			try {
				tmpmgr.saveOntology(o2,
						new ManchesterOWLSyntaxOntologyFormat(), tgt);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (format.equals(KReSFormat.FUNCTIONAL_OWL)) {
			try {
				tmpmgr.saveOntology(o2,
						new OWLFunctionalSyntaxOntologyFormat(), tgt);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (format.equals(KReSFormat.TURTLE)) {
			try {
				tmpmgr.saveOntology(o2, new TurtleOntologyFormat(), tgt);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return tgt.toString();
	}

}
