/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.iksproject.fise.clerezza;

import eu.iksproject.fise.servicesapi.ContentItem;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.clerezza.platform.content.DiscobitsHandler;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.utils.GraphNode;

/**
 *
 * @author andreas
 */
public class ClerezzaContentItem implements ContentItem {

    private final GraphNode idNode;
    private final DiscobitsHandler handler;
    private final MGraph metadataGraph;

    public ClerezzaContentItem(GraphNode idNode, MGraph metadataGraph, DiscobitsHandler handler) {
        this.idNode = idNode;
        this.metadataGraph = metadataGraph;
        this.handler = handler;
    }

    public String getId() {
        return ((UriRef) idNode.getNode()).getUnicodeString();
    }

    public InputStream getStream() {
        return new ByteArrayInputStream(handler.getData((UriRef) idNode.getNode()));
    }

    public String getMimeType() {
        return handler.getMediaType((UriRef) idNode.getNode()).toString();
    }

    public MGraph getMetadata() {
        return metadataGraph;
    }
}
