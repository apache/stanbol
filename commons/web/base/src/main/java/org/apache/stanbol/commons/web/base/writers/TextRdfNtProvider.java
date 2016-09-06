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

package org.apache.stanbol.commons.web.base.writers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;


/**
 * With Clerezza 1.0 the deprecated <code>text/rdf+nt</code> content type is no longer
 * support. This provider allows the usage of this content type by forwarding the
 * request to the provider registered for the new {@link SupportedFormat#N_TRIPLE} content type
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true)
@Service(SerializingProvider.class)
@Property(name="supportedFormat", value={SupportedFormat.TEXT_RDF_NT})
@SupportedFormat({SupportedFormat.TEXT_RDF_NT})
public class TextRdfNtProvider implements SerializingProvider, ParsingProvider {

    @Reference
    Serializer serializer;
    
    @Reference
    Parser parser;
    
    
    @Override
    public void serialize(OutputStream outputStream, Graph tc, String format) {
        String formatIdentifier = cleanFormat(format);
        if(SupportedFormat.TEXT_RDF_NT.equals(formatIdentifier)){
            serializer.serialize(outputStream, tc, SupportedFormat.N_TRIPLE);
        } else {
            throw new IllegalArgumentException("This serializer only supports "+ SupportedFormat.TEXT_RDF_NT + 
                    "(parsed: " + format +" | format: " + formatIdentifier + ")!");
        }
    }


    @Override
    public void parse(Graph target, InputStream serializedGraph, String format, IRI baseUri) {
        String formatIdentifier = cleanFormat(format);
        if(SupportedFormat.TEXT_RDF_NT.equals(formatIdentifier)){
            parser.parse(target, serializedGraph, SupportedFormat.N_TRIPLE, baseUri);
        } else {
            throw new IllegalArgumentException("This serializer only supports "+ SupportedFormat.TEXT_RDF_NT + 
                    "(parsed: " + format +" | format: " + formatIdentifier + ")!");
        }
    }
    /**
     * Used to strip parameters; ensure lower case and trim the media type
     * @param formatIdentifier
     * @return
     */
    private static String cleanFormat(String formatIdentifier){
        return formatIdentifier == null ? null :  formatIdentifier.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }
    public static void main(String[] args) {
        System.out.println(cleanFormat("text/rdf+nt;charset=UTF-8"));
    }
}
