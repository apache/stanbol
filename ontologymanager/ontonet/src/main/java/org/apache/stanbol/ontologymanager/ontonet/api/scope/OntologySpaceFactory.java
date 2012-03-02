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
package org.apache.stanbol.ontologymanager.ontonet.api.scope;

import org.apache.stanbol.ontologymanager.ontonet.api.NamedResource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;

/**
 * An ontology space factory is responsible for the creation of new, readily specialized ontology spaces from
 * supplied ontology input sources.
 * 
 * Implementations should not call the setup method of the ontology space once it is created, so that it is
 * not locked from editing since creation time.
 * 
 * @author alexdma
 */
public interface OntologySpaceFactory extends NamedResource {

    /**
     * Creates and sets up a default core ontology space. Equivalent to calling
     * <code>createOntologySpace(IRI, SpaceTypes.CORE, OntologyInputSource...)</code>.
     * 
     * @param scopeId
     *            the unique identifier of the ontology scope that will reference this space. It can be used
     *            for generating the identifier for this ontology space.
     * @param coreSources
     *            the sources of the optional ontologies to be immediately loaded upon space creation.
     * @return the generated ontology space.
     */
    CoreOntologySpace createCoreOntologySpace(String scopeId, OntologyInputSource<?,?>... coreSources);

    /**
     * Creates and sets up a default custom ontology space. Equivalent to calling
     * <code>createOntologySpace(IRI, SpaceTypes.CUSTOM, OntologyInputSource...)</code>.
     * 
     * @param scopeId
     *            the unique identifier of the ontology scope that will reference this space. It can be used
     *            for generating the identifier for this ontology space.
     * @param customSources
     *            the sources of the optional ontologies to be immediately loaded upon space creation.
     * @return the generated ontology space.
     */
    CustomOntologySpace createCustomOntologySpace(String scopeId, OntologyInputSource<?,?>... customSources);

    /**
     * Creates an ontology space of the specified type.
     * 
     * @param scopeId
     *            the unique identifier of the ontology scope that will reference this space. It can be used
     *            for generating the identifier for this ontology space.
     * @param type
     *            the space type.
     * @param ontologySources
     *            the sources of the optional ontologies to be immediately loaded upon space creation.
     * @return the generated ontology space.
     */
    OntologySpace createOntologySpace(String scopeId,
                                      SpaceType type,
                                      OntologyInputSource<?,?>... ontologySources);

    /**
     * Creates and sets up a default session ontology space. Equivalent to calling
     * <code>createOntologySpace(IRI, SpaceTypes.SESSION, OntologyInputSource...)</code>.
     * 
     * @deprecated session spaces should no longer exist, as session data should be loaded in {@link Session}
     *             objects. Therefore, implementations of this method are expected to throw an
     *             {@link UnsupportedOperationException}.
     * 
     * @param scopeId
     *            the unique identifier of the ontology scope that will reference this space. It can be used
     *            for generating the identifier for this ontology space.
     * @param sessionSources
     *            the sources of the optional ontologies to be immediately loaded upon space creation.
     * @return the generated ontology space.
     */
    SessionOntologySpace createSessionOntologySpace(String scopeId,
                                                    OntologyInputSource<?,?>... sessionSources);

}
