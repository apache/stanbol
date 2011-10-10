package org.apache.stanbol.explanation.impl;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.explanation.api.SchemaMatcher;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;

public interface ClerezzaSchemaMatcher extends SchemaMatcher<UriRef,Graph,Library> {

}
