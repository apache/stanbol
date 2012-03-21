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
package org.apache.stanbol.entityhub.site.linkeddata.impl;

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
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.entityhub.core.site.AbstractEntityDereferencer;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.slf4j.LoggerFactory;



@Component(
        name="org.apache.stanbol.entityhub.dereferencer.CoolUriDereferencer",
        factory="org.apache.stanbol.entityhub.dereferencer.CoolUriDereferencerFactory",
        policy=ConfigurationPolicy.REQUIRE, //the baseUri is required!
        specVersion="1.1"
        )
public class CoolUriDereferencer extends AbstractEntityDereferencer implements EntityDereferencer{
    @Reference
    private Parser parser;

    private final RdfValueFactory valueFactory = RdfValueFactory.getInstance();


    public CoolUriDereferencer(){
        super(LoggerFactory.getLogger(CoolUriDereferencer.class));
    }

    @Override
    public final InputStream dereference(String uri, String contentType) throws IOException{
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
    public final Representation dereference(String uri) throws IOException{
        long start = System.currentTimeMillis();
        String format = SupportedFormat.RDF_XML;
        InputStream in = dereference(uri, format);
        long queryEnd = System.currentTimeMillis();
        log.info("  > DereferenceTime: "+(queryEnd-start));
        if(in != null){
            MGraph rdfData = new IndexedMGraph(parser.parse(in, format,new UriRef(getBaseUri())));
            long parseEnd = System.currentTimeMillis();
            log.info("  > ParseTime: "+(parseEnd-queryEnd));
            return valueFactory.createRdfRepresentation(new UriRef(uri), rdfData);
        } else {
            return null;
        }
    }
}
