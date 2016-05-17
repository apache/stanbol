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
package org.apache.stanbol.commons.web.base.writers;

import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.clerezza.commons.rdf.BlankNode;
import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.Variable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class ResultSetToXml {

    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    Document toDocument(ResultSet rs) throws ParserConfigurationException {
        final Document doc = dbf.newDocumentBuilder().newDocument();

        // root element
        Element root = doc.createElement("sparql");
        root.setAttribute("xmlns", "http://www.w3.org/2005/sparql-results#");
        doc.appendChild(root);
        Element head = doc.createElement("head");
        root.appendChild(head);

        // result set
        Element results = doc.createElement("results");
        SolutionMapping solutionMapping = null;
        while (rs.hasNext()) {
            solutionMapping = rs.next();
            createResultElement(solutionMapping, results, doc);
        }
        createVariable(solutionMapping, head, doc);
        root.appendChild(results);

        return doc;
    }

    private void createResultElement(SolutionMapping solutionMap, Element results, Document doc) {
        Set<Variable> keys = solutionMap.keySet();
        Element result = doc.createElement("result");
        results.appendChild(result);
        for (Variable key : keys) {
            Element bindingElement = doc.createElement("binding");
            bindingElement.setAttribute("name", key.getName());
            bindingElement.appendChild(createValueElement(
                    solutionMap.get(key), doc));
            result.appendChild(bindingElement);
        }
    }

    private void createVariable(SolutionMapping solutionMap, Element head, Document doc) {
        if(solutionMap != null) {
            Set<Variable> keys = solutionMap.keySet();
            for (Variable key : keys) {
                Element varElement = doc.createElement("variable");
                varElement.setAttribute("name", key.getName());
                head.appendChild(varElement);
            }
        }
    }

    private Element createValueElement(RDFTerm resource, Document doc) {
        Element value;
        if (resource instanceof IRI) {
            value = doc.createElement("uri");
            value.appendChild(doc.createTextNode(((IRI) resource)
                    .getUnicodeString()));
        } else if (resource instanceof Literal) {
            value = doc.createElement("literal");
            value.appendChild(doc.createTextNode(((Literal)resource)
                    .getLexicalForm()));
            value.setAttribute("datatype", (((Literal) resource)
                    .getDataType().getUnicodeString()));
            Language lang = ((Literal) resource).getLanguage();
            if (lang != null) {
                value.setAttribute("xml:lang", (lang.toString()));
            }
        } else {
            value = doc.createElement("bnode");
            value.appendChild(doc.createTextNode(resource.toString()));
        }
        return value;
    }
}
