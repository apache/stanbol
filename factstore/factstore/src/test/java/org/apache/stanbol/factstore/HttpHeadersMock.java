package org.apache.stanbol.factstore;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public class HttpHeadersMock implements HttpHeaders {

    @Override
    public List<Locale> getAcceptableLanguages() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String,Cookie> getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Locale getLanguage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MediaType getMediaType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getRequestHeader(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MultivaluedMap<String,String> getRequestHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

}
