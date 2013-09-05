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
package org.apache.stanbol.ontologymanager.servicesapi;

import org.apache.clerezza.rdf.core.UriRef;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

/**
 * An internal representation of the vocabulary that Stanbol uses internally for representing stored
 * ontologies and virtual ontology networks, and restoring them on startup. This vocabulary is not intended to
 * be used for ontologies exposed to the public.
 * 
 * TODO create the objects through reflection after parsing the corresponding OWL schema.
 * 
 * @author alexdma
 * 
 */
public class Vocabulary {

    private static OWLDataFactory __df = OWLManager.getOWLDataFactory();

    /**
     * The default namespace for the Stanbol OntoNet metadata vocabulary
     */
    public static final String _NS_ONTONET = "http://stanbol.apache.org/ontology/meta/ontonet#";

    /**
     * This namespace is used for representing Stanbol resources internally. It should applied to all portable
     * resources that might be moved from one host to another, e.g. scopes and sessions.<br/>
     * <br>
     * This namespace MUST NOT be used for identifying resources in the outside world, e.g. RESTful services:
     * it MUST be converted to the public namespace before exporting.
     */
    public static final String _NS_STANBOL_INTERNAL = "http://stanbol.apache.org/ontology/.internal/";

    private static final String _SHORT_APPENDED_TO = "isAppendedTo";

    private static final String _SHORT_DEPENDS_ON = "dependsOn";

    private static final String _SHORT_ENTRY = "Entry";

    private static final String _SHORT_GRAPH = "Graph";

    private static final String _SHORT_HAS_APPENDED = "hasAppended";

    private static final String _SHORT_HAS_DEPENDENT = "hasDependenct";

    private static final String _SHORT_HAS_ONTOLOGY_IRI = "hasOntologyIRI";

    private static final String _SHORT_HAS_SPACE_CORE = "hasCoreSpace";

    private static final String _SHORT_HAS_SPACE_CUSTOM = "hasCustomSpace";

    private static final String _SHORT_HAS_STATUS = "hasStatus";

    private static final String _SHORT_HAS_VERSION_IRI = "hasVersionIRI";

    private static final String _SHORT_IS_MANAGED_BY = "isManagedBy";

    private static final String _SHORT_IS_MANAGED_BY_CORE = "isManagedByCore";

    private static final String _SHORT_IS_MANAGED_BY_CUSTOM = "isManagedByCustom";

    private static final String _SHORT_IS_SPACE_CORE_OF = "isCoreSpaceOf";

    private static final String _SHORT_IS_SPACE_CUSTOM_OF = "isCustomSpaceOf";

    private static final String _SHORT_MANAGES = "manages";

    private static final String _SHORT_MANAGES_IN_CORE = "managesInCore";

    private static final String _SHORT_MANAGES_IN_CUSTOM = "managesInCustom";

    private static final String _SHORT_MAPS_TO_GRAPH = "mapsToGraph";

    private static final String _SHORT_PRIMARY_ENTRY = "PrimaryEntry";

    private static final String _SHORT_RETRIEVED_FROM = "retrievedFrom";

    private static final String _SHORT_SCOPE = "Scope";

    private static final String _SHORT_SESSION = "Session";

    private static final String _SHORT_SIZE_IN_AXIOMS = "hasSizeInAxioms";

    private static final String _SHORT_SIZE_IN_TRIPLES = "hasSizeInTriples";

    private static final String _SHORT_SPACE = "Space";

    private static final String _SHORT_STATUS = "Status";

    private static final String _SHORT_STATUS_ACTIVE = _SHORT_STATUS + ".ACTIVE";

    private static final String _SHORT_STATUS_INACTIVE = _SHORT_STATUS + ".INACTIVE";

