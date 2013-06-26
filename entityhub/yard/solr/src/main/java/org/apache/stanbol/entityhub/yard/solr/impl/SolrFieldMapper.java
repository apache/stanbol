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
package org.apache.stanbol.entityhub.yard.solr.impl;

import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.DEPENDENT_DOCUMENT_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.DOCUMENT_ID_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.DOMAIN_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.FULL_TEXT_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.PATH_SEPERATOR;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.REFERRED_DOCUMENT_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.SPECIAL_CONFIG_FIELD;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.solr.utils.SolrUtil;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.defaults.SpecialFieldEnum;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.apache.stanbol.entityhub.yard.solr.defaults.IndexDataTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst;
import org.apache.stanbol.entityhub.yard.solr.model.FieldMapper;
import org.apache.stanbol.entityhub.yard.solr.model.IndexField;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the FieldMapper for a Solr Index.
 * 
 * @author Rupert Westenthaler
 * 
 */
public class SolrFieldMapper implements FieldMapper {

    private static Logger log = LoggerFactory.getLogger(SolrFieldMapper.class);
    /**
     * Char used to separate the prefix from the local name of uri's
     */
    private static final char NAMESPACE_PREFIX_SEPERATOR_CHAR = ':';
    private static final String LANG_MERGER_PREFIX = "" + SolrConst.SPECIAL_FIELD_PREFIX
                                                     + SolrConst.MERGER_INDICATOR + SolrConst.LANG_INDICATOR;
    /**
     * The size of the LRU cache for FieldName to IndexField as well as IndexField to collection of FieldNames
     * mappings.
     * <p>
     * Note that both caches may have a maximum of elements as configured by this property.
     */
    private static final int LRU_MAPPINGS_CACHE_SIZE = 1024;
    /**
     * The IndexField for the Solr score. This field is mapped to the field
     * {@link RdfResourceEnum#resultScore} and uses {@link IndexDataTypeEnum#FLOAT}
     */
    private static final IndexField scoreField = new IndexField(
            Collections.singletonList(RdfResourceEnum.resultScore.getUri()),
            IndexDataTypeEnum.FLOAT.getIndexType());
    /**
     * The Solr Server of this FieldMapper
     */
    protected final SolrServer server;

