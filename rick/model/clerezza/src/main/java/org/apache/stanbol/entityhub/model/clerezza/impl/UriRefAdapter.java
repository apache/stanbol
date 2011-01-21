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

    static Logger log = LoggerFactory.getLogger(UriRefAdapter.class);

    @SuppressWarnings("unchecked")
    @Override
    public A adapt(UriRef value, Class<A> type) {
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
