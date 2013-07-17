package org.apache.stanbol.enhancer.engines.entitycomention;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;

public interface CoMentionConstants {

    /**
     * The {@link EntityLinkerConfig#NAME_FIELD} uri internally used by the
     * {@link EntityCoMentionEngine}.
     */
    UriRef CO_MENTION_LABEL_FIELD = new UriRef("urn:org.stanbol:enhander.engine.entitycomention:co-mention-label");
    
    /**
     * The {@link EntityLinkerConfig#TYPE_FIELD} uri internally used by the
     * {@link EntityCoMentionEngine}.
     */
    UriRef CO_MENTION_TYPE_FIELD = new UriRef("urn:org.stanbol:enhander.engine.entitycomention:co-mention-type");
}
