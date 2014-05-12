package org.apache.stanbol.entityhub.jersey.fragment;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;
/**
 * The {@link NavigationLink} to the Entityhub component
 */
@Component
@Service(value=NavigationLink.class)
public class EntityhubMenueItem extends NavigationLink {
	private static final String htmlDescription = 
			"The <strong>Entityhub</strong> componnet allows users to manage "
			+ "knowledge of the domain of interest. <strong>Referenced Sites"
			+ "</strong> allow to refer remote datasets and/or to provide fast "
			+ "local indexes for such datasets (e.g. as needed for entity linking "
			+ "with the Stanbol Enhancer. <strong>Managed Sites</strong> provide "
			+ "a full CRUD interface for managing data sets.";
	
	public EntityhubMenueItem() {
		super("entityhub", "/entityhub",htmlDescription,30);
	}

}
