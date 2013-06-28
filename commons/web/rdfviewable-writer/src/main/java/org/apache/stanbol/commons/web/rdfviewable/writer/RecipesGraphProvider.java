/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.commons.web.rdfviewable.writer;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(RecipesGraphProvider.class)
public class RecipesGraphProvider implements BundleListener {
    
    private static final String RECIPES_PATH_IN_BUNDLE = "META-INF/graphs/recipes/";
    
    @Reference
    private Parser parser;
    
    private static Logger log = 
            LoggerFactory.getLogger(RecipesGraphProvider.class);
    
    private MGraph recipesGraph = null;
    
    public TripleCollection getRecipesGraph() {
        return recipesGraph;
    }
    
    @Activate
    protected void activate(BundleContext context) {
        //Work around because of STANBOL-1130
        recipesGraph = new SimpleMGraph();//new IndexedMGraph();
        context.addBundleListener(this);
        for (Bundle b : context.getBundles()) {
            if (b.getState() == Bundle.ACTIVE) {
                loadRecipesData(b);
            }
        }
    }
    
    @Deactivate
    protected void deactivate(BundleContext context) {
        context.removeBundleListener(this);
        recipesGraph = null;
    }

    private void loadRecipesData(Bundle b) {
        Enumeration e = b.findEntries(RECIPES_PATH_IN_BUNDLE, "*", false);
        if (e != null) {
            while (e.hasMoreElements()) {
                URL rdfResource = (URL) e.nextElement();
                try {
                    parser.parse(recipesGraph, rdfResource.openStream(), guessFormat(rdfResource));
                } catch (IOException ex) {
                    log.error("Couldn't parse recipe data "+e+" in bundle"+b, ex);
                }
            }
        }
    }
    
    private String guessFormat(URL url) {
        final String path = url.getPath();
        if (path.endsWith("ttl") || path.endsWith("turtle")) {
            return SupportedFormat.TURTLE;
        }
        if (path.endsWith("rdf") || path.endsWith("xml")) {
            return SupportedFormat.RDF_XML;
        }
        if (path.endsWith("nt") || path.endsWith("n3")) {
            return SupportedFormat.N3;
        }
        throw new RuntimeException("Don't know the mediatype of "+path);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED) {
                loadRecipesData(event.getBundle());
        }
    }
}