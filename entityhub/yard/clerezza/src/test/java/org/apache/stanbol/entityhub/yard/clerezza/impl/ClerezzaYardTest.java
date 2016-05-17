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
package org.apache.stanbol.entityhub.yard.clerezza.impl;

import junit.framework.Assert;

import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.entityhub.core.yard.SimpleYardConfig;
import org.apache.stanbol.entityhub.core.yard.AbstractYard.YardConfig;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.test.yard.YardTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClerezzaYardTest extends YardTest {
    
    private static Yard yard;
    
    @BeforeClass
    public static final void initYard(){
        ClerezzaYardConfig config = new ClerezzaYardConfig("testYardId");
        config.setName("Clerezza Yard Test");
        config.setDescription("The Clerezza Yard instance used to execute the Unit Tests defined for the Yard Interface");
        yard = new ClerezzaYard(config);
    }
    
    @Override
    protected Yard getYard() {
        return yard;
    }
    
    /**
     * The Clerezza Yard uses the Statement<br>
     * <code>representationId -> rdf:type -> Representation</code><br>
     * to identify that an IRI in the RDF graph (Graph) represents a
     * Representation. This Triple is added when a Representation is stored and
     * removed if retrieved from the Yard.<p>
     * This tests if this functions as expected
     * @throws YardException
     */
    @Test
    public void testRemovalOfTypeRepresentationStatement() throws YardException {
        Yard yard = getYard();
        ValueFactory vf = yard.getValueFactory();
        Reference representationType = vf.createReference(RdfResourceEnum.Representation.getUri());
        Representation test = create();
        //the rdf:type Representation MUST NOT be within the Representation
        Assert.assertFalse(test.get(RDF.type.getUnicodeString()).hasNext());
        //now add the statement and see if an IllegalStateException is thrown
        /*
         * The triple within this Statement is internally used to "mark" the
         * URI of the Representation as 
         */
        test.add(RDF.type.getUnicodeString(), representationType);
    }
    /**
     * This Method removes all Representations create via {@link #create()} or
     * {@link #create(String, boolean)} from the tested {@link Yard}.
     * It also removes all Representations there ID was manually added to the
     * {@link #representationIds} list.
     */
    @AfterClass
    public static final void clearUpRepresentations() throws YardException {
        yard.remove(representationIds);
    }
    
}
