package org.apache.stanbol.enhancer.servicesapi;

import org.apache.stanbol.enhancer.servicesapi.helper.Rdf;

@Rdf(id="http://fise.iks-project.eu/ontology/TextAnnotation")
public interface TextAnnotation extends Enhancement {

    @Rdf(id="http://fise.iks-project.eu/ontology/start")
    Integer getStart();
    @Rdf(id="http://fise.iks-project.eu/ontology/start")
    void setStart(Integer start);

    @Rdf(id="http://fise.iks-project.eu/ontology/end")
    Integer getEnd();
    @Rdf(id="http://fise.iks-project.eu/ontology/end")
    void setEnd(Integer end);

    @Rdf(id="http://fise.iks-project.eu/ontology/selected-text")
    String getSelectedText();
    @Rdf(id="http://fise.iks-project.eu/ontology/selected-text")
    void setSelectedText(String selectedText);

    @Rdf(id="http://fise.iks-project.eu/ontology/selection-context")
    String getSelectionContext();
    @Rdf(id="http://fise.iks-project.eu/ontology/selection-context")
    void setSelectionContext(String selectionContext);
}
