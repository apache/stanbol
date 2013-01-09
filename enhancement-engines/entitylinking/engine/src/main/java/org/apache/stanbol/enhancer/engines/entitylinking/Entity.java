package org.apache.stanbol.enhancer.engines.entitylinking;

import java.util.Iterator;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.TransformIterator;

/**
 * An Entity as returned by the {@link EntitySearcher} interface.
 * {@link EntitySearcher} implementations that do support rankings for
 * entities SHOULD override the {@link #getEntityRanking()} method.
 */
public class Entity {

    protected static final LiteralFactory lf = LiteralFactory.getInstance();
    
    protected static final Transformer TRIPLE2OBJECT = new Transformer() {
        @Override
        public Object transform(Object input) {
            return ((Triple)input).getObject();
        }
    };
    protected static final Predicate PLAIN_LITERALS = PredicateUtils.instanceofPredicate(PlainLiteral.class);
    protected static final Predicate TYPED_LITERALS = PredicateUtils.instanceofPredicate(TypedLiteral.class);
    protected static final Predicate REFERENCES = PredicateUtils.instanceofPredicate(UriRef.class);
    /**
     * The URI of the Entity
     */
     protected final UriRef uri;
    /**
     * The data of the Entity. The graph is expected to contain all information
     * of the entity by containing {@link Triple}s that use the {@link #uri} as
     * {@link Triple#getSubject() subject}
     */
    protected final MGraph data;
    
    /**
     * Constructs a new Entity
     * @param uri
     * @param data
     */
    public Entity(UriRef uri, MGraph data) {
        this.uri = uri;
        this.data = data;
    }
    public UriRef getUri() {
        return uri;
    }
    public String getId(){
        return uri.getUnicodeString();
    }
    public MGraph getData() {
        return data;
    }
    @SuppressWarnings("unchecked")
    public Iterator<PlainLiteral> getText(UriRef field) {
        return new FilterIterator(new TransformIterator(data.filter(uri, field, null), TRIPLE2OBJECT), PLAIN_LITERALS);
    }
    @SuppressWarnings("unchecked")
    public Iterator<UriRef> getReferences(UriRef field){
        return new FilterIterator(new TransformIterator(data.filter(uri, field, null), TRIPLE2OBJECT), REFERENCES);
    }
    
    /**
     * The ranking for the entity in the range [0..1] or <code>null</code> 
     * if not support.<p>
     * This default implementation will returns <code>null</code>
     * @return returns <code>null</code> as this default implementation
     * does not support entity rankings
     */
    public Float getEntityRanking(){
        return null;
    }
}