package org.apache.stanbol.explanation.api;

import java.util.Set;

/**
 * 
 * @author alexdma
 * 
 * @param <E>
 *            the entity to be matched against, or <i><explainable/i>
 * @param <KB>
 *            knowledge base
 * @param <C>
 *            schema library, or <i>catalog</i>.
 */
public interface SchemaMatcher<E,KB,C> {

    void setKnowledgeBase(KB knowledgeBase);

    Set<Schema> getSatisfiableSchemas(Set<C> catalogs, E entity);

}
