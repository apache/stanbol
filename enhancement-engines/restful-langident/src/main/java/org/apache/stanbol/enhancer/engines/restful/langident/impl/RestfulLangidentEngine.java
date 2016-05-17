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
package org.apache.stanbol.enhancer.engines.restful.langident.impl;

import static java.util.Collections.singleton;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.DCTERMS_LINGUISTIC_SYSTEM;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An enhancement engine that uses a RESTful service for NLP processing of
 * the pain text content part of processed {@link ContentItem}s.<p>
 * The RESTful API of the remote service is standardised by 
 * <a href="https://issues.apache.org/jira/browse/STANBOL-894">STANBOL-894</a> <p>
 * 
 * @author Rupert Westenthaler
 */

@Component(immediate = true, metatype = true,
    policy = ConfigurationPolicy.REQUIRE, configurationFactory=true)
@Service
@Properties(value={
        @Property(name= EnhancementEngine.PROPERTY_NAME,value="changeme"),
        @Property(name=RestfulLangidentEngine.ANALYSIS_SERVICE_URL, value ="http://changeme"),
        @Property(name=RestfulLangidentEngine.ANALYSIS_SERVICE_USER, value =""),
        @Property(name=RestfulLangidentEngine.ANALYSIS_SERVICE_PWD, value =""),
        @Property(name=Constants.SERVICE_RANKING,intValue=0)
})
public class RestfulLangidentEngine extends AbstractEnhancementEngine<IOException,RuntimeException> implements ServiceProperties {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    /**
     * The URI for the remote analyses service
     */
    public static final String ANALYSIS_SERVICE_URL = "enhancer.engine.restful.langident.service";
    /**
     * The User for the remote analyses service
     */
    public static final String ANALYSIS_SERVICE_USER = "enhancer.engine.restful.langident.service.user";
    /**
     * The User for the remote analyses service
     */
    public static final String ANALYSIS_SERVICE_PWD = "enhancer.engine.restful.langident.service.pwd";
        
