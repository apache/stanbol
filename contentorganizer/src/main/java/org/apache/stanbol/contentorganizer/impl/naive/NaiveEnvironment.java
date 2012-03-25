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
package org.apache.stanbol.contentorganizer.impl.naive;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.FOAF;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphSource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.registry.api.RegistryContentException;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.io.LibrarySource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to setup dummy data.
 * 
 * @author alexdma
 * 
 */
public class NaiveEnvironment {

    private final String DEFAULT_REGISTRY_NS = "http://stanbol.apache.org/ontologies/registries/stanbol_network/";

    private final String THIS_ONTOLOGY = "http://stanbol.apache.org/ontologies/cms/network";

    private final String ME = "http://people.apache.org/~alexdma";

    private final String CHRIS_LINKEDIN = "http://de.linkedin.com/in/chrisbizer";

    private final String CHRIS = "http://revyu.com/people/ChrisBizer";

    private final String TOM = "http://tomheath.com/id/me";

    private final String TOM_LINKEDIN = "http://uk.linkedin.com/in/tomheath";

    private final String ME_LINKEDIN = "http://it.linkedin.com/pub/alessandro-adamou/12/379/345";

    private final String ME_1STDEG = "http://it.linkedin.com/pub/alessandro-adamou/connections/1";

    private Logger log = LoggerFactory.getLogger(getClass());

    private ONManager onMgr;

    private RegistryManager regMgr;

    public NaiveEnvironment(ONManager onMgr, RegistryManager regMgr) {
        this.onMgr = onMgr;
        this.regMgr = regMgr;
    }

    public void activate() {
        OntologyScopeFactory factory = onMgr.getOntologyScopeFactory();
        OntologyScope scSocial = null;
        try {
            scSocial = factory.createOntologyScope("social",
                new LibrarySource(IRI.create(DEFAULT_REGISTRY_NS + "SocialNetworks"), regMgr));
            onMgr.getScopeRegistry().registerScope(scSocial, true);
        } catch (DuplicateIDException e) {
            scSocial = onMgr.getScopeRegistry().getScope("social");
            onMgr.getScopeRegistry().setScopeActive("social", true);
        } catch (RegistryContentException e) {
            log.error("Cannot access SocialNetworks library. ", e);
        } catch (OWLOntologyCreationException e) {
            log.error("Cannot create parent ontology. ", e);
        }
        UriRef me = new UriRef(ME), me_linkedin = new UriRef(ME_LINKEDIN), me_1st = new UriRef(ME_1STDEG), tom = new UriRef(
                TOM), tom_linkedin = new UriRef(TOM_LINKEDIN), chris = new UriRef(CHRIS), chris_linkedin = new UriRef(
                CHRIS_LINKEDIN);
        MGraph mg = new IndexedMGraph();
        mg.add(new TripleImpl(new UriRef(THIS_ONTOLOGY), RDF.type, OWL.Ontology));

        // TODO get rid of the following once we get cross-imports working
        mg.add(new TripleImpl(FOAF.holdsAccount, RDF.type, OWL.ObjectProperty));
        mg.add(new TripleImpl(SIOC.member_of, RDF.type, OWL.ObjectProperty));
        mg.add(new TripleImpl(SIOC.owner_of, RDF.type, OWL.ObjectProperty));

        mg.add(new TripleImpl(me, RDF.type, FOAF.Person));
        // mg.add(new TripleImpl(me_linkedin, RDF.type, OWL.Thing));
        mg.add(new TripleImpl(me, FOAF.holdsAccount, me_linkedin));

        mg.add(new TripleImpl(tom, RDF.type, FOAF.Person));
        // mg.add(new TripleImpl(tom_linkedin, RDF.type, OWL.Thing));
        mg.add(new TripleImpl(tom, FOAF.holdsAccount, tom_linkedin));

        mg.add(new TripleImpl(chris, RDF.type, FOAF.Person));
        mg.add(new TripleImpl(chris, FOAF.holdsAccount, chris_linkedin));

        // mg.add(new TripleImpl(me_1st, RDF.type, OWL.Thing));
        mg.add(new TripleImpl(me_linkedin, SIOC.owner_of, me_1st));
        mg.add(new TripleImpl(chris_linkedin, SIOC.member_of, me_1st));

        try {
            scSocial.getCustomSpace().addOntology(new GraphSource(mg));
        } catch (UnmodifiableOntologyCollectorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void deactivate() {

    }

}
