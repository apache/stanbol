package org.apache.stanbol.enhancer.jersey.fragment;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

/**
 * The menue item for the Stanbol Enhancer component
 */
@Component
@Service(value=NavigationLink.class)
public class EnhancerMenueItem extends NavigationLink {

	private static final String htmlDescription = 
			"This is a <strong>stateless interface</strong> to allow clients to submit"+
			"content to <strong>analyze</strong> by the <code>EnhancementEngine</code>s"+
			"and get the resulting <strong>RDF enhancements</strong> at once without"+
			"storing anything on the server-side.";

	public EnhancerMenueItem() {
		super("enhancer", "/enhancer", htmlDescription, 10);
	}
	
}
