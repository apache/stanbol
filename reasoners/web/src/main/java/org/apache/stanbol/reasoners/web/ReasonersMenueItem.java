package org.apache.stanbol.reasoners.web;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

/**
 * The menue item for the Stanbol Reasoners component
 */
@Component
@Service(value=NavigationLink.class)
public class ReasonersMenueItem extends NavigationLink {

    private static final String NAME = "reasoners";

    private static final String htmlDescription = 
            "The entry point to multiple <strong>reasoning services</strong> that are used for"+
            "obtaining unexpressed additional knowledge from the explicit axioms in an ontology."+
            "Multiple reasoning profiles are available, each with its expressive power and computational cost.";

	public ReasonersMenueItem() {
		super(NAME, "/"+NAME, htmlDescription, 50);
	}
	
}
