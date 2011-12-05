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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportEvent;
import org.semanticweb.owlapi.model.MissingImportListener;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * <p>
 * This class is intended to manage the OWLOntologyAlreadyExistsException when it is thrown
 * by the OWLOntologyManager as a missing import event.</p>
 * 
 * <p>The OWLOntologyAlreadyExistsException sometimes 
 * occurs while loading ontologies from the owl:import statement. In some
 * cases, duplicate import statements in the ontology network will fail when you want to
 * do not tolerate missing imports. </p>
 * 
 * <p>See this scenario: if you set silent missing
 * import handling to true in the OWLOntologyManager, then the manager will
 * tolerate this unsuccesful tries, but you will not be sure that you have all
 * the ontologies you need. If you set silent missing import handling to false,
 * then the loading of the network will fail if there are duplicate owl:import
 * statements in different places.</p>
 * 
 * @author enridaga
 * 
 */
public class OWLDuplicateSafeLoader {
	/**
	 * <p>
	 * This is the central method that execute the loading.
	 * The OWLOntologyManager method internally used is loadOntology().
	 * The original state of the manager (silent missing import handling) is restored
	 * after the operation.
	 * If the operation fails, an exception is thrown.
	 * Elsewhere, the OWLOntology instance is returned.
	 * </p>
	 * 
	 * @param manager - an OWLOntologyManager
	 * @param location - a string uri to load from  
	 * @return ontology - the OWLOntology object
	 */
	public synchronized OWLOntology load(OWLOntologyManager manager,
			String location) throws OWLOntologyCreationException {
		/**
		 * We save the state of the passed manager
		 */
		boolean stateImportsHandling = manager.isSilentMissingImportsHandling();

		/**
		 * We set the silent missing imports handling to true. (We want to
		 * manage this without considering duplicated import statements)
		 */
		manager.setSilentMissingImportsHandling(true);
		
		/**
		 * we first create a set to save any missing uri as string
		 */
		final Set<String> missing = new HashSet<String>();
		/**
		 * We setup our missing import listener
		 */
		MissingImportListener missingImportListener = new MissingImportListener() {
			@Override
			public void importMissing(MissingImportEvent arg0) {
				if (arg0.getCreationException() instanceof OWLOntologyDocumentAlreadyExistsException) {
					// Simply do not consider already existent ontologies as
					// missing imports
				} else
					missing.add(arg0.getImportedOntologyURI().toString());
			}
		};
		/**
		 * Then we add the import listener, which will remember all the missed
		 * uris
		 */
		manager.addMissingImportListener(missingImportListener);
		
		OWLOntology owlontology=null;
		
		/**
		 * We do the actual loading. Now, if some exception occurs we return it,
		 * since the missing import is the one we are interested to control.
		 */
		try {
			owlontology=manager.loadOntology(IRI.create(location));
		} catch (OWLOntologyCreationException e) {
			throw e;
		}
		
		/**
		 * Now we re-set the original state of the manager
		 */
		manager.setSilentMissingImportsHandling(stateImportsHandling);
		manager.removeMissingImportListener(missingImportListener);
		
		/**
		 * Now - this is the key part - we remove from the list of not available
		 * ontologies the document uris which have been alredy loaded (of course
		 * after this method clients can overwrite such ontology document
		 * location)
		 */
		for (OWLOntology o : manager.getOntologies()) {
			String miss = manager.getOntologyDocumentIRI(o).toString();
			missing.remove(miss);
		}
		/**
		 * Are there any missing imports?
		 */
		int missingImports = missing.size();
		
		/**
		 * If yes, throw an exception!
		 */
		if (missingImports != 0) {
			String[] missuris = new String[missing.size()];
			missuris = missing.toArray(missuris);
			throw new OWLOntologyCreationException(
					"There are missing imports: " + Arrays.toString(missuris));
		}
		/**
		 * If no, just return the filled ontology manager.
		 */
		return owlontology;
	}
}
