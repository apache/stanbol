package org.apache.stanbol.reasoners.jena.filters;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.Filter;

/**
 * A filter to get only statements with the given property
 */
public class PropertyFilter extends Filter<Statement> {
    private Property property;

    public PropertyFilter(Property property) {
        this.property = property;
    }

    @Override
    public boolean accept(Statement statement) {
        /**
         * Only statements with the given property
         */
        return statement.getPredicate().equals(property);
    }
}
