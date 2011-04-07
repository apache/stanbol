/*
 * XMLUtil.java
 *
 * Created on November 9, 2007, 1:13 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package org.apache.stanbol.ontologymanager.store.rest.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.stanbol.ontologymanager.store.model.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author tuncay
 */
public class XMLUtil {
    private static final Logger logger = LoggerFactory.getLogger(XMLUtil.class);

    /** Creates a new instance of XMLUtil */
    public XMLUtil() {}

    /** returns null if Node is null */
    public static Node extractFromDOMTree(Node node) throws ParserConfigurationException {
        if (node == null) {
            return null;
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        org.w3c.dom.Document theDocument = db.newDocument();
        theDocument.appendChild(theDocument.importNode(node, true));
        // logger.info(XMLUtil.convertToString(theDocument));
        return (Node) theDocument.getDocumentElement();
    }

    public static org.w3c.dom.Document parseContent(byte[] byteContent) throws ParserConfigurationException,
                                                                       SAXException,
                                                                       IOException {
        org.w3c.dom.Document doc = null;
        String content = new String(byteContent);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // dbf.setIgnoringComments(false);
        dbf.setNamespaceAware(true);

        // dbf.setNamespaceAware(false);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        StringReader lReader = new StringReader(content);
        InputSource inputSource = new InputSource(lReader);
        doc = docBuilder.parse(inputSource);
        return doc;
    }

    public static org.w3c.dom.Document parseContent(String content) throws ParserConfigurationException,
                                                                   SAXException,
                                                                   IOException {
        org.w3c.dom.Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // dbf.setIgnoringComments(false);
        dbf.setNamespaceAware(true);
        // dbf.setNamespaceAware(false);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        StringReader lReader = new StringReader(content);
        InputSource inputSource = new InputSource(lReader);
        doc = docBuilder.parse(inputSource);
        return doc;
    }

    public static String convertToString(Node node) throws TransformerException {
        StringWriter sw = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(node), new StreamResult(sw));
        return sw.toString();
    }

    public static byte[] convertToByteArray(Node node) throws TransformerException {
        /**
         * FIXME: We assume that Transfor deals with encoding/charset internally
         */
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(node), new StreamResult(bos));
        return bos.toByteArray();
    }

    public static Node makeNamespaceUnaware(Node node) throws TransformerException,
                                                      ParserConfigurationException,
                                                      SAXException,
                                                      IOException {
        if (node == null) return null;
        String xmlString = convertToString(node);
        org.w3c.dom.Document doc = null;
        // DocumentBuilderFactoryImpl dbf = new DocumentBuilderFactoryImpl();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // dbf.setNamespaceAware(false);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        StringReader lReader = new StringReader(xmlString);
        InputSource inputSource = new InputSource(lReader);
        doc = docBuilder.parse(inputSource);
        return doc;
    }

    public static Map<String,String> parseNamespaceBindings(String namespaceBindings) {
        if (namespaceBindings == null) return null;
        // remove { and }
        namespaceBindings = namespaceBindings.substring(1, namespaceBindings.length() - 1);
        String[] bindings = namespaceBindings.split(",");
        Map<String,String> namespaces = new HashMap<String,String>();
        for (int i = 0; i < bindings.length; i++) {
            String[] pair = bindings[i].trim().split("=");
            String prefix = pair[0].trim();
            String namespace = pair[1].trim();
            // Remove ' and '
            // namespace = namespace.substring(1,namespace.length()-1);
            namespaces.put(prefix, namespace);
        }
        return namespaces;
    }

    public static Document marshall(Object object, String context, String[] schemaLocations) {
        Locale oldLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en"));
        try {
            ClassLoader cl = ObjectFactory.class.getClassLoader();
            JAXBContext jc = JAXBContext.newInstance(context, cl);
            Marshaller marshaller = jc.createMarshaller();
            // Cihan SchemaFactory.newInstance() do not use current threads
            // class loader any more (OSGI problem)
            // FIXME How to dynamically change the SchemaFactory implementation
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI,
                "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory",
                XMLUtil.class.getClassLoader());

            List<StreamSource> streamSourceList = new Vector<StreamSource>();
            for (int i = 0; i < schemaLocations.length; i++) {
                InputStream is = cl.getResourceAsStream(schemaLocations[i]);
                StreamSource streamSource = new StreamSource(is);
                streamSourceList.add(streamSource);
            }
            StreamSource sources[] = new StreamSource[streamSourceList.size()];
            Schema schema = schemaFactory.newSchema(streamSourceList.toArray(sources));
            marshaller.setSchema(schema);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();
            marshaller.marshal(object, doc);
            Locale.setDefault(oldLocale);
            return doc;
        } catch (Exception ex) {
            logger.error("Error ", ex);
        }
        Locale.setDefault(oldLocale);
        return null;
    }

    public static Object unmarshall(String context, String[] schemaLocations, String content) {
        Locale oldLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en"));
        try {
            ClassLoader cl = ObjectFactory.class.getClassLoader();
            JAXBContext jc = JAXBContext.newInstance(context, cl);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI,
                "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory",
                XMLUtil.class.getClassLoader());
            List<StreamSource> streamSourceList = new Vector<StreamSource>();
            for (int i = 0; i < schemaLocations.length; i++) {
                InputStream is = cl.getResourceAsStream(schemaLocations[i]);
                StreamSource streamSource = new StreamSource(is);
                streamSourceList.add(streamSource);
            }
            StreamSource sources[] = new StreamSource[streamSourceList.size()];
            Schema schema = schemaFactory.newSchema(streamSourceList.toArray(sources));
            unmarshaller.setSchema(schema);
            Object obj = unmarshaller.unmarshal(new StringReader(content));
            Locale.setDefault(oldLocale);
            return obj;
        } catch (Exception ex) {
            logger.error("Error ", ex);
        }
        Locale.setDefault(oldLocale);
        return null;
    }

    public static String stringToHTMLString(String string) {
        StringBuffer sb = new StringBuffer(string.length());
        // true if last char was blank
        boolean lastWasBlankChar = false;
        int len = string.length();
        char c;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);
            if (c == ' ') {
                // blank gets extra work,
                // this solves the problem you get if you replace all
                // blanks with &nbsp;, if you do that you loss
                // word breaking
                if (lastWasBlankChar) {
                    lastWasBlankChar = false;
                    sb.append("&nbsp;");
                } else {
                    lastWasBlankChar = true;
                    sb.append(' ');
                }
            } else {
                lastWasBlankChar = false;
                //
                // HTML Special Chars
                if (c == '"') sb.append("&quot;");
                else if (c == '&') sb.append("&amp;");
                else if (c == '<') sb.append("&lt;");
                else if (c == '>') sb.append("&gt;");
                else if (c == '\n')
                // Handle Newline
                sb.append("&lt;br/&gt;");
                else {
                    int ci = 0xffff & c;
                    if (ci < 160)
                    // nothing special only 7 Bit
                    sb.append(c);
                    else {
                        // Not 7 Bit use the unicode system
                        sb.append("&#");
                        sb.append(new Integer(ci).toString());
                        sb.append(';');
                    }
                }
            }
        }
        return sb.toString();
    }
}
