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

package org.apache.stanbol.enhancer.engines.metaxa.core.mail.simple;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.stanbol.enhancer.engines.metaxa.core.html.HtmlTextExtractUtil;
import org.apache.stanbol.enhancer.engines.metaxa.core.html.InitializationException;
import org.ontoware.rdf2go.exception.ModelException;
import org.ontoware.rdf2go.model.Model;
import org.ontoware.rdf2go.model.Syntax;
import org.ontoware.rdf2go.model.impl.URIGenerator;
import org.ontoware.rdf2go.model.node.URI;
import org.ontoware.rdf2go.model.node.impl.URIImpl;
import org.ontoware.rdf2go.vocabulary.RDF;
import org.semanticdesktop.aperture.extractor.Extractor;
import org.semanticdesktop.aperture.extractor.ExtractorException;
import org.semanticdesktop.aperture.extractor.mime.MailUtil;
import org.semanticdesktop.aperture.rdf.RDFContainer;
import org.semanticdesktop.aperture.rdf.RDFContainerFactory;
import org.semanticdesktop.aperture.rdf.impl.RDFContainerFactoryImpl;
import org.semanticdesktop.aperture.vocabulary.NFO;
import org.semanticdesktop.aperture.vocabulary.NIE;
import org.semanticdesktop.aperture.vocabulary.NMO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Extractor implementation for message/rfc822-style messages.
 * 
 * <p>
 * Only typical body parts are processed during full-text extraction. Attachments are only listed but not
 * further handled. In case of mails in HTML format, the full HTML is included in the extracted data as value
 * of the <code>nmo:htmlMessageContent</code> property. The plain text (extract) is represented by the
 * <code>nmo:plainTextMessageContent</code> property and as value of the <code>nie:plainTextContent</code>
 * property for compliance with the representation from other extractors.
 * 
 * 
 */
public class SimpleMailExtractor implements Extractor {
    private static final Logger logger = LoggerFactory.getLogger(SimpleMailExtractor.class);
    
    public void extract(URI id, InputStream stream, Charset charset, String mimeType, RDFContainer result) throws ExtractorException {
        try {
            // parse the stream
            MimeMessage message = new MimeMessage(null, stream);
            
            result.add(RDF.type, NMO.Email);
            
            // extract the full-text
            StringBuilder buffer = new StringBuilder(10000);
            processMessage(message, buffer, result);
            String text = buffer.toString().trim();
            if (text.length() > 0) {
                result.add(NMO.plainTextMessageContent, text);
                result.add(NIE.plainTextContent, text);
            }
            
            // extract other metadata
            String title = message.getSubject();
            if (title != null) {
                title = title.trim();
                if (title.length() > 0) {
                    result.add(NMO.messageSubject, title);
                }
            }
            
            try {
                copyAddress(message.getFrom(), NMO.from, result);
            } catch (AddressException e) {
                // ignore
            }
            
            copyAddress(getRecipients(message, RecipientType.TO), NMO.to, result);
            copyAddress(getRecipients(message, RecipientType.CC), NMO.cc, result);
            copyAddress(getRecipients(message, RecipientType.BCC), NMO.bcc, result);
            
            MailUtil.getDates(message, result);
            
        } catch (MessagingException e) {
            throw new ExtractorException(e);
        } catch (IOException e) {
            throw new ExtractorException(e);
        }
    }
    
    // the top level message
    protected void processMessage(MimeMessage msg, StringBuilder buffer, RDFContainer rdf) throws MessagingException,
                                                                                          IOException,
                                                                                          ExtractorException {
        if (msg.isMimeType("text/plain")) {
            processContent(msg.getContent(), buffer, rdf);
        } else if (msg.isMimeType("text/html")) {
            String encoding = getContentEncoding(new ContentType(msg.getContentType()));
            logger.debug("HTML encoding: {}", encoding);
            if (msg.getContent() instanceof String) {
                String text = extractTextFromHtml(((String) msg.getContent()).trim(), encoding, rdf);
                rdf.add(NMO.htmlMessageContent, (String) msg.getContent());
                processContent(text, buffer, rdf);
            } else {
                processContent(msg.getContent(), buffer, rdf);
            }
        } else {
            processContent(msg.getContent(), buffer, rdf);
        }
    }
    
