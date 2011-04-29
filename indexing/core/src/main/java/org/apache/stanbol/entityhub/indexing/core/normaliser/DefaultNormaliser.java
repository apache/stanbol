package org.apache.stanbol.entityhub.indexing.core.normaliser;

import java.util.Map;


/**
 * This default implementation returns the parsed value. Intended to be used
 * in cases where parsing <code>null</code> as {@link ScoreNormaliser} is not
 * supported for some reason.
 * @author Rupert Westenthaler
 */
public class DefaultNormaliser implements ScoreNormaliser{

    
    private ScoreNormaliser normaliser;

    @Override
    public Float normalise(Float score) {
        if(normaliser != null){
            score = normaliser.normalise(score);
        }
        return score;
    }

    @Override
    public void setConfiguration(Map<String,Object> config) {
        Object value = config.get(CHAINED_SCORE_NORMALISER);
        if(value != null){
            this.normaliser = (ScoreNormaliser) value;
        }
    }

    @Override
    public ScoreNormaliser getChained() {
        return normaliser;
    }

}
