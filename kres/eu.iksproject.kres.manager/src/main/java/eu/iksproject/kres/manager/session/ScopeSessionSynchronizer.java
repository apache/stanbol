package eu.iksproject.kres.manager.session;

import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.session.KReSSession;
import eu.iksproject.kres.api.manager.session.SessionEvent;
import eu.iksproject.kres.api.manager.session.SessionListener;
import eu.iksproject.kres.manager.ONManager;

public class ScopeSessionSynchronizer implements SessionListener {

	private static ScopeSessionSynchronizer me = null;

	public static ScopeSessionSynchronizer get() {
		if (me == null)
			me = new ScopeSessionSynchronizer();
		return me;
	}

	private ScopeSessionSynchronizer() {
		// WARN do not use ONManager here, as it will most probably be
		// instantiated by it.
	}

	private void addSessionSpaces(IRI sessionId) {
		OntologySpaceFactory factory = ONManager.get()
				.getOntologySpaceFactory();
		for (OntologyScope scope : ONManager.get().getScopeRegistry()
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
