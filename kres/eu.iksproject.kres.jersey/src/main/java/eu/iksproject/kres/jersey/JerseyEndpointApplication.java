package eu.iksproject.kres.jersey;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import eu.iksproject.kres.jersey.processors.KReSViewProcessor;
import eu.iksproject.kres.jersey.reasoners.ConsistencyCheck;
import eu.iksproject.kres.jersey.reasoners.Classify;
import eu.iksproject.kres.jersey.reasoners.Enrichment;
import eu.iksproject.kres.jersey.manager.Recipe;
import eu.iksproject.kres.jersey.manager.Rule;
import eu.iksproject.kres.jersey.resource.DocumentationResource;
import eu.iksproject.kres.jersey.resource.GraphsResource;
import eu.iksproject.kres.jersey.resource.KReSResource;
import eu.iksproject.kres.jersey.resource.KReSSessionIDResource;
import eu.iksproject.kres.jersey.resource.KReSSessionResource;
import eu.iksproject.kres.jersey.resource.ONMOntResource;
//import eu.iksproject.kres.jersey.resource.LinkDiscoveryResource;
import eu.iksproject.kres.jersey.resource.ProvaResource;
import eu.iksproject.kres.jersey.resource.RESTfulResource;
import eu.iksproject.kres.jersey.resource.ONMRootResource;
import eu.iksproject.kres.jersey.resource.ONMScopeOntologyResource;
import eu.iksproject.kres.jersey.resource.ONMScopeResource;
import eu.iksproject.kres.jersey.resource.OntologyStorageResource;
import eu.iksproject.kres.jersey.resource.RuleStoreResource;
import eu.iksproject.kres.jersey.resource.SemionReengineerResource;
import eu.iksproject.kres.jersey.resource.SemionRefactorerResource;
import eu.iksproject.kres.jersey.writers.GraphWriter;
import eu.iksproject.kres.jersey.writers.OWLOntologyWriter;
import eu.iksproject.kres.jersey.writers.OwlModelWriter;
import eu.iksproject.kres.jersey.writers.ResultSetWriter;

/**
 * Statically define the list of available resources and providers to be used by
 * the KReS JAX-RS Endpoint.
 * 
 * The jersey auto-scan mechanism does not seem to work when deployed through
 * OSGi's HttpService initialization.
 * 
 * In the future this class might get refactored as an OSGi service to allow for
 * dynamic configuration and deployment of additional JAX-RS resources and
 * providers.
 * 
 * @author andrea.nuzzolese
 */

public class JerseyEndpointApplication extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> classes = new HashSet<Class<?>>();

		// Rules manager
		classes.add(RuleStoreResource.class);
		classes.add(Recipe.class);
		classes.add(Rule.class);

		// Reasoner
		classes.add(ConsistencyCheck.class);
		classes.add(Classify.class);
		classes.add(Enrichment.class);

		// resources
		classes.add(KReSResource.class);
		classes.add(SemionReengineerResource.class);
		classes.add(SemionRefactorerResource.class);
//		classes.add(LinkDiscoveryResource.class);

		classes.add(ONMRootResource.class);
        classes.add(ONMScopeResource.class);
        classes.add(ONMScopeOntologyResource.class);
        classes.add(ONMOntResource.class);
        classes.add(KReSSessionResource.class);
        classes.add(KReSSessionIDResource.class);
        classes.add(GraphsResource.class);
        classes.add(DocumentationResource.class);
        
        
        classes.add(ProvaResource.class);
        
        
/* REST services */
	classes.add(RESTfulResource.class);
/* end rest services */
		// message body writers
		classes.add(GraphWriter.class);
		classes.add(ResultSetWriter.class);
		//classes.add(OwlModelWriter.class);
		classes.add(OWLOntologyWriter.class);
		return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		Set<Object> singletons = new HashSet<Object>();
		// view processors
		singletons.add(new KReSViewProcessor());
		return singletons;
	}

}
