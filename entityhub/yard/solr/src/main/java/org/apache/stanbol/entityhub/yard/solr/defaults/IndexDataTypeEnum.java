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
 * See the License for the specific languageType governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.entityhub.yard.solr.defaults;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.datatype.Duration;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.yard.solr.model.IndexDataType;

/**
 * Holds the default configuration for
 * <ul>
 * <li> {@link IndexDataType}s
 * <li>Default mapping of {@link IndexDataType}s to Java Objects
 * <li>Prefixes/Suffixes used to mark {@link IndexDataType}s in SolrDocument fields
 * </ul>
 * This Enumeration may be replaced later on by a more flexible way to configure such things.
 * 
 * @author Rupert Westenthaler
 * 
 */
public enum IndexDataTypeEnum {
    BOOLEAN(NamespaceEnum.xsd + "boolean", "bool", Boolean.class),
    // BYTE("byt",Byte.class),
    INT(NamespaceEnum.xsd + "int", "int", Integer.class),
    LONG(NamespaceEnum.xsd + "long", "lon", Long.class),
    FLOAT(NamespaceEnum.xsd + "float", "flo", Float.class),
    DOUBLE(NamespaceEnum.xsd + "double", "dou", Double.class),
    REF(RdfResourceEnum.ReferenceDataType.getUri(), "ref", Reference.class),
    // URI(NamespaceEnum.xsd+"anyURI","uri",URI.class), //currently URIs are modelled as REF
    // TODO: DATE & DUR to be removed. The plan is to add explicit support for ranged queries over time
    // spans/points!
    DATE(NamespaceEnum.xsd + "dateTime", "cal", Date.class),
    DUR(NamespaceEnum.xsd + "duration", "dur", Duration.class),
    TXT(RdfResourceEnum.TextDataType.getUri(), null, Text.class, true), // no type prefix, but typically
                                                                        // languageType prefixes
    STR(NamespaceEnum.xsd + "string", "str", String.class, true), // string values (not used for languageType)
    ID(NamespaceEnum.xsd + "id", "id", UUID.class), ;
    private IndexDataType indexType;
    private Class<?> javaType;
    private String prefix;
    private String suffix;
    /**
     * if true, values of this dataType should be treated as natural languageType texts and added to the
     * {@link SolrConst#LANG_MERGER_FIELD}
     */
    private boolean languageType;

    IndexDataTypeEnum(String name, String prefix, Class<?> type) {
        this(name, prefix, null, type, false);
    }

    IndexDataTypeEnum(String name, String prefix, Class<?> type, boolean language) {
        this(name, prefix, null, type, language);
    }

    IndexDataTypeEnum(String name, String prefix, String suffix, Class<?> type, boolean language) {
        this.indexType = new IndexDataType(name);
        this.prefix = prefix;
        this.suffix = suffix;
        this.javaType = type;
        this.languageType = language;
    }

    /**
     * The prefix to be used for index fields of that type
     * 
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * The suffix to be used for index fields of that type
     * 
     * @return
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * The index type
     * 
     * @return the indexType
     */
    public IndexDataType getIndexType() {
        return indexType;
    }

    /**
     * The java type
     * 
     * @return the java class for the index type
     */
    public final Class<?> getJavaType() {
        return javaType;
    }

    /**
     * Returns <code>true</code> if values of this dataType should be included in searches for natural
     * language texts. This means that such values are added to the {@link SolrConst#LANG_MERGER_FIELD}
     * 
     * @return the languageType
     */
    public boolean isLanguageType() {
        return languageType;
    }

    /*--------------------------------------------------------------------------
     * Code that reads the config and inits lookup tables (also checks config)
     * --------------------------------------------------------------------------
     */

