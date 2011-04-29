package org.apache.stanbol.entityhub.indexing.core.normaliser;

import java.util.Map;


/**
 * Uses {@link Math#log1p(double)} to normalise parsed scores.
 * @author Rupert Westenthaler
 */
public class NaturalLogNormaliser implements ScoreNormaliser {

    private ScoreNormaliser normaliser;
    @Override
    public Float normalise(Float score) {
        if(normaliser != null){
            score = normaliser.normalise(score);
        }
        if(score == null || score.compareTo(ZERO) < 0){
            return score;
        } else {
            return Float.valueOf((float)Math.log1p(score.doubleValue()));
        }
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
