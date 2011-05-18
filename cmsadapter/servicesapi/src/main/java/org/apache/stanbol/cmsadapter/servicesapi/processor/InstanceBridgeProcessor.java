package org.apache.stanbol.cmsadapter.servicesapi.internal;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.InstanceBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;

public interface InstanceBridgeProcessor {
	void executeInstanceBridges(List<InstanceBridge> bridges,
			ConnectionInfo connectionInfo);
}