    /**
     * Internally used as LRU Cache with {@link SolrFieldMapper#LRU_MAPPINGS_CACHE_SIZE} elements. This
     * subclass of {@link LinkedHashMap} overrides the {@link LinkedHashMap#removeEldestEntry(Entry)} as
     * suggested by the java doc. It also uses the constructor that activates the ordering based on access
     * time rather tan insertion time.
     * 
     * @author Rupert Westenthaler
     * 
     * @param <K>
     *            generic type of the key
     * @param <V>
     *            generic type of the value
     */
    private static final class LRU<K,V> extends LinkedHashMap<K,V> {
        public LRU() {
            super(16, 0.75f, true); // access order!
        }

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > LRU_MAPPINGS_CACHE_SIZE;
        }
    }

    /**
     * The assumption is, that only a handful of {@link IndexField}s are used very often.
     * <p>
     * So it makes sense to keep some mappings within a cache rather than calculating them again and again.
     * 
     * @see LinkedHashMap#
     */
    private final Map<IndexField,List<String>> indexFieldMappings = 
            //STANBOL-669: LRU chaches MUST BE synchronized!
            Collections.synchronizedMap(new LRU<IndexField,List<String>>());
    /**
     * The assumption is, that only a handful of fields appear in index documents. So it makes sense to keep
     * some mappings within a cache rather than calculating them again and again.
     */
    private final Map<String,IndexField> fieldMappings = 
            //STANBOL-669: LRU chaches MUST BE synchronized!
            Collections.synchronizedMap(new LRU<String,IndexField>());
    
    private NamespacePrefixService nsPrefixService;

    public SolrFieldMapper(SolrServer server, NamespacePrefixService nps) {
        if (server == null) {
            log.warn("NULL parsed as SolrServer: Loading and Saving of the Namespace Prefix Settings will be deactivated!");
            log.warn("  This is OK for Unit Test but should not happen in productive use!");
        }
        this.nsPrefixService = nps;
        this.server = server;
    }

    @Override
    public IndexField getField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("The parsed field name MUST NOT be NULL!");
        }
        IndexField field = fieldMappings.get(fieldName);
        if (field == null) {
            if (getDocumentIdField().equals(fieldName)
                || fieldName.charAt(0) == SolrConst.SPECIAL_FIELD_PREFIX) {
                // in case of special field or the document ID, return null ->
                // meaning, that this index document field does not represent
                // an logical IndexField and should be ignored
                return null;
            } else if (SolrConst.SCORE_FIELD.equals(fieldName)) {
                return scoreField;
            }
            // parse the prefix and suffix
            String[] tokens = fieldName.split(Character.toString(SolrConst.PATH_SEPERATOR));
            int numTokens = tokens.length;
            int pathElements = numTokens;
            String prefix = null;
            String suffix = null;
            if (tokens.length >= 2) {
                prefix = tokens[0];
                pathElements--;
            }
            if (tokens.length >= 3) {
                suffix = tokens[numTokens - 1].substring(1);
                pathElements--;
            }

            // parse the path
            String[] path = new String[pathElements];
            System.arraycopy(tokens, prefix == null ? 0 : 1, path, 0, pathElements);
            tokens = null;
            // process the parsed data
            field = parseIndexField(prefix, suffix, path);
            if (field != null) {
                fieldMappings.put(fieldName, field);
            }
        }
        return field;
    }

    /**
     * This method does the dirty work of parsing the different parts of the field in the SolrDocument to the
     * logical field as used by the semantic indexing API. This method assumes the following encoding
     * <code><pre>
     *   .        ... path separator
     *   _        ... special field indicator
     *   __       ... escaped special field
     *   !        ... merger - collected values of other fields.
     *                Such fields do not have an mapping to logical IndexFields.
     *                All mergers are created by copyField configurations within the
     *                Solr Schema configuration
     *   @        ... '@' indicates a field in a given language
     *     _@.&lt;field&gt;: A value for a field with no language defined
     *     _@en.&lt;field&gt;: A value for a field in English
     *     _!@.&lt;field&gt;: Contains all labels regardless of language
     *     _!@en.&lt;field&gt;: Contains all labels of languages that start with "en"
     *   &lt;prefix&gt; ... indicates an dataType that used this prefix
     *     _str.&lt;field&gt;: A string field (containing no language)
     *     _ref.&lt;field&gt;: A reference (similar to xsd:anyURI)
     *     _bool.&lt;field&gt;: A boolean value
     * 
     * NOTE: Prefixes/Suffixes can be used to define a hierarchy of data types
     * e.g. use Prefixes for dataTypes:
     *   _n   ... any kind of numeric value
     *   _ni  ... any kind of integer value (BigInteger)
     *   _nib ... a byte
     *   _nii ... a integer
     *   _nil ... a long
     *   _nd  ... a decimal value
     *   _ndf ... float
     *   _ndd ... double
     *   _s   ... any kind of string value
     *   _si  ... an string based ID
     *   _sr  ... a reference
     * e.g. use Suffixes for semantic meanings
     *   ._ct ... a tag
     *   ._cr ... a category using a reference to an entity ID (xsd:anyURI)
     *   ._ci ... a categorisation using an local id (e.g 2 letter country codes)
     * 
     *  one can now create Solr copyField commands to support searches spanning
     *  over multiple types
     *  _!n  ... search for any kind of numbers
     *  _!ni ... search for any kind of integers
     *  _!s  ... search in all kind of string values
     *  _!sc ... search for all categories of this document
     * 
     * </pre><code>
     * 
     * @param prefix
     * @param suffix
     * @param pathElements
     * @return
     */
    private IndexField parseIndexField(final String prefix, final String suffix, final String[] pathElements) {
        final String language;
        boolean isLanguage = false;
        final String dataTypePrefix;
        // first use the prefix to parse the language
        // -> note that the prefix might also be used for the data type!
        if (prefix != null && !prefix.isEmpty()) {
            if (prefix.charAt(0) == SolrConst.LANG_INDICATOR) {
                isLanguage = true;
                // it is a language prefix!
                // set dataTypePrefix to null
                dataTypePrefix = null;
                if (prefix.length() > 1) {
                    language = prefix.substring(1);
                } else { // it is a language prefix, but for the default language!
                    language = null;
                }
            } else { // it is no language prefix
                language = null;
                isLanguage = false;
                dataTypePrefix = prefix;
            }
        } else { // no prefix at all
            // set no-language and the dataType prefix to null;
            isLanguage = false;
            language = null;
            dataTypePrefix = null;
        }
        // now parse the indexDataType!
        IndexDataTypeEnum dataTypeEnumEntry = IndexDataTypeEnum.forPrefixSuffix(dataTypePrefix, suffix);
        if (dataTypeEnumEntry == null) {
            log.warn(String.format(
                "No IndexDataType registered for prefix: %s and suffix: %s -> unable to process path %s",
                dataTypePrefix, suffix, Arrays.toString(pathElements)));
            return null; // we might also throw an exception at this point
        }
        // parse the path
        List<String> path = new ArrayList<String>(pathElements.length);
        for (String pathElement : pathElements) {
            if (pathElement.charAt(0) == SolrConst.SPECIAL_FIELD_PREFIX) {
                if (pathElement.charAt(1) == SolrConst.SPECIAL_FIELD_PREFIX) {
                    path.add(getFullFieldName(pathElement.substring(1)));
                } else {
                    throw new IllegalStateException(
                            String.format(
                                "Found special field \"%s\" within the path \"%s\" -> Special fields are only allowed as prefix and suffix!",
                                pathElement, Arrays.toString(pathElements)));
                }
            } else {
                String fullName = getFullFieldName(pathElement);
                if (fullName == null) {
                    throw new IllegalStateException(String.format(
                        "Unable to map PathElement %s to it's full Name (path=%s)!", pathElement,
                        Arrays.toString(pathElements)));
                } else {
                    path.add(fullName);
                }
            }
        }
        if (isLanguage) {
            return new IndexField(path, dataTypeEnumEntry.getIndexType(), language);
        } else {
            return new IndexField(path, dataTypeEnumEntry.getIndexType());
        }
    }

    @Override
    public Collection<String> getFieldNames(List<String> path, IndexValue indexValue) throws IllegalArgumentException {
        IndexField field;
        if (indexValue.hasLanguage()) {
            field = new IndexField(path, indexValue.getType(), indexValue.getLanguage());
        } else {
            field = new IndexField(path, indexValue.getType());
        }
        return getFieldNames(field);
    }

    @Override
    public Collection<String> getQueryFieldNames(IndexField indexField) throws IllegalArgumentException {
        List<String> fields = getFieldNames(indexField);
        if((indexField.getLanguages() != null && !indexField.getLanguages().isEmpty()) &&
                IndexDataTypeEnum.forIndexType(indexField.getDataType()).isLanguageType()){
            return fields.subList(0, fields.size()-1); //cut of the field with all languages
        } else {
            return fields;
        }
    }
    @Override
    public List<String> getFieldNames(IndexField indexField) throws IllegalArgumentException {
        if (indexField == null) {
            throw new IllegalArgumentException("The parsed IndexField name MUST NOT be NULL!");
        }
        List<String> fieldNames = indexFieldMappings.get(indexField);
        if (fieldNames == null) {
            SpecialFieldEnum specialField = indexField.getSpecialField();//check for special field;
            if(specialField != null){
                switch (specialField) {
                    case fullText:
                        fieldNames = Collections.singletonList(getFullTextSearchField());
                        break;
                    case references:
                        fieldNames = Collections.singletonList(getReferredDocumentField());
                        break;
                    default:
                        throw new IllegalStateException("Unsupported Special Field '"
                            +specialField.getUri()+"! Please report this to the "
                            + "Stanbol Developer Mailing list or create an according"
                            + "JIRA issue at https://issues.apache.org/jira/browse/STANBOL!");
                }
            } else {
                fieldNames = new ArrayList<String>(2); //typically only 1 or 2 values
                IndexDataTypeEnum dataTypeConfig = IndexDataTypeEnum.forIndexType(indexField.getDataType());
                if (dataTypeConfig == null) {
                    throw new IllegalStateException(String.format(
                        "No Config found for the parsed IndexDataType %s", indexField.getDataType()));
                }
                // Three things need to be done
                // 1) Encode the Path
                String pathName = encodePathName(indexField);
                // 2) Encode the DataType
                fieldNames.addAll(encodeDataType(pathName, dataTypeConfig));
                // 3) Encode the Languages
                if (indexField.hasLanguage()) {
                    fieldNames.addAll(encodeLanguages(pathName, indexField.getLanguages()));
                }
                // 4) add the language merger field (in case the dataType represent natural
                // language texts)
                if (dataTypeConfig.isLanguageType()) {
                    fieldNames.add(SolrConst.LANG_MERGER_FIELD + pathName);
                }
            }
            // cache the mappings
            indexFieldMappings.put(indexField, fieldNames);
        }
        return fieldNames;
    }

    /**
     * Getter for the string used to index a the parsed path. This method replaces the URI's of all elements
     * within the path with <code>prefix+NAMESPACE_PREFIX_SEPERATOR_CHAR+localName</code>. In addition it
     * places the <code>PATH_SEPERATOR</code> char between the elements.
     * <p>
     * NOTES: <ul>
     *  <li>This Method assumes that no empty or <code>null</code> elements are
     *  containted in the parsed list.
     *  <li>This Method supports special encoding of fields registered in the
     *  {@link SpecialFieldEnum}. However those fields are only allowed to be
     *  used in paths with the length <code>1</code>. 
     *  An {@link IllegalArgumentException} is thrown if a special field is used
     *  in a longer path.
     * </ul>
     * @param path
     *            the path to encode
     * @return the path name
     * @throws IllegalArgumentException if <code>null</code> or an empty list is
     * parsed as path or a special field is used in a path with a length &gt; 1
     * @throws IllegalStateException if an unknown {@link SpecialFieldEnum
     * special field} is encountered.
     */
    private String encodePathName(IndexField indexField) {
        SpecialFieldEnum specialField = indexField.getSpecialField();
        if(specialField != null){ //handel special fields
            switch (specialField) {
                case fullText:
                    return getFullTextSearchField();
                case references:
                    return getReferredDocumentField();
                default:
                    throw new IllegalStateException("Unsupported Special Field '"
                            + specialField.getUri()+"'! Please report this to"
                            + "the Apache Stanbol Developer Mailing List!");
            }
        } else { //normal field
            StringBuilder pathName = new StringBuilder();
            // Now Iterate over the Path
            pathName.append(PATH_SEPERATOR); // add the leading PathSeperator
            Iterator<String> fields = indexField.getPath().iterator();
            while (fields.hasNext()) {
                String field = fields.next();
                String[] namespaceLocalName = ModelUtils.getNamespaceLocalName(field);
                // QName qName = getQName(field);
                if (namespaceLocalName[0] != null && !namespaceLocalName[0].isEmpty()) {
                    pathName.append(getPrefix(namespaceLocalName[0], true));
                    // second the local name
                    pathName.append(NAMESPACE_PREFIX_SEPERATOR_CHAR);
                }
                pathName.append(namespaceLocalName[1]);
                // third add Path Separator if there are additional Elements
                if (fields.hasNext()) {
                    pathName.append(PATH_SEPERATOR);
                }
            }
            pathName.append(PATH_SEPERATOR); // add the tailing PathSeperator
            return pathName.toString();
        }
    }

    @Override
    public String encodePath(IndexField indexField) throws IllegalArgumentException {
        return encodePathName(indexField);
    }

    /**
     * Encodes the datatype by adding the prefix and the suffix to the parsed path name. If no prefix nor
     * suffix is defined for the parsed data type, than this method returns an empty collection (indicating
     * that no encoding is necessary)
     * 
     * @param pathName
     *            the path name to add the prefix and the suffix.
     * @param dataType
     *            the dataType to encode.
     * @return The fields representing the encoded dataType for the parsed field.
     */
    private Collection<String> encodeDataType(String pathName, IndexDataTypeEnum dataType) {
        String[] prefixSuffix = encodeDataType(dataType);
        if ((prefixSuffix[0] == null || prefixSuffix[0].isEmpty())
            && (prefixSuffix[1] == null || prefixSuffix[1].isEmpty())) {
            // no prefix nor suffix defined -> return empty collection
            return Collections.emptyList();
        } else {
            // return prefix+fieldName+suffix
            return Collections.singleton((prefixSuffix[0] != null ? prefixSuffix[0] : "") + pathName
                                         + (prefixSuffix[1] != null ? prefixSuffix[1] : ""));
        }
    }

    @Override
    public String[] encodeDataType(IndexField indexField) throws IllegalArgumentException {
        IndexDataTypeEnum dataTypeConfig = IndexDataTypeEnum.forIndexType(indexField.getDataType());
        if (dataTypeConfig == null) {
            throw new IllegalStateException(String.format("No Config found for the parsed IndexDataType %s",
                indexField.getDataType()));
        }
        return encodeDataType(dataTypeConfig);
    }

    private String[] encodeDataType(IndexDataTypeEnum dataType) {
        String[] prefixSuffix = new String[] {null, null};
        if (dataType.getPrefix() != null && !dataType.getPrefix().isEmpty()) {
            prefixSuffix[0] = dataType.getPrefix();
        }
        if (dataType.getSuffix() != null && !dataType.getSuffix().isEmpty()) {
            prefixSuffix[1] = dataType.getSuffix();
        }
        return prefixSuffix;
    }

    /**
     * Encodes the prefixes for the parsed languages and returns the according field names for the languages.
     * <p>
     * Languages are encoded using the {@link SolrConst#LANG_INDICATOR} and the parsed language as field
     * prefix.
     * <p>
     * Note that this implementation adds dataTypes that are marked as natural language text values ( all
     * dataTypes where <code>{@link IndexDataTypeEnum#isLanguageType()} == true</code>) to the special
     * {@link SolrConst#LANG_MERGER_FIELD}. This can be used to search for values of an field in any language.
     * <p>
     * In addition to that the default schema.xml also defines a copyField command that puts natural language
     * values of all fields into the default search field "_text".
     * <p>
     * The collection returned by this method does not include
     * <code>"{@link SolrConst#LANG_MERGER_FIELD}"+feildName</code>!
     * 
     * @param fieldName
     *            the string representing the field without encoded languages
     * @param languages
     *            the languages.
     * @return
     */
    private Collection<String> encodeLanguages(String fieldName, Collection<String> languages) {
        if (languages == null || languages.isEmpty()) { // no language
            return Collections.singleton(fieldName);// just return the field
        } else {
            // I assume that this will be the case in most of the calls
            Collection<String> fieldNames = new ArrayList<String>(languages.size() * 2);
            for (String prefix : encodeLanguages(languages)) {
                fieldNames.add(prefix + fieldName);
            }
            return fieldNames;
        }
    }

    @Override
    public Collection<String> encodeLanguages(IndexField indexField) {
        return encodeLanguages(indexField.getLanguages());
    }
    /**
     * Internally used instead of {@link #encodeLanguages(String...)}
     * 
     * @param languages
     *            the languages
     * @return the prefixes
     * @see FieldMapper#encodeLanguages(String...)
     */
    private Collection<String> encodeLanguages(Collection<String> languages) {
        if (languages == null || languages.isEmpty()) { // no language
            return Collections.emptySet();// just return the field
        } else if (languages.size() == 1) {
            return Collections.singleton(encodeLanguage(languages.iterator().next()));
        } else {
            Set<String> langPrefixes = new HashSet<String>();
            for (String lang : languages) {
                langPrefixes.add(encodeLanguage(lang));
            }
            return langPrefixes;
        }
    }

    @Override
    public String getLanguageMergerField(String lang) {
        return LANG_MERGER_PREFIX + (lang != null ? lang : "");
    }

    /**
     * Encodes the language prefixes of for the parsed language
     * 
     * @param lang
     *            the language
     * @return the field with the encoded language
     */
    private String encodeLanguage(String lang) {
        StringBuilder langField = new StringBuilder();
        langField.append(SolrConst.LANG_INDICATOR);
        if (lang != null) {
            langField.append(lang);
        }
        return langField.toString();
    }

    /*--------------------------------------------------------------------------
     * The remaining (boring) methods that provide static field names for
     * special fields.
     * -------------------------------------------------------------------------
     */
    @Override
    public String getDocumentIdField() {
        return DOCUMENT_ID_FIELD;
    }

    @Override
    public String getReferredDocumentField() {
        return REFERRED_DOCUMENT_FIELD;
    }

    @Override
    public String getDependentDocumentField() {
        return DEPENDENT_DOCUMENT_FIELD;
    }
    @Override
    public String getFullTextSearchField() {
        return FULL_TEXT_FIELD;
    }

    /*--------------------------------------------------------------------------
     * Helper Methods to store/read the Mapping Config within the index.
     * TODO: Move this to an own class e.g. IndexConfig or something like that
     * -------------------------------------------------------------------------
     */
    private int defaultNsPrefixNumber = 1;
    private static final String DEFAULT_NS_PREFIX_STRING = "ns";
    // private static final char NAMESPACE_PREFIX_SEPERATOR_CHAR = ':';
    /**
     * Do never access this Map directly! Use {@link #getNamespaceMap()}!
     */
    private Map<String,String> __namespaceMap = null;

    /**
     * Getter for the namespace to prefix mapping
     * 
     * @return the map holding the namespace to prefix mappings
     */
    private Map<String,String> getNamespaceMap() {
        prefixNamespaceMappingsLock.readLock().lock();
        Map<String,String> m = __namespaceMap;
        prefixNamespaceMappingsLock.readLock().unlock();
        if (m == null) {
            prefixNamespaceMappingsLock.writeLock().lock();
            try {
                m = __namespaceMap; //might be concurrently be initialised
                if(m == null){
                    loadNamespaceConfig();
                    m = __namespaceMap;
                }
            } finally {
                prefixNamespaceMappingsLock.writeLock().unlock();
            }
            
        }
        return m;
    }

    /**
     * Do never access this Map directly! Use {@link #getPrefixMap()}!
     */
    private Map<String,String> __prefixMap = null;
    /**
     * used as lock during loading of the namespace <-> prefix mappings
     * (fixes STANBOL-668)
     */
    private ReentrantReadWriteLock prefixNamespaceMappingsLock = new ReentrantReadWriteLock();

    /**
     * Getter for the prefix to namespace mappings
     * 
     * @return the map holding the prefix to namespace mappings
     */
    private Map<String,String> getPrefixMap() {
        prefixNamespaceMappingsLock.readLock().lock();
        Map<String,String> m = __prefixMap;
        prefixNamespaceMappingsLock.readLock().unlock();
        if (m == null) {
            prefixNamespaceMappingsLock.writeLock().lock();
            try {
                m = __prefixMap; //might be concurrently be initialised
                if(m == null){
                    loadNamespaceConfig();
                    m = __prefixMap;
                }
            } finally {
                prefixNamespaceMappingsLock.writeLock().unlock();
            }
        }
        return m;
    }

    /**
     * Getter for the full name based on the short name. The short name is defined as the prefix followed by
     * the {@link #NAMESPACE_PREFIX_SEPERATOR_CHAR} and the local name of the field. The returned field name
     * is defined as the namespace followed by the local name.
     * <p>
     * If the parsed short field name does not contain the {@link #NAMESPACE_PREFIX_SEPERATOR_CHAR} this
     * method returns the parsed String.
     * <p>
     * The local name may contain the {@link #NAMESPACE_PREFIX_SEPERATOR_CHAR}
     * {@link #NAMESPACE_PREFIX_SEPERATOR_CHAR}'. The prefix MUST NOT contain this char, because
     * {@link String#indexOf(int)} is used to split prefix and local name.
     * 
     * @param shortFieldName
     *            the short name
     * @return the full name
     * @throws IllegalArgumentException
     *             if <code>null</code> is parsed as shortFieldName
     * @throws IllegalStateException
     *             if the found prefix is not contained in the configuration
     */
    protected final String getFullFieldName(String shortFieldName) throws IllegalArgumentException,
                                                                  IllegalStateException {
        if (shortFieldName == null) {
            throw new IllegalArgumentException("Parameter shortFieldName MUST NOT be NULL");
        }
        int seperatorIndex = shortFieldName.indexOf(NAMESPACE_PREFIX_SEPERATOR_CHAR);
        if (seperatorIndex >= 0) {
            String prefix = shortFieldName.substring(0, seperatorIndex); // seperatorIndex does not include
                                                                         // the separator char
            String namespace = getNamespace(prefix);
            if (namespace != null) {
                return namespace + shortFieldName.substring(seperatorIndex + 1);
            } else {
                log.error("Unknown prefix {} used by Field {}",prefix,shortFieldName);
                log.error("known prefixes: {}",getPrefixMap());
                throw new IllegalStateException("Unknown prefix " + prefix + " (parsed from field "
                                                + shortFieldName + ")!");
            }
        } else {
            return shortFieldName;
        }
    }

    protected final String getNamespace(String prefix) {
        if (prefix.equals("urn")) {
            // than the parsed URI is something like "urn:my.test.uuid-123"
            // -> this is no real prefix, but an urn with only one ':'
            // we need to return "urn:" as namespace!
            return "urn:";
        } else { // else we have an real namespace -> use the current mappings!
            return getPrefixMap().get(prefix);
        }
    }

    protected final String addNamespace(String namespace) {
        return getPrefix(namespace, true);
    }

    protected final String getPrefix(String namespace) {
        return getPrefix(namespace, false);
    }

    protected final String getPrefix(String namespace, boolean create) {
        if (namespace == null) {
            return null;
        }
        Map<String,String> namespaceMap = getNamespaceMap();
        String prefix = namespaceMap.get(namespace);
        if (prefix != null) {
            return prefix;
        } else if (create) { // only if not present and prefix is true
            prefixNamespaceMappingsLock.writeLock().lock();
            try {
                //try again to get the prefix ... there might be a concurrent change
                prefix = getNamespaceMap().get(namespace);
                if(prefix != null){ //added by an other thread
                    return prefix; //nothing else to do
                }
                Map<String,String> prefixMap = getPrefixMap();
                String defaultprefix;
                if(nsPrefixService != null){
                    defaultprefix = nsPrefixService.getPrefix(namespace);
                } else {
                    NamespaceEnum defaultMapping = NamespaceEnum.forNamespace(namespace);
                    defaultprefix = defaultMapping != null ? defaultMapping.getPrefix() : null;
                }
                /*
                 * NOTE: we need to check here also if the default prefix is not yet taken, because the Solr
                 * Index used to store the prefixes might be older than the latest change within the
                 * NamespaceEnum. Therefore there might be cases where a default prefix configured by this
                 * Enum is already assigned to a different namespace within the Solr index! In such cases, we
                 * need to create a new prefix for this namespace
                 */
                if (defaultprefix != null && !prefixMap.containsKey(defaultprefix)) {
                    prefix = defaultprefix;
                } else { // need to generate a default mapping
                    prefix = createPrefix(prefixMap);
                }
                //add an namespace
                log.debug("add namespace prefix '{}' for '{}'",prefix,namespace);
                prefixMap.put(prefix, namespace);
                namespaceMap.put(namespace, prefix);
                // save the configuration and parse true to make  sure the 
                //namespaces are committed to the Solr Server
                saveNamespaceConfig(true); 
            } finally {
                prefixNamespaceMappingsLock.writeLock().unlock();
            }
        }
        return prefix; // may return null if !create
    }

    private String createPrefix(Map<String,String> prefixMap) {
        String defaultPrefix;
        do { // as long an prefix is not any of the default prefixes or one of the prefixes defined by
             // NamespaceEnum
            defaultNsPrefixNumber++;
            defaultPrefix = DEFAULT_NS_PREFIX_STRING + defaultNsPrefixNumber;
        } while (prefixMap.containsKey(defaultPrefix) || 
                NamespaceEnum.forPrefix(defaultPrefix) != null ||
                (nsPrefixService != null && nsPrefixService.getNamespace(defaultPrefix) != null));
        return defaultPrefix;
    }

    /**
     * Leads the prefix to namespace mappings from the configured Solr server and inits the two mapps holding
     * the prefix &lt;-&gt; namespace mappings.<p>
     * Needs to be called under a write lock on {@link #prefixNamespaceMappingsLock}
     */
    private void loadNamespaceConfig() {
        log.debug("loadNamespaceConfig for {}",server);
        if(__prefixMap != null || __namespaceMap != null){
            log.warn("LoadNamespaceConfig called while mapping maps are NOT NULL!");
        }
        __prefixMap = new HashMap<String,String>();
        __namespaceMap = new HashMap<String,String>();
        SolrDocument config = null;
        try {
            config = getSolrDocument(FieldMapper.URI);
        } catch (IOException e) {
            log.error("Unable to load PathField Config from Index. (may be OK for the first run!)");
        } catch (SolrServerException e) {
            log.error("Unable to load PathField Config from Index. (may be OK for the first run!)");
        }
        if (config == null) {
            log.info("No PathFieldMapping Configuration present. Start with an empty mapping");
        } else {
            for (String fieldName : config.getFieldNames()) {
                String[] configFieldElements = fieldName.split(Character.toString(SolrConst.PATH_SEPERATOR));
                if (SPECIAL_CONFIG_FIELD.equals(configFieldElements[0])) {
                    if (SPECIAL_CONFIG_FIELD.length() > 1) {
                        String prefix = configFieldElements[1];
                        Object value = config.getFirstValue(fieldName);
                        if (value != null) {
                            if (__namespaceMap.containsKey(value.toString())) {
                                log.error("found two prefixes (" + __namespaceMap.get(value.toString())
                                          + " and " + prefix + ") for Namespace " + value.toString()
                                          + " keep the first one");
                            } else {
                                log.debug(" > prefix: " + prefix + " value: " + value);
                                __prefixMap.put(prefix, value.toString());
                                __namespaceMap.put(value.toString(), prefix);
                                // check for default NS
                                if (prefix.startsWith(DEFAULT_NS_PREFIX_STRING)) {
                                    String prefixNumber = prefix.substring(DEFAULT_NS_PREFIX_STRING.length());
                                    try {
                                        int num = Integer.parseInt(prefixNumber);
                                        if (num > defaultNsPrefixNumber) {
                                            defaultNsPrefixNumber = num;
                                        }
                                    } catch (NumberFormatException e) {
                                        log.warn("Unable to parse Integer for Number part of default prefix "
                                                 + prefix
                                                 + " (this is OK if by accident an other Namespace prefix starts with '"
                                                 + DEFAULT_NS_PREFIX_STRING + "')");
                                    }
                                }
                            }
                        } else {
                            log.warn("No value for prefix " + prefix
                                     + " found in the Configuration (Field Name: " + fieldName + ")");
                        }
                    } else {
                        log.warn("encountered wrong Formatted Config field " + fieldName);
                    }
                }
            }
        }
    }

    private String getConfigFieldName(String configName) {
        return SPECIAL_CONFIG_FIELD + PATH_SEPERATOR + configName;
    }

    /**
     * Saves the current configuration to the index! This does NOT commit the
     * changes!
     */
    public void saveNamespaceConfig(final boolean commit) {
        if(server != null){
            prefixNamespaceMappingsLock.writeLock().lock();
            try {
                log.debug("saveNamespaceConfig on {}",server);
                Map<String,String> prefixMap = getPrefixMap();
                final SolrInputDocument inputDoc = new SolrInputDocument();
                inputDoc.addField(getDocumentIdField(), FieldMapper.URI);
                for (Entry<String,String> entry : prefixMap.entrySet()) {
                    log.debug("  > {}: {}",entry.getKey(),entry.getValue());
                    inputDoc.addField(getConfigFieldName(entry.getKey()), entry.getValue());
                }
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        public Object run() throws IOException, SolrServerException {
                                server.add(inputDoc);
                                if(commit){
                                    server.commit();
                                }
                                return null;
                        }
                    });
                } catch (PrivilegedActionException pae) {
                    log.error("Unable save Configuration to SolrProvider", pae.getException());
                }
            } finally {
                prefixNamespaceMappingsLock.writeLock().unlock();
            }
        } else {
            log.warn("Unable to save NamespaceCondig because no SolrServer is set");
        }
    }

    /**
     * Getter for a SolrDocument based on the ID. Used to load the config from the index.
     * 
     * @param inputDoc
     *            the document to store
     */
    protected SolrDocument getSolrDocument(String uri) throws SolrServerException, IOException {
        if(server == null){
            return null;
        }
        final SolrQuery solrQuery = new SolrQuery();
        solrQuery.addField("*"); // select all fields
        solrQuery.setRows(1); // we query for the id, there is only one result
        String queryString = String.format("%s:%s", this.getDocumentIdField(),
            SolrUtil.escapeSolrSpecialChars(uri));
        solrQuery.setQuery(queryString);
        QueryResponse queryResponse;
        try {
            queryResponse = AccessController.doPrivileged(new PrivilegedExceptionAction<QueryResponse>() {
                public QueryResponse run() throws IOException, SolrServerException {
                    return server.query(solrQuery);
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if(e instanceof SolrServerException){
                throw (SolrServerException)e;
            } else if(e instanceof IOException){
                throw (IOException)e;
            } else {
                throw RuntimeException.class.cast(e);
            }
        }
        if (queryResponse.getResults().isEmpty()) {
            return null;
        } else {
            return queryResponse.getResults().get(0);
        }
    }

    @Override
    public String getDocumentDomainField() {
        return DOMAIN_FIELD;
    }
}
