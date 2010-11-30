package eu.iksproject.kres.manager.util;

import java.io.PrintStream;

import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;

/**
 * Prints ontology loading events to standard output.
 * 
 * @author alessandro
 * 
 */
public class OntologyLoaderPrinter implements OWLOntologyLoaderListener {

	private PrintStream printer = System.out;

	public OntologyLoaderPrinter() {
		this(System.out);
	}

	public OntologyLoaderPrinter(PrintStream printer) {
		if (printer != null)
			this.printer = printer;
	}

	/*
	 * (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyLoaderListener#finishedLoadingOntology(org.semanticweb.owlapi.model.OWLOntologyLoaderListener.LoadingFinishedEvent)
	 */
	@Override
	public void finishedLoadingOntology(LoadingFinishedEvent arg0) {
		printer.print("KReS :: Loading of registry ontology "
				+ arg0.getDocumentIRI() + " ");
		if (arg0.isSuccessful())
			printer.println("OK");
		else {
			OWLOntologyCreationException ex = arg0.getException();
			if (ex != null) {
				if (ex instanceof OWLOntologyAlreadyExistsException)
					printer.println("EXISTS");
				else if (ex instanceof OWLOntologyCreationIOException)
					printer.println("NOT FOUND");
				else
					printer.println("FAILED");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLOntologyLoaderListener#startedLoadingOntology(org.semanticweb.owlapi.model.OWLOntologyLoaderListener.LoadingStartedEvent)
	 */
	@Override
	public void startedLoadingOntology(LoadingStartedEvent arg0) {
		// System.out.print("KReS :: Loading registry ontology "+arg0.getDocumentIRI()+" ... ");

	}

}
