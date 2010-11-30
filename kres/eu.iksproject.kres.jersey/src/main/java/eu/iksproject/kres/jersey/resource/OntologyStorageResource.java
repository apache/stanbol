package eu.iksproject.kres.jersey.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stlab.xd.registry.io.IRIRegistrySource;

import com.sun.jersey.api.view.ImplicitProduces;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.semion.SemionManager;
import eu.iksproject.kres.api.storage.NoSuchStoreException;
import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.api.storage.OntologyStoreProvider;

import org.semanticweb.owlapi.model.IRI;

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
