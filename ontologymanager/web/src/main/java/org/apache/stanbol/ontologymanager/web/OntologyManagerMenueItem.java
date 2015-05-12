package org.apache.stanbol.ontologymanager.web;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

/**
 * The menue item for the Stanbol OntologyManager component
 */
@Component
@Service(value=NavigationLink.class)
public class OntologyManagerMenueItem extends NavigationLink {

    private static final String NAME = "ontonet";

    private static final String htmlDescription = 
            "A <strong>controlled environment</strong> for managing Web ontologies, "+
            "<strong>ontology networks</strong> and user sessions that put them to use.";

	public OntologyManagerMenueItem() {
		super(NAME, "/"+NAME, htmlDescription, 50);
	}
	
}
