package org.apache.stanbol.cmsadapter.core.decorated;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ChildObjectDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DChildObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public class DObjectTypeImp implements DObjectType {

	private ObjectTypeDefinition instance;
	private DObjectAdapter factory;
	private RepositoryAccess access;
	private List<DPropertyDefinition> propertyDefinitions;
	private List<DObjectType> parentDefinitions;
	private List<DChildObjectType> childDefinitions;

	public DObjectTypeImp(ObjectTypeDefinition instance,
			DObjectAdapter factory, RepositoryAccess access) {
		this.instance = instance;
		this.factory = factory;
		this.access = access;
	}

	@Override
	public String getID() {
		return instance.getUniqueRef();
	}

	@Override
	public String getName() {
		return instance.getLocalname();
	}

	@Override
	public String getNamespace() {
		return instance.getNamespace();
	}

	@Override
	public List<DPropertyDefinition> getPropertyDefinitions()
			throws RepositoryAccessException {
		if (propertyDefinitions != null) {
			return propertyDefinitions;
		} else {
			List<PropertyDefinition> propDefinitions = access
					.getPropertyDefinitions(instance, factory.getSession());

			propertyDefinitions = new ArrayList<DPropertyDefinition>(
					propDefinitions.size());
			for (PropertyDefinition propDefinition : propDefinitions) {
				propertyDefinitions.add(factory
						.wrapAsDPropertyDefinition(propDefinition));
			}

			return propertyDefinitions;
		}
	}

	@Override
	public List<DObjectType> getParentDefinitions()
			throws RepositoryAccessException {
		if (parentDefinitions != null) {
			return parentDefinitions;
		}

		List<ObjectTypeDefinition> parDefinitions = access
				.getParentTypeDefinitions(instance, factory.getSession());
		propertyDefinitions = new ArrayList<DPropertyDefinition>(
				parDefinitions.size());
		for (ObjectTypeDefinition parentTypeRef : parDefinitions) {
			parentDefinitions.add(factory.wrapAsDObjectType(parentTypeRef));
		}

		return parentDefinitions;
	}

	@Override
	public List<DChildObjectType> getChildDefinitions()
			throws RepositoryAccessException {
		if (childDefinitions != null) {
			return childDefinitions;
		}

		List<ChildObjectDefinition> childDefs = access
				.getChildObjectTypeDefinitions(instance, factory.getSession());

		childDefinitions = new ArrayList<DChildObjectType>(childDefs.size());
		for (ChildObjectDefinition childDef : childDefs) {
			childDefinitions.add(factory.wrapAsDChildObjectType(childDef));
		}

		return childDefinitions;
	}

	@Override
	public ObjectTypeDefinition getInstance() {
		return instance;
	}

}
