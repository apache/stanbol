package org.apache.stanbol.enhancer.nlp.morpho;

import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Had some problems with the initialization of {@link Tense} enum ...
 * so this rather simple looking test ...
 * @author Rupert Westenthaler
 *
 */
public class TenseEnumTest {

    /**
     * Because the transitive closure can not be initialized
     * in the constructor of the Tense this
     * checkes if they are correctly written to the
     * private static map
     */
    @Test
    public void testTransitiveClosure(){
        for(Tense tense : Tense.values()){
            Set<Tense> transParent = tense.getTenses();
            Tense test = tense;
            while(test != null){
                Assert.assertTrue(transParent.contains(test));
                test = test.getParent();
            }
        }
    }
    
}
