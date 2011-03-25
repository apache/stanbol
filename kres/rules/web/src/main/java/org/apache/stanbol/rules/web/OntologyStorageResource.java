package org.apache.stanbol.rules.web;

import org.apache.stanbol.kres.jersey.resource.NavigationMixin;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.OntologyStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/graphs/{graphid:.+}")
public class OntologyStorageResource extends NavigationMixin {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private OntologyStorage storage;
	private ONManager onManager;
	private TcManager tcManager;
	
	public OntologyStorageResource(@Context ServletContext servletContext) {
		storage  = (OntologyStorage) (servletContext.getAttribute(OntologyStorage.class.getName()));
		onManager = (ONManager) (servletContext.getAttribute(ONManager.class.getName()));
		tcManager = (TcManager) (servletContext.getAttribute(TcManager.class.getName()));
        if (storage == null) {
            throw new IllegalStateException(
                    "OntologyStoreProvider missing in ServletContext");
        }
    }
	
	
	
}
