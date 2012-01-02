package org.apache.stanbol.contenthub.store.clerezza;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.clerezza.platform.content.DiscobitsHandler;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.stanbol.enhancer.servicesapi.Blob;

public class ClerezzaBlob implements Blob {

    private final GraphNode idNode;
    private final DiscobitsHandler handler;

    protected ClerezzaBlob(DiscobitsHandler handler, GraphNode idNode){
        this.handler = handler;
        this.idNode = idNode;
    }
    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(handler.getData((UriRef) idNode.getNode()));
    }
    @Override
    public String getMimeType() {
        return handler.getMediaType((UriRef) idNode.getNode()).toString();
    }
    @Override
    public Map<String,String> getParameter() {
        return Collections.emptyMap();
    }
    @Override
    public long getContentLength() {
        return -1;
    }

}
