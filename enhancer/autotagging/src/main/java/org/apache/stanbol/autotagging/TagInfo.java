package org.apache.stanbol.autotagging;


/**
 * Simple data transfer object to hold the results of the Autotagger annotation
 * process. This then can be mapped to a very simple RDF graph to publish the
 * results annotations to third party applications.
 *
 * @author ogrisel
 */
public class TagInfo {

    /**
     * Unique ID of the entity that is related to the text content. This is
     * typically the DBpedia unique URI of the entity.
     */
    private final String id;

    /**
     * Human readable label (or name) of the related entity.
     */
    private final String label;

    /**
     * Measure of the estimated quality of the suggestion, the bigger, the
     * better. The actual range of values is data and implementation specific.
     */
    private final Double confidence;

    /**
     * List of types of the related entity. This typically a list of owl:Class
     * from the DBpedia ontology (e.g. 'http://dbpedia.org/ontology/Person').
     */
    private final String[] type;

    public TagInfo(String id, String label, String[] type, double confidence) {
        if (id == null){
            throw new IllegalArgumentException("Parameter id MUST NOT be NULL");
        }
        this.id = id;
        this.label = label;
        this.type = type;
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return String.format("%s [%f]", label, confidence);
    }
    /**
     * Checks for != null, instanceof TagInfor and equals id
     */
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof TagInfo && ((TagInfo)obj).id.equals(id) && ((TagInfo)obj).confidence.equals(confidence);
    }

    public final String getId() {
        return id;
    }

    public final String getLabel() {
        return label;
    }

    public final Double getConfidence() {
        return confidence;
    }

    public final String[] getType() {
        return type;
    }

    /**
     * Implementation based on the id and confidence property
     */
    @Override
    public int hashCode() {
        return id.hashCode() + confidence.hashCode();
    }

}