    /**
     * The OWL <b>object property</b> <tt>isAppendedTo</tt>.
     */
    public static final OWLObjectProperty APPENDED_TO = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_APPENDED_TO));

    /**
     * The OWL <b>object property</b> <tt>isAppendedTo</tt> (in UriRef form).
     */
    public static final UriRef APPENDED_TO_URIREF = new UriRef(_NS_ONTONET + _SHORT_APPENDED_TO);

    /**
     * The OWL <b>object property</b> <tt>dependsOn</tt>.
     */
    public static final OWLObjectProperty DEPENDS_ON = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_DEPENDS_ON));

    /**
     * The OWL <b>object property</b> <tt>dependsOn</tt> (in UriRef form).
     */
    public static final UriRef DEPENDS_ON_URIREF = new UriRef(_NS_ONTONET + _SHORT_DEPENDS_ON);

    /**
     * The OWL <b>class</b> <tt>Entry</tt>.
     */
    public static final OWLClass ENTRY = __df.getOWLClass(IRI.create(_NS_ONTONET + _SHORT_ENTRY));

    /**
     * The OWL <b>class</b> <tt>Entry</tt> (in UriRef form).
     */
    public static final UriRef ENTRY_URIREF = new UriRef(_NS_ONTONET + _SHORT_ENTRY);

    /**
     * The OWL <b>class</b> <tt>Graph</tt>.
     */
    public static final OWLClass GRAPH = __df.getOWLClass(IRI.create(_NS_ONTONET + _SHORT_GRAPH));

    /**
     * The OWL <b>class</b> <tt>Graph</tt> (in UriRef form).
     */
    public static final UriRef GRAPH_URIREF = new UriRef(_NS_ONTONET + _SHORT_GRAPH);

    /**
     * The OWL <b>object property</b> <tt>hasAppended</tt>.
     */
    public static final OWLObjectProperty HAS_APPENDED = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_HAS_APPENDED));

    /**
     * The OWL <b>object property</b> <tt>hasAppended</tt> (in UriRef form).
     */
    public static final UriRef HAS_APPENDED_URIREF = new UriRef(_NS_ONTONET + _SHORT_HAS_APPENDED);

    /**
     * The OWL <b>object property</b> <tt>hasDependent</tt>.
     */
    public static final OWLObjectProperty HAS_DEPENDENT = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_HAS_DEPENDENT));

    /**
     * The OWL <b>datatype property</b> <tt>hasDependent</tt> (in UriRef form).
     */
    public static final UriRef HAS_DEPENDENT_URIREF = new UriRef(_NS_ONTONET + _SHORT_HAS_DEPENDENT);

    /**
     * The OWL <b>datatype property</b> <tt>hasOntologyIRI</tt>.
     */
    public static final OWLDataProperty HAS_ONTOLOGY_IRI = __df.getOWLDataProperty(IRI
            .create(_NS_ONTONET + _SHORT_HAS_ONTOLOGY_IRI));

    /**
     * The OWL <b>datatype property</b> <tt>hasOntologyIRI</tt> (in UriRef form).
     */
    public static final UriRef HAS_ONTOLOGY_IRI_URIREF = new UriRef(_NS_ONTONET + _SHORT_HAS_ONTOLOGY_IRI);

    /**
     * The OWL <b>object property</b> <tt>isManagedBy</tt>.
     */
    public static final OWLObjectProperty HAS_SPACE_CORE = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_HAS_SPACE_CORE));

    /**
     * The OWL <b>object property</b> <tt>hasCoreSpace</tt> (in UriRef form).
     */
    public static final UriRef HAS_SPACE_CORE_URIREF = new UriRef(_NS_ONTONET + _SHORT_HAS_SPACE_CORE);

    /**
     * The OWL <b>object property</b> <tt>isManagedBy</tt>.
     */
    public static final OWLObjectProperty HAS_SPACE_CUSTOM = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_HAS_SPACE_CUSTOM));

    /**
     * The OWL <b>object property</b> <tt>hasCustomSpace</tt> (in UriRef form).
     */
    public static final UriRef HAS_SPACE_CUSTOM_URIREF = new UriRef(_NS_ONTONET + _SHORT_HAS_SPACE_CUSTOM);

    /**
     * The OWL <b>object property</b> <tt>hasStatus</tt> (in UriRef form).
     */
    public static final OWLObjectProperty HAS_STATUS = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_HAS_STATUS));

    /**
     * The OWL <b>object property</b> <tt>hasStatus</tt>.
     */
    public static final UriRef HAS_STATUS_URIREF = new UriRef(_NS_ONTONET + _SHORT_HAS_STATUS);

    /**
     * The OWL <b>datatype property</b> <tt>hasVersionIRI</tt>.
     */
    public static final OWLDataProperty HAS_VERSION_IRI = __df.getOWLDataProperty(IRI
            .create(_NS_ONTONET + _SHORT_HAS_VERSION_IRI));

    /**
     * The OWL <b>datatype property</b> <tt>hasVersionIRI</tt> (in UriRef form).
     */
    public static final UriRef HAS_VERSION_IRI_URIREF = new UriRef(_NS_ONTONET + _SHORT_HAS_VERSION_IRI);

    /**
     * The OWL <b>object property</b> <tt>isManagedBy</tt>.
     */
    public static final OWLObjectProperty IS_MANAGED_BY = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_IS_MANAGED_BY));

    /**
     * The OWL <b>object property</b> <tt>isManagedByCore</tt>.
     */
    public static final OWLObjectProperty IS_MANAGED_BY_CORE = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_IS_MANAGED_BY_CORE));

    /**
     * The OWL <b>object property</b> <tt>isManagedByCore</tt> (in UriRef form).
     */
    public static final UriRef IS_MANAGED_BY_CORE_URIREF = new UriRef(_NS_ONTONET + _SHORT_IS_MANAGED_BY_CORE);

    /**
     * The OWL <b>object property</b> <tt>isManagedByCustom</tt>.
     */
    public static final OWLObjectProperty IS_MANAGED_BY_CUSTOM = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_IS_MANAGED_BY_CUSTOM));

    /**
     * The OWL <b>object property</b> <tt>isManagedByCustom</tt> (in UriRef form).
     */
    public static final UriRef IS_MANAGED_BY_CUSTOM_URIREF = new UriRef(_NS_ONTONET
                                                                        + _SHORT_IS_MANAGED_BY_CUSTOM);

    /**
     * The OWL <b>object property</b> <tt>isManagedBy</tt> (in UriRef form).
     */
    public static final UriRef IS_MANAGED_BY_URIREF = new UriRef(_NS_ONTONET + _SHORT_IS_MANAGED_BY);

    /**
     * The OWL <b>object property</b> <tt>isCoreSpaceOf</tt>.
     */
    public static final OWLObjectProperty IS_SPACE_CORE_OF = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_IS_SPACE_CORE_OF));

    /**
     * The OWL <b>object property</b> <tt>isCoreSpaceOf</tt> (in UriRef form).
     */
    public static final UriRef IS_SPACE_CORE_OF_URIREF = new UriRef(_NS_ONTONET + _SHORT_IS_SPACE_CORE_OF);

    /**
     * The OWL <b>object property</b> <tt>isCustomSpaceOf</tt>.
     */
    public static final OWLObjectProperty IS_SPACE_CUSTOM_OF = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_IS_SPACE_CUSTOM_OF));

    /**
     * The OWL <b>object property</b> <tt>isCustomSpaceOf</tt> (in UriRef form).
     */
    public static final UriRef IS_SPACE_CUSTOM_OF_URIREF = new UriRef(_NS_ONTONET + _SHORT_IS_SPACE_CUSTOM_OF);

    /**
     * The OWL <b>object property</b> <tt>manages</tt>.
     */
    public static final OWLObjectProperty MANAGES = __df.getOWLObjectProperty(IRI.create(_NS_ONTONET
                                                                                         + _SHORT_MANAGES));

    /**
     * The OWL <b>object property</b> <tt>managesInCore</tt>.
     */
    public static final OWLObjectProperty MANAGES_IN_CORE = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_MANAGES_IN_CORE));

    /**
     * The OWL <b>object property</b> <tt>managesInCore</tt> (in UriRef form).
     */
    public static final UriRef MANAGES_IN_CORE_URIREF = new UriRef(_NS_ONTONET + _SHORT_MANAGES_IN_CORE);
    /**
     * The OWL <b>object property</b> <tt>managesInCustom</tt>.
     */
    public static final OWLObjectProperty MANAGES_IN_CUSTOM = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_MANAGES_IN_CUSTOM));

    /**
     * The OWL <b>object property</b> <tt>managesInCustom</tt> (in UriRef form).
     */
    public static final UriRef MANAGES_IN_CUSTOM_URIREF = new UriRef(_NS_ONTONET + _SHORT_MANAGES_IN_CUSTOM);

    /**
     * The OWL <b>object property</b> <tt>manages</tt> (in UriRef form).
     */
    public static final UriRef MANAGES_URIREF = new UriRef(_NS_ONTONET + _SHORT_MANAGES);

    /**
     * The OWL <b>object property</b> <tt>mapsToGraph</tt>.
     */
    public static final OWLObjectProperty MAPS_TO_GRAPH = __df.getOWLObjectProperty(IRI
            .create(_NS_ONTONET + _SHORT_MAPS_TO_GRAPH));

    /**
     * The OWL <b>object property</b> <tt>mapsToGraph</tt> (in UriRef form).
     */
    public static final UriRef MAPS_TO_GRAPH_URIREF = new UriRef(_NS_ONTONET + _SHORT_MAPS_TO_GRAPH);

    /**
     * The OWL <b>class</b> <tt>PrimaryEntry</tt>.
     */
    public static final OWLClass PRIMARY_ENTRY = __df.getOWLClass(IRI.create(_NS_ONTONET
                                                                             + _SHORT_PRIMARY_ENTRY));

    /**
     * The OWL <b>class</b> <tt>PrimaryEntry</tt> (in UriRef form).
     */
    public static final UriRef PRIMARY_ENTRY_URIREF = new UriRef(_NS_ONTONET + _SHORT_PRIMARY_ENTRY);

    /**
     * The OWL <b>datatype property</b> <tt>retrievedFrom</tt>.
     */
    public static final OWLDataProperty RETRIEVED_FROM = __df.getOWLDataProperty(IRI
            .create(_NS_ONTONET + _SHORT_RETRIEVED_FROM));

    /**
     * The OWL <b>datatype property</b> <tt>retrievedFrom</tt> (in UriRef form).
     */
    public static final UriRef RETRIEVED_FROM_URIREF = new UriRef(_NS_ONTONET + _SHORT_RETRIEVED_FROM);

    /**
     * The OWL <b>class</b> <tt>Scope</tt>.
     */
    public static final OWLClass SCOPE = __df.getOWLClass(IRI.create(_NS_ONTONET + _SHORT_SCOPE));

    /**
     * The OWL <b>class</b> <tt>Scope</tt> (in UriRef form).
     */
    public static final UriRef SCOPE_URIREF = new UriRef(_NS_ONTONET + _SHORT_SCOPE);

    /**
     * The OWL <b>class</b> <tt>Session</tt>.
     */
    public static final OWLClass SESSION = __df.getOWLClass(IRI.create(_NS_ONTONET + _SHORT_SESSION));

    /**
     * The OWL <b>class</b> <tt>Session</tt> (in UriRef form).
     */
    public static final UriRef SESSION_URIREF = new UriRef(_NS_ONTONET + _SHORT_SESSION);

    /**
     * The OWL <b>datatype property</b> <tt>hasSizeInAxioms</tt>.
     */
    public static final OWLDataProperty SIZE_IN_AXIOMS = __df.getOWLDataProperty(IRI
            .create(_NS_ONTONET + _SHORT_SIZE_IN_AXIOMS));

    /**
     * The OWL <b>datatype property</b> <tt>hasSizeInAxioms</tt> (in UriRef form).
     */
    public static final UriRef SIZE_IN_AXIOMS_URIREF = new UriRef(_NS_ONTONET + _SHORT_SIZE_IN_AXIOMS);

    /**
     * The OWL <b>datatype property</b> <tt>hasSizeInTriples</tt>.
     */
    public static final OWLDataProperty SIZE_IN_TRIPLES = __df.getOWLDataProperty(IRI
            .create(_NS_ONTONET + _SHORT_SIZE_IN_TRIPLES));

    /**
     * The OWL <b>datatype property</b> <tt>hasSizeInTriples</tt> (in UriRef form).
     */
    public static final UriRef SIZE_IN_TRIPLES_URIREF = new UriRef(_NS_ONTONET + _SHORT_SIZE_IN_TRIPLES);

    /**
     * The OWL <b>class</b> <tt>Space</tt>.
     */
    public static final OWLClass SPACE = __df.getOWLClass(IRI.create(_NS_ONTONET + _SHORT_SPACE));

    /**
     * The OWL <b>class</b> <tt>Space</tt> (in UriRef form).
     */
    public static final UriRef SPACE_URIREF = new UriRef(_NS_ONTONET + _SHORT_SPACE);

    /**
     * The OWL <b>class</b> <tt>Status</tt>.
     */
    public static final OWLClass STATUS = __df.getOWLClass(IRI.create(_NS_ONTONET + _SHORT_STATUS));

    /**
     * The OWL <b>individual</b> <tt>Status.ACTIVE</tt>.
     */
    public static final OWLIndividual STATUS_ACTIVE = __df.getOWLNamedIndividual(IRI
            .create(_NS_ONTONET + _SHORT_STATUS_ACTIVE));

    /**
     * The OWL <b>individual</b> <tt>Status.ACTIVE</tt> (in UriRef form).
     */
    public static final UriRef STATUS_ACTIVE_URIREF = new UriRef(_NS_ONTONET + _SHORT_STATUS_ACTIVE);

    /**
     * The OWL <b>individual</b> <tt>Status.INACTIVE</tt>.
     */
    public static final OWLIndividual STATUS_INACTIVE = __df.getOWLNamedIndividual(IRI
            .create(_NS_ONTONET + _SHORT_STATUS_INACTIVE));

    /**
     * The OWL <b>individual</b> <tt>Status.INACTIVE</tt> (in UriRef form).
     */
    public static final UriRef STATUS_INACTIVE_URIREF = new UriRef(_NS_ONTONET + _SHORT_STATUS_INACTIVE);

    /**
     * The OWL <b>class</b> <tt>Status</tt> (in UriRef form).
     */
    public static final UriRef STATUS_URIREF = new UriRef(_NS_ONTONET + _SHORT_STATUS);

}
