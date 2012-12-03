package org.apache.stanbol.enhancer.engines.entityhublinking;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

public class EntityhubEntity extends Entity {
    
    private static RdfValueFactory vf = RdfValueFactory.getInstance();
    private static UriRef entityRanking = new UriRef(RdfResourceEnum.entityRank.getUri());
    
    public EntityhubEntity(Representation rep) {
        super(new UriRef(rep.getId()), 
            (MGraph)vf.toRdfRepresentation(rep).getRdfGraph());
    }
    @Override
    public Float getEntityRanking() {
        return EnhancementEngineHelper.get(data, uri, entityRanking, Float.class, lf);
    }
}