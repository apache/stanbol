package org.stlab.xd.registry.models;


import java.util.ArrayList;


public class RegistryLibrary extends AbstractRegistryItem {
	
	private ArrayList<AbstractRegistryItem> children;

	public RegistryLibrary(String name) {
		super(name);
		children = new ArrayList<AbstractRegistryItem>();
	}

	public void addChild(AbstractRegistryItem child) {
		children.add(child);
		child.setParent(this);
	}

	public void removeChild(RegistryItem child) {
		children.remove(child);
		child.setParent(null);
	}

	public void removeChildren(){
		children = new ArrayList<AbstractRegistryItem>();
	}
	public RegistryItem[] getChildren() {
		return children
				.toArray(new AbstractRegistryItem[children.size()]);
	}

	public boolean hasChildren() {
		return children.size() > 0;
	}

	@Override
	public boolean isLibrary() {
		return true;
	}

	@Override
	public boolean isOntology() {
		return false;
	}

}
