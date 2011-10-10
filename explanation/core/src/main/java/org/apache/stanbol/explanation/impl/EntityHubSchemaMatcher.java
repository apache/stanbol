package org.apache.stanbol.explanation.impl;

import java.net.URI;

import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.apache.stanbol.explanation.api.SchemaMatcher;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;

/**
 * A {@link SchemaMatcher} that uses the Apache Stanbol Entity Hub as a knowledge base connector.
 */
public interface EntityHubSchemaMatcher extends SchemaMatcher<URI,ReferencedSiteManager,Library> {

}
