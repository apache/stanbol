package org.apache.stanbol.enhancer.sace.util;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jrdf.graph.Graph;
import org.jrdf.parser.RdfReader;

public class Util {

    private static SAXBuilder xmlParser;

    static {
        xmlParser = new SAXBuilder();
    }

    public static Document transformToXML (String xml) {
        StringReader inReader = new StringReader(xml);
        Document doc = null;
        try {
            doc = xmlParser.build(inReader);

            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Graph parseXML (String xml) {
        RdfReader reader = new RdfReader();
        return reader.parseRdfXml(new ByteArrayInputStream(xml.getBytes()));
    }


}
