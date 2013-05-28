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
package org.apache.stanbol.contenthub.ldpath.solr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.contenthub.servicesapi.Constants;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDPathException;
import org.apache.stanbol.entityhub.ldpath.backend.SiteManagerBackend;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.model.fields.FieldMapping;
import at.newmedialab.ldpath.model.programs.Program;
import at.newmedialab.ldpath.parser.ParseException;
import at.newmedialab.ldpath.parser.RdfPathParser;

/**
 * Class containing utility methods for LDPath functionalities.
 * 
 * @author anil.sinaci
 * 
 */
public class LDPathUtils {
    private static final Logger logger = LoggerFactory.getLogger(LDPathUtils.class);

    public static final String NS_XSD = "http://www.w3.org/2001/XMLSchema#";

    private static final String SOLR_CORE_PATH = "solr/core/";
    private static final String SOLR_TEMPLATE_NAME = "template";
    private static final String SOLR_TEMPLATE_ZIP = SOLR_TEMPLATE_NAME + ".zip";
    private static final String SOLR_TEMPLATE_SCHEMA = SOLR_TEMPLATE_NAME + "/conf/schema-template.xml";
    private static final String SOLR_SCHEMA = "/conf/schema.xml";
    private static final String SOLR_ALLTEXT_FIELD = "stanbolreserved_text_all";

    private static final int BUFFER_SIZE = 8024;

    private static final Set<String> SOLR_FIELD_OPTIONS;
    static {
        HashSet<String> opt = new HashSet<String>();
        opt.add("default");
        opt.add("indexed");
        opt.add("stored");
        opt.add("compressed");
        opt.add("compressThreshold");
        opt.add("multiValued");
        opt.add("omitNorms");
        opt.add("omitTermFreqAndPositions");
        opt.add("termVectors");
        opt.add("termPositions");
        opt.add("termOffsets");

        SOLR_FIELD_OPTIONS = Collections.unmodifiableSet(opt);
    }
    private static final String SOLR_COPY_FIELD_OPTION = "copy";

    /**
     * A map mapping from XSD types to SOLR types.
     */
    private static final Map<String,String> xsdSolrTypeMap;
    static {
        Map<String,String> typeMap = new HashMap<String,String>();

        typeMap.put(NS_XSD + "decimal", "long");
        typeMap.put(NS_XSD + "integer", "int");
        typeMap.put(NS_XSD + "int", "int");
        typeMap.put(NS_XSD + "long", "long");
        typeMap.put(NS_XSD + "short", "int");
        typeMap.put(NS_XSD + "double", "double");
        typeMap.put(NS_XSD + "float", "float");
        typeMap.put(NS_XSD + "dateTime", "date");
        typeMap.put(NS_XSD + "date", "date");
        typeMap.put(NS_XSD + "time", "date");
        typeMap.put(NS_XSD + "boolean", "boolean");
        typeMap.put(NS_XSD + "anyURI", "uri");
        typeMap.put(NS_XSD + "string", "string");

        xsdSolrTypeMap = Collections.unmodifiableMap(typeMap);
    }

    private Bundle bundle;

    private SiteManager referencedSiteManager;

    /**
     * Constructor taking a {@link Bundle} parameter. This bundle is used when obtaining Solr schema template.
     * 
     * @param bundle
     *            From which the template Solr schema is obtained.
     */
    public LDPathUtils(Bundle bundle, SiteManager referencedSiteManager) {
        this.bundle = bundle;
        this.referencedSiteManager = referencedSiteManager;
    }

    /**
     * Return the SOLR field type for the XSD type passed as argument. The xsdType needs to be a fully
     * qualified URI. If no field type is defined, will return null.
     * 
     * @param xsdType
     *            a URI identifying the XML Schema datatype
     * @return
     */
    public String getSolrFieldType(String xsdType) {
        String result = xsdSolrTypeMap.get(xsdType);
        if (result == null) {
            logger.error("Could not find SOLR field type for type " + xsdType);
            return null;
        } else {
            return result;
        }
    }

