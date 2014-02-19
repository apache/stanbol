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
package org.apache.stanbol.ontologymanager.store.clerezza;

import java.util.ArrayList;
import java.util.List;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.jena.facade.JenaGraph;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.store.api.JenaPersistenceProvider;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

@Component
@Service
public class ClerezzaAdaptor implements JenaPersistenceProvider {

    Logger logger = LoggerFactory.getLogger(ClerezzaAdaptor.class);

    @Reference
    private TcManager tcManager;

    @Activate
    public void activate(ComponentContext cc) {

    }

    @Deactivate
    public void deactivate(ComponentContext cc) {

    }

    @Override
    public boolean clear() {
        try {
            for (UriRef uri : tcManager.listMGraphs()) {
                tcManager.deleteTripleCollection(uri);
            }
            return true;
        } catch (Exception e) {
            logger.error("Can not clear triple store", e);
        }
        return false;
    }

    @Override
    public boolean commit(Model model) {
        if (model != null && model.supportsTransactions()) {
            model.commit();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Model createModel(String ontologyURI) {
        MGraph graph = null;
        if (hasModel(ontologyURI)) {
            removeModel(ontologyURI);
            graph = tcManager.getMGraph(new UriRef(ontologyURI));
        } else {
            graph = tcManager.createMGraph(new UriRef(ontologyURI));

        }
        JenaGraph jenaGraph = new JenaGraph(graph);
        Model model = ModelFactory.createModelForGraph(jenaGraph);
        return model;
    }

    @Override
    public Model getModel(String ontologyURI) {
        MGraph graph = tcManager.getMGraph(new UriRef(ontologyURI));
        JenaGraph jenaGraph = new JenaGraph(graph);
        Model model = ModelFactory.createModelForGraph(jenaGraph);
        return model;
    }

    @Override
    public boolean hasModel(String ontologyURI) {
        boolean res = tcManager.listMGraphs().contains(new UriRef(ontologyURI));
        return res;
    }

    @Override
    public List<String> listModels() {
        List<String> modelURIs = new ArrayList<String>();
        for (UriRef uri : tcManager.listMGraphs()) {
            modelURIs.add(uri.getUnicodeString());
        }
        return modelURIs;
    }

    @Override
    public void removeModel(String ontologyURI) {
        tcManager.deleteTripleCollection(new UriRef(ontologyURI));
    }

    protected void bindTcManager(TcManager tcManager) {
        this.tcManager = tcManager;
    }

    protected void unbindTcManager(TcManager tcManager) {
        synchronized (tcManager) {
            this.tcManager = null;
        }
    }
}
