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
package org.apache.stanbol.commons.solr.impl;

import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_DIR;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_NAME;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_CORE_SERVER_ID;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_DIR;
import static org.apache.stanbol.commons.solr.SolrConstants.PROPERTY_SERVER_NAME;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.SolrServerAdapter;
import org.apache.stanbol.commons.solr.SolrServerProvider;
import org.apache.stanbol.commons.solr.SolrServerTypeEnum;
import org.apache.stanbol.commons.solr.utils.ConfigUtils;
import org.apache.stanbol.commons.solr.utils.ServiceReferenceRankingComparator;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SolrServerProvider} that supports all {@link CoreContainer}s and
 * {@link SolrCore}s registered as OSGI services. <p>
 * This implementation is intended to be used with {@link CoreContainer}s and
 * {@link SolrCore}s as registered by the {@link SolrServerAdapter} however it
 * can work with all {@link CoreContainer}s and {@link SolrCore}s if they provide
 * the required meta data.
 * @author Rupert Westenthaler
 * 
 */
@Component(immediate = true, metatype = true)
@Service
public class RegisteredSolrServerProvider implements SolrServerProvider {
    private final Logger log = LoggerFactory.getLogger(RegisteredSolrServerProvider.class);
    // define the default values here because they are not accessible via the Solr API

    private ServiceTracker coreTracker;
    private ServiceTracker serverTracker;
    
   

    public RegisteredSolrServerProvider() {}

    /**
     * This supports the following uriOrPath values to lookup internally
     * managed {@link SolrCore}s and to return an {@link EmbeddedSolrServer}
     * instance:<ol>
     * <li> file URLs to the Core directory
     * <li> file paths to the Core directory
     * <li> [{server-name}:]{core-name} where both the server-name and the
     *      core-name MUST NOT contain '/' and '\' (on windows) chars. If the 
     *      server-name is not specified the server with the highest 
     *      {@link Constants#SERVICE_RANKING} is assumed.
     * </ol><p>
     * If the server-name is not known (the case for the first two options),
     * than <code>null</code> is returned if the referenced Core is not present.
     * In case of the third option an {@link EmbeddedSolrServer} instance is
     * also returned if the referenced SolrServer does currently not define
     * a core with the specified name. Therefore users are encouraged to test
     * by calling {@link SolrServer#ping()} whether the core is ready to be used
     * or not. This behaviour was chosen to avoid the need to wait for the 
     * completion of the initialisation of a Core. In addition typically users
     * might not know if a used SolrCore is local (embedded) or accessed via
     * the RESTful interface. Therefore code for dealing with temporarily
     * unavailable services is typically needed anyway.
     */
    @Override
    public SolrServer getSolrServer(SolrServerTypeEnum type, String uriOrPath, String... additional) {
        log.debug("getSolrServer Request for {} and path {}", type, uriOrPath);
        if (uriOrPath == null) {
            throw new IllegalArgumentException("The name of requested SolrCore MUST NOT be NULL!");
        }
        if(type != SolrServerTypeEnum.EMBEDDED){
            throw new IllegalStateException("Parsed SolrServer type '"+type+
                "' is not supported by this Implementation (suppoted: '"+
                SolrServerTypeEnum.EMBEDDED+"')!");
        }
        //(1) parse the pathOrUir -> serverName & coreName
//        String serverName;
        String coreName;
        IndexReference indexReference = IndexReference.parse(uriOrPath);
        ServiceReference serverRef;
        if(indexReference.getServer() != null){
//            if(indexReference.getServer().isEmpty()){
//                serverRef = getDefaultServerReference();
//            } else {
                serverRef = getServerReference(indexReference.getServer());
//            }
            if(serverRef == null){
                log.info(" > {} SolrServer {} is currently not present",
                    indexReference.getServer().isEmpty()?"Default":"Parsed",indexReference.getServer());
                coreName = null;
            } else {
                coreName = indexReference.getIndex();
            }
        } else {
            ServiceReference coreRef = getCoreReference(indexReference.getIndex());
            if(coreRef == null){ 
                log.info(" > Unable to locate Core '{}' on any active Solr Server",
                    indexReference.getIndex());
                serverRef = null;
                coreName = null;
            } else {
                serverRef = getServerReference(coreRef);
                if(serverRef == null){
                    log.info(" > SolrServer for Core {} no longer available ",
                        indexReference.getIndex());
                    coreName = null;
                } else {
                    coreName = (String)coreRef.getProperty(PROPERTY_CORE_NAME);
                }
            }
        }
        if(serverRef != null){
        //we need not to check for the core -> create anyway!
            return new EmbeddedSolrServer(
                (CoreContainer)serverTracker.getService(serverRef), coreName);
        } else {
            return null;
        }
    }