    private static final Map<String,Object> SERVICE_PROPERTIES;
    static {
        Map<String,Object> props = new HashMap<String,Object>();
        //by default register as Tokenizing engine
        props.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ServiceProperties.ORDERING_NLP_LANGAUGE_DETECTION);
//        props.put(NlpServiceProperties.ENHANCEMENT_ENGINE_NLP_ROLE, 
//            NlpProcessingRole.Tokenizing);
        SERVICE_PROPERTIES = Collections.unmodifiableMap(props);
    }


    private static Logger log = LoggerFactory.getLogger(RestfulLangidentEngine.class);

    private URI serviceUrl;
    
    private final LiteralFactory literalFactory = LiteralFactory.getInstance();
    //JSON Parser
    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
    //HTTP client
    protected DefaultHttpClient httpClient;
    private BasicHttpParams httpParams;
    private PoolingClientConnectionManager connectionManager;
    
    /**
     * Indicate if this engine can enhance supplied ContentItem, and if it
     * suggests enhancing it synchronously or asynchronously. The
     * {@link org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager} can 
     * force sync/async mode if desired, it is just a suggestion from the engine.
     * <p/>
     * Returns ENHANCE_ASYNC in case there is a text/plain content part and a tagger 
     * for the language identified for the content item, CANNOT_ENHANCE otherwise.
     *
     * @throws org.apache.stanbol.enhancer.servicesapi.EngineException
     *          if the introspecting process of the content item
     *          fails
     */
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        Map.Entry<IRI,Blob> entry = getPlainText(this, ci, false);
        if(entry == null || entry.getValue() == null) {
            return CANNOT_ENHANCE;
        }

        log.trace(" > can enhance ContentItem {} by processing blob {}",ci, entry.getKey());
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
    public void computeEnhancements(final ContentItem ci) throws EngineException {
        //get the plain text Blob
        Map.Entry<IRI,Blob> textBlob = getPlainText(this, ci, false);
        Blob blob = textBlob.getValue();
        //send the text to the server
        final HttpPost request = new HttpPost(serviceUrl);
        request.setEntity(new InputStreamEntity(
            blob.getStream(), blob.getContentLength(),
            ContentType.create(blob.getMimeType(), 
                blob.getParameter().get("charset"))));
        //execute the request
        List<LangSuggestion> detected;
        try {
            detected = AccessController.doPrivileged(new PrivilegedExceptionAction<List<LangSuggestion>>() {
                public List<LangSuggestion> run() throws ClientProtocolException, IOException {
                    return httpClient.execute(request, new LangIdentResponseHandler(ci,objectMapper));
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof ClientProtocolException) {
                throw new EngineException(this, ci, "Exception while executing Request "
                        + "on RESTful Language Identification Service at "+serviceUrl, e);
            } else if(e instanceof IOException) {
                throw new EngineException(this, ci, "Exception while executing Request "
                        + "on RESTful Language Identification Service at "+serviceUrl, e);
            } else {
                throw RuntimeException.class.cast(e);
            }
        }
        Graph metadata = ci.getMetadata();
        log.debug("Detected Languages for ContentItem {} and Blob {}");
        ci.getLock().writeLock().lock();
        try { //write TextAnnotations for the detected languages
            for(LangSuggestion suggestion : detected){
                // add a hypothesis
                log.debug(" > {}@{}", suggestion.getLanguage(),
                    suggestion.hasProbability() ? suggestion.getProbability() : "-,--");
                IRI textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
                metadata.add(new TripleImpl(textEnhancement, DC_LANGUAGE, new PlainLiteralImpl(suggestion.getLanguage())));
                metadata.add(new TripleImpl(textEnhancement, DC_TYPE, DCTERMS_LINGUISTIC_SYSTEM));
                if(suggestion.hasProbability()){
                    metadata.add(new TripleImpl(textEnhancement, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(suggestion.getProbability())));
                }
            }
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    protected class LangIdentResponseHandler implements ResponseHandler<List<LangSuggestion>>{
        
        protected final ContentItem ci;
        protected final JsonFactory jsonFactory;
        protected final ObjectMapper mapper;


        protected LangIdentResponseHandler(ContentItem ci, ObjectMapper objectMapper){
            this.ci = ci;
            this.mapper = objectMapper;
            this.jsonFactory = objectMapper.getJsonFactory();
        }

        @Override
        public List<LangSuggestion> handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
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
                JsonNode root = mapper.readTree(jsonFactory.createJsonParser(new InputStreamReader(in,charset)));
                if(root.isArray()){
                    List<LangSuggestion> detected = new ArrayList<LangSuggestion>(
                            root.size());
                    for(int i=0;i< root.size();i++){
                        String lang;
                        double prob;
                        JsonNode entry = root.get(i);
                        if(entry.isObject()){
                            JsonNode field = entry.path("lang");
                            if(field.isTextual()){
                                lang = field.getTextValue();
                            } else {
                                throw new IOException("Unable to prsed LanguageIdent Service response! "
                                    + "The field 'lang' MUST BE presnet and have a textual value! "
                                    + "(entry: "+entry+", received: "+root+")!");
                            }
                            field = entry.path("prob");
                            if(field.isNumber()){
                                prob = field.asDouble();
                            } else {
                                prob = -1;
                            }
                            detected.add(new LangSuggestion(lang, prob));
                            
                        } else {
                            throw new IOException("Unable to prsed LanguageIdent Service response! "
                                + "All members of the root Json Array MUST BE Json Objects "
                                + "(received: "+root+")");
                        }
                    }
                    return detected;
                } else {
                    throw new IOException("Unable to prsed LanguageIdent Service response! "
                        +" Root Element MUST BE an Json Array (received: "+root+")");
                }
            } finally {
                //ensure that the stream is closed
                IOUtils.closeQuietly(in);
            }
        }
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

        Object value = properties.get(ANALYSIS_SERVICE_URL);
        if(value == null){
            throw new ConfigurationException(ANALYSIS_SERVICE_URL, 
                "The RESTful Language Identification Service URL is missing in the provided configuration!");
        } else {
            try {
                serviceUrl = new URI(value.toString());
                log.info("  ... service: {}",serviceUrl);
            } catch (URISyntaxException e) {
                throw new ConfigurationException(ANALYSIS_SERVICE_URL, 
                        "The parsed RESTful Language Identification Service URL '"+ value
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
        httpParams.setParameter(CoreProtocolPNames.USER_AGENT, 
            "Apache Stanbol RESTful Language Identification Engine");
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
    }
    
    @Deactivate
    protected void deactivate(ComponentContext context) {
        //shutdown the Http Client
        httpClient = null;
        httpParams = null;
        connectionManager.shutdown();
        connectionManager = null;
        super.deactivate(context);
    }
    
    /**
     * Getter for the language of the content
     * @param ci the ContentItem
     * @param exception <code>false</code> id used in {@link #canEnhance(ContentItem)}
     * and <code>true</code> when called from {@link #computeEnhancements(ContentItem)}
     * @return the AnalysedText or <code>null</code> if not found.
     * @throws IllegalStateException if exception is <code>true</code> and the
     * language could not be retrieved from the parsed {@link ContentItem}.
     */
    public static Entry<IRI,Blob> getPlainText(EnhancementEngine engine, ContentItem ci, boolean exception) {
        Entry<IRI,Blob> textBlob = ContentItemHelper.getBlob(
            ci, singleton("text/plain"));
        if(textBlob != null) {
            return textBlob;
        }
        if(exception){
            throw new IllegalStateException("Unable to retrieve 'text/plain' ContentPart for ContentItem "
                    + ci+". As this is also checked in canEnhancer this may indicate an Bug in the "
                    + "used EnhancementJobManager!");
        } else {
            log.warn("The Enhancement Engine '{} (impl: {})' CAN NOT enhance "
                    + "ContentItem {} because no 'text/plain' ContentPart is "
                    + "present in this ContentItem. Users that need to enhance "
                    + "non-plain-text Content need to add an EnhancementEngine "
                    + "that supports the conversion of '{}' files to plain text "
                    + "to the current EnhancementChain!",
                    new Object[]{engine.getName(), engine.getClass().getSimpleName(),ci,ci.getMimeType()});
            return null;
        }
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
