package org.apache.stanbol.enhancer.jersey.writers;

import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
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
                    (Resource) solutionMap.get(key), doc));
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

    private Element createValueElement(Resource resource, Document doc) {
        Element value;
        if (resource instanceof UriRef) {
            value = doc.createElement("uri");
            value.appendChild(doc.createTextNode(((UriRef) resource)
                    .getUnicodeString()));
        } else if (resource instanceof TypedLiteral) {
            value = doc.createElement("literal");
            value.appendChild(doc.createTextNode(((TypedLiteral) resource)
                    .getLexicalForm()));
            value.setAttribute("datatype", (((TypedLiteral) resource)
                    .getDataType().getUnicodeString()));
        } else if (resource instanceof PlainLiteral) {
            value = doc.createElement("literal");
            value.appendChild(doc.createTextNode(((PlainLiteral) resource)
                    .getLexicalForm()));
            Language lang = ((PlainLiteral) resource).getLanguage();
            if (lang != null) {
                value.setAttribute("xml:lang", (lang.toString()));
            }
        } else {
            value = doc.createElement("bnode");
            value.appendChild(doc.createTextNode(((BNode) resource).toString()));
        }
        return value;
    }
}
