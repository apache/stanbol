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
package org.apache.stanbol.enhancer.engines.htmlextractor.impl;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * <code>DOMUtils</code> provides convenience methods for working with DOM
 * documents.
 *
 * @author Walter Kasper, DFKI
 * @author Joerg Steffen, DFKI
 * @version $Id: DOMUtils.java 1068358 2011-02-08 12:58:11Z bdelacretaz $
 */
public final class DOMUtils {

    /**
     * Restrict instantiation
     */
    private DOMUtils() {}

   /**
   * This prints the specified node and all of its children to a PrintStream.
   * 
   * @param node a DOM <code>Node</code>
   */
  public static void printDOM(Node node, PrintStream out) {
    
    int type = node.getNodeType();
    switch (type) {
      // print the document element
      case Node.DOCUMENT_NODE: 
        out.println("<?xml version=\"1.0\" ?>");
        printDOM(((Document)node).getDocumentElement(),out);
        break;

        // print element with attributes
      case Node.ELEMENT_NODE: 
        out.print("<");
        out.print(node.getNodeName());
        NamedNodeMap attrs = node.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
          Node attr = attrs.item(i);
          out.print(" " + attr.getNodeName().trim() + "=\""
            + quoteXMLChars(attr.getNodeValue().trim()) + "\"");
        }
        out.println(">");
        
        NodeList children = node.getChildNodes();
        if (children != null) {
          int len = children.getLength();
          for (int i = 0; i < len; i++) {
            printDOM(children.item(i),out);
          }
        }
        
        break;
        
        // handle entity reference nodes
      case Node.ENTITY_REFERENCE_NODE:
        out.print("&");
        out.print(node.getNodeName().trim());
        out.print(";");
        break;
        
        // print cdata sections
      case Node.CDATA_SECTION_NODE:
        out.print("<![CDATA[");
        out.print(node.getNodeValue().trim());
        out.print("]]>");
        break;
        
        // print text
      case Node.TEXT_NODE:
        out.print(quoteXMLChars(node.getNodeValue().trim()));
        break;
        
        // print processing instruction
      case Node.PROCESSING_INSTRUCTION_NODE:
        out.print("<?");
        out.print(node.getNodeName().trim());
        String data = node.getNodeValue().trim();
        out.print(" ");
        out.print(data);
        out.print("?>");
        break;
        
      default:
        System.err.println("unknown type " + type);
        break;
    }
    
