package org.apache.stanbol.enhancer.servicesapi;

import java.util.Collection;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.helper.Rdf;


@Rdf(id="http://fise.iks-project.eu/ontology/EntityAnnotation")
public interface EntityAnnotation extends Enhancement {

    @Rdf(id="http://fise.iks-project.eu/ontology/entity-reference")
    UriRef getEntityReference();
    @Rdf(id="http://fise.iks-project.eu/ontology/entity-reference")
    void setEntityReference(UriRef reference);

    @Rdf(id="http://fise.iks-project.eu/ontology/entity-label")
    String getEntityLabel();
    @Rdf(id="http://fise.iks-project.eu/ontology/entity-label")
    void setEntityLabel(String label);

    @Rdf(id="http://fise.iks-project.eu/ontology/entity-type")
    Collection<UriRef> getEntityTypes();
}