    private static Map<Class<?>,IndexDataTypeEnum> javaTypeMap;
    private static Map<IndexDataType,IndexDataTypeEnum> indexTypeMap;
    private static Map<List<String>,IndexDataTypeEnum> prefixSuffixMap;
    private static Map<String,IndexDataTypeEnum> uriMap;
    static {
        /*
         * This inits the Mappings and also validates the configuration provided by the Enumeration!
         */
        Map<Class<?>,IndexDataTypeEnum> jtm = new HashMap<Class<?>,IndexDataTypeEnum>();
        Map<IndexDataType,IndexDataTypeEnum> itm = new HashMap<IndexDataType,IndexDataTypeEnum>();
        Map<List<String>,IndexDataTypeEnum> psm = new HashMap<List<String>,IndexDataTypeEnum>();
        Map<String,IndexDataTypeEnum> um = new HashMap<String,IndexDataTypeEnum>();
        for (IndexDataTypeEnum dt : IndexDataTypeEnum.values()) {
            if (jtm.containsKey(dt.javaType)) {
                throw new IllegalStateException(String.format(
                    "Found multiple IndexTypes %s and %s for Class %s! Wrong Data provided by %s",
                    dt.indexType, jtm.get(dt.javaType).indexType, dt.javaType, IndexDataTypeEnum.class));
            } else {
                jtm.put(dt.javaType, dt);
            }
            if (itm.containsKey(dt.indexType)) {
                throw new IllegalStateException(String.format(
                    "Found multiple Entries with IndexType %s! Wrong Data provided by %s", dt.indexType,
                    IndexDataTypeEnum.class));
            } else {
                itm.put(dt.indexType, dt);
            }
            // NOTE: Do not use Arrays.asList(..) directly, because it does not
            // implement equals and hashCode!
            List<String> ps = new ArrayList<String>(2);
            ps.add(dt.prefix);
            ps.add(dt.suffix);
            psm.put(ps, dt);
            if (um.containsKey(dt.getIndexType().getId())) {
                throw new IllegalStateException(String.format(
                    "Found multiple Entries with the same data type URI %s! Uri used by %s and %s!", dt
                            .getIndexType().getId(), dt.name(), um.get(dt.getIndexType().getName()).name()));
            } else {
                um.put(dt.getIndexType().getId(), dt);
            }
        }
        javaTypeMap = Collections.unmodifiableMap(jtm);
        indexTypeMap = Collections.unmodifiableMap(itm);
        prefixSuffixMap = Collections.unmodifiableMap(psm);
        uriMap = Collections.unmodifiableMap(um);
    }

    /**
     * Lookup table for the IndexDataTypeEnum based on the java type
     * 
     * @param type
     *            the java type
     * @return the IndexDataTypeEnum for the parsed type or <code>null</code> if no IndexDataTypeEnum is
     *         configured for the parsed type.
     */
    public static IndexDataTypeEnum forJavaType(Class<?> type) {
        return javaTypeMap.get(type);
    }

    /**
     * Lookup table for the IndexDataTypeEnum based on the IndexType.
     * 
     * @param indexType
     *            the indexType
     * @return the IndexDataTypeEnum for the parsed IndexTyep or <code>null</code> if no IndexDataTypeEnum is
     *         configured for the parsed IndexType.
     */
    public static IndexDataTypeEnum forIndexType(IndexDataType indexType) {
        return indexTypeMap.get(indexType);
    }

    /**
     * Lookup table for the IndexDataTypeEnum based on the prefix and suffix
     * 
     * @param prefix
     *            the prefix (might be <code>null</code>)
     * @param suffix
     *            the suffix ( (might be <code>null</code>)
     * @return the IndexDataTypeEnum for the parsed prefix and suffix or <code>null</code> if no
     *         IndexDataTypeEnum is configured for the parsed parameter.
     */
    public static IndexDataTypeEnum forPrefixSuffix(String prefix, String suffix) {
        List<String> ps = new ArrayList<String>(2);
        ps.add(prefix);
        ps.add(suffix);
        return prefixSuffixMap.get(ps);
    }

    /**
     * Lookup table for the IndexDataTypeEnum based on the data type uri as stored in the
     * {@link IndexDataType#getName()} property.
     * 
     * @param uri
     *            the uri of the dataType
     * @return the IndexDataTypeEnum for the parsed uri or <code>null</code> if no IndexDataTypeEnum is
     *         configured for the parsed parameter.
     */
    public static IndexDataTypeEnum forUri(String uri) {
        return uriMap.get(uri);
    }
}
