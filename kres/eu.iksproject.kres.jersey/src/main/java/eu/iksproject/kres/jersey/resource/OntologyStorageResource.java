package eu.iksproject.kres.jersey.resource;

import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.ontologymanager.store.api.OntologyStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/graphs/{graphid:.+}")
public class OntologyStorageResource extends NavigationMixin {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private OntologyStoreProvider ontologyStoreProvider;
	private KReSONManager onManager;
	private TcManager tcManager;
	
	public OntologyStorageResource(@Context ServletContext servletContext) {
		ontologyStoreProvider  = (OntologyStoreProvider) (servletContext.getAttribute(OntologyStoreProvider.class.getName()));
		onManager = (KReSONManager) (servletContext.getAttribute(KReSONManager.class.getName()));
		tcManager = (TcManager) (servletContext.getAttribute(TcManager.class.getName()));
        if (ontologyStoreProvider == null) {
            throw new IllegalStateException(
                    "OntologyStoreProvider missing in ServletContext");
        }
    }
	
	
	
}
