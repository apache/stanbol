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
package org.apache.stanbol.ontologymanager.web.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.owl.OWLOntologyManagerFactory;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
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
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * Contains hacks to regular ontology renderers with replacements for input statements.
 * 
 * @author alexdma
 *
 */
public class OntologyRenderUtils {

	/**
	 * TODO : make a writer for this.
	 * 
	 * @param ont
	 * @param format
	 * @return
	 * @throws OWLOntologyStorageException
	 */
    @Deprecated
	public static String renderOntology(OWLOntology ont,
			OWLOntologyFormat format, String rewritePrefix, ONManager onm)
			throws OWLOntologyStorageException {
		OWLOntologyManager tmpmgr = OWLOntologyManagerFactory.createOWLOntologyManager(null);
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

	@Deprecated
	public static String renderOntology(OWLOntology ont, String format,
			String rewritePrefix, ONManager onm)
			throws OWLOntologyStorageException {
		OWLOntologyManager tmpmgr = OWLOntologyManagerFactory.createOWLOntologyManager(null);
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

		if (format.equals(KRFormat.RDF_XML)) {
			try {
				tmpmgr.saveOntology(o2, new RDFXMLOntologyFormat(), tgt);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (format.equals(KRFormat.OWL_XML)) {
			try {
				tmpmgr.saveOntology(o2, new OWLXMLOntologyFormat(), tgt);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (format.equals(KRFormat.MANCHESTER_OWL)) {
			try {
				tmpmgr.saveOntology(o2,
						new ManchesterOWLSyntaxOntologyFormat(), tgt);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (format.equals(KRFormat.FUNCTIONAL_OWL)) {
			try {
				tmpmgr.saveOntology(o2,
						new OWLFunctionalSyntaxOntologyFormat(), tgt);
			} catch (OWLOntologyStorageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (format.equals(KRFormat.TURTLE)) {
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
