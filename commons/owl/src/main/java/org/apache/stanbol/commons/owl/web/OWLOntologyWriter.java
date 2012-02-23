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
package org.apache.stanbol.commons.owl.web;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.ServletContext;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.rdfjson.serializer.RdfJsonSerializingProvider;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Produces({KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.MANCHESTER_OWL, KRFormat.FUNCTIONAL_OWL,
           KRFormat.TURTLE, KRFormat.RDF_JSON})
public class OWLOntologyWriter implements MessageBodyWriter<OWLOntology> {

    protected Serializer serializer;

    protected ServletContext servletContext;

    public OWLOntologyWriter(@Context ServletContext servletContext) {
        Logger log = LoggerFactory.getLogger(getClass());
        this.servletContext = servletContext;
        log.info("Setting context to " + servletContext);
        serializer = (Serializer) this.servletContext.getAttribute(Serializer.class.getName());
        if (serializer == null) {
            log.info("Serializer not found in Servlet context.");
            serializer = new Serializer();
        }
    }

    @Override
    public long getSize(OWLOntology arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        // TODO Auto-generated method stub
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return OWLOntology.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(OWLOntology ontology,
                        Class<?> arg1,
                        Type arg2,
                        Annotation[] arg3,
                        MediaType mediaType,
                        MultivaluedMap<String,Object> arg5,
                        OutputStream out) throws IOException, WebApplicationException {

        Logger log = LoggerFactory.getLogger(getClass());
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        log.debug("Rendering ontology " + ontology.getOntologyID() + "to KReS format " + mediaType);

        if (mediaType.toString().equals(KRFormat.RDF_XML)) {
            try {
                manager.saveOntology(ontology, new RDFXMLOntologyFormat(), out);
            } catch (OWLOntologyStorageException e) {
                log.error("Failed to store ontology for rendering.", e);
            }
        } else if (mediaType.toString().equals(KRFormat.OWL_XML)) {
            try {
                manager.saveOntology(ontology, new OWLXMLOntologyFormat(), out);
            } catch (OWLOntologyStorageException e) {
                log.error("Failed to store ontology for rendering.", e);
            }
        } else if (mediaType.toString().equals(KRFormat.MANCHESTER_OWL)) {
            try {
                manager.saveOntology(ontology, new ManchesterOWLSyntaxOntologyFormat(), out);
            } catch (OWLOntologyStorageException e) {
                log.error("Failed to store ontology for rendering.", e);
            }
        } else if (mediaType.toString().equals(KRFormat.FUNCTIONAL_OWL)) {
            try {
                manager.saveOntology(ontology, new OWLFunctionalSyntaxOntologyFormat(), out);
            } catch (OWLOntologyStorageException e) {
                log.error("Failed to store ontology for rendering.", e);
            }
        } else if (mediaType.toString().equals(KRFormat.TURTLE)) {
            try {
                manager.saveOntology(ontology, new TurtleOntologyFormat(), out);
            } catch (OWLOntologyStorageException e) {
                log.error("Failed to store ontology for rendering.", e);
            }
        } else if (mediaType.toString().equals(KRFormat.RDF_JSON)) {

            TripleCollection mGraph = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(ontology);

            RdfJsonSerializingProvider provider = new RdfJsonSerializingProvider();
            provider.serialize(out, mGraph, SupportedFormat.RDF_JSON);

        }

        out.flush();
    }

}
