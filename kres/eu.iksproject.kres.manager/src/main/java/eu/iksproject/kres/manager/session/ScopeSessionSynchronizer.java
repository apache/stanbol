package eu.iksproject.kres.manager.session;

import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.session.KReSSession;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionListener;
import org.semanticweb.owlapi.model.IRI;

public class ScopeSessionSynchronizer implements SessionListener {

	private KReSONManager manager;

	public ScopeSessionSynchronizer(KReSONManager manager) {
		// WARN do not use ONManager here, as it will most probably be
		// instantiated by it.
		this.manager = manager;
	}

	private void addSessionSpaces(IRI sessionId) {
		OntologySpaceFactory factory = manager
				.getOntologySpaceFactory();
		for (OntologyScope scope : manager.getScopeRegistry()
				.getActiveScopes()) {
			scope.addSessionSpace(factory.createSessionOntologySpace(scope
					.getID()), sessionId);
		}
	}

	@Override
	public void sessionChanged(SessionEvent event) {
		// System.err.println("Session " + event.getSession() + " has been "
		// + event.getOperationType());
		KReSSession ses = event.getSession();
		switch (event.getOperationType()) {
		case CREATE:
			ses.addSessionListener(this);
			addSessionSpaces(ses.getID());
			break;
		case CLOSE:
			break;
		case KILL:
			ses.removeSessionListener(this);
			break;
		default:
			break;
		}
	}

}
