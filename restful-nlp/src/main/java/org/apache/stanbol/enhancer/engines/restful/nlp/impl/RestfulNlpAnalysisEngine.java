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
package org.apache.stanbol.enhancer.engines.restful.nlp.impl;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.NER_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.print.attribute.TextSyntax;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.json.AnalyzedTextParser;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Span.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An enhancement engine that uses a RESTful service for NLP processing of
 * the pain text content part of processed {@link ContentItem}s.<p>
 * The RESTful API of the remote service is standardised by  
 * <a href="https://issues.apache.org/jira/browse/STANBOL-892">STANBOL-892</a> <p>
 * 
 * @author Rupert Westenthaler
 */

@Component(immediate = true, metatype = true,  
    policy = ConfigurationPolicy.REQUIRE, configurationFactory=true)
@Service
@Properties(value={
        @Property(name= EnhancementEngine.PROPERTY_NAME,value="changeme"),
        @Property(name=RestfulNlpAnalysisEngine.CONFIG_LANGUAGES, value = {"*"},cardinality=Integer.MAX_VALUE),
        @Property(name=RestfulNlpAnalysisEngine.ANALYSIS_SERVICE_URL, value ="http://changeme"),
        @Property(name=RestfulNlpAnalysisEngine.ANALYSIS_SERVICE_USER, value =""),
        @Property(name=RestfulNlpAnalysisEngine.ANALYSIS_SERVICE_PWD, value =""),
        @Property(name=Constants.SERVICE_RANKING,intValue=0)
})
public class RestfulNlpAnalysisEngine extends AbstractEnhancementEngine<IOException,RuntimeException> implements ServiceProperties {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    /**
     * The URI for the remote analyses service
     */
    public static final String ANALYSIS_SERVICE_URL = "enhancer.engine.restful.nlp.analysis.service";
    /**
     * The User for the remote analyses service
     */
    public static final String ANALYSIS_SERVICE_USER = "enhancer.engine.restful.nlp.analysis.service.user";
    /**
     * The User for the remote analyses service
     */
    public static final String ANALYSIS_SERVICE_PWD = "enhancer.engine.restful.nlp.analysis.service.pwd";
        
    /**
     * Language configuration. Takes a list of ISO language codes to be processed
     * by this engine. This list will be joined with the list of languages supported
     * by the RESTful NLP analysis service.
     */
    public static final String CONFIG_LANGUAGES = "enhancer.engine.restful.nlp.languages";