    // the recursive part
    protected void processContent(Object content, StringBuilder buffer, RDFContainer rdf) throws MessagingException,
                                                                                         IOException,
                                                                                         ExtractorException {
        if (content instanceof String) {
            buffer.append(content);
            buffer.append(' ');
        } else if (content instanceof BodyPart) {
            BodyPart bodyPart = (BodyPart) content;
            DataHandler handler = bodyPart.getDataHandler();
            String encoding = null;
            if (handler != null) {
                encoding = MimeUtility.getEncoding(handler);
            }
            String fileName = bodyPart.getFileName();
            String contentType = bodyPart.getContentType();
            if (fileName != null) {
                try {
                    fileName = MimeUtility.decodeWord(fileName);
                } catch (MessagingException e) {
                    // happens on unencoded file names! so just ignore it and leave the file name as it is
                }
                URI attachURI = URIGenerator.createNewRandomUniqueURI();
                rdf.add(NMO.hasAttachment, attachURI);
                Model m = rdf.getModel();
                m.addStatement(attachURI, RDF.type, NFO.Attachment);
                m.addStatement(attachURI, NFO.fileName, fileName);
                if (handler != null) {
                    if (encoding != null) {
                        m.addStatement(attachURI, NFO.encoding, encoding);
                    }
                }
                if (contentType != null) {
                    contentType = (new ContentType(contentType)).getBaseType();
                    m.addStatement(attachURI, NIE.mimeType, contentType.trim());
                }
                // TODO: encoding?
            }
            
            // append the content, if any
            content = bodyPart.getContent();
            
            // remove any html markup if necessary
            if (contentType != null && content instanceof String) {
                contentType = contentType.toLowerCase();
                if (contentType.indexOf("text/html") >= 0) {
                    if (encoding != null) {
                        encoding = MimeUtility.javaCharset(encoding);
                    }
                    content = extractTextFromHtml((String) content, encoding, rdf);
                }
            }
            
            processContent(content, buffer, rdf);
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            String subType = null;
            
            String contentType = multipart.getContentType();
            if (contentType != null) {
                ContentType ct = new ContentType(contentType);
                subType = ct.getSubType();
                if (subType != null) {
                    subType = subType.trim().toLowerCase();
                }
            }
            
            if ("alternative".equals(subType)) {
                handleAlternativePart(multipart, buffer, rdf);
            } else if ("signed".equals(subType)) {
                handleProtectedPart(multipart, 0, buffer, rdf);
            } else if ("encrypted".equals(subType)) {
                handleProtectedPart(multipart, 1, buffer, rdf);
            } else {
                // handles multipart/mixed, /digest, /related, /parallel, /report and unknown subtypes
                handleMixedPart(multipart, buffer, rdf);
            }
        }
    }
    
    protected void handleAlternativePart(Multipart multipart, StringBuilder buffer, RDFContainer rdf) throws MessagingException,
                                                                                                     IOException,
                                                                                                     ExtractorException {
        // find the first text/plain part or else the first text/html part
        boolean isHtml = false;
        
        int idx = getPartWithMimeType(multipart, "text/plain");
        int idxh = getPartWithMimeType(multipart, "text/html");
        if (idx < 0) {
            isHtml = true;
        }
        // add nmo:htmlMessageContent property
        if (idxh >= 0) {
            Object html = multipart.getBodyPart(idxh).getContent();
            if (html != null && html instanceof String) {
                rdf.add(NMO.htmlMessageContent, (String) html);
            }
        }
        if (idx >= 0) {
            Object content = multipart.getBodyPart(idx).getContent();
            if (content != null) {
                if (content instanceof String && isHtml) {
                    String encoding = getEncoding(multipart.getBodyPart(idx));
                    if (encoding != null) {
                        encoding = MimeUtility.javaCharset(encoding);
                    }
                    content = extractTextFromHtml((String) content, encoding, rdf);
                }
                
                processContent(content, buffer, rdf);
            }
        }
    }
    
    protected void handleMixedPart(Multipart multipart, StringBuilder buffer, RDFContainer rdf) throws MessagingException,
                                                                                               IOException,
                                                                                               ExtractorException {
        int count = multipart.getCount();
        for (int i = 0; i < count; i++) {
            processContent(multipart.getBodyPart(i), buffer, rdf);
        }
    }
    
