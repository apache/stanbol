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
package org.apache.stanbol.ontologymanager.ontonet.impl.util;

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
