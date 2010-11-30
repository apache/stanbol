package eu.iksproject.kres.manager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

public class ConfigurationManagement {

	private static OWLDataFactory _df = OWLManager.getOWLDataFactory();

	private static final String[] EMPTY_IRI_ARRAY = new String[0];

	private static final OWLClass cScope = _df.getOWLClass(IRI
			.create("http://kres.iks-project.eu/ontology/meta/onm.owl#Scope"));

	private static final OWLDataProperty activateOnStart = _df
			.getOWLDataProperty(IRI
					.create("http://kres.iks-project.eu/ontology/meta/onm.owl#activateOnStart"));

	public static String[] getScopesToActivate(OWLOntology config) {

		Set<OWLIndividual> scopes = cScope.getIndividuals(config);
		List<String> result = new ArrayList<String>();
		boolean doActivate = false;
		for (OWLIndividual iScope : scopes) {
			Set<OWLLiteral> activate = iScope.getDataPropertyValues(
					activateOnStart, config);

			Iterator<OWLLiteral> it = activate.iterator();
			while (it.hasNext() && !doActivate) {
				OWLLiteral l = it.next();
				if (l.isOWLTypedLiteral())
					doActivate |= Boolean.parseBoolean(l.asOWLTypedLiteral()
							.getLiteral());
			}

			if (iScope.isNamed() && doActivate)
				result.add(((OWLNamedIndividual) iScope).getIRI().toString());
		}

		return result.toArray(EMPTY_IRI_ARRAY);
	}

}
