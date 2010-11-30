package eu.iksproject.kres.api.manager.session;

/**
 * An event that encompasses a change in the state of a KReS session.
 * 
 * @author alessandro
 * 
 */
public class SessionEvent {

	public static enum OperationType {
		ACTIVATE, CLOSE, CREATE, DEACTIVATE, KILL, STORE;
	};

	/**
	 * The KReS session affected by this event.
	 */
	private KReSSession affectedSession;

	private OperationType operationType;

	public OperationType getOperationType() {
		return operationType;
	}

	/**
	 * Creates a new instance of SessionEvent.
	 * 
	 * @param session
	 *            the KReS session affected by this event
	 */
	public SessionEvent(KReSSession session, OperationType operationType)
			throws Exception {
		if (operationType == null)
			throw new Exception(
					"No operation type specified for this session event.");
		if (session == null)
			throw new Exception(
					"No KReS session specified for this session event.");
		this.operationType = operationType;
		this.affectedSession = session;
	}

	/**
	 * Returns the KReS session affected by this event.
	 * 
	 * @return the affected KReS session
	 */
	public KReSSession getSession() {
		return affectedSession;
	}

}
