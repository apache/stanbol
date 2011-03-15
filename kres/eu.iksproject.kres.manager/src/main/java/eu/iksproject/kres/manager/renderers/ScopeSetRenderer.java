package eu.iksproject.kres.manager.renderers;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.LoggerFactory;
import org.stlab.xd.vocabulary.Vocabulary;

/**
 * Just an attempt. If we like it, make an API out of it.
 * 
 * @author alessandro
 * 
 */
public class ScopeSetRenderer {

	private static OWLDataFactory __factory = OWLManager.getOWLDataFactory();

	private static IRI _scopeIri = IRI
			.create("http://kres.iks-project.eu/ontology/onm/meta.owl#Scope");

	private static OWLClass cScope = __factory.getOWLClass(_scopeIri);

	public static OWLOntology getScopes(Set<OntologyScope> scopes) {

		OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
		OWLOntology ont = null;
		try {
			ont = mgr.createOntology();
		} catch (OWLOntologyCreationException e) {
			LoggerFactory
					.getLogger(ScopeSetRenderer.class)
					.error(
							"KReS :: could not create empty ontology for rendering scopes.",
							e);
			return null;
		}
		List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
		// The ODP metadata vocabulary is always imported.
		// TODO : also import the ONM meta when it goes online.
		additions.add(new AddImport(ont, __factory
				.getOWLImportsDeclaration(Vocabulary.ODPM.getIRI())));
		for (OntologyScope scope : scopes) {
			OWLNamedIndividual iScope = __factory.getOWLNamedIndividual(scope
					.getID());
			OWLAxiom ax = __factory.getOWLClassAssertionAxiom(cScope, iScope);
			additions.add(new AddAxiom(ont, ax));
		}
		mgr.applyChanges(additions);

		return ont;
	}

}
