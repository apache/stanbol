package org.apache.stanbol.flow.filepoolgraphflow;

import org.apache.camel.builder.RouteBuilder;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;

/**
 *  @scr.component immediate="true"
 *  @scr.service
 *
 */
public class FileRoute extends RouteBuilder {
	
	@Override
	public void configure() throws Exception {
		
		//process files in /tmp/chaininput and write enhancement result in /tmp/chainoutput
		from("file:///tmp/chaininput/")
		.convertBodyTo(ContentItem.class)
		.to("engine://org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine")
		.to("file:///tmp/chainoutput/?fileName=${file:onlyname.noext}.rdf");
		
		//tika enpoint, just process throw tika
		from("direct://tika")
		.to("engine://org.apache.stanbol.enhancer.engines.tika.TikaEngine");
		
		//tika then langID then NER then save the result in /tmp/chainoutput folder
		from("direct://tika2")
		.to("engine://org.apache.stanbol.enhancer.engines.tika.TikaEngine")
		.to("engine://org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine")
		.to("engine://org.apache.stanbol.enhancer.engines.opennlp.impl.NamedEntityExtractionEnhancementEngine")
		.to("file:///tmp/chainoutput/?fileName=metaxa2-${date:now:yyyyMMdd-hhmmss}.rdf");
		
		//tika then default chain link
		from("direct://tika-default")
		.to("engine://org.apache.stanbol.enhancer.engines.tika.TikaEngine")
		.to("direct://"+"default");
		
		
	}
}
