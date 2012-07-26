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
package org.apache.stanbol.entityhub.model.clerezza.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.entityhub.core.utils.AdaptingIterator.Adapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UriRefAdapter<A> implements Adapter<UriRef, A> {

    private static Logger log = LoggerFactory.getLogger(UriRefAdapter.class);

    @SuppressWarnings("unchecked")
    @Override
    public final A adapt(UriRef value, Class<A> type) {
        if(type.equals(URI.class)){
            try {
                return (A) new URI(value.getUnicodeString());
            } catch (URISyntaxException e) {
                log.warn("Unable to parse an URI for UriRef "+value,e);
                return null;
            }
        } else if(type.equals(URL.class)){
            try {
                return (A) new URL(value.getUnicodeString());
            } catch (MalformedURLException e) {
                log.warn("Unable to parse an URL for UriRef "+value,e);
            }
        } else if(type.equals(String.class)){
            return (A) value.getUnicodeString();
        } else if(type.equals(UriRef.class)){ //Who converts UriRef -> UriRef ^
            return (A) value;
        } else {
            log.warn(type+" is not a supported target type for "+UriRef.class);
        }
        return null;
    }

}