    /**
     * Creates a {@link Reader} instance from the given program string.
     * 
     * @param program
     * @return a {@link InputStreamReader}.
     * @throws LDPathException
     *             if {@link Constants#DEFAULT_ENCODING} is not supported
     */
    public static Reader constructReader(String program) throws LDPathException {
        try {
            return new InputStreamReader(new ByteArrayInputStream(
                    program.getBytes(Constants.DEFAULT_ENCODING)), Constants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            String msg = String.format("Encoding %s should be supported by the system",
                Constants.DEFAULT_ENCODING);
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        }
    }

    private Program<Object> getLDPathProgram(String ldPathProgram) throws LDPathException {
        if (ldPathProgram == null || ldPathProgram.isEmpty()) {
            String msg = "LDPath Program cannot be null.";
            logger.error(msg);
            throw new LDPathException(msg);
        }
        RDFBackend<Object> rdfBackend = new SiteManagerBackend(referencedSiteManager);
        RdfPathParser<Object> LDparser = new RdfPathParser<Object>(rdfBackend, constructReader(ldPathProgram));
        Program<Object> program = null;
        try {
            program = LDparser.parseProgram();
        } catch (ParseException e) {
            String msg = "Cannot parse LDPath Program";
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        }
        return program;
    }

    /**
     * This method creates an {@link ArchiveInputStream} containing Solr schema configurations based on the
     * provided <code>ldPathProgram</code>. All folders and files except <b>"schema-template.xml"</b> is took
     * from a default Solr configuration template which is located in the resources of the bundle specified in
     * the constructor of this class i.e {@link LDPathUtils}. Instead of the "schema-template" file, a
     * <b>"schema.xml"</b> is created.
     * 
     * @param coreName
     *            Name of the Solr core that is used instead of template
     * @param ldPathProgram
     *            Program for which the Solr core will be created
     * @return {@link ArchiveInputStream} containing the Solr configurations for the provided
     *         <code>ldPathProgram</code>
     * @throws LDPathException
     */
    public ArchiveInputStream createSchemaArchive(String coreName, String ldPathProgram) throws LDPathException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
        ArchiveStreamFactory asf = new ArchiveStreamFactory();
        TarArchiveOutputStream tarOutputStream = null;
        try {
            tarOutputStream = (TarArchiveOutputStream) asf.createArchiveOutputStream("tar", out);
        } catch (ArchiveException e) {
            String msg = "Cannot create an empty tar archive";
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        }

        try {
            InputStream is = getSolrTemplateStream();
            ZipArchiveInputStream zis = new ZipArchiveInputStream(is);
            ZipArchiveEntry ze = null;
            byte[] schemaFile = null;
            while ((ze = zis.getNextZipEntry()) != null) {
                if (SOLR_TEMPLATE_SCHEMA.equals(ze.getName())) {
                    schemaFile = createSchemaXML(getLDPathProgram(ldPathProgram), IOUtils.toByteArray(zis));
                    TarArchiveEntry te = new TarArchiveEntry(coreName + SOLR_SCHEMA);
                    te.setSize(schemaFile.length);
                    tarOutputStream.putArchiveEntry(te);
                    tarOutputStream.write(schemaFile);
                    tarOutputStream.closeArchiveEntry();
                } else {
                    TarArchiveEntry te = new TarArchiveEntry(ze.getName().replaceAll(SOLR_TEMPLATE_NAME,
                        coreName));
                    te.setSize(ze.getSize());
                    tarOutputStream.putArchiveEntry(te);
                    tarOutputStream.write(IOUtils.toByteArray(zis));
                    tarOutputStream.closeArchiveEntry();
                }

            }
            if (schemaFile == null) {
                throw new LDPathException("Schema template ZIP should include: " + SOLR_TEMPLATE_SCHEMA);
            }
            tarOutputStream.finish();
            tarOutputStream.close();
        } catch (IOException e) {
            logger.error("", e);
            throw new LDPathException(e);
        }

        ArchiveInputStream ret;
        try {
            ret = asf.createArchiveInputStream(new ByteArrayInputStream(out.toByteArray()));
        } catch (ArchiveException e) {
            String msg = "Cannot create a final tar archive while creating an ArchiveInputStream to create a Solr core";
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        }
        return ret;
    }

    private InputStream getSolrTemplateStream() throws LDPathException {
        String solrCorePath = SOLR_CORE_PATH;
        //The name of a resource is independent of the Java implementation; in particular, the path separator is always a slash (/). 
        if (!solrCorePath.endsWith("/")) solrCorePath += '/';
        String templateZip = solrCorePath + SOLR_TEMPLATE_ZIP;

        URL resource = bundle.getEntry(templateZip);
        InputStream is = null;
        try {
            is = resource != null ? resource.openStream() : null;
        } catch (IOException e) {
            String msg = "Cannot open input stream on URL resource gathered from bundle.getEntry()";
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        }
        if (is == null) {
            String msg = "Solr Template ZIP cannot be found in:" + templateZip;
            logger.error(msg);
            throw new LDPathException(msg);
        }
        return is;
    }

