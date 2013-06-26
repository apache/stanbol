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
package org.apache.stanbol.commons.namespaceprefix.provider.prefixcc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.impl.NamespacePrefixProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixccProvider implements NamespacePrefixProvider {

    private static final Logger log = LoggerFactory.getLogger(PrefixccProvider.class);
        
    public static final URL GET_ALL;
    static {
        try {
            GET_ALL = new URL("http://prefix.cc/popular/all.file.txt");
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Unable to create http://prefix.cc URL",e);
        }
    }
    private final ScheduledExecutorService scheduler = 
            Executors.newScheduledThreadPool(1);
    

    private NamespacePrefixProvider cache;
    private long cacheStamp;

    /**
     * Intended to be used by the {@link ServiceLoader} utility.
     * uses 1 hour delay and DOES a sync initial load of the mappings
     * before returning.
     */
    public PrefixccProvider(){ //by default update once every hour
        this(1,TimeUnit.HOURS, true);
    }
    /**
     * Creates a prefix.cc provider with the specified delay. The initial
     * load of the mappings is done immediately but asynchronously. That means
     * that the mappings will not be available when the constructor returns. <p>
     * While this implementation does not restrict configured delays expected 
     * values are in the era of hours.
     * @param delay the delay
     * @param unit the unit of the delay.
     */
    public PrefixccProvider(int delay,TimeUnit unit){
        this(delay,unit,false);
    }
    /**
     * Creates a prefix.cc provider. If syncInitialLoad is enabled the initial
     * load of the data is done before the constructor returns. Otherwise
     * mappings are loaded asynchronously as specified by the parsed delay.<p>
     * While this implementation does not restrict configured delays expected 
     * values are in the era of hours.
     * @param delay the delay
     * @param unit the time unit of the delay
     * @param syncInitialLoad if <code>true</code> mappings are loaded before
     * the constructor returns. Otherwise mappings are loaded asynchronously
     */
    public PrefixccProvider(int delay,TimeUnit unit, boolean syncInitialLoad){
        if(delay <= 0){
            throw new IllegalArgumentException("The parsed delay '"
                +delay+"' MUST NOT be <= 0");
        }
        if(unit == null){
            unit = TimeUnit.SECONDS;
        }
        int initialDelay;
        if(syncInitialLoad){
            loadMappings();
            initialDelay = delay;
        } else {
            initialDelay = 0;
        }
        scheduler.scheduleWithFixedDelay(
            new Runnable() {
                
                @Override
                public void run() {
                    loadMappings();
                }
            }, initialDelay, delay, unit);
    }
    
    protected final void loadMappings() {
        try {
            log.info("Load Namespace Prefix Mappings form {}",GET_ALL);
            HttpURLConnection con = (HttpURLConnection)GET_ALL.openConnection();
            con.setReadTimeout(5000); //set the max connect & read timeout to 5sec
            con.setConnectTimeout(5000);
            con.connect();
            String contentType = con.getContentType();
            if("text/plain".equalsIgnoreCase(contentType)){
                InputStream in = con.getInputStream();
                try {
                    cache = new NamespacePrefixProviderImpl(in);
                    cacheStamp = System.currentTimeMillis();
                    log.info("  ... completed");
                } finally {
                    IOUtils.closeQuietly(in);
                }
            } else {
                log.warn("Response from prefix.cc does have the wrong content type '"
                    + contentType + "' (expected: text/plain). This indicates that the "
                    + "service is currently unavailable!");
            }
            con.disconnect(); //we connect once every {long-period}
        } catch (IOException e) {
            log.warn("Unable to load prefix.cc NamespaceMappings (Message: "
                + e.getMessage() +")",e);
            ;
        }
    }
    /**
     * deletes the local cahe and stops the periodical updates of the cache
     */
    public void close(){
        scheduler.shutdown();
        cache = null;
    }
    
    /**
     * If prefix.cc data are available
     * @return
     */
    public boolean isAvailable(){
        return cache != null;
    }
    /**
     * The Date where the locally cached data where synced the last time with
     * prefix.cc. Will return <code>null</code> if no data where received from
     * prefix.cc (<code>{@link #isAvailable()} == false</code>)
     * @return the date where the local cache was received from prefix.cc
     */
    public Date getCacheTimeStamp(){
        if(cache != null){
            return new Date(cacheStamp);
        } else {
            return null;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
    @Override
    public String getNamespace(String prefix) {
        NamespacePrefixProvider npp = cache;
        return npp == null ? null : npp.getNamespace(prefix);
    }
    
    @Override
    public String getPrefix(String namespace) {
        NamespacePrefixProvider npp = cache;
        return npp == null ? null : npp.getPrefix(namespace);
    }
    @Override
    public List<String> getPrefixes(String namespace) {
        NamespacePrefixProvider npp = cache;
        return npp == null ? null : npp.getPrefixes(namespace);
    }
}
