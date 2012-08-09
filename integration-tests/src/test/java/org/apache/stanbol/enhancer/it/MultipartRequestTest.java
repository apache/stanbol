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
package org.apache.stanbol.enhancer.it;

import static org.apache.stanbol.enhancer.it.MultipartContentItemTestUtils.buildPathWithParams;
import static org.apache.stanbol.enhancer.it.MultipartContentItemTestUtils.getHTMLContent;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PERSON;
import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_PLACE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.HttpMultipart;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.ContentDescriptor;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests RESTful API extensions to the Stanbol Enhancer as described by
 * STANBOL-481
 */
public class MultipartRequestTest extends EnhancerTestBase {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static Serializer serializer = new Serializer();
    static {
        serializer.bindSerializingProvider(new JenaSerializerProvider());
    }

    
    private static final Logger log = LoggerFactory.getLogger(MultipartRequestTest.class);

    private final String TEXT_CONTENT = "The Apache Stanbol Enhancer.\n" +
    		"The Stanbol enhancer can detect famous cities such as Paris and " +
    		"people such as Bob Marley.";
    private final String[] TEXT_CONTENT_LINES = TEXT_CONTENT.split("\n");
    private final String HTML_CONTENT = getHTMLContent(TEXT_CONTENT_LINES);
    private final String[] HTML_CONTENT_LINES = HTML_CONTENT.split("\n");
    public MultipartRequestTest() {
        super(); //use the default endpoint
    }

    @Test
    public void testIllegalRdfFormat() throws IOException {
        String[] params = new String []{
                    "outputContent","*/*",
                    "rdfFormat","notAvalidMimeFormat"};
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(400); //BAD_REQUEST
    }
    @Test
    public void testIllegalOutputContent() throws IOException {
        String[] params = new String []{
                    "outputContent","notAvalidMimeFormat"};
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(400); //BAD_REQUEST
    }
    @Test
    public void testDefaultContentTypes() throws IOException {
        //'*/*', 'text/plain' and 'application/octet-stream' where considered as
        // Indicators that the default RDF serialisation format for the metadata
        // should be used. 
        //This is basically testing backward compatibility 
        
        String[] jsonLDMetadataTests = new String[]{
            "\"@subject\": \"http://dbpedia.org/resource/Paris\",",
            "\"@subject\": \"http://dbpedia.org/resource/Bob_Marley\",",
            "\"dc:creator\": \"org.apache.stanbol.enhancer.engines.entitytagging.impl.NamedEntityTaggingEngine\","
        };
        String[] params = new String []{
                    "outputContent","text/plain"
        };
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","text/plain") //must be multipart/from-data
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200) //metadata as JSONLD
        .assertContentRegexp(jsonLDMetadataTests);
        
