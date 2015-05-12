package org.apache.stanbol.rules.web;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

/**
 * The menue item for the Stanbol Rules component
 */
@Component
@Service(value=NavigationLink.class)
public class RulesMenueItem extends NavigationLink {

    private static final String NAME = "rules";

    private static final String htmlDescription = 
            "This is the implementation of Stanbol Rules which can be used both "+
            "for <strong>reasoning</strong> and <strong>refactoring</strong>";

	public RulesMenueItem() {
		super(NAME, "/"+NAME, htmlDescription, 50);
	}
	
}
