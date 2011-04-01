package org.apache.stanbol.ontologymanager.ontonet.api.registry.models;

import java.net.URL;



public abstract class AbstractRegistryItem implements RegistryItem {
	private URL url;
	private String name;
	private RegistryLibrary parent;

	public AbstractRegistryItem(String name) {
		setName(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#setURL(java.net.URL)
	 */
	public void setURL(URL url) {
		this.url = url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#getName()
	 */
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#getURL()
	 */
	public URL getURL() {
		return this.url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.stlab.xd.registry.models.RegistryItem#setParent(org.stlab.xd.registry
	 * .models.RegistryLibrary)
	 */
	public void setParent(RegistryLibrary parent) {
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#getParent()
	 */
	public RegistryLibrary getParent() {
		return parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.stlab.xd.registry.models.RegistryItem#setName(java.lang.String)
	 */
	public void setName(String string) {
		this.name = string;
	}
}
