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
package org.apache.stanbol.reasoners.hermit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.reasoners.owlapi.AbstractOWLApiReasoningService;
import org.apache.stanbol.reasoners.owlapi.OWLApiReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.annotations.Documentation;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the implementation of {@see OWLApiReasoningService} using the HermiT reasoner
 * 
 * @author enridaga
 */
@Component(immediate = true, metatype = true)
@Service
@Documentation(name = "HermiT OWL 2", description = "This service supports OWL 2 profiles as implemented by the HermiT reasoner.")
public class HermitReasoningService extends AbstractOWLApiReasoningService implements OWLApiReasoningService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String _DEFAULT_PATH = "owl2";

    @Property(name = ReasoningService.SERVICE_PATH, value = _DEFAULT_PATH)
    private String path;

    @Override
    protected OWLReasoner getReasoner(OWLOntology ontology) {
        log.debug("Creating HermiT reasoner: {}",ontology);
        Configuration config = new Configuration();
        config.ignoreUnsupportedDatatypes = true; // This must be true!
        config.throwInconsistentOntologyException = true; // This must be true!
        //config.monitor = new Debugger(null, false);
        log.debug("Configuration: {}, debugger {}",config,config.monitor);
        ReasonerFactory risfactory = new ReasonerFactory();
        log.debug("factory: {}",risfactory);
        OWLReasoner reasoner = null;
        reasoner = risfactory.createReasoner(ontology, config);
        
        log.debug("Reasoner : {}",reasoner);
        if(reasoner == null){
            log.error("Cannot create the reasner!!");
         throw new IllegalArgumentException("Cannot create the reasoner");   
        }
        return reasoner;
    }


    @Override
    public String getPath() {
        return path;
    }

    @Activate
    public void activate(ComponentContext context) {
        this.path = (String) context.getProperties().get(ReasoningService.SERVICE_PATH);
    }
}
