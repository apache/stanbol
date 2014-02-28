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
package org.apache.stanbol.entityhub.ldpath;

import java.util.Collection;

import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.model.fields.FieldMapping;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.marmotta.ldpath.parser.DefaultConfiguration;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory.AnyUriConverter;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory.ReferenceConverter;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory.TextConverter;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory.ValueConverter;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.ldpath.transformer.ValueConverterTransformerAdapter;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;

/**
 * {@link LDPath} with Entityhub specific configurations.
 * In detail this registers {@link NodeTransformer} for:<ul>
 * <li> {@link DataTypeEnum#Reference} returning {@link Reference} instances
 * <li> xsd:anyURI also returning {@link Reference} instances
 * <li> {@link DataTypeEnum#Text} returning {@link Text} instances
 * <li> xsd:string also returning {@link Text} instances
 * </ul><p>
 * It adds also support for returning {@link Representation} instances as
 * result of executing {@link Program}s on a context. This is important because
 * it allows a seamless integration of LDPath with existing Entityhub
 * functionality/interfaces
 * 
 * <p>Because there is currently
 * no way to get the LDPath parser to instantiate an extension of {@link Program}
 * this feature is currently implemented by {@link #execute(Reference, Program)}
 * of this class.
 * 
 * @author Rupert Westenthaler
 *
 */
public class EntityhubLDPath extends LDPath<Object> {

    private final ValueFactory vf;
    private final RDFBackend<Object> backend;
    /**
     * Creates a {@link LDPath} instance configured as used with the Entityhub.
     * This means support for<ul>
     * <li> namespaces defined by the {@link NamespaceEnum}
     * <li> {@link NodeTransformer} for {@link DataTypeEnum#Text} and 
     * {@link DataTypeEnum#Reference}
     * <li> and the usage of {@link Text} for <code>xsd:string</code> and
     * {@link Reference} for <code>xsd:anyURI</code>
     * @param backend the {@link RDFBackend}
     */
    public EntityhubLDPath(RDFBackend<Object> backend) {
        this(backend,null);
    }
    /**
     * Creates a {@link LDPath} instance configured as used with the Entityhub.
     * This means support for<ul>
     * <li> namespaces defined by the {@link NamespaceEnum}
     * <li> {@link NodeTransformer} for {@link DataTypeEnum#Text} and 
     * {@link DataTypeEnum#Reference}
     * <li> and the usage of {@link Text} for <code>xsd:string</code> and
     * {@link Reference} for <code>xsd:anyURI</code>
     * @param backend the {@link RDFBackend}
     * @param vf the {@link ValueFactory} or <code>null</code> to use the default.
     */
    public EntityhubLDPath(RDFBackend<Object> backend,ValueFactory vf) {
        super(backend, new EntityhubConfiguration(vf));
        this.vf = vf == null ? InMemoryValueFactory.getInstance() : vf;
        this.backend = backend;
    }
    
    /**
     * Executes the parsed {@link Program} and stores the 
     * {@link Program#getFields() fields} in a {@link Representation}. The actual
     * implementation used for the {@link Representation} depends on the
     * {@link ValueFactory} of this EntityhubLDPath instance
     * @param context the context
     * @param program the program 
     * @return the {@link Representation} holding the results of the execution
     * @throws IllegalArgumentException if the parsed context or the program is
     * <code>null</code>
     */
    public Representation execute(Reference context,Program<Object> program){
        if(context == null){
            throw new IllegalArgumentException("The parsed context MUST NOT be NULL!");
        }
        if(program == null){
            throw new IllegalArgumentException("The parsed program MUST NOT be NULL!");
        }
        Representation result = vf.createRepresentation(context.getReference());
        for(FieldMapping<?,Object> mapping : program.getFields()) {
            Collection<?> values = mapping.getValues(backend,context);
            if(values !=null && !values.isEmpty()){
                result.add(mapping.getFieldName(),values);
            }
        }
        return result;
        
    }
    /**
     * The default configuration for the Entityhub
     * @author Rupert Westenthaler
     *
     */
    public static class EntityhubConfiguration extends DefaultConfiguration<Object>{
        public EntityhubConfiguration(ValueFactory vf){
            super();
            vf = vf == null ? InMemoryValueFactory.getInstance() : vf;
            //register special Entutyhub Transformer for
            // * entityhub:reference
            ValueConverter<Reference> referenceConverter = new ReferenceConverter();
            addTransformer(referenceConverter.getDataType(), 
                new ValueConverterTransformerAdapter<Reference>(referenceConverter,vf));
            // * xsd:anyURI
            ValueConverter<Reference> uriConverter = new AnyUriConverter();
            addTransformer(uriConverter.getDataType(), 
                new ValueConverterTransformerAdapter<Reference>(uriConverter,vf));
            // * entityhub:text
            ValueConverter<Text> literalConverter = new TextConverter();
            addTransformer(literalConverter.getDataType(), 
                new ValueConverterTransformerAdapter<Text>(literalConverter,vf));
            // xsd:string (use also the literal converter for xsd:string
            addTransformer(DataTypeEnum.String.getUri(), 
                new ValueConverterTransformerAdapter<Text>(literalConverter,vf));
            //Register the default namespaces
            for(NamespaceEnum ns : NamespaceEnum.values()){
                addNamespace(ns.getPrefix(), ns.getNamespace());
            }
        }
    }
}