    /**
     * The maximum size of the preix/suffix for the selection context
     */
    private static final int DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE = 50;

    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        //by default register as Tokenizing engine
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_TOKENIZING);
//        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
//            NlpProcessingRole.Tokenizing);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }


    private static Logger log = LoggerFactory.getLogger(RestfulNlpAnalysisEngine.class);

    private URI analysisServiceUrl;
    
    //Langauge configuration
    private LanguageConfiguration languageConfig = new LanguageConfiguration(CONFIG_LANGUAGES,new String[]{"*"});
    
    private final Set<String> supportedLanguages = new HashSet<String>();
    
    protected DefaultHttpClient httpClient;
    private BasicHttpParams httpParams;
    private PoolingClientConnectionManager connectionManager;
    /**
     * List of HttpHeaders reused for each request. This avoids to re-create them
     * for every request
     */
    private static List<? extends Header> DEFAULT_HEADERS = Arrays.asList(
        new BasicHeader(HttpHeaders.ACCEPT_ENCODING, UTF8.name()),
        new BasicHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset="+UTF8.name()));
    
    @Reference
    private AnalysedTextFactory analysedTextFactory;
    
    /**
     * Used to parse {@link AnalysedText} instances from responses of the
     * RESTful analysis service.
     */
    @Reference
    private AnalyzedTextParser analyzedTextParser;
    
    /**
     * Indicate if this engine can enhance supplied ContentItem, and if it
     * suggests enhancing it synchronously or asynchronously. The
     * {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager} can force sync/async mode if desired, it is
     * just a suggestion from the engine.
     * <p/>
     * Returns ENHANCE_ASYNC in case there is a text/plain content part and a tagger for the language identified for
     * the content item, CANNOT_ENHANCE otherwise.
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the introspecting process of the content item
     *          fails
     */
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        Map.Entry<UriRef,Blob> entry = NlpEngineHelper.getPlainText(this, ci, false);
        if(entry == null || entry.getValue() == null) {
            return CANNOT_ENHANCE;
        }

        String language = getLanguage(this,ci,false);
        if(language == null) {
            return CANNOT_ENHANCE;
        }
        if(!languageConfig.isLanguage(language)){
            log.trace(" > can NOT enhance ContentItem {} because language {} is "
                + "not enabled by this engines configuration",ci,language);
            return CANNOT_ENHANCE;
        }
        if(!supportedLanguages.contains(language)){
            log.trace(" > the RESTful Analysis service does not support '{}' (supported: {})",
                language, supportedLanguages);
            return CANNOT_ENHANCE;
        }
        log.trace(" > can enhance ContentItem {} with language {}",ci,language);
        return ENHANCE_ASYNC;
    }

    /**
     * Compute enhancements for supplied ContentItem. The results of the process
     * are expected to be stored in the metadata of the content item.
     * <p/>
     * The client (usually an {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager}) should take care of
     * persistent storage of the enhanced {@link org.apache.stanbol.enhancer.servicesapi.ContentItem}.
     * <p/>
     * This method creates a new POSContentPart using {@link org.apache.stanbol.enhancer.engines.pos.api.POSTaggerHelper#createContentPart} from a text/plain part and
     * stores it as a new part in the content item. The metadata is not changed.
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the underlying process failed to work as
     *          expected
     */
    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        //get the plain text Blob
        Map.Entry<UriRef,Blob> textBlob = NlpEngineHelper.getPlainText(this, ci, false);
        Blob blob = textBlob.getValue();
        //send the text to the server
        String language = getLanguage(this, ci, true);
        HttpPost request = new HttpPost(analysisServiceUrl);
        request.addHeader(HttpHeaders.CONTENT_LANGUAGE, language);
        request.setEntity(new InputStreamEntity(
            blob.getStream(), blob.getContentLength(),
            ContentType.create(blob.getMimeType(), 
                blob.getParameter().get("charset"))));
        //execute the request
        AnalysedText at;
        try {
            at = httpClient.execute(request, new AnalysisResponseHandler(ci, textBlob));
        } catch (ClientProtocolException e) {
            throw new EngineException(this, ci, "Exception while executing Request "
                + "on RESTful NLP Analysis Service at "+analysisServiceUrl, e);
        } catch (IOException e) {
            throw new EngineException(this, ci, "Exception while executing Request "
                    + "on RESTful NLP Analysis Service at "+analysisServiceUrl, e);
        }
        Iterator<Span> spans = at.getEnclosed(EnumSet.of(SpanTypeEnum.Sentence,SpanTypeEnum.Chunk));
        Sentence context = null;
        MGraph metadata = ci.getMetadata();
        Language lang = new Language(language);
        LiteralFactory lf = LiteralFactory.getInstance();
        ci.getLock().writeLock().lock();
        try { //write TextAnnotations for Named Entities
            while(spans.hasNext()){
                Span span = spans.next();
                switch (span.getType()) {
                    case Sentence:
                        context = (Sentence)context;
                        break;
                    default:
                        Value<NerTag> nerAnno = span.getAnnotation(NER_ANNOTATION);
                        if(nerAnno != null){
                            UriRef ta = EnhancementEngineHelper.createTextEnhancement(ci, this);
                            //add span related data
                            metadata.add(new TripleImpl(ta, ENHANCER_SELECTED_TEXT, 
                                new PlainLiteralImpl(span.getSpan(), lang)));
                            metadata.add(new TripleImpl(ta, ENHANCER_START, 
                                lf.createTypedLiteral(span.getStart())));
                            metadata.add(new TripleImpl(ta, ENHANCER_END, 
                                lf.createTypedLiteral(span.getEnd())));
                            metadata.add(new TripleImpl(ta, ENHANCER_SELECTION_CONTEXT, 
                                new PlainLiteralImpl(context == null ?
                                        getDefaultSelectionContext(at.getSpan(), span.getSpan(), span.getStart()) :
                                            context.getSpan(), lang)));
                            //add the NER type
                            if(nerAnno.value().getType() != null){
                                metadata.add(new TripleImpl(ta,DC_TYPE,nerAnno.value().getType()));
                            }
                            if(nerAnno.probability() >= 0) {
                                metadata.add(new TripleImpl(ta, ENHANCER_CONFIDENCE, 
                                    lf.createTypedLiteral(nerAnno.probability())));
                            }
                        }
                        break;
                }
            }
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    protected class AnalysisResponseHandler implements ResponseHandler<AnalysedText>{
        
        protected final ContentItem ci;
        protected final Entry<UriRef,Blob> textBlob;


        protected AnalysisResponseHandler(ContentItem ci, Map.Entry<UriRef,Blob> textBlob){
            this.ci = ci;
            this.textBlob = textBlob;
        }

        @Override
        public AnalysedText handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
            StatusLine statusLine = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (statusLine.getStatusCode() >= 300) {
                EntityUtils.consume(entity);
                throw new HttpResponseException(statusLine.getStatusCode(),
                    statusLine.getReasonPhrase());
            }
            //parse the results
            InputStream in = null;
            try {
                in = entity.getContent();
                Charset charset = entity.getContentEncoding() != null ? 
                        Charset.forName(entity.getContentEncoding().getValue()) : UTF8;
                //parse the received data and add it to the AnalysedText of the 
                //contentItem
                return parseAnalysedText(ci, textBlob, in, charset);
            } finally {
                //ensure that the stream is closed
                IOUtils.closeQuietly(in);
            }
        }
    }
    
    /**
     * @param ci
     * @param entry
     * @param in
     * @param charset
     * @throws EngineException
     */
    private AnalysedText parseAnalysedText(ContentItem ci, Map.Entry<UriRef,Blob> entry,
            InputStream in,Charset charset) throws IOException {
        AnalysedText at;
        ci.getLock().writeLock().lock();
        try {
            at = analysedTextFactory.createAnalysedText(ci, entry.getValue());
        } finally {
            ci.getLock().writeLock().unlock();
        }
        analyzedTextParser.parse(in, charset, at);
        return at;
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }
    /**
     * Activate and read the properties. Configures and initialises a POSTagger for each language configured in
     * CONFIG_LANGUAGES.
     *
     * @param ce the {@link org.osgi.service.component.ComponentContext}
     */
    @Activate
    protected void activate(ComponentContext ce) throws ConfigurationException, IOException {
        super.activate(ce);
        log.info("activate {} '{}'",getClass().getSimpleName(),getName());
        @SuppressWarnings("unchecked")
        Dictionary<String, Object> properties = ce.getProperties();
        languageConfig.setConfiguration(properties);

        Object value = properties.get(ANALYSIS_SERVICE_URL);
        if(value == null){
            throw new ConfigurationException(ANALYSIS_SERVICE_URL, 
                "The RESTful Analysis Service URL is missing in the provided configuration!");
        } else {
            try {
                analysisServiceUrl = new URI(value.toString());
                log.info("  ... service: {}",analysisServiceUrl);
            } catch (URISyntaxException e) {
                throw new ConfigurationException(ANALYSIS_SERVICE_URL, 
                        "The parsed RESTful Analysis Service URL '"+ value
                        + "'is not a valid URL!",e);
            }
        }
        String usr;
        String pwd;
        value = properties.get(ANALYSIS_SERVICE_USER);
        if(value != null && !value.toString().isEmpty()){
            usr = value.toString();
            value = properties.get(ANALYSIS_SERVICE_PWD);
            pwd = value == null ? null : value.toString();
        } else { // no user set
            usr = null;
            pwd = null;
        }
        
        //init the http client
        httpParams = new BasicHttpParams();
        httpParams.setParameter(CoreProtocolPNames.USER_AGENT, "Apache Stanbol RESTful NLP Analysis Engine");
        httpParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
        httpParams.setIntParameter(ClientPNames.MAX_REDIRECTS, 3);
        httpParams.setBooleanParameter(CoreConnectionPNames.SO_KEEPALIVE, true);

        connectionManager = new PoolingClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(20);

        httpClient = new DefaultHttpClient(connectionManager,httpParams);
        if(usr != null){
            log.info("  ... setting user to {}",usr);
            httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(usr, pwd));
            // And add request interceptor to have preemptive authentication
            httpClient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
        }
        //get the supported languages
        String supported = httpClient.execute(new HttpGet(analysisServiceUrl), 
            new BasicResponseHandler());
        StringTokenizer st = new StringTokenizer(supported, "{[\",]}");
        while(st.hasMoreElements()){
            supportedLanguages.add(st.nextToken());
        }
        
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        languageConfig.setDefault();
        supportedLanguages.clear();
        //shutdown the Http Client
        httpClient = null;
        httpParams = null;
        connectionManager.shutdown();
        connectionManager = null;
        super.deactivate(context);
    }
    
    /**
     * Extracts the selection context based on the content, selection and
     * the start char offset of the selection
     * @param content the content
     * @param selection the selected text
     * @param selectionStartPos the start char position of the selection
     * @return the context
     */
    private String getDefaultSelectionContext(String content, String selection,int selectionStartPos){
        //extract the selection context
        int beginPos;
        if(selectionStartPos <= DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE){
            beginPos = 0;
        } else {
            int start = selectionStartPos-DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            beginPos = content.indexOf(' ',start);
            if(beginPos < 0 || beginPos >= selectionStartPos){ //no words
                beginPos = start; //begin within a word
            }
        }
        int endPos;
        if(selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE >= content.length()){
            endPos = content.length();
        } else {
            int start = selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            endPos = content.lastIndexOf(' ', start);
            if(endPos <= selectionStartPos+selection.length()){
                endPos = start; //end within a word;
            }
        }
        return content.substring(beginPos, endPos);
    }
    
    /**
     * HttpRequestInterceptor for preemptive authentication, based on httpclient
     * 4.0 example
     */
    private static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {

        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {

            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
            CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
            HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);

            // If not auth scheme has been initialized yet
            if (authState.getAuthScheme() == null) {
                AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());

                // Obtain credentials matching the target host
                Credentials creds = credsProvider.getCredentials(authScope);

                // If found, generate BasicScheme preemptively
                if (creds != null) {
                    authState.update(new BasicScheme(), creds);
                }
            }
        }
    }
}
