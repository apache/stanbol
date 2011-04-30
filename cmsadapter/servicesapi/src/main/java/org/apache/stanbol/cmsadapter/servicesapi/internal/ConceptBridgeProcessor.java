package org.apache.stanbol.cmsadapter.servicesapi.internal;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.ConceptBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;

public interface ConceptBridgeProcessor {
	void processConceptBridges(List<ConceptBridge> bridges,
			ConnectionInfo connectionInfo);
}
