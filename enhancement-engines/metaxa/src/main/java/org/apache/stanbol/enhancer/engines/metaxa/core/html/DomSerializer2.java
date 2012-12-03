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

package org.apache.stanbol.enhancer.engines.metaxa.core.html;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CommentToken;
import org.htmlcleaner.ContentToken;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.Utils;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *  Patches for HtmlCleaner-2.1 for namespace handling and correcting XML serialization.
 *  The patches are not applicable for HtmlCleaner-2.2 that suffers from losing namespaces altogether.
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 * 
 */

public class DomSerializer2 extends DomSerializer {

  public DomSerializer2(CleanerProperties props, boolean escapeXml) {
    super(props,escapeXml);
  }
  
  public DomSerializer2(CleanerProperties props) {
    this(props, true);
  }

  public Document createDOM(TagNode rootNode) throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(props.isNamespacesAware());
    Document document = factory.newDocumentBuilder().newDocument();
    Element rootElement = document.createElement(rootNode.getName());;
    document.appendChild(rootElement);
    setAttributes(rootNode, rootElement);

    createSubnodes(document, rootElement, rootNode.getChildren());

    return document;
}
 
  private void setAttributes(TagNode node, Element element) {
    Map<?,?> attributes =  node.getAttributes();
    Iterator<?> entryIterator = attributes.entrySet().iterator();
    while (entryIterator.hasNext()) {
      Map.Entry<?,?> entry = (Map.Entry<?,?>) entryIterator.next();
      String attrName = (String)entry.getKey();
      String attrValue = (String)entry.getValue();
      if (escapeXml) {
        attrValue = escapeXml(attrValue, props, true);
      }
      // avoid xhtml declarations
      if (!attrName.equals("xmlns")) {
        element.setAttribute(attrName, attrValue);
      }
    }
  }
  
  private void createSubnodes(Document document, Element element, List tagChildren) {
    if (tagChildren != null) {
      Iterator it = tagChildren.iterator();
      while (it.hasNext()) {
        Object item = it.next();
        if (item instanceof CommentToken) {
          CommentToken commentNode = (CommentToken) item;
          Comment comment = document.createComment( commentNode.getContent().toString() );
          element.appendChild(comment);
        } else if (item instanceof ContentToken) {
          ContentToken contentToken = (ContentToken) item;
          String content = contentToken.getContent();
          String nodeName = element.getNodeName();
          boolean specialCase = props.isUseCdataForScriptAndStyle() &&
          ("script".equalsIgnoreCase(nodeName) || "style".equalsIgnoreCase(nodeName));
          if (escapeXml && !specialCase) {
            content = escapeXml(content, props, true);
          }
          element.appendChild( specialCase ? document.createCDATASection(content) : document.createTextNode(content) );
        } else if (item instanceof TagNode) {
          TagNode subTagNode = (TagNode) item;
          Element subelement = document.createElement( subTagNode.getName() );;
          
          setAttributes(subTagNode, subelement);
          
          // recursively create subnodes
          createSubnodes(document, subelement, subTagNode.getChildren());
          
          element.appendChild(subelement);
        } else if (item instanceof List) {
          List sublist = (List) item;
          createSubnodes(document, element, sublist);
        }
      }
    }
  }
    
  /**
   * Escapes XML string.
   * @param s String to be escaped
   * @param props Cleaner properties gover affect escaping behaviour
   * @param isDomCreation Tells if escaped content will be part of the DOM
   */
  public static String escapeXml(String s, CleanerProperties props, boolean isDomCreation) {
      boolean advanced = props.isAdvancedXmlEscape();
      boolean recognizeUnicodeChars = props.isRecognizeUnicodeChars();
      boolean translateSpecialEntities = props.isTranslateSpecialEntities();

      if (s != null) {
      int len = s.length();
      StringBuffer result = new StringBuffer(len);
      
      for (int i = 0; i < len; i++) {
        char ch = s.charAt(i);
        
        if (ch == '&') {
          if ( (advanced || recognizeUnicodeChars) && (i < len-1) && (s.charAt(i+1) == '#') ) {
            int charIndex = i + 2;
            String unicode = "";
            while ( charIndex < len &&
                              (Utils.isHexadecimalDigit(s.charAt(charIndex)) || s.charAt(charIndex) == 'x' || s.charAt(charIndex) == 'X') 
                            ) {
              unicode += s.charAt(charIndex);
              charIndex++;
            }
            if (charIndex == len || !"".equals(unicode)) {
              try {
                char unicodeChar = unicode.toLowerCase().startsWith("x") ?
                                                      (char)Integer.parseInt(unicode.substring(1), 16) :                                
                                                      (char)Integer.parseInt(unicode);
//                 if ( "&<>\'\"".indexOf(unicodeChar) < 0 ) {
                  int replaceChunkSize = (charIndex < len && s.charAt(charIndex) == ';') ? unicode.length()+1 : unicode.length();
                  result.append( recognizeUnicodeChars ? String.valueOf(unicodeChar) : "&#" + unicode + ";" );
                  i += replaceChunkSize + 1;
//                } else {
//                    i = charIndex;
//                    result.append("&#" + unicode + ";");
//                }
              } catch (NumberFormatException e) {
                i = charIndex;
                result.append("&#" + unicode + ";");
              }
            } else {
              result.append("&");
            }
          } else {
            if (translateSpecialEntities) {
              // get following sequence of most 10 characters
              String seq = s.substring(i, i+Math.min(10, len-i));
              int semiIndex = seq.indexOf(';');
              if (semiIndex > 0) {
                String entity = seq.substring(1, semiIndex);
                Integer code = entities.get(entity);
                if (code != null) {
                  int entityLen = entity.length();
                                  if (recognizeUnicodeChars) {
                                      result.append( (char)code.intValue() );
                                  } else {
                                      result.append( "&#" + code + ";" );
                                  }
                  i += entityLen + 1;
                  continue;
                }
              }
            }
            
            if (advanced) {
                          String sub = s.substring(i);
                          if ( sub.startsWith("&amp;") ) {
                              result.append(isDomCreation ? "&" : "&amp;");
                              i += 4;
                          } else if ( sub.startsWith("&apos;") ) {
                              result.append(isDomCreation ? "'" : "&apos;");
                              i += 5;
                          } else if ( sub.startsWith("&gt;") ) {
                              result.append(isDomCreation ? ">" : "&gt;");
                              i += 3;
                          } else if ( sub.startsWith("&lt;") ) {
                              result.append(isDomCreation ? "<" : "&lt;");
                              i += 3;
                          } else if ( sub.startsWith("&quot;") ) {
                              result.append(isDomCreation ? "\"" : "&quot;");
                              i += 5;
                          } else {
                              result.append(isDomCreation ? "&" : "&amp;");
                          }
              
              continue;
            }
            
            result.append("&");
          }
        }
        else if (!isDomCreation) {
          if (ch == '\'') {
            result.append("&apos;");
          } else if (ch == '>') {
            result.append("&gt;");
          } else if (ch == '<') {
            result.append("&lt;");
          } else if (ch == '\"') {
            result.append("&quot;");
          }
        }
        else {
          result.append(ch);
        }
      }
      return result.toString();
    }
    
    return null;
  }

  // copied from SpecialEntities class because map is not visible only package internal
  public static Map<String,Integer> entities = new HashMap<String,Integer>();

  static {
    entities.put("nbsp",  new Integer(160));
    entities.put("iexcl", new Integer(161));
    entities.put("curren",  new Integer(164));
    entities.put("cent",  new Integer(162));
    entities.put("pound", new Integer(163));
    entities.put("yen",   new Integer(165));
    entities.put("brvbar",  new Integer(166));
    entities.put("sect",  new Integer(167));
    entities.put("uml",   new Integer(168));
    entities.put("copy",  new Integer(169));
    entities.put("ordf",  new Integer(170));
    entities.put("laquo", new Integer(171));
    entities.put("not",   new Integer(172));
    entities.put("shy",   new Integer(173));
    entities.put("reg",   new Integer(174));
    entities.put("trade", new Integer(8482));
    entities.put("macr",  new Integer(175));
    entities.put("deg",   new Integer(176));
    entities.put("plusmn",  new Integer(177));
    entities.put("sup2",  new Integer(178));
    entities.put("sup3",  new Integer(179));
    entities.put("acute", new Integer(180));
    entities.put("micro", new Integer(181));
    entities.put("para",  new Integer(182));
    entities.put("middot",  new Integer(183));
    entities.put("cedil", new Integer(184));
    entities.put("sup1",  new Integer(185));
    entities.put("ordm",  new Integer(186));
    entities.put("raquo", new Integer(187));
    entities.put("frac14",  new Integer(188));
    entities.put("frac12",  new Integer(189));
    entities.put("frac34",  new Integer(190));
    entities.put("iquest",  new Integer(191));
    entities.put("times", new Integer(215));
    entities.put("divide",  new Integer(247));

    entities.put("Agrave",  new Integer(192));
    entities.put("Aacute",  new Integer(193));
    entities.put("Acirc", new Integer(194));
    entities.put("Atilde",  new Integer(195));
    entities.put("Auml",  new Integer(196));
    entities.put("Aring", new Integer(197));
    entities.put("AElig", new Integer(198));
    entities.put("Ccedil",  new Integer(199));
    entities.put("Egrave",  new Integer(200));
    entities.put("Eacute",  new Integer(201));
    entities.put("Ecirc", new Integer(202));
    entities.put("Euml",  new Integer(203));
    entities.put("Igrave",  new Integer(204));
    entities.put("Iacute",  new Integer(205));
    entities.put("Icirc", new Integer(206));
    entities.put("Iuml",  new Integer(207));
    entities.put("ETH",   new Integer(208));
    entities.put("Ntilde",  new Integer(209));
    entities.put("Ograve",  new Integer(210));
    entities.put("Oacute",  new Integer(211));
    entities.put("Ocirc", new Integer(212));
    entities.put("Otilde",  new Integer(213));
    entities.put("Ouml",  new Integer(214));
    entities.put("Oslash",  new Integer(216));
    entities.put("Ugrave",  new Integer(217));
    entities.put("Uacute",  new Integer(218));
    entities.put("Ucirc", new Integer(219));
    entities.put("Uuml",  new Integer(220));
    entities.put("Yacute",  new Integer(221));
    entities.put("THORN", new Integer(222));
    entities.put("szlig", new Integer(223));
    entities.put("agrave",  new Integer(224));
    entities.put("aacute",  new Integer(225));
    entities.put("acirc", new Integer(226));
    entities.put("atilde",  new Integer(227));
    entities.put("auml",  new Integer(228));
    entities.put("aring", new Integer(229));
    entities.put("aelig", new Integer(230));
    entities.put("ccedil",  new Integer(231));
    entities.put("egrave",  new Integer(232));
    entities.put("eacute",  new Integer(233));
    entities.put("ecirc", new Integer(234));
    entities.put("euml",  new Integer(235));
    entities.put("igrave",  new Integer(236));
    entities.put("iacute",  new Integer(237));
    entities.put("icirc", new Integer(238));
    entities.put("iuml",  new Integer(239));
    entities.put("eth",   new Integer(240));
    entities.put("ntilde",  new Integer(241));
    entities.put("ograve",  new Integer(242));
    entities.put("oacute",  new Integer(243));
    entities.put("ocirc", new Integer(244));
    entities.put("otilde",  new Integer(245));
    entities.put("ouml",  new Integer(246));
    entities.put("oslash",  new Integer(248));
    entities.put("ugrave",  new Integer(249));
    entities.put("uacute",  new Integer(250));
    entities.put("ucirc", new Integer(251));
    entities.put("uuml",  new Integer(252));
    entities.put("yacute",  new Integer(253));
    entities.put("thorn", new Integer(254));
    entities.put("yuml",  new Integer(255));

    entities.put("OElig", new Integer(338));
    entities.put("oelig", new Integer(339));
    entities.put("Scaron",  new Integer(352));
    entities.put("scaron",  new Integer(353));
    entities.put("Yuml",  new Integer(376));
    entities.put("circ",  new Integer(710));
    entities.put("tilde", new Integer(732));
    entities.put("ensp",  new Integer(8194));
    entities.put("emsp",  new Integer(8195));
    entities.put("thinsp",  new Integer(8201));
    entities.put("zwnj",  new Integer(8204));
    entities.put("zwj",   new Integer(8205));
    entities.put("lrm",   new Integer(8206));
    entities.put("rlm",   new Integer(8207));
    entities.put("ndash", new Integer(8211));
    entities.put("mdash", new Integer(8212));
    entities.put("lsquo", new Integer(8216));
    entities.put("rsquo", new Integer(8217));
    entities.put("sbquo", new Integer(8218));
    entities.put("ldquo", new Integer(8220));
    entities.put("rdquo", new Integer(8221));
    entities.put("bdquo", new Integer(8222));
    entities.put("dagger",  new Integer(8224));
    entities.put("Dagger",  new Integer(8225));
    entities.put("hellip",  new Integer(8230));
    entities.put("permil",  new Integer(8240));
    entities.put("lsaquo",  new Integer(8249));
    entities.put("rsaquo",  new Integer(8250));
    entities.put("euro",  new Integer(8364));
  }

  
}
