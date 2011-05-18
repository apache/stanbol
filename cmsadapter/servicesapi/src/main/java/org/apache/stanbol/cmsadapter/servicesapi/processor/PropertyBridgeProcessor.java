package org.apache.stanbol.cmsadapter.servicesapi.internal;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.PropertyBridge;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;

public interface PropertyBridgeProcessor {
	void executePropertyBridges(List<PropertyBridge> bridges,
			ConnectionInfo connectionInfo);
}
