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
package org.apache.stanbol.entityhub.core.site;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheInitialisationException;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class CacheUtils {
    private CacheUtils(){}

    private static Logger log = LoggerFactory.getLogger(CacheUtils.class);

    /**
     * Loads the base mappings form the parsed Yard
     * @param yard The yard
     * @param nsPrefixService if present '{prefix}:{localname}' configurations are
     * @return The baseMappings
     * @throws YardException on any Error while getting the Representation holding
     * the Configuration from the Yard.
     * @throws CacheInitialisationException if the configuration is found but not
     * valid.
     * @throws IllegalArgumentException if <code>null</code> is parsed as {@link Yard}
     */
    public static FieldMapper loadBaseMappings(Yard yard, NamespacePrefixService nsPrefixService) throws YardException,CacheInitialisationException{
        if(yard == null){
            throw new IllegalArgumentException("The parsed Yard MUST NOT be NULL!");
        }
        Representation baseConfig = yard.getRepresentation(Cache.BASE_CONFIGURATION_URI);
        if(baseConfig != null){
            FieldMapper mapper = readFieldConfig(yard,baseConfig, nsPrefixService);
            if(mapper == null){
                String msg = "Invalid Base Configuration: Unable to parse FieldMappings from Field "+Cache.FIELD_MAPPING_CONFIG_FIELD;
                log.error(msg);
                if(log.isErrorEnabled()){
                    log.error(ModelUtils.getRepresentationInfo(baseConfig));
                }
                throw new CacheInitialisationException(msg);
            } else {
                return mapper;
            }
        } else {
            return null;
            //throw new CacheInitialisationException("Base Configuration not present");
        }
    }
    /**
     * Loads the additional field mappings used by this cache from the yard.
     * This method sets the {@link #baseMapper} field during initialisation.
     * @param yard The yard
     * @param nsPrefixService if present '{prefix}:{localname}' configurations are
     * @return The parsed mappings or <code>null</code> if no found
     * @throws YardException on any Error while reading the {@link Representation}
     * holding the configuration from the {@link Yard}.
     * @throws IllegalArgumentException if <code>null</code> is parsed as {@link Yard}.
     */
    protected static FieldMapper loadAdditionalMappings(Yard yard, NamespacePrefixService nsPrefixService) throws YardException {
        if(yard == null){
            throw new IllegalArgumentException("The parsed Yard MUST NOT be NULL!");
        }
        Representation addConfig = yard.getRepresentation(Cache.ADDITIONAL_CONFIGURATION_URI);
        if(addConfig != null){
            FieldMapper mapper = readFieldConfig(yard,addConfig, nsPrefixService);
            if(mapper == null){
                log.warn("Invalid Additinal Configuration: Unable to parse FieldMappings from Field "+Cache.FIELD_MAPPING_CONFIG_FIELD+"-> return NULL (no additional Configuration)");
                if(log.isWarnEnabled()){
                    log.warn(ModelUtils.getRepresentationInfo(addConfig));
                }
            }
            return mapper;
        } else {
            return null;
        }
    }
    /**
     * Reads the field mapping config from an document
     * @param yard the yard of the parsed Representation
     * @param config the configuration MUST NOT be <code>null</code>
     * @param nsPrefixService if present '{prefix}:{localname}' configurations are
     * supported for the fieldmappings used by the cache. 
     * @return A field mapper configured based on the configuration in the parsed {@link Representation}
     * @throws if the parsed {@link Representation} does not contain a value for {@value CacheConstants.FIELD_MAPPING_CONFIG_FIELD}.
     */
    private static FieldMapper readFieldConfig(Yard yard,Representation config, NamespacePrefixService nsPrefixService) {
        Object mappingValue = config.getFirst(Cache.FIELD_MAPPING_CONFIG_FIELD);
        if(mappingValue != null){
            DefaultFieldMapperImpl fieldMapper = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
            for(String mappingStirng : mappingValue.toString().split("\n")){
                FieldMapping mapping = FieldMappingUtils.parseFieldMapping(mappingStirng, nsPrefixService);
                if(mapping != null){
                    log.info("  > add Mapping: "+mappingStirng);
                    fieldMapper.addMapping(mapping);
                }
            }
            return fieldMapper;
        } else {
            return null;
        }
    }
    /**
     * Stores the baseMappings to the {@link Yard}. This may cause unexpected
     * behaviour for subsequest calls of the stored configuration does not
     * correspond with the actual data stored within the cache.<p>
     * Typically this is only used at the start or end of the creation of a
     * full Cache ({@link CacheStrategy#all}) of an referenced site (entity source).<p>
     * Note also that if the {@link #baseMapper} is <code>null</code> this
     * method removes any existing configuration from the yard.
     * @throws YardException an any error while storing the config to the yard.
     * @throws IllegalArgumentException if <code>null</code> is parsed as {@link Yard}.
     */
    public static void storeBaseMappingsConfiguration(Yard yard,FieldMapper baseMapper) throws YardException,IllegalArgumentException {
        if(yard == null){
            throw new IllegalArgumentException("The parsed Yard MUST NOT be NULL!");
        }
        if(baseMapper == null){
            yard.remove(Cache.BASE_CONFIGURATION_URI);
        } else {
            Representation config = yard.getValueFactory().createRepresentation(Cache.BASE_CONFIGURATION_URI);
            writeFieldConfig(config,baseMapper);
            yard.store(config);
        }
    }
    /**
     * Stores the current configuration used for caching documents back to the
     * {@link Yard}. This configuration is present in the  {@link #additionalMapper}).
     * If this field is <code>null</code> than any existing configuration is
     * removed form the index.
     * @throws YardException on any error while changing the configuration in the
     * yard.
     * @throws IllegalArgumentException if <code>null</code> is parsed as {@link Yard}.
     */
    protected static void storeAdditionalMappingsConfiguration(Yard yard,FieldMapper additionalMapper) throws YardException,IllegalArgumentException {
        if(yard == null){
            throw new IllegalArgumentException("The parsed Yard MUST NOT be NULL!");
        }
        if(additionalMapper == null){
            yard.remove(Cache.ADDITIONAL_CONFIGURATION_URI);
        } else {
            Representation config = yard.getValueFactory().createRepresentation(Cache.ADDITIONAL_CONFIGURATION_URI);
            writeFieldConfig(config,additionalMapper);
            yard.store(config);
        }
    }
    /**
     * Serialises all {@link FieldMapping}s of the parsed {@link FieldMapper}
     * and stores them in the {@value Cache#FIELD_MAPPING_CONFIG_FIELD} of the
     * parsed {@link Representation}
     * @param config the representation to store the field mapping configuration
     * @param mapper the field mapper with the configuration to store
     */
    private static void writeFieldConfig(Representation config, FieldMapper mapper){
        StringBuilder builder = new StringBuilder();
        for(FieldMapping mapping : mapper.getMappings()){
            builder.append(FieldMappingUtils.serialiseFieldMapping(mapping));
            builder.append('\n');
        }
        config.set(Cache.FIELD_MAPPING_CONFIG_FIELD, builder.toString());
    }
}
