package org.apache.stanbol.enhancer.chainjobmanager.weightedchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.camel.Component;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.stanbol.enhancer.jobmanager.chainjobmanager.engineprotocol.EngineComponent;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;

/**
 *  @scr.component immediate="true"
 *  @scr.service
 *  @scr.reference name="Ec"
 * 				  interface="org.apache.camel.Component" 
 * 				  policy="dynamic"
 *
 */
public class WeightedChain extends RouteBuilder {
	
	//Bind Engine Component in order to get all current engines
	EngineComponent ec;
	
	protected void bindEc(Component e) {
		ec= (EngineComponent)e;
    }

    protected void unbindEc(Component e) {
        ec = null;
    }
	
    
    private static final ExecutionOrderComparator executionOrderComparator = new ExecutionOrderComparator();
    
	@Override
	public void configure() throws Exception {
        List<EnhancementEngine> newList = new ArrayList<EnhancementEngine>(ec.getEnhancementEngines()) ;
        Collections.sort(newList,executionOrderComparator);
        
        Iterator<EnhancementEngine> engines = newList.iterator();
        
        RouteDefinition rd = from("direct://"+this.getRouteName()); 
        while (engines.hasNext()) {
        	rd = rd.to("engine://"+engines.next().getClass().getName());
        }
	}

	//Override
	public String getRouteName() {
		return EnhancementJobManager.DEFAULT_CHAIN_NAME;
	}
	
	private static class ExecutionOrderComparator implements Comparator<EnhancementEngine> {

        @Override
        public int compare(EnhancementEngine engine1, EnhancementEngine engine2) {
            Integer order1 = getOrder(engine1);
            Integer order2 = getOrder(engine2);
            //start with the highest number finish with the lowest ...
            return order1 == order2?0:order1<order2?1:-1;
        }

        public Integer getOrder(EnhancementEngine engine){
            if (engine instanceof ServiceProperties){
                Object value = ((ServiceProperties)engine).getServiceProperties().get(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING);
                if (value !=null && value instanceof Integer){
                    return (Integer)value;
                }
            }
            return ServiceProperties.ORDERING_DEFAULT;
        }
    }
}
