package org.apache.stanbol.entityhub.model.clerezza.utils;

import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.entityhub.core.utils.AdaptingIterator.Adapter;

/**
 * Needed because UriRefs and Literals use the RDF representation for the
 * toString Method
 *
 * @author Rupert Westenthaler
 *
 * @param <T>
 */
public class Resource2StringAdapter<T extends Resource> implements Adapter<T, String> {

    @Override
    public String adapt(T value, Class<String> type) {
        if (value == null) {
            return null;
        } else if (value instanceof UriRef) {
            return ((UriRef) value).getUnicodeString();
        } else if (value instanceof Literal) {
            return ((Literal) value).getLexicalForm();
        } else {
            return value.toString();
        }
    }

}