    /**
     * Creates <b>"schema.xml"</b> file for the Solr configurations to be created for the provided LDPath
     * program. Creates <b>Solr fields</b> for each field obtained by calling {@link Program#getFields()} of
     * provided <code>program</code>. By default, <i>name</i>, <i>type</i>, <i>stored</i>, <i>indexed</i> and
     * <i>multiValued</i> attributes of fields are set. Furthermore, any attribute obtained from the fields of
     * the program is also set if it is included in {@link LDPathUtils#SOLR_FIELD_OPTIONS}. Another
     * configuration about the fields obtained from the program is {@link LDPathUtils#SOLR_COPY_FIELD_OPTION}.
     * If there is a specified configuration about this field, <b>destination</b> of <b>copyField</b> element
     * is set accordingly. Otherwise, the destination is set as <b>stanbolreserved_text_all</b>
     * 
     * @param program
     *            LDPath program of which fields will be obtained
     * @param template
     *            Solr schema template to be populated with the fields based on the provided
     *            <code>program</code>
     * @return created template in an array of bytes.
     * @throws LDPathException
     */
    private byte[] createSchemaXML(Program<Object> program, byte[] template) throws LDPathException {
        Document document;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(template));
        } catch (SAXException e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        } catch (IOException e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        } catch (ParserConfigurationException e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        }

        Element schemaNode = document.getDocumentElement();
        NodeList fieldsNodeList = schemaNode.getElementsByTagName("fields");
        if (fieldsNodeList.getLength() != 1) {
            throw new LDPathException("Template is an invalid SOLR schema. It should be a valid a byte array");
        }

        Node fieldsNode = fieldsNodeList.item(0);

        for (FieldMapping<?,Object> fieldMapping : program.getFields()) {
            String fieldName = fieldMapping.getFieldName();
            String solrType = getSolrFieldType(fieldMapping.getFieldType());

            if (solrType == null) {
                logger.error("field {} has an invalid field type; ignoring field definition", fieldName);
            } else {
                Element fieldElement = document.createElement("field");
                fieldElement.setAttribute("name", fieldName);
                fieldElement.setAttribute("type", solrType);
                fieldElement.setAttribute("stored", "true");
                fieldElement.setAttribute("indexed", "true");
                fieldElement.setAttribute("multiValued", "true");

                // Handle extra field configuration
                final Map<String,String> fieldConfig = fieldMapping.getFieldConfig();
                if (fieldConfig != null) {
                    for (String attr : fieldConfig.keySet()) {
                        if (SOLR_FIELD_OPTIONS.contains(attr)) {
                            fieldElement.setAttribute(attr, fieldConfig.get(attr));
                        }
                    }
                }
                fieldsNode.appendChild(fieldElement);

                if (fieldConfig != null && fieldConfig.keySet().contains(SOLR_COPY_FIELD_OPTION)) {
                    String[] copyFields = fieldConfig.get(SOLR_COPY_FIELD_OPTION).split(",\\s*");
                    for (String copyField : copyFields) {
                        Element copyElement = document.createElement("copyField");
                        copyElement.setAttribute("source", fieldName);
                        copyElement.setAttribute("dest", copyField);
                        schemaNode.appendChild(copyElement);
                    }
                } else {
                    Element copyElement = document.createElement("copyField");
                    copyElement.setAttribute("source", fieldName);
                    copyElement.setAttribute("dest", SOLR_ALLTEXT_FIELD);
                    schemaNode.appendChild(copyElement);
                }
            }
        }

        DOMImplementationRegistry registry;
        try {
            registry = DOMImplementationRegistry.newInstance();
        } catch (ClassCastException e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        } catch (ClassNotFoundException e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        } catch (InstantiationException e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        } catch (IllegalAccessException e) {
            String msg = e.getMessage();
            logger.error(msg, e);
            throw new LDPathException(msg, e);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DOMImplementationLS lsImpl = (DOMImplementationLS) registry.getDOMImplementation("LS");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();
        LSOutput lsOutput = lsImpl.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        lsOutput.setByteStream(baos);
        lsSerializer.write(document, lsOutput);
        String schemaStr = new String(baos.toByteArray());
        return schemaStr.getBytes();
    }

}
