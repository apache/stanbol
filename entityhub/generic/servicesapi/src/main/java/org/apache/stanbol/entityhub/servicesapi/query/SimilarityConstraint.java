package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.ArrayList;
import java.util.List;

/**
 * Ensure that results have fields that is contextually similar. The implementation is typically based on a
 * cosine similarity score a normalized vector space of term frequencies - inverse document frequencies as
 * done by the MoreLikeThis feature of Solr for instance.
 * 
 * This type of constraint might not be supported by all the yard implementations. If it is not supported it
 * is just ignored.
 */
public class SimilarityConstraint extends Constraint {

    protected final String context;

    protected final List<String> additionalFields = new ArrayList<String>();

    public SimilarityConstraint(String context) {
        super(ConstraintType.similarity);
        this.context = context;
    }

    public SimilarityConstraint(String context, List<String> additionalFields) {
        super(ConstraintType.similarity);
        this.context = context;
        this.additionalFields.addAll(additionalFields);
    }

    public List<String> getAdditionalFields() {
        return additionalFields;
    }
    
    public String getContext() {
        return context;
    }

}
