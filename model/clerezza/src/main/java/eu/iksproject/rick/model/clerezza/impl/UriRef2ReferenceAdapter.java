package eu.iksproject.rick.model.clerezza.impl;

import org.apache.clerezza.rdf.core.UriRef;

import eu.iksproject.rick.core.utils.AdaptingIterator.Adapter;
import eu.iksproject.rick.model.clerezza.RdfValueFactory;
import eu.iksproject.rick.servicesapi.model.Reference;

/**
 * Adapter that converts Clerezza {@link UriRef} instances to {@link Reference}s.
 * The {@link RdfValueFactory} is used to create {@link Reference} instances.
 * @author Rupert Westenthaler
 *
 */
public class UriRef2ReferenceAdapter implements Adapter<UriRef,Reference> {

    private final RdfValueFactory valueFactory = RdfValueFactory.getInstance();

    @Override
    public Reference adapt(UriRef value, Class<Reference> type) {
        return valueFactory.createReference(value);
    }

}
