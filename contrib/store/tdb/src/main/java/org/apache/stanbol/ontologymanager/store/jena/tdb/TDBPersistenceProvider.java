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
package org.apache.stanbol.ontologymanager.store.jena.tdb;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.store.api.JenaPersistenceProvider;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;

@Component(enabled = true)
@Service
public class TDBPersistenceProvider implements JenaPersistenceProvider {

    private static final Logger logger = LoggerFactory.getLogger(TDBPersistenceProvider.class);

    protected String datasetPath;

    protected Dataset dataset;

    public TDBPersistenceProvider() {

    }

    @Activate
    public void activate(ComponentContext cc) {

        long start = System.currentTimeMillis();
        datasetPath = cc.getBundleContext().getDataFile("Jena TDB Store").getAbsolutePath();
        dataset = TDBFactory.createDataset(datasetPath);
        long end = System.currentTimeMillis();
        logger.info("Dataset restored in {} ms ", (end - start));
    }

    @Deactivate
    public void deactivate(ComponentContext cc) {
        dataset.close();
    }

    @Override
    public boolean clear() {
        Iterator<String> URIs = dataset.listNames();
        while (URIs.hasNext()) {
            dataset.getNamedModel(URIs.next()).removeAll();
        }
        dataset.close();
        File file = new File(datasetPath);
        boolean deleted = file.delete();
        if (deleted) {
            logger.info("Deleted TDB Store at {} ", datasetPath);
        } else {
            logger.warn("Can not delete TDB Store at {} ", datasetPath);
        }
        dataset = TDBFactory.createDataset(datasetPath);
        return true;
    }

    @Override
    public Model createModel(String ontologyURI) {
        if (dataset.containsNamedModel(ontologyURI)) {
            Model model = dataset.getNamedModel(ontologyURI);
            model.removeAll();
            logger.info("Deleted model at  {}", ontologyURI);
        }

        return dataset.getNamedModel(ontologyURI);
    }

    @Override
    public Model getModel(String ontologyURI) {
        if (dataset.containsNamedModel(ontologyURI)) {
            return dataset.getNamedModel(ontologyURI);
        } else {
            return null;
        }
    }

    @Override
    public boolean hasModel(String ontologyURI) {
        return dataset.containsNamedModel(ontologyURI);
    }

    @Override
    public List<String> listModels() {
        List<String> models = new ArrayList<String>();
        try {

            Iterator<String> modelIt = dataset.listNames();
            while (modelIt.hasNext()) {
                models.add(modelIt.next());
            }
        } catch (Exception e) {
            logger.error("Error ", e);
        }
        return models;
    }

    @Override
    public void removeModel(String ontologyURI) {
        if (dataset.containsNamedModel(ontologyURI)) {
            Model model = dataset.getNamedModel(ontologyURI);
            model.removeAll();
        }
    }

    @Override
    public boolean commit(Model model) {
        if (model == null) {
            logger.warn("Can not commit null model");
        }
        if (model.supportsTransactions()) {
            model.commit();
            return true;
        }
        return false;
    }
}