        params = new String []{
            "outputContent","application/octet-stream"}; //omitMetadata=false
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","text/plain") //must be multipart/from-data
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200) //metadata as JSONLD
        .assertContentRegexp(jsonLDMetadataTests);

        params = new String []{
           "outputContent","application/octet-stream"}; //omitMetadata=false
        executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","text/plain") //must be multipart/from-data
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200) //metadata as JSONLD
        .assertContentRegexp(jsonLDMetadataTests);
    }
    
    @Test
    public void testOutputMetadataAndAllContent() throws IOException {
        String[] params = new String []{
                    "outputContent","*/*",
                    "rdfFormat","text/rdf+nt"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
            "--contentItem",
            "--contentItem--",
            "Content-Disposition: form-data; name=\"content\"",
            "Content-Type: multipart/alternate; boundary=contentParts; charset=UTF-8",
            "Content-Type: text/plain; charset=UTF-8",
            "Content-Type: text/html",
            "--contentParts",
            "--contentParts--")
        .assertContentContains(TEXT_CONTENT_LINES)
        .assertContentContains(HTML_CONTENT_LINES) //line by line the HTML content
        .assertContentRegexp(
            "Content-Disposition: form-data; name=\"metadata\"; filename=.*",
            "Content-Disposition: form-data; name=\"urn:tika:text:.*",
            "Content-Disposition: form-data; name=\"urn:content-item-sha1-.*",
            //and the expected enhancements in the metadata
            "http://purl.org/dc/terms/creator.*LangIdEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*EngineCore",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley")
        .getContent();
        log.debug("Content:\n{}\n",content);

    }
    @Test
    public void testOutputAllContentOmitMetadata() throws IOException {
        String[] params = new String []{
                    "outputContent","*/*",
                    "omitMetadata","true",
                    "rdfFormat","text/rdf+nt"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
            "--contentItem",
            "--contentItem--",
            "Content-Disposition: form-data; name=\"content\"",
            "Content-Type: multipart/alternate; boundary=contentParts; charset=UTF-8",
            "Content-Type: text/plain; charset=UTF-8",
            "Content-Type: text/html",
            "--contentParts",
            "--contentParts--")
        .assertContentContains(TEXT_CONTENT_LINES)
        .assertContentContains(HTML_CONTENT_LINES) //line by line the HTML content
        .assertContentRegexp( //MUST contain
            "Content-Disposition: form-data; name=\"urn:tika:text:.*",
            "Content-Disposition: form-data; name=\"urn:content-item-sha1-.*")
        .assertContentRegexp(false, //MUST NOT contain
            "Content-Disposition: form-data; name=\"metadata\"; filename=.*",
            //and the expected enhancements in the metadata
            "http://purl.org/dc/terms/creator.*LangIdEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*EngineCore",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley")
        .getContent();
        log.debug("Content:\n{}\n",content);

    }

    @Test
    public void testOutputPlainTextContent() throws IOException {
        String[] params = new String []{
                    "outputContent","text/plain",
                    "rdfFormat","text/rdf+nt"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
            "--contentItem",
            "--contentItem--",
            "Content-Disposition: form-data; name=\"content\"",
            "Content-Type: multipart/alternate; boundary=contentParts; charset=UTF-8",
            "Content-Type: text/plain; charset=UTF-8",
            "--contentParts",
            "--contentParts--")
        .assertContentContains(TEXT_CONTENT_LINES)
        .assertContentRegexp(
            "Content-Disposition: form-data; name=\"metadata\"; filename=.*",
            "Content-Disposition: form-data; name=\"urn:tika:text:.*",
            //and the expected enhancements in the metadata
            "http://purl.org/dc/terms/creator.*LangIdEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*EngineCore",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley")
        .getContent();
        log.debug("Content:\n{}\n",content);

    }
    @Test
    public void testOutputContentOtherThanParsed() throws IOException {
        //metadata and text content
        String[] params = new String []{
                    "outputContent","*/*",
                    "omitParsed","true",
                    "rdfFormat","text/rdf+nt"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/html; charset=UTF-8")
            .withContent(HTML_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
             "--contentItem",
             "--contentItem--",
             "Content-Disposition: form-data; name=\"content\"",
             "Content-Type: multipart/alternate; boundary=contentParts; charset=UTF-8",
             "Content-Type: text/plain; charset=UTF-8",
             "--contentParts",
             "--contentParts--")
         .assertContentContains(TEXT_CONTENT_LINES)
         .assertContentRegexp(
             "Content-Disposition: form-data; name=\"metadata\"; filename=.*",
             "Content-Disposition: form-data; name=\"urn:tika:text:.*",
             //and the expected enhancements in the metadata
             "http://purl.org/dc/terms/creator.*LangIdEnhancementEngine",
             "http://purl.org/dc/terms/language.*en",
             "http://fise.iks-project.eu/ontology/entity-label.*Paris",
             "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*EngineCore",
             "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley")
         .getContent();
        log.debug("Content:\n{}\n",content);
    }
    @Test
    public void testOutputContentPart() throws IOException {
        String[] params = new String []{
                    "outputContentPart","http://stanbol.apache.org/ontology/enhancer/executionmetadata#ChainExecution",
                    "omitMetadata","true",
                    "rdfFormat","application/rdf+xml"};
        String content = executor.execute(
            builder.buildPostRequest(buildPathWithParams(getEndpoint(), params))
            .withHeader("Accept","multipart/from-data")
            .withHeader("Content-Type", "text/plain; charset=UTF-8")
            .withContent(TEXT_CONTENT)
        )
        .assertStatus(200)
        .assertContentContains(
             "--contentItem",
             "--contentItem--",
             "Content-Disposition: form-data; name=\"http://stanbol.apache.org/ontology/enhancer/executionmetadata#ChainExecution\"",
             "Content-Type: application/rdf+xml; charset=UTF-8",
             "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionplan#ExecutionPlan\"/>",
             "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionplan#ExecutionNode\"/>",
             "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionmetadata#EngineExecution\"/>",
             "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionmetadata#ChainExecution\"/>")
         .getContent();
        log.debug("Content:\n{}\n",content);
    }
    /**
     * This uploads the HTML as well as the plain text version of an content.
     * This allows it CMS to parse already available alternate content versions
     * in a single request. Stanbol can than still use the original content
     * (e.g. to extract metadata) but other engines that require the alternate
     * version (e.g. plain text version) of an document will directly use the
     * parsed version .<p>
     * This UnitTest ensures this by adding a "secret" extension the to plain
     * text version and than checks if the two entities mentioned in that
     * part are included in the extracted entities.
     * @throws IOException
     */
    @Test
    public void testUploadMultipleContents() throws IOException {
        //It is a secret, that Berlin is the capital of Germany
        String extraTextConent = TEXT_CONTENT + 
                "\nIt is a secret, that the city of Berlin is the capital of Germany since 1990.";
        
        //The multipart entity for the contentItem
        MultipartEntity contentItem = new MultipartEntity(null, null ,UTF8);
        //The multipart/alternate mime part for the parsed content versions
        HttpMultipart content = new HttpMultipart("alternate", UTF8 ,"contentParts");
        //add the content part to the contentItem
        contentItem.addPart(
            "content", //the name MUST BE "content"
            new MultipartContentBody(content));
        //now add the content (ordering is important, because the first
        //part will be assumed the original document and all following are
        //assumed alternate - transformed - versions
        content.addBodyPart(new FormBodyPart(
            "http://www.example.com/test.html", //the id of the content
            new StringBody(HTML_CONTENT, "text/html", UTF8)));
        content.addBodyPart(new FormBodyPart(
            "http://www.example.com/test.txt",
            new StringBody(extraTextConent, "text/plain", UTF8)));
        
        String receivedContent = executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withEntity(contentItem)
        )
        .assertStatus(200)
        .assertContentRegexp(
            //and the expected enhancements in the metadata
            "http://purl.org/dc/terms/creator.*LangIdEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*EngineCore",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley",
            //check also for expeted entities extracted from the secret Text part!
            "http://fise.iks-project.eu/ontology/entity-label.*Berlin",
            "http://fise.iks-project.eu/ontology/entity-label.*Germany")
        .getContent();
        log.debug("Content:\n{}\n",receivedContent);

    }
    
    
    @Test
    public void testContentBeforeMetadata() throws IOException{
        final UriRef contentItemId = new UriRef("http://www.example.com/test.html");
        String rdfContentType = SupportedFormat.RDF_XML;
        String rdfContent = getDummyRdfMetadata(contentItemId, rdfContentType);
        MultipartEntity contentItem = new MultipartEntity(null, null ,UTF8);
        //first the content -> illegal
        contentItem.addPart(
            "content", //the name MUST BE "content"
            new StringBody(HTML_CONTENT,"text/html",UTF8));
        //after that the metadata
        contentItem.addPart(
            "metadata", //the name MUST BE "metadata" 
            new StringBody(rdfContent,rdfContentType,UTF8));

        String receivedContent = executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withEntity(contentItem)
        )
        .assertStatus(400) //BAD request
        .getContent();
        //check also the error message
        Assert.assertTrue(receivedContent.contains(
            "The Multipart MIME part with the 'metadata' MUST BE before the " +
            "MIME part containing the 'content'"));
    }
    @Test
    public void testMissingContent() throws IOException{
        final UriRef contentItemId = new UriRef("http://www.example.com/test.html");
        String rdfContentType = SupportedFormat.RDF_XML;
        String rdfContent = getDummyRdfMetadata(contentItemId, rdfContentType);
        MultipartEntity contentItem = new MultipartEntity(null, null ,UTF8);
        //after that the metadata
        contentItem.addPart(
            "metadata", //the name MUST BE "metadata" 
            new StringBody(rdfContent,rdfContentType,UTF8));

        String receivedContent = executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withEntity(contentItem)
        )
        .assertStatus(400) //BAD request
        .getContent();
        //check also the error message
        Assert.assertTrue(receivedContent.contains(
            "The parsed multipart content item does not contain any content."));
    }

    /**
     * @param contentItemId
     * @param rdfContentType
     * @return
     */
    private String getDummyRdfMetadata(final UriRef contentItemId, String rdfContentType) {
        MGraph metadata = new SimpleMGraph();
        metadata.add(new TripleImpl(new BNode(), Properties.ENHANCER_EXTRACTED_FROM, contentItemId));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, metadata, rdfContentType);
        String rdfContent = new String(out.toByteArray(),UTF8);
        return rdfContent;
    }
    
    /**
     * Stanbol also supports to upload pre-existing metadata with the content.
     * This UnitTest uses an example that parsed TextAnnotations for free text
     * tags provided by users that are than linked to Entities in DBPedia
     * @throws IOException
     */
    @Test
    public void testUploadWithMetadata() throws IOException {
        //create the metadata
        Resource user = new PlainLiteralImpl("Rupert Westenthaler");
        final UriRef contentItemId = new UriRef("http://www.example.com/test.html");
        MGraph metadata = new SimpleMGraph();
        addTagAsTextAnnotation(metadata, contentItemId, 
            "Germany",DBPEDIA_PLACE, user);
        addTagAsTextAnnotation(metadata, contentItemId, 
            "Europe",DBPEDIA_PLACE, user);
        addTagAsTextAnnotation(metadata, contentItemId, 
            "NATO",DBPEDIA_ORGANISATION, user);
        addTagAsTextAnnotation(metadata, contentItemId, 
            "Silvio Berlusconi",DBPEDIA_PERSON, user);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, metadata, SupportedFormat.RDF_XML);
        String rdfContent = new String(out.toByteArray(),UTF8);
        
        //The multipart entity for the contentItem
        MultipartEntity contentItem = new MultipartEntity(null, null ,UTF8);
        //the "metadata" MUST BE the first element
        /*
         * NOTE: We need here to override the getFilename, because this MUST
         *       BE the URI of the ContentItem. This is important, because the
         *       Metadata do contain triples about that ContentItem and therefore
         *       it MUST BE assured that the URI of the ContentItem created by
         *       the Stanbol Enhancer is the same of as the URI used in the
         *       Metadata!
         */
        contentItem.addPart(
            "metadata", //the name MUST BE "metadata" 
            new StringBody(rdfContent,SupportedFormat.RDF_XML,UTF8){
                @Override
                public String getFilename() { //The filename MUST BE the
                    return contentItemId.getUnicodeString(); //uri of the ContentItem
                }
            });
        //Add the Content
        /*
         * NOTE: If we only parse a single content than we can also directly
         *       add it with the name "content". This means that the useage of
         *       a "multipart/alternate" container is in such cases optional.
         */
        contentItem.addPart(
            "content", //the name MUST BE "content"
            new StringBody(HTML_CONTENT,"text/html",UTF8));
        
        //send the request
        String receivedContent = executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withEntity(contentItem)
        )
        .assertStatus(200)
        .assertContentRegexp(
            //and the expected enhancements based on the parsed content
            "http://purl.org/dc/terms/creator.*LangIdEnhancementEngine",
            "http://purl.org/dc/terms/language.*en",
            "http://fise.iks-project.eu/ontology/entity-label.*Paris",
            "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*EngineCore",
            "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley",
            //additional enhancements based on parsed metadata
            "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Germany.*",
            "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/NATO.*",
            "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Silvio_Berlusconi.*",
            "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Europe.*")
        .getContent();
        log.debug("Content:\n{}\n",receivedContent);
    }
    /**
     * Utility that creates an {@link TechnicalClasses#ENHANCER_TEXTANNOTATION TextAnnotation}
     * for the parsed contentItem, free text tag an user. 
     * @param graph the grpah to add the information
     * @param contentItem the {@link ContentItem#getUri() uri} of the {@link ContentItem}
     * @param tag the free text tag for the document
     * @param tagType the type of the tag. Typically Stanbol supports: <ul>
     * <li>{@link OntologicalClasses#DBPEDIA_PERSON}
     * <li>{@link OntologicalClasses#DBPEDIA_ORGANISATION}
     * <li>{@link OntologicalClasses#DBPEDIA_PLACE}
     * </ul>
     * But specific {@link EnhancementEngine}s might also process other types
     * or even TextAnnotations without an type
     * @param user the user that created the tag
     * @return the uri of the created annotation
     */
    private static final UriRef addTagAsTextAnnotation(MGraph graph, UriRef contentItem, 
                                                       String tag, UriRef tagType, Resource user){
        UriRef ta = new UriRef("urn:user-annotation:"+EnhancementEngineHelper.randomUUID());
        graph.add(new TripleImpl(ta, RDF.type, TechnicalClasses.ENHANCER_TEXTANNOTATION));
        graph.add(new TripleImpl(ta, Properties.ENHANCER_EXTRACTED_FROM,contentItem));
        if(tagType != null){
            graph.add(new TripleImpl(ta, Properties.DC_TYPE, tagType));
        }
        graph.add(new TripleImpl(ta, Properties.ENHANCER_SELECTED_TEXT, new PlainLiteralImpl(tag)));
        graph.add(new TripleImpl(ta, RDF.type, TechnicalClasses.ENHANCER_ENHANCEMENT));
        if(user != null){
            graph.add(new TripleImpl(ta, Properties.DC_CREATOR,user));
        }
        return ta;
    }    
    /**
     * Supports sending multipart mime as {@link ContentBody}.
     * TODO: maybe move such utilities to an own Multipart ContentItem
     * utility module
     * @author Rupert Westenthaler
     *
     */
    private static class MultipartContentBody extends AbstractContentBody implements ContentBody,ContentDescriptor {

        private HttpMultipart multipart;

        public MultipartContentBody(HttpMultipart multipart){
            super(String.format("multipart/%s; boundary=%s",
                multipart.getSubType(), multipart.getBoundary()));
            this.multipart = multipart;
        }
        @Override
        public String getCharset() {
            return multipart.getCharset().toString();
        }

        @Override
        public String getTransferEncoding() {
            return MIME.ENC_8BIT;
        }

        @Override
        public long getContentLength() {
            return multipart.getTotalLength();
        }

        @Override
        public String getFilename() {
            return null;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            multipart.writeTo(out);
        }
        
    }
}
