package org.apache.stanbol.enhancer.engines.entitycoreference;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

/**
 * 
 * @author Cristian Petroaca
 *
 */
public class MockComponentContext implements ComponentContext {
	
	private final Dictionary<String, Object> properties;

    public MockComponentContext() {
        properties = new Hashtable<String, Object>();
    }

    public MockComponentContext(Dictionary<String, Object> properties) {
        this.properties = properties;
    }
    
	@Override
	public Dictionary<String, Object> getProperties() {
		return properties;
	}

	@Override
	public Object locateService(String name) {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object locateService(String name, ServiceReference reference) {
		return null;
	}

	@Override
	public Object[] locateServices(String name) {
		return null;
	}

	@Override
	public BundleContext getBundleContext() {
		return null;
	}

	@Override
	public Bundle getUsingBundle() {
		return null;
	}

	@Override
	public ComponentInstance getComponentInstance() {
		return null;
	}

	@Override
	public void enableComponent(String name) {
	}

	@Override
	public void disableComponent(String name) {
	}

	@SuppressWarnings("rawtypes")
	@Override
	public ServiceReference getServiceReference() {
		return null;
	}

}
