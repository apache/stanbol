package org.apache.stanbol.entityhub.site.linkedData.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.entityhub.core.site.AbstractEntityDereferencer;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.slf4j.LoggerFactory;



@Component(
        name="org.apache.stanbol.entityhub.site.CoolUriDereferencer",
        factory="org.apache.stanbol.entityhub.site.CoolUriDereferencerFactory",
        policy=ConfigurationPolicy.REQUIRE, //the baseUri is required!
        specVersion="1.1"
        )
public class CoolUriDereferencer extends AbstractEntityDereferencer implements EntityDereferencer{
    @Reference
    protected Parser parser;

    private final RdfValueFactory valueFactory = RdfValueFactory.getInstance();


    public CoolUriDereferencer(){
        super(LoggerFactory.getLogger(CoolUriDereferencer.class));
    }

    @Override
    public InputStream dereference(String uri, String contentType) throws IOException{
        if(uri!=null){
            final URL url = new URL(uri);
            final URLConnection con = url.openConnection();
            con.addRequestProperty("Accept", contentType);
            return con.getInputStream();
        } else {
            return null;
        }
    }

    @Override
    public Representation dereference(String uri) throws IOException{
        long start = System.currentTimeMillis();
        String format = SupportedFormat.RDF_XML;
        InputStream in = dereference(uri, format);
        long queryEnd = System.currentTimeMillis();
        log.info("  > DereferenceTime: "+(queryEnd-start));
        if(in != null){
            MGraph rdfData = new SimpleMGraph(parser.parse(in, format));
            long parseEnd = System.currentTimeMillis();
            log.info("  > ParseTime: "+(parseEnd-queryEnd));
            return valueFactory.createRdfRepresentation(new UriRef(uri), rdfData);
        } else {
            return null;
        }
    }
}