    /**
     * Getter for the reference to the tracked {@link CoreContainer} instance 
     * with the highest {@link Constants#SERVICE_RANKING} - the default 
     * Server
     */
    private ServiceReference getDefaultServerReference() {
        ServiceReference[] refs = serverTracker.getServiceReferences();
        if(refs == null){
            return null;
        } else {
            if (refs.length > 1){
                Arrays.sort(refs,ServiceReferenceRankingComparator.INSTANCE);
            }
            return refs[0];
        }
    }
    /**
     * Getter for the Reference to the {@link CoreContainer} for the {@link SolrCore}
     * referenced bythe parsed {@link ServiceReference}.<p>
     * <b>IMPLEMENTATION NOTE:</b> <br>
     * This method Iterates over all tracked {@link ServiceReference} and
     * can therefore be assumed as low-performance. However the assumptions are that
     * first there are only a few tracked services and this method is not called
     * frequently. If one of this assumptions is not true one should consider to
     * re-implement this by building an in-memory model of tracked 
     * {@link CoreContainer}s and {@link SolrCore}s with the required indexes
     * required to implement lookups like that.
     * @param name the name (or path) of the {@link SolrCore}
     * @return the reference to the {@link CoreContainer} or <code>null</code>
     * if not found
     */
    private ServiceReference getServerReference(ServiceReference coreRef){
        if(coreRef != null){
            Long serverId = (Long)coreRef.getProperty(PROPERTY_CORE_SERVER_ID);
            if(serverId != null){
                ServiceReference[] serverRefs = serverTracker.getServiceReferences();
                if(serverRefs != null){
                    for(ServiceReference serverRef : serverRefs){
                        if(serverId.equals(serverRef.getProperty(Constants.SERVICE_ID))){
                            return serverRef;
                        }
                    }
                }
                return null;
            } else { //search based on name
                String serverName = (String) coreRef.getProperty(PROPERTY_SERVER_NAME);
                if(serverName != null){
                    ServiceReference[] serverRefs = serverTracker.getServiceReferences();
                    if(serverRefs != null){
                        for(ServiceReference serverRef : serverRefs){
                            if(serverName.equals(serverRef.getProperty(PROPERTY_SERVER_NAME))){
                                //we need to get the name, because maybe the path was parsed
                                String coreName = (String)coreRef.getProperty(PROPERTY_CORE_NAME);
                                Collection<?> cores = (Collection<?>)serverRef.getProperty(SolrConstants.PROPERTY_SERVER_CORES);
                                if(cores.contains(coreName)){
                                    return serverRef;
                                } //else the wrong server
                            } //else  server with wrong server name
                        }
                    }
                    return null; //not found
                } else { 
                    //no PROPERTY_CORE_SERVER_PID and PROPERTY_SERVER_NAME
                    //property for this core
                    return null;
                }
            }
        } else {//the requested core was not found
            return null;
        }
    }
    /**
     * Getter for the {@link ServiceReference} with the highest Ranking for
     * the requested Core name<p>
     * <b>IMPLEMENTATION NOTE:</b> <br>
     * This method Iterates over all tracked {@link ServiceReference} and
     * can therefore be assumed as low-performance. However the assumptions are that
     * first there are only a few tracked services and this method is not called
     * frequently. If one of this assumptions is not true one should consider to
     * re-implement this by building an in-memory model of tracked 
     * {@link CoreContainer}s and {@link SolrCore}s with the required indexes
     * required to implement lookups like that.     
     * @param name the name (or path) of the core
     * @return the reference or <code>null</code> if not found
     */
    private ServiceReference getCoreReference(String name){
        List<ServiceReference> matches;
        ServiceReference[] refs = coreTracker.getServiceReferences();
        if(refs != null){
            matches = new ArrayList<ServiceReference>();
            for(ServiceReference ref : refs){
                if(name.equals(ref.getProperty(PROPERTY_CORE_NAME)) ||
                        name.equals(ref.getProperty(PROPERTY_CORE_DIR))){
                    matches.add(ref);
                }
            }
            if(matches.size()>1){
                Collections.sort(matches, ServiceReferenceRankingComparator.INSTANCE);
            }
        } else {
            matches = null;
        }
        return matches == null || matches.isEmpty() ? null : matches.get(0);
        
    }
    /**
     * Getter for the {@link ServiceReference} to the {@link CoreContainer} for
     * the parsed name (or path) with the highest service rank<p>
     * <b>IMPLEMENTATION NOTE:</b> <br>
     * This method Iterates over all tracked {@link ServiceReference} and
     * can therefore be assumed as low-performance. However the assumptions are that
     * first there are only a few tracked services and this method is not called
     * frequently. If one of this assumptions is not true one should consider to
     * re-implement this by building an in-memory model of tracked 
     * {@link CoreContainer}s and {@link SolrCore}s with the required indexes
     * required to implement lookups like that.
     * @param name The name (or path) of the server
     * @return The reference or <code>null</code> if not found
     */
    private ServiceReference getServerReference(String name){
        List<ServiceReference> matches;
        ServiceReference[] refs = serverTracker.getServiceReferences();
        if(refs != null){
            matches = new ArrayList<ServiceReference>();
            for(ServiceReference ref : refs){
                if(name.equals(ref.getProperty(PROPERTY_SERVER_NAME)) ||
                        name.equals(ref.getProperty(PROPERTY_SERVER_DIR))){
                    matches.add(ref);
                }
            }
            if(matches.size()>1){
                Collections.sort(matches, ServiceReferenceRankingComparator.INSTANCE);
            }
        } else {
            matches = null;
        }
        return matches == null || matches.isEmpty() ? null : matches.get(0);
    }
    

