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
package org.apache.stanbol.entityhub.yard.sesame;

import org.junit.Assert;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.test.yard.YardTest;
import org.apache.stanbol.entityhub.yard.sesame.SesameYard;
import org.apache.stanbol.entityhub.yard.sesame.SesameYardConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public class SesameYardTest extends YardTest {
    
    private static SailRepository repo;
    private static SesameYard yard;
    
    @BeforeClass
    public static final void initYard() throws RepositoryException{
        SesameYardConfig config = new SesameYardConfig("testYardId");
        config.setName("Sesame Yard Test");
        config.setDescription("The Sesame Yard instance used to execute the Unit Tests defined for the Yard Interface");
        repo = new SailRepository(new MemoryStore());
        repo.initialize();
        yard = new SesameYard(repo,config);
    }
    
    @Override
    protected Yard getYard() {
        return yard;
    }
    
    /**
     * The Clerezza Yard uses the Statement<br>
     * <code>representationId -> rdf:type -> Representation</code><br>
     * to identify that an UriRef in the RDF graph (MGraph) represents a
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
        Assert.assertFalse(test.get(NamespaceEnum.rdf+"type").hasNext());
        //now add the statement and see if an IllegalStateException is thrown
        /*
         * The triple within this Statement is internally used to "mark" the
         * URI of the Representation as 
         */
        test.add(NamespaceEnum.rdf+"type", representationType);
    }
    /**
     * This Method removes all Representations create via {@link #create()} or
     * {@link #create(String, boolean)} from the tested {@link Yard}.
     * It also removes all Representations there ID was manually added to the
     * {@link #representationIds} list.
     * @throws RepositoryException 
     */
    @AfterClass
    public static final void clearUpRepresentations() throws YardException, RepositoryException {
        yard.remove(representationIds);
        yard.close();
        repo.shutDown();
    }
    
}
