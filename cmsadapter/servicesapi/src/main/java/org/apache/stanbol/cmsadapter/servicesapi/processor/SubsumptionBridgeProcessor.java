package org.apache.stanbol.cmsadapter.servicesapi.internal;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.SubsumptionBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;

public interface SubsumptionBridgeProcessor {
	void executePropertyBridges(List<SubsumptionBridge> bridges,
			ConnectionInfo connectionInfo);
}
