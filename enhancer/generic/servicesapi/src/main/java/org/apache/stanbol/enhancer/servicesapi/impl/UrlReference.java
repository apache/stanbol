package org.apache.stanbol.enhancer.servicesapi.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.apache.stanbol.enhancer.servicesapi.ContentReference;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;


/**
 * Allows to use a URL for referencing a content.
 */
public class UrlReference implements ContentReference {

    final URL url;
    /**
     * Uses the passed URI string to parse the URL.
     * @param uri an absolute URI that can be converted to an URL
     * @throws IllegalArgumentException if the passed URI string is <code>null</code>
     * or can not be converted to an {@link URL}
     */
    public UrlReference(String uri) {
        if(uri == null){
            throw new IllegalArgumentException("The parsed URI reference MUST NOT be NULL!");
        }
        try {
            this.url = URI.create(uri).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("the passed URI can not be converted to an URL",e);
        }
    }
    public UrlReference(URL url) {
        if(url == null){
            throw new IllegalArgumentException("The parsed URL MUST NOT be NULL!");
        }
        this.url = url;
    }
    
    @Override
    public String getReference() {
        return url.toString();
    }

    @Override
    public ContentSource dereference() throws IOException {
        URLConnection uc = url.openConnection();
        return new StreamSource(uc.getInputStream(),
            uc.getContentType(), uc.getHeaderFields());
    }
    
}