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

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N3_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.N_TRIPLE_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE_TYPE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE_TYPE;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.clerezza.rdf.rdfjson.serializer.RdfJsonSerializingProvider;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Provider
@Produces({RDF_XML, OWL_XML, MANCHESTER_OWL, FUNCTIONAL_OWL, TURTLE, X_TURTLE, N3, N_TRIPLE, RDF_JSON,
           TEXT_PLAIN})
public class OWLOntologyWriter implements MessageBodyWriter<OWLOntology> {

    @Reference
    protected Serializer serializer;


    @Override
    public long getSize(OWLOntology arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
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

        log.debug("Rendering ontology " + ontology.getOntologyID() + " to knowledge representation format "
                  + mediaType);

        // Native formats first
        if (RDF_XML_TYPE.equals(mediaType) || OWL_XML_TYPE.equals(mediaType)
            || MANCHESTER_OWL_TYPE.equals(mediaType) || FUNCTIONAL_OWL_TYPE.equals(mediaType)
            || TURTLE_TYPE.equals(mediaType) || X_TURTLE_TYPE.equals(mediaType)) {

            OWLOntologyFormat format = null;
            if (RDF_XML_TYPE.equals(mediaType)) format = new RDFXMLOntologyFormat();
            else if (OWL_XML_TYPE.equals(mediaType)) format = new OWLXMLOntologyFormat();
            else if (MANCHESTER_OWL_TYPE.equals(mediaType)) format = new ManchesterOWLSyntaxOntologyFormat();
            else if (FUNCTIONAL_OWL_TYPE.equals(mediaType)) format = new OWLFunctionalSyntaxOntologyFormat();
            else if (TURTLE_TYPE.equals(mediaType) || X_TURTLE_TYPE.equals(mediaType)) format = new TurtleOntologyFormat();

            if (format != null) try {
                manager.saveOntology(ontology, format, out);
            } catch (OWLOntologyStorageException e) {
                log.error("Failed to store ontology for rendering.", e);
                throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
            }
            else throw new IOException();

        } else {
            // Non-native formats that require a conversion to Clerezza
            if (RDF_JSON_TYPE.equals(mediaType) || N3_TYPE.equals(mediaType)
                || TEXT_PLAIN.equals(mediaType.toString()) || N_TRIPLE_TYPE.equals(mediaType)) {
                Graph mGraph = OWLAPIToClerezzaConverter.owlOntologyToClerezzaGraph(ontology);
                SerializingProvider serializer = null;
                if (RDF_JSON_TYPE.equals(mediaType)) serializer = new RdfJsonSerializingProvider();
                else if (N3_TYPE.equals(mediaType) || N_TRIPLE_TYPE.equals(mediaType)
                         || TEXT_PLAIN.equals(mediaType.toString())) serializer = new JenaSerializerProvider();

                // text/plain is interpreted as N3.
                if (serializer != null) serializer.serialize(out, mGraph,
                    TEXT_PLAIN.equals(mediaType.toString()) ? N3 : mediaType.toString());
            }
        }

        // JSON_LD not supported until both parser and serializer are stable.

        out.flush();
    }

}
