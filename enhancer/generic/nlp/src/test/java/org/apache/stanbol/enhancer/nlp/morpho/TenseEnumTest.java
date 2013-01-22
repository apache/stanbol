/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
