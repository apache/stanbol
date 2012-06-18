package org.apache.stanbol.enhancer.jobmanager.defaultRoute;

import org.apache.camel.builder.RouteBuilder;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;

/**
 *  @scr.component immediate="true"
 *  @scr.service
 *
 */
//public class FileRoute extends StanbolRouteBuilder {
public class FileRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		//take data from chain endpoint "fileroute"
		from("direct://metaxa")
		.to("engine://org.apache.stanbol.enhancer.engines.metaxa.MetaxaEngine");
		//.to("file:///tmp/chainoutput/");
		
		from("direct://metaxa2")
		.to("engine://org.apache.stanbol.enhancer.engines.metaxa.MetaxaEngine")
		.to("engine://org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine")
		.to("file:///tmp/chainoutput/?fileName=metaxa2-${date:now:yyyyMMdd-hhmmss}.rdf");
		
		from("direct://chainlink")
		.to("engine://org.apache.stanbol.enhancer.engines.metaxa.MetaxaEngine")
		.to("direct://"+EnhancementJobManager.DEFAULT_CHAIN_NAME);
		
		from("file:///tmp/chaininput/")
		.convertBodyTo(ContentItem.class)
		.to("engine://org.apache.stanbol.enhancer.engines.metaxa.MetaxaEngine")
		.to("file:///tmp/chainoutput/?fileName=${file:onlyname.noext}.rdf");
	}
}
