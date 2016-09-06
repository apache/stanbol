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

package org.apache.stanbol.enhancer.engines.dereference.entityhub;

import static org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants.DEREFERENCE_ENTITIES_LDPATH;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceConfigurationException;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceConstants;
import org.apache.stanbol.enhancer.engines.dereference.DereferenceContext;
import org.apache.stanbol.enhancer.engines.dereference.EntityDereferenceEngine;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;

public class EntityhubDereferenceContext extends DereferenceContext {
    
    private FieldMapper fieldMapper;
    private Program<Object> ldpathProgram;
    
    protected EntityhubDereferenceContext(EntityDereferenceEngine engine, 
            Map<String,Object> enhancementProps) throws DereferenceConfigurationException {
        super(engine, enhancementProps);
    }

    @Override
    protected void initialise() throws DereferenceConfigurationException {
        initFieldMappings(getFields());
        initLdPath(getLdPathProgram());
    }

    protected void initFieldMappings(List<String> fields) throws DereferenceConfigurationException {
        TrackingDereferencerBase<?> dereferencer = getEntityhubDereferencer();
        FieldMapper fieldMapper;
        if(fields != null && !fields.isEmpty()){
            log.debug("parse FieldMappings from EnhancementProperties");
            List<FieldMapping> mappings = new ArrayList<FieldMapping>(fields.size());
            for(String configuredMapping : fields){
                FieldMapping mapping = FieldMappingUtils.parseFieldMapping(configuredMapping,
                    dereferencer.getNsPrefixService());
                if(mapping != null){
                    log.debug("   - add FieldMapping {}",mapping);
                    mappings.add(mapping);
                } else if(configuredMapping != null && !configuredMapping.isEmpty()){
                    log.warn("   - unable to parse FieldMapping '{}'", configuredMapping);
                }
            }
            if(!mappings.isEmpty()){
                log.debug(" > apply {} valid mappings",mappings.size());
                fieldMapper = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
                for(FieldMapping mapping : mappings){
                    fieldMapper.addMapping(mapping);
                }
            } else { //no valid mapping parsed
                log.debug(" > no valid mapping parsed ... will dereference all fields");
                fieldMapper = null;
            }
        } else if(dereferencer.getFieldMapper() != null){
            fieldMapper = dereferencer.getFieldMapper().clone();
        } else {
            fieldMapper = null;
        }
        
        //TODO: uncomment this to merge context with engine mappings. Not sure
        //      if this is desirable 
//      if(fieldMapper != null){
//            if(dereferencer.getFieldMapper() != null){
//                //add mappings of the engine configuration to the context mappings
//                for(FieldMapping mapping : dereferencer.getFieldMapper().getMappings()){
//                    fieldMapper.addMapping(mapping);
//                }
//            }
//        }

        //if a fieldMapper is present and languages are set we will add a language
        //filter to the fieldMapper. If the fieldmapper is null languages are
        //filtered separately. 
        Collection<String> langs = getLanguages();
        if(langs != null && !langs.isEmpty()){
            if(fieldMapper == null){ //create a fieldMapper for filtering languages
                fieldMapper = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
            }
            fieldMapper.addMapping(new FieldMapping(new TextConstraint(
                (String)null, langs.toArray(new String[langs.size()]))));
        }

        this.fieldMapper = fieldMapper; //set the field
    }
    
    protected void initLdPath(String program) throws DereferenceConfigurationException {
        TrackingDereferencerBase<?> dereferencer = getEntityhubDereferencer();
        ValueFactory valueFactory = dereferencer.getValueFactory();
        Program<Object> ldpathProgram;
        if(!StringUtils.isBlank(program)){
            @SuppressWarnings("rawtypes")
            RDFBackend<Object> parseBackend = new ParseBackend<Object>(valueFactory);
            EntityhubLDPath parseLdPath = new EntityhubLDPath(parseBackend, valueFactory);
            try {
                ldpathProgram = parseLdPath.parseProgram(new StringReader(program));
            } catch (LDPathParseException e) {
                log.error("Unable to parse Context LDPath pogram: \n {}", program);
                throw new DereferenceConfigurationException(
                    "Unable to parse context LDPath program !",
                    e,dereferencer.getClass(), DEREFERENCE_ENTITIES_LDPATH);
            }
            //finally validate if all mappings of the program do use a URI as key
            //also store used fieldNames as we need them later
            Set<String> contextFields = new HashSet<String>();
            for(org.apache.marmotta.ldpath.model.fields.FieldMapping<?,Object> mapping : ldpathProgram.getFields()) {
                try {
                    new URI(mapping.getFieldName());
                    contextFields.add(mapping.getFieldName());
                } catch (URISyntaxException e){
                    throw new DereferenceConfigurationException( 
                        "Parsed LDPath MUST use valid URIs as field names (invalid field name: '"
                        + mapping.getFieldName()+"' | selector: '" 
                        + mapping.getSelector().getPathExpression(parseBackend)+"')!",
                        dereferencer.getClass(),DereferenceConstants.DEREFERENCE_ENTITIES_LDPATH);
                }
            }
            //append the mappings configured for the engine
            if(dereferencer.getLdPathProgram() != null){
                for(org.apache.marmotta.ldpath.model.fields.FieldMapping<?,Object> mapping : dereferencer.getLdPathProgram().getFields()) {
                    if(!contextFields.contains(mapping.getFieldName())){
                        ldpathProgram.addMapping(mapping);
                    }//else ignore mappings for fields specified in the context
                }
            }
        } else { //no context specific - use the one of the config
            ldpathProgram = dereferencer.getLdPathProgram();
        }
        if(ldpathProgram != null && !ldpathProgram.getFields().isEmpty()){
            this.ldpathProgram = ldpathProgram;
        } else {
            this.ldpathProgram = null;
        }
    }

    /**
     * Getter for the Entityhub Dereferencer base
     * @return
     */
    protected TrackingDereferencerBase<?> getEntityhubDereferencer(){
        return (TrackingDereferencerBase<?>)engine.getDereferencer();
    }

    /**
     * Getter for the FieldMapper parsed for the {@link #getFields()} parsed
     * in the context (or the config if no fields where present in the context)
     * @return the field mapper to be used for dereferencing entities or
     * <code>null</code> if no field mappings are present
     */
    public FieldMapper getFieldMapper() {
        return fieldMapper;
    }
    /**
     * Getter for the LDPath Program parsed from the {@link #getLdPathProgram()}
     * parsed in the context and the program set in the configuration. Fields
     * present in the context will override (replace) those that are also
     * present in the program set for the engine configuration. 
     * @return the parsed LDPath {@link Program} or <code>null</code> if none
     */
    public Program<Object> getProgram() {
        return ldpathProgram;
    }
    
}
