/*
 * Copyright 2016 The Apache Software Foundation.
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
package org.apache.stanbol.commons.jsonld.clerezza;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;

import com.github.jsonldjava.core.JsonLdTripleCallback;
import com.github.jsonldjava.core.RDFDataset;
import org.apache.clerezza.commons.rdf.impl.utils.TypedLiteralImpl;

public class ClerezzaTripleCallback implements JsonLdTripleCallback {

    private static final String RDF_LANG_STRING = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString";

    private Graph mGraph = new SimpleGraph();
    private Map<String, BlankNode> bNodeMap = new HashMap<String, BlankNode>();

    public void setGraph(Graph mGraph) {
        this.mGraph = mGraph;
        bNodeMap = new HashMap<String, BlankNode>();
    }

    public Graph getGraph() {
        return mGraph;
    }

    private void triple(String s, String p, String o, String graph) {
        if (s == null || p == null || o == null) {
            // TODO: i don't know what to do here!!!!
            return;
        }

        final BlankNodeOrIRI subject = getBlankNodeOrIRI(s);
        final IRI predicate = new IRI(p);
        final BlankNodeOrIRI object = getBlankNodeOrIRI(o);
        mGraph.add(new TripleImpl(subject, predicate, object));
    }

    private void triple(String s, String p, String value, String datatype, String language,
            String graph) {
        final BlankNodeOrIRI subject = getBlankNodeOrIRI(s);
        final IRI predicate = new IRI(p);
        RDFTerm object;
        if (language != null) {
            object = new PlainLiteralImpl(value, new Language(language));
        } else if (datatype == null || RDF_LANG_STRING.equals(datatype)) {
            object = new PlainLiteralImpl(value);
        } else {
            object = new TypedLiteralImpl(value, new IRI(datatype));
        }

        mGraph.add(new TripleImpl(subject, predicate, object));
    }

    private BlankNodeOrIRI getBlankNodeOrIRI(String s) {
        if (s.startsWith("_:")) {
            return getBlankNode(s);
        } else {
            return new IRI(s);
        }
    }

    private BlankNode getBlankNode(String s) {
        if (bNodeMap.containsKey(s)) {
            return bNodeMap.get(s);
        } else {
            final BlankNode result = new BlankNode();
            bNodeMap.put(s, result);
            return result;
        }
    }

    @Override
    public Object call(RDFDataset dataset) {
        for (String graphName : dataset.graphNames()) {
            final List<RDFDataset.Quad> quads = dataset.getQuads(graphName);
            if ("@default".equals(graphName)) {
                graphName = null;
            }
            for (final RDFDataset.Quad quad : quads) {
                if (quad.getObject().isLiteral()) {
                    triple(quad.getSubject().getValue(), quad.getPredicate().getValue(), quad
                            .getObject().getValue(), quad.getObject().getDatatype(), quad
                            .getObject().getLanguage(), graphName);
                } else {
                    triple(quad.getSubject().getValue(), quad.getPredicate().getValue(), quad
                            .getObject().getValue(), graphName);
                }
            }
        }

        return getGraph();
    }

}