    protected void handleProtectedPart(Multipart multipart, int index, StringBuilder buffer, RDFContainer rdf) throws MessagingException,
                                                                                                              IOException,
                                                                                                              ExtractorException {
        if (index < multipart.getCount()) {
            processContent(multipart.getBodyPart(index), buffer, rdf);
        }
    }
    
    protected int getPartWithMimeType(Multipart multipart, String mimeType) throws MessagingException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (mimeType.equalsIgnoreCase(getMimeType(bodyPart))) {
                return i;
            }
        }
        
        return -1;
    }
    
    protected String getContentEncoding(ContentType contentType) {
        if (contentType != null) {
            return contentType.getParameter("charset");
        }
        return null;
    }
    
    protected String getEncoding(Part mailPart) throws MessagingException {
        DataHandler handler = mailPart.getDataHandler();
        if (handler != null) {
            return MimeUtility.getEncoding(handler);
        }
        return null;
    }
    
    protected String getMimeType(Part mailPart) throws MessagingException {
        String contentType = mailPart.getContentType();
        if (contentType != null) {
            ContentType ct = new ContentType(contentType);
            return ct.getBaseType();
        }
        
        return null;
    }
    
    protected String extractTextFromHtml(String string, String charset, RDFContainer rdf) throws ExtractorException {
        // parse the HTML and extract full-text and metadata
        HtmlTextExtractUtil extractor;
        try {
            extractor = new HtmlTextExtractUtil();
        } catch (InitializationException e) {
            throw new ExtractorException("Could not initialize HtmlExtractor: " + e.getMessage());
        }
        InputStream stream = new ByteArrayInputStream(string.getBytes());
        RDFContainerFactory containerFactory = new RDFContainerFactoryImpl();
        URI id = rdf.getDescribedUri();
        RDFContainer result = containerFactory.getRDFContainer(id);
        extractor.extract(id, charset, stream, result);
        Model meta = result.getModel();
        
        // append metadata and full-text to a string buffer
        StringBuilder buffer = new StringBuilder(32 * 1024);
        append(buffer, extractor.getTitle(meta), "\n");
        append(buffer, extractor.getAuthor(meta), "\n");
        append(buffer, extractor.getDescription(meta), "\n");
        List<String> keywords = extractor.getKeywords(meta);
        for (String kw : keywords) {
            append(buffer, kw, " ");
        }
        buffer.append("\n");
        append(buffer, extractor.getText(meta), " ");
        logger.debug("text extracted:\n{}", buffer);
        meta.close();
        
        // return the buffer's content
        return buffer.toString();
    }
    
    protected void append(StringBuilder buffer, String text, String sep) {
        if (text != null) {
            buffer.append(text);
            buffer.append(sep);
        }
    }
    
    protected Address[] getRecipients(MimeMessage message, RecipientType type) throws MessagingException {
        Address[] result = null;
        
        try {
            result = message.getRecipients(type);
        } catch (AddressException e) {
            // ignore
        }
        
        return result;
    }
    
    protected void copyAddress(Object address, URI predicate, RDFContainer result) {
        try {
            if (address instanceof InternetAddress) {
                MailUtil.addAddressMetadata((InternetAddress) address, predicate, result);
            } else if (address instanceof InternetAddress[]) {
                InternetAddress[] array = (InternetAddress[]) address;
                for (int i = 0; i < array.length; i++) {
                    MailUtil.addAddressMetadata(array[i], predicate, result);
                }
            }
        } catch (ModelException e) {
            logger.error("ModelException while adding address metadata", e);
        }
    }
    
    public static void main(String[] args) throws Exception {
        int argv = 0;
        SimpleMailExtractor extractor = new SimpleMailExtractor();
        
        RDFContainerFactory rdfFactory = new RDFContainerFactoryImpl();
        for (int i = argv; i < args.length; ++i) {
            File file = new File(args[i]);
            InputStream in = new FileInputStream(file);
            URI uri = new URIImpl(file.toURI().toString());
            RDFContainer rdfContainer = rdfFactory.getRDFContainer(uri);
            extractor.extract(uri, in, null, null, rdfContainer);
            Model model = rdfContainer.getModel();
            model.writeTo(System.out, Syntax.RdfXml);
            model.close();
        }
    }
}