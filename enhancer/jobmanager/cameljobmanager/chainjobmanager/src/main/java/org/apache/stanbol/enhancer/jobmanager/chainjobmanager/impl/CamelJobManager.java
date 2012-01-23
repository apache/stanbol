package org.apache.stanbol.enhancer.jobmanager.chainjobmanager.impl;

import java.io.IOException;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.stanbol.enhancer.jobmanager.chainjobmanager.engineprotocol.EngineComponent;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.osgi.service.component.ComponentContext;


/**
 * Naive EnhancementJobManager implementation that keeps its request queue in
 * memory.
 *
 * @scr.component immediate="true"
 * @scr.service
 *
 * @scr.reference name="Ec"
 * 				  interface="org.apache.camel.Component" 
 * 				  policy="dynamic"
 *
 *@scr.reference name="Route"
 * 				  interface="org.apache.camel.RoutesBuilder" 
 * 				  cardinality="0..n" policy="dynamic"
 */
public class CamelJobManager implements EnhancementJobManager {
	
	EngineComponent ec;
	
	private CamelContext cContext = null;
	
	protected void bindRoute(RoutesBuilder e) throws Exception {
		//StanbolRouteBuilder srb = (StanbolRouteBuilder)e;
		RouteBuilder srb = (RouteBuilder)e;
		cContext.addRoutes(srb);
		for (RouteDefinition rd : srb.getRouteCollection().getRoutes() ){
			cContext.startRoute(rd.getId());
		}
    }
	
	/**
	 * Remove route for the Camel context
	 * @param e : The route builder
	 * @throws Exception 
	 */
	protected void unbindRoute(RoutesBuilder e) throws Exception {
		//StanbolRouteBuilder srb = (StanbolRouteBuilder)e;
		RouteBuilder srb = (RouteBuilder)e;
		for (RouteDefinition routeDefs : srb.getRouteCollection().getRoutes()){
			cContext.stopRoute(routeDefs);
			cContext.removeRouteDefinition(routeDefs);
		}
	}
	
	protected void bindEc(Component e) {
		ec= (EngineComponent)e;
    }

    protected void unbindEc(Component e) {
        ec = null;
    }
    
	@Activate
    public void activate(ComponentContext ce) throws IOException {
		cContext = new OsgiDefaultCamelContext(ce.getBundleContext());

		try {
			cContext.addComponent("engine", ec);
			cContext.start();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	@Deactivate
    public void deactivate(ComponentContext ce) throws Exception {
    	cContext.stop();
    }
	
	public void enhanceContent(ContentItem ci) throws EngineException {
		String routePart = EnhancementJobManager.DEFAULT_CHAIN_NAME;
		enhanceContent(ci,routePart);
	}
    
    @Override
	public void enhanceContent(ContentItem ci, String chain) throws EngineException {
    	//TODO : better integration with REST :
		//http://camel.apache.org/cxfrs.html
    	ProducerTemplate tpl = cContext.createProducerTemplate();
		ContentItem result = tpl.requestBody("direct://"+chain, ci, ContentItem.class);
    }
    

	public List<EnhancementEngine> getActiveEngines() {
		return ec.getEnhancementEngines();
	}

}