    @Override
    public Set<SolrServerTypeEnum> supportedTypes() {
        return Collections.singleton(SolrServerTypeEnum.EMBEDDED);
    }

    @Activate
    protected void activate(ComponentContext context) throws InvalidSyntaxException {
        log.debug("activating" + RegisteredSolrServerProvider.class.getSimpleName());
        coreTracker = new ServiceTracker(context.getBundleContext(), 
            SolrCore.class.getName(), null);
        serverTracker = new ServiceTracker(context.getBundleContext(),
            CoreContainer.class.getName(), null);
        coreTracker.open();
        serverTracker.open();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.debug("deactivating" + RegisteredSolrServerProvider.class.getSimpleName());
        if(coreTracker != null){
            coreTracker.close();
            coreTracker = null;
        }
        if(serverTracker != null){
            serverTracker.close();
            serverTracker = null;
        }
    }

    // Keeping for now because this might be useful when checking for required files
    // /**
    // * Checks if the parsed directory contains a file that starts with the parsed
    // * name. Parsing "hallo" will find "hallo.all", "hallo.ween" as well as "hallo".
    // * @param dir the Directory. This assumes that the parsed File is not
    // * <code>null</code>, exists and is an directory
    // * @param name the name. If <code>null</code> any file is accepted, meaning
    // * that this will return true if the directory contains any file
    // * @return the state
    // */
    // private boolean hasFile(File dir, String name){
    // return dir.list(new NameFileFilter(name)).length>0;
    // }
    // /**
    // * Returns the first file that matches the parsed name.
    // * Parsing "hallo" will find "hallo.all", "hallo.ween" as well as "hallo".
    // * @param dir the Directory. This assumes that the parsed File is not
    // * <code>null</code>, exists and is an directory.
    // * @param name the name. If <code>null</code> any file is accepted, meaning
    // * that this will return true if the directory contains any file
    // * @return the first file matching the parsed prefix.
    // */
    // private File getFileByPrefix(File dir, String prefix){
    // String[] files = dir.list(new PrefixFileFilter(prefix));
    // return files.length>0?new File(dir,files[0]):null;
    // }
    /**
     * Returns the first file that matches the parsed name (case sensitive)
     * 
     * @param dir
     *            the Directory. This assumes that the parsed File is not <code>null</code>, exists and is an
     *            directory.
     * @param name
     *            the name. If <code>null</code> any file is accepted, meaning that this will return true if
     *            the directory contains any file
     * @return the first file matching the parsed name.
     */
    private File getFile(File dir, String name) {
        String[] files = dir.list(new NameFileFilter(name));
        return files.length > 0 ? new File(dir, files[0]) : null;
    }

    public static void main(String[] args) {
      System.out.println(Integer.MAX_VALUE);
    }
}
