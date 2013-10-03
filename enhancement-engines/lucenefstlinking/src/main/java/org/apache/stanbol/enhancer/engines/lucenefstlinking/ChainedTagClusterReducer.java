package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import org.apache.commons.lang.ArrayUtils;
import org.opensextant.solrtexttagger.TagClusterReducer;
import org.opensextant.solrtexttagger.TagLL;
import org.opensextant.solrtexttagger.Tagger;

/**
 * Allow to use multiple {@link TagClusterReducer} with a {@link Tagger}
 * instance.
 * @author Rupert Westenthaler
 *
 */
public class ChainedTagClusterReducer implements TagClusterReducer {
    
    private final TagClusterReducer[] reducers;

    public ChainedTagClusterReducer(TagClusterReducer... reducers){
        if(reducers == null || reducers == null || ArrayUtils.contains(reducers, null)){
            throw new IllegalArgumentException("The parsed TagClusterReducers MUST NOT"
                + "be NULL an emoty array or contain any NULL element!");
        }
        this.reducers = reducers;
    }

    @Override
    public void reduce(TagLL[] head) {
        for(TagClusterReducer reducer : reducers){
            if(head[0] == null){
                return; //no more tags left
            }
            reducer.reduce(head);
        }

    }

}