    if (type == Node.ELEMENT_NODE) {
      out.println();
      out.print("</");
      out.print(node.getNodeName().trim());
      out.println('>');
    }
  }

    /**
     * This prints the given DOM document to System.out with indentation and
     * utf-8 encoding.
     *
     * @param doc
     *            a DOM <code>Document</code>
     */
    public static void printXML(Document doc) {

        try {
            // prepare the DOM document for writing
            Source source = new DOMSource(doc);

            // prepare the output
            Result result = new StreamResult(System.out);

            // write the DOM document to the file
            // get Transformer
            Transformer xformer =
                TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(
                OutputKeys.INDENT, "yes");
            xformer.setOutputProperty(
                OutputKeys.ENCODING, "UTF-8");
            xformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");
            xformer.setOutputProperty(OutputKeys.METHOD, "xml");

            // write to System.out
            xformer.transform(source, result);

        } catch (TransformerConfigurationException tce) {
            // error generated during transformer configuration
            System.err.println(tce.getMessage());
            // use the contained exception, if any
            Throwable x = tce;
            if (tce.getException() != null) {
                x = tce.getException();
            }
            x.printStackTrace();
        } catch (TransformerException te) {
            // error generated by the transformer
            System.err.println(te.getMessage());
            // use the contained exception, if any
            Throwable x = te;
            if (te.getException() != null) {
                x = te.getException();
            }
            x.printStackTrace();
        }
    }


    /**
     * This returns a string representation of the given document.
     *
     * @param doc
     *            an XML <code>Document</code>
     * @param encoding
     *            a <code>String</code> with the encoding to use
     * @param docTypeDef
     *            a <code>String</code> with the DTD name; use <code>null</code>
     *            for no DTD
     * @return a <code>String</code> with the XML string
     */
    public static String getStringFromDoc(
            Document doc, String encoding, String docTypeDef) {

        try {

            // use a Transformer for output
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer xformer = tFactory.newTransformer();
            xformer.setOutputProperty(
                OutputKeys.INDENT, "yes");
            xformer.setOutputProperty(
                OutputKeys.ENCODING, encoding);
            xformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");
            xformer.setOutputProperty(OutputKeys.METHOD, "xml");
            if (null != docTypeDef) {
                xformer
                    .setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docTypeDef);
            }

            DOMSource source = new DOMSource(doc);
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            xformer.transform(source, result);
            return sw.toString();

        } catch (TransformerConfigurationException tce) {
            // error generated by the parser
            System.err.println("** Transformer Factory error");
            System.err.println("   " + tce.getMessage());

            // use the contained exception, if any
            Throwable x = tce;
            if (tce.getException() != null) {
                x = tce.getException();
            }
            x.printStackTrace();

        } catch (TransformerException te) {
            // error generated by the parser
            System.err.println("** Transformation error");
            System.err.println("   " + te.getMessage());

            // use the contained exception, if any
            Throwable x = te;
            if (te.getException() != null) {
                x = te.getException();
            }
            x.printStackTrace();
        }

        return null;
    }

    /**
     * This method writes a DOM document to the given output stream.
     *
     * @param doc
     *            a DOM <code>Document</code>
     * @param encoding
     *            a <code>String</code> with the encoding to use
     * @param docTypeDef
     *            a <code>String</code> with the DTD name; use <code>null</code>
     *            for no DTD
     * @param out
     *            an <code>OutputStream</code> where to write the DOM document
     */
    public static void writeXml(
            Document doc, String encoding, String docTypeDef, OutputStream out) {

        try {
            // prepare the DOM document
            Source source = new DOMSource(doc);

            // prepare the output
            Result result = new StreamResult(out);

            // write the DOM document to the file
            // get Transformer
            Transformer xformer =
                TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(
                OutputKeys.INDENT, "yes");
            xformer.setOutputProperty(
                OutputKeys.ENCODING, encoding);
            xformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");
            xformer.setOutputProperty(OutputKeys.METHOD, "xml");
            if (null != docTypeDef) {
                xformer
                    .setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docTypeDef);
            }
            // write to a file
            xformer.transform(source, result);

        } catch (TransformerConfigurationException tce) {
            // error generated during transformer configuration
            System.err.println(tce.getMessage());
            // use the contained exception, if any
            Throwable x = tce;
            if (tce.getException() != null) {
                x = tce.getException();
            }
            x.printStackTrace();
        } catch (TransformerException te) {
            // error generated by the transformer
            System.err.println(te.getMessage());
            // use the contained exception, if any
            Throwable x = te;
            if (te.getException() != null) {
                x = te.getException();
            }
            x.printStackTrace();
        }
    }


    /**
     * This parses the given XML string and creates a DOM Document.
     *
     * @param fileName
     *            a <code>String</code> with the source file name
     * @param encoding
     *            a <code>String</code> denoting the encoding of the XML string
     * @return Document a DOM <code>Document</code>, <code>null</code> if
     *         parsing fails
     */
    public static Document parse(String xml, String encoding) {

        if (encoding == null)
            encoding = "UTF-8";
        Document document = null;
        // initiate DocumentBuilderFactory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // to get a validating parser
        factory.setValidating(false);
        // to get one that understands namespaces
        factory.setNamespaceAware(true);
        factory.setIgnoringElementContentWhitespace(true);
        try {
            // get DocumentBuilder
            DocumentBuilder builder = factory.newDocumentBuilder();
            // parse and load into memory the Document
            document =
                builder.parse(new ByteArrayInputStream(xml.getBytes(encoding)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return document;
    }

    /**
     * This parses the given XML file and creates a DOM Document.
     *
     * @param fileName
     *            a <code>String</code> with the source file name
     * @param validation
     *            a <code>boolean</code> indicatiing if the parsing uses DTD
     *            valudation
     * @return Document a DOM <code>Document</code>, <code>null</code> if
     *         parsing fails
     */
    public static Document parse(String fileName, boolean validation) {

        Document document = null;
        // initiate DocumentBuilderFactory
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // to get a validating parser
        factory.setValidating(validation);
        // to get one that understands namespaces
        factory.setNamespaceAware(true);
        factory.setIgnoringElementContentWhitespace(true);

        try {
            // get DocumentBuilder
            DocumentBuilder builder = factory.newDocumentBuilder();
            // parse and load into memory the Document
            document = builder.parse(new File(fileName));
            return document;

        } catch (SAXParseException spe) {
            // error generated by the parser
            System.err.println(
                "Parsing error, line " + spe.getLineNumber() + ", uri "
                + spe.getSystemId());
            System.err.println(" " + spe.getMessage());
            // use the contained exception, if any
            Exception x = spe;
            if (spe.getException() != null) {
                x = spe.getException();
            }
            x.printStackTrace();
        } catch (SAXException sxe) {
            // error generated during parsing
            System.err.println(sxe.getMessage());
            // use the contained exception, if any
            Exception x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            x.printStackTrace();
        } catch (ParserConfigurationException pce) {
            // parser with specified options can't be built
            System.err.println(pce.getMessage());
            pce.printStackTrace();
        } catch (IOException ioe) {
            // i/o error
            System.err.println(ioe.getMessage());
            ioe.printStackTrace();
        }

        return null;
    }

    /**
     * This counts the elements in the given document by tag name.
     *
     * @param tag
     *            a <code>String</code> with a tag name
     * @param doc
     *            a DOM <code>Document</code>
     * @return number an <code>int</code> with the number of elements by tag
     *         name
     */
    public static int countByTagName(String tag, Document doc) {

        NodeList list = doc.getElementsByTagName(tag);
        return list.getLength();
    }

    /**
     * This realizes the <code>indexOf</code> method of the
     * <code>java.util.List</code> interface for <code>NodeList</code>.
     *
     * @param list
     *            a <code>NodeList</code> value
     * @param node
     *            a <code>Node</code> value
     * @return an <code>int</code> value, giving the position of
     *         <code>node</code> in <code>list</code> or -1, if node is not
     *         contained in the list
     */
    public static int indexOf(NodeList list, Node node) {

        for (int i = 0, j = list.getLength(); i < j; ++i) {
            if (list.item(i) == node) {
                return i;
            }
        }
        return -1;
    }

    /**
     * This concatenates the string values of all text nodes which are direct
     * children of the given node. If <code>node</code> is a text or attribute
     * node, its value is returned. Otherwise <code>null</code> is returned
     * (improvement potential!).
     *
     * @param node
     *            a <code>Node</code> value
     * @return a <code>String</code> with the concatenated text
     */
    public static String getText(Node node) {

        short nodeType = node.getNodeType();
        if ((nodeType == Node.TEXT_NODE) || (nodeType == Node.ATTRIBUTE_NODE)
            || (nodeType == Node.CDATA_SECTION_NODE)) {
            return node.getNodeValue();
        }
        else if (nodeType == Node.ELEMENT_NODE) {
            NodeList dtrs = node.getChildNodes();
            StringBuffer sb = new StringBuffer();
            for (int i = 0, j = dtrs.getLength(); i < j; ++i) {
                Node item = dtrs.item(i);
                if (item.getNodeType() == Node.TEXT_NODE
                    || item.getNodeType() == Node.CDATA_SECTION_NODE) {
                    sb.append(item.getNodeValue());
                }
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * This selects all direct children of the given element with the given
     * name. If the name is <code>null</code>, all children are returned.
     *
     * @param ele
     *            an <code>Element</code> value
     * @param name
     *            a <code>String</code> with the children's name
     * @return a <code>List</code> of <code>Node</code>s with the children
     */
    public static List<org.w3c.dom.Node> getChildren(
            Element ele, String name) {

        NodeList dtrs = ele.getChildNodes();
        List<org.w3c.dom.Node> eles = new ArrayList<org.w3c.dom.Node>();
        for (int i = 0, j = dtrs.getLength(); i < j; ++i) {
            org.w3c.dom.Node item = dtrs.item(i);
            if (name == null || item.getNodeName().equals(name)) {
                eles.add(item);
            }
        }

        return eles;
    }

    /**
     * This selects all direct children of type 'Element' of the given element.
     *
     * @param ele
     *            an <code>Element</code> value
     * @return a <code>List</code> of <code>Elmenet</code>s with the element
     *         children
     */
    public static List<Element> getChildrenElements(Element ele) {
        NodeList dtrs = ele.getChildNodes();
        List<Element> eles = new ArrayList<Element>();
        for (int i = 0, j = dtrs.getLength(); i < j; ++i) {
            org.w3c.dom.Node item = dtrs.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                eles.add((Element)item);
            }
        }

        return eles;
    }

    /**
     * This returns the first child element with the given name found at the
     * given element.
     *
     * @param ele
     *            an <code>Element</code> value
     * @param name
     *            a <code>String</code> with the name of the child element
     * @return a <code>Element</code> with the child or <code>null</code> if no
     *         such child was found
     */
    public static Element getFirstChild(Element ele, String name) {
        NodeList dtrs = ele.getChildNodes();
        for (int i = 0, iMax = dtrs.getLength(); i < iMax; ++i) {
            org.w3c.dom.Node item = dtrs.item(i);
            if (item.getNodeName().equals(name)) {
                return (Element)item;
            }
        }
        return null;
    }

    /**
     * This adds a new child with the given name to the given element.
     *
     * @param ele
     *            an <code>Element</code>
     * @param name
     *            a <code>String</code> with the name of the child
     * @return a <code>Element</code> with the newly created child
     */
    public static Element addChild(Element ele, String name) {

        Element child = ele.getOwnerDocument().createElement(name);
        ele.appendChild(child);
        return child;
    }
    
    public static String quoteXMLChars(String text) {
      if (text != null) {
        return text.replace("&", "&amp;").replace("<","&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
      }
      return text;
    }
 
}

