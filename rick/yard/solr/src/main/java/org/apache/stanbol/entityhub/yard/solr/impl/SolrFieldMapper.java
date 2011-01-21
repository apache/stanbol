package org.apache.stanbol.entityhub.yard.solr.impl;

import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.DEPENDENT_DOCUMENT_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.DOCUMENT_ID_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.DOMAIN_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.LANG_MERGER_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.PATH_SEPERATOR;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.REFERRED_DOCUMENT_FIELD;
import static org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst.SPECIAL_CONFIG_FIELD;

import java.io.IOException;
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

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.stanbol.entityhub.core.utils.ModelUtils;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.yard.solr.defaults.IndexDataTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.defaults.SolrConst;
import org.apache.stanbol.entityhub.yard.solr.model.FieldMapper;
import org.apache.stanbol.entityhub.yard.solr.model.IndexDataType;
import org.apache.stanbol.entityhub.yard.solr.model.IndexField;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;
import org.apache.stanbol.entityhub.yard.solr.utils.SolrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the FieldMapper for a Solr Index.
 * @author Rupert Westenthaler
 *
 */
public class SolrFieldMapper implements FieldMapper {

    Logger log = LoggerFactory.getLogger(SolrFieldMapper.class);
    /**
     * Char used to separate the prefix from the local name of uri's
     */
    private static final char NAMESPACE_PREFIX_SEPERATOR_CHAR = ':';
    private static final String LANG_MERGER_PREFIX = ""+SolrConst.SPECIAL_FIELD_PREFIX+SolrConst.MERGER_INDICATOR+SolrConst.LANG_INDICATOR;
    /**
     * The size of the LRU cache for FieldName to IndexField as well as
     * IndexField to collection of FieldNames mappings.<p>
     * Note that both caches may have a maximum of elements as configured by this
     * property.
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
     * Internally used as LRU Cache with {@link SolrFieldMapper#LRU_MAPPINGS_CACHE_SIZE}
     * elements. This subclass of {@link LinkedHashMap} overrides the
     * {@link LinkedHashMap#removeEldestEntry(Entry)} as suggested by the java
     * doc. It also uses the constructor that activates the ordering based on
     * access time rather tan insertion time.
     *
     * @author Rupert Westenthaler
     *
     * @param <K> generic type of the key
     * @param <V> generic type of the value
     */
    private static final class LRU<K,V> extends LinkedHashMap<K, V>{
        public LRU() {
            super(16,0.75f,true); //access order!
        }
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
            return size() > LRU_MAPPINGS_CACHE_SIZE;
        }
    }
    /**
     * The assumption is, that only a handful of {@link IndexField}s are used
     * very often.<p>
     * So it makes sense to keep some mappings within a cache rather than calculating
     * them again and again.
     * @see LinkedHashMap#
     */
    private final LinkedHashMap<IndexField, Collection<String>> indexFieldMappings =
        new LRU<IndexField, Collection<String>>();
    /**
     * The assumption is, that only a handful of fields appear in index documents.
     * So it makes sense to keep some mappings within a cache rather than calculating
     * them again and again.
     */
    private final LinkedHashMap<String, IndexField> fieldMappings =
        new LRU<String, IndexField>();

    protected final SolrServer server;
    public SolrFieldMapper(SolrServer server){
        if(server == null){
            throw new IllegalArgumentException("The parsed SolrServer MUST NOT be NULL");
        }
        this.server = server;
    }
    @Override
    public IndexField getField(String fieldName) {
        if(fieldName == null || fieldName.isEmpty()){
            throw new IllegalArgumentException("The parsed field name MUST NOT be NULL!");
        }
        IndexField field = fieldMappings.get(fieldName);
        if(field == null){
            if(getDocumentIdField().equals(fieldName) ||
                    fieldName.charAt(0) == SolrConst.SPECIAL_FIELD_PREFIX){
                //in case of special field or the document ID, return null ->
                //   meaning, that this index document field does not represent
                //   an logical IndexField and should be ignored
                return null;
            } else if (SolrConst.SCORE_FIELD.equals(fieldName)){
                return scoreField;
            }
            //parse the prefix and suffix
            String[] tokens = fieldName.split(Character.toString(SolrConst.PATH_SEPERATOR));
            int numTokens = tokens.length;
            int pathElements = numTokens;
            String prefix = null;
            String suffix = null;
            if (tokens.length >= 2){
                prefix = tokens[0];
                pathElements--;
            }
            if(tokens.length >= 3){
                suffix = tokens[numTokens-1].substring(1);
                pathElements--;
            }

            //parse the path
            String[] path = new String[pathElements];
            System.arraycopy(tokens, prefix==null?0:1, path, 0, pathElements);
            tokens = null;
            //process the parsed data
            field = parseIndexField(prefix,suffix,path);
            if(field != null){
                fieldMappings.put(fieldName, field);
            }
        }
        return field;
    }
    /**
     * This method does the dirty work of parsing the different parts of the
     * field in the SolrDocument to the logical field as used by the semantic
     * indexing API.
     * This method assumes the following encoding
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
     * @param prefix
     * @param suffix
     * @param pathElements
     * @return
     */
    private IndexField parseIndexField(final String prefix, final String suffix, final String[] pathElements) {
        final String language;
        boolean isLanguage = false;
        final String dataTypePrefix;
        //first use the prefix to parse the language
        // -> note that the prefix might also be used for the data type!
        if(prefix != null && !prefix.isEmpty()){
            if(prefix.charAt(0) == SolrConst.LANG_INDICATOR){
                isLanguage = true;
                //it is a language prefix!
                //set dataTypePrefix to null
                dataTypePrefix = null;
                if(prefix.length()>1){
                    language = prefix.substring(1);
                } else { //it is a language prefix, but for the default language!
                    language = null;
                }
            } else { //it is no language prefix
                language = null;
                isLanguage = false;
                dataTypePrefix = prefix;
            }
        } else { //no prefix at all
            //set no-language and the dataType prefix to null;
            isLanguage = false;
            language = null;
            dataTypePrefix = null;
        }
        //now parse the indexDataType!
        IndexDataTypeEnum dataTypeEnumEntry = IndexDataTypeEnum.forPrefixSuffix(dataTypePrefix, suffix);
        if(dataTypeEnumEntry == null){
            log.warn(String.format("No IndexDataType registered for prefix: %s and suffix: %s -> unable to process path %s",
                    dataTypePrefix,suffix,Arrays.toString(pathElements)));
            return null; // we might also throw an exception at this point
        }
        //parse the path
        List<String> path = new ArrayList<String>(pathElements.length);
        for(String pathElement : pathElements){
            if(pathElement.charAt(0) == SolrConst.SPECIAL_FIELD_PREFIX){
                if(pathElement.charAt(1)== SolrConst.SPECIAL_FIELD_PREFIX){
                    path.add(getFullFieldName(pathElement.substring(1)));
                } else {
                    throw new IllegalStateException(String.format("Found special field \"%s\" within the path \"%s\" -> Special fields are only allowed as prefix and suffix!",
                            pathElement,Arrays.toString(pathElements)));
                }
            } else {
                String fullName = getFullFieldName(pathElement);
                if(fullName == null){
                    throw new IllegalStateException(String.format("Unable to map PathElement %s to it's full Name (path=%s)!",pathElement,Arrays.toString(pathElements)));
                } else {
                    path.add(fullName);
                }
            }
        }
        if(isLanguage){
            return new IndexField(path, dataTypeEnumEntry.getIndexType(), language);
        } else {
            return new IndexField(path, dataTypeEnumEntry.getIndexType());
        }
    }

    @Override
    public Collection<String> getFieldNames(List<String> path, IndexValue indexValue) throws IllegalArgumentException {
        IndexField field;
        if(indexValue.hasLanguage()){
            field = new IndexField(path, indexValue.getType(),indexValue.getLanguage());
        } else {
            field = new IndexField(path, indexValue.getType());
        }
        return getFieldNames(field);
    }

    @Override
    public Collection<String> getFieldNames(IndexField indexField) throws IllegalArgumentException {
        if(indexField == null){
            throw new IllegalArgumentException("The parsed IndexField name MUST NOT be NULL!");
        }
        Collection<String> fieldNames = indexFieldMappings.get(indexField);
        if(fieldNames == null){
            fieldNames = new HashSet<String>();
            //Three things need to be done
            //1) Replace the path with the prefix:localName
            StringBuilder pathName = new StringBuilder();
            encodePathName(pathName, indexField.getPath());
            //2) add the prefix and/or suffix for the IndexType
            encodeDataType(pathName,indexField.getDataType());
            //3) add the prefix for the languages
            if(indexField.hasLanguage()){
                fieldNames = encodeLanguages(pathName.toString(),indexField.getLanguages());
            } else {
                fieldNames = Collections.singleton(pathName.toString());
            }
            //cache the mappings
            indexFieldMappings.put(indexField, fieldNames);
        }
        return fieldNames;
    }
    /**
     * Getter for the string used to index a the parsed path. This method
     * replaces the URI's of all elements within the path with
     * <code>prefix+NAMESPACE_PREFIX_SEPERATOR_CHAR+localName</code>. In addition
     * it places the <code>PATH_SEPERATOR</code> char between the elements.<p>
     * NOTE: This Method assumes that both Parameters are not NULL and that
     * the Path is not empty and contains no NULL nor emtpy element!
     * @param pathName the StringBuilder used to add the path
     * @param path the path to encode
     */
    private void encodePathName(StringBuilder pathName,List<String> path){
        //Now Iterate over the Path
        pathName.append(PATH_SEPERATOR); //add the leading PathSeperator
        Iterator<String> fields = path.iterator();
        while(fields.hasNext()){
            String field = fields.next();
            //PathElement element = it.next();
            String [] namespaceLocalName = ModelUtils.getNamespaceLocalName(field);
            //QName qName = getQName(field);
            if(namespaceLocalName[0]!=null && !namespaceLocalName[0].isEmpty()){
                pathName.append(getPrefix(namespaceLocalName[0], true));
                //second the local name
                pathName.append(NAMESPACE_PREFIX_SEPERATOR_CHAR);
            }
            pathName.append(namespaceLocalName[1]);
            //third add Path Separator if there are additional Elements
            if(fields.hasNext()){
                pathName.append(PATH_SEPERATOR);
            }
        }
        pathName.append(PATH_SEPERATOR); //add the tailing PathSeperator
    }
    @Override
    public String encodePath(List<String> path) throws IllegalArgumentException {
        IndexField.validatePath(path);
        StringBuilder sb = new StringBuilder();
        encodePathName(sb, path);
        return sb.toString();
    }
    /**
     * Encodes the prefix and/or Suffix that indicates the data type.<p>
     * NOTE: This Method assumes that both parameters are not NULL.<p>
     * TODO: Currently such mappings are "hard coded" within the
     * {@link IndexDataTypeEnum}. It would be also possible to store such
     * mappings within the Solr index. However this is currently not implemented
     * because the Solr Server needs also to recognise such prefixes and suffixes
     * - meaning they need to be configured in the SchemaXML used by the Solr
     * Server. If there is a possibility to modify this configuration
     * programmatically than adding new dataTypes should be exposed via the
     * configuration tab of the OSGI Web Console!
     * @param pathName the StringBuilder to add the prefix and the suffix. This
     * method assumes, that the encoded path is already contained in the parsed
     * StringBuilder.
     * @param dataType the dataType to encode.
     */
    private void encodeDataType(StringBuilder pathName,IndexDataType dataType){
        IndexDataTypeEnum dataTypeConfig = IndexDataTypeEnum.forIndexType(dataType);
        if(dataTypeConfig == null){
            throw new IllegalStateException(String.format("No Config found for the parsed IndexDataType %s",dataType));
        }
        String[] prefixSuffix = encodeDataType(dataType);
        if(prefixSuffix[0] != null){
            pathName.insert(0,prefixSuffix[0]);
        }
        if(prefixSuffix[1] != null){
            pathName.append(prefixSuffix[1]);
        }
    }
    @Override
    public String[] encodeDataType(IndexDataType dataType) throws IllegalArgumentException {
        IndexDataTypeEnum dataTypeConfig = IndexDataTypeEnum.forIndexType(dataType);
        if(dataTypeConfig == null){
            throw new IllegalStateException(String.format("No Config found for the parsed IndexDataType %s",dataType));
        }
        String[] prefixSuffix = new String[] {null,null};
        if(dataTypeConfig.getPrefix() != null && !dataTypeConfig.getPrefix().isEmpty()){
            prefixSuffix[0] = dataTypeConfig.getPrefix();
        }
        if(dataTypeConfig.getSuffix() != null && !dataTypeConfig.getSuffix().isEmpty()){
            prefixSuffix[1] = dataTypeConfig.getSuffix();
        }
        return prefixSuffix;
    }
    /**
     * Encodes the prefixes for the parsed languages and returns the according
     * field names for the languages.<p>
     * Languages are encodes using the {@link SolrConst#LANG_INDICATOR} and the
     * parsed language as field prefix.<p>
     * Note the on the server there is typically a copy field configuration that
     * adds all fields that start with the {@link SolrConst#LANG_INDICATOR} and
     * fields of the {@link IndexDataTypeEnum#STR} to a field with the prefix
     * {@link SolrConst#LANG_INDICATOR}{@link SolrConst#MERGER_INDICATOR}.
     * This field can be used by queries to search for strings in any language!
     * @param fieldName the string representing the field without encoded languages
     * @param languages the languages.
     * @return
     */
    private Collection<String> encodeLanguages(String fieldName, Collection<String> languages) {
        if(languages == null || languages.isEmpty()){ //no language
            return Collections.singleton(fieldName);//just return the field
        } else {
            //I assume that this will be the case in most of the calls
            Collection<String> fieldNames = new ArrayList<String>(languages.size()*2);
            for(String prefix : encodeLanguages(languages)){
                fieldNames.add(prefix+fieldName);
            }
            return fieldNames;
        }
    }
    /**
     * Internally used instead of {@link #encodeLanguages(String...)}
     * @param languages the languages
     * @return the prefixes
     * @see FieldMapper#encodeLanguages(String...)
     */
    public Collection<String> encodeLanguages(Collection<String> languages) {
        if(languages == null || languages.isEmpty()){ //no language
            return Collections.emptySet();//just return the field
        } else if (languages.size()==1){
            return encodeLanguage(languages.iterator().next());
        } else {
            Set<String> langPrefixes = new HashSet<String>();
            for(String lang : languages){
                langPrefixes.addAll(encodeLanguage(lang));
            }
            return langPrefixes;
        }
    }
    @Override
    public String getLanguageMergerField(String lang) {
        return LANG_MERGER_PREFIX+(lang!=null?lang:"");
    }
    /**
     * Encodes the language prefixes of for the parsed language
     * @param lang the language
     * @return the field with the encoded language
     */
    private Collection<String> encodeLanguage(String lang){
        StringBuilder langField = new StringBuilder();
        langField.append(SolrConst.LANG_INDICATOR);
        if(lang != null){
            langField.append(lang);
        }
        return Arrays.asList(langField.toString(),LANG_MERGER_FIELD);
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

    /*--------------------------------------------------------------------------
     * Helper Methods to store/read the Mapping Config within the index.
     * TODO: Move this to an own class e.g. IndexConfig or something like that
     * -------------------------------------------------------------------------
     */
    private int defaultNsPrefixNumber = 1;
    private static final String DEFAULT_NS_PREFIX_STRING = "ns";
    //private static final char NAMESPACE_PREFIX_SEPERATOR_CHAR = ':';
    /**
     * Do never access this Map directly! Use {@link #getNamespaceMap()}!
     */
    private Map<String,String> __namespaceMap = null;
    /**
     * Getter for the namespace to prefix mapping
     * @return the map holding the namespace to prefix mappings
     */
    private Map<String, String> getNamespaceMap(){
        if(__namespaceMap == null){
            loadNamespaceConfig();
        }
        return __namespaceMap;
    }
    /**
     * Do never access this Map directly! Use {@link #getPrefixMap()}!
     */
    private Map<String,String> __prefixMap = null;
    /**
     * Getter for the prefix to namespace mappings
     * @return the map holding the prefix to namespace mappings
     */
    private Map<String,String> getPrefixMap(){
        if(__prefixMap == null){
            loadNamespaceConfig();
        }
        return __prefixMap;
    }
    /**
     * Getter for the full name based on the short name. The short name is defined
     * as the prefix followed by the {@link #NAMESPACE_PREFIX_SEPERATOR_CHAR} and
     * the local name of the field. The returned field name is defined as the
     * namespace followed by the local name.<p>
     * If the parsed short field name does not contain the
     * {@link #NAMESPACE_PREFIX_SEPERATOR_CHAR} this method returns the parsed
     * String.<p>
     * The local name may contain the {@link #NAMESPACE_PREFIX_SEPERATOR_CHAR}
     * {@link #NAMESPACE_PREFIX_SEPERATOR_CHAR}'. The prefix MUST NOT contain
     * this char, because {@link String#indexOf(int)} is used to split prefix
     * and local name.
     * @param shortFieldName the short name
     * @return the full name
     * @throws IllegalArgumentException if <code>null</code> is parsed as shortFieldName
     * @throws IllegalStateException if the found prefix is not contained in the configuration
     */
    protected final String getFullFieldName(String shortFieldName) throws IllegalArgumentException, IllegalStateException {
        if(shortFieldName == null){
            throw new IllegalArgumentException("Parameter shortFieldName MUST NOT be NULL");
        }
        int seperatorIndex = shortFieldName.indexOf(NAMESPACE_PREFIX_SEPERATOR_CHAR);
        if(seperatorIndex >= 0){
            String prefix = shortFieldName.substring(0,seperatorIndex); //seperatorIndex does not include the separator char
            String namespace = getNamespace(prefix);
            if(namespace != null){
                return namespace+shortFieldName.substring(seperatorIndex+1);
            } else {
                throw new IllegalStateException("Unknown prefix "+prefix+" (parsed from field "+shortFieldName+")!");
            }
        } else {
            return shortFieldName;
        }
    }
    protected final String getNamespace(String prefix){
        if(prefix.equals("urn")){
            //than the parsed URI is something like "urn:my.test.uuid-123"
            // -> this is no real prefix, but an urn with only one ':'
            //    we need to return "urn:" as namespace!
            return "urn:";
        } else { //else we have an real namespace -> use the current mappings!
            return getPrefixMap().get(prefix);
        }
    }
    protected final String addNamespace(String namespace){
        return getPrefix(namespace, true);
    }
    protected final String getPrefix(String namespace){
        return getPrefix(namespace, false);
    }
    protected final String getPrefix(String namespace, boolean create){
        if(namespace == null){
            return null;
        }
        Map<String,String> prefixMap = getPrefixMap();
        String prefix = getNamespaceMap().get(namespace);
        if(prefix != null){
            return prefix;
        } else if(create){ //only if not present and prefix is true
            NamespaceEnum defaultMapping = NamespaceEnum.forNamespace(namespace);
            if(defaultMapping != null && !prefixMap.containsKey(defaultMapping.getPrefix())){
                /*
                 * NOTE: we need to check here also if the default prefix is not
                 * yet taken, because the Solr Index used to store the prefixes
                 * might be older than the latest change within the NamespaceEnum.
                 * Therefore there might be cases where a default prefix configured
                 * by this Enum is already assigned to a different namespace within
                 * the Solr index!
                 * In such cases, we need to create a new prefix for this namespace
                 */
                prefix = defaultMapping.getPrefix();
            } else {
                //need to generate a default mapping
                prefix =  createPrefix(prefixMap);
            }
            addNamespaceMapping(prefix, namespace); //we need to add the new mapping
            saveNamespaceConfig(); //save the configuration
            // (TODO: we do not make a flush here ... so maybe we need to ensure that a flush is called sometimes)
        }
        return prefix; //may return null if !create
    }
    private String createPrefix(Map<String,String> prefixMap){
        String defaultPrefix;
        do { //as long an prefix is not any of the default prefixes or one of the prefixes defined by NamespaceEnum
            defaultNsPrefixNumber++;
            defaultPrefix = DEFAULT_NS_PREFIX_STRING+defaultNsPrefixNumber;
        } while(prefixMap.containsKey(defaultPrefix) || NamespaceEnum.forPrefix(defaultPrefix) != null);
        return defaultPrefix;
    }
    private void addNamespaceMapping(String prefix, String namespace){
        getPrefixMap().put(prefix, namespace);
        getNamespaceMap().put(namespace, prefix);
    }
    /**
     * Leads the prefix to namespace mappings from the configured Solr server
     * and inits the two mapps holding the prefix &lt;-&gt; namespace mappings
     */
    private void loadNamespaceConfig() {
        __prefixMap = new HashMap<String, String>();
        __namespaceMap = new HashMap<String, String>();
        SolrDocument config = null;
        try {
            config = getSolrDocument(FieldMapper.URI);
        } catch (IOException e) {
            log.error("Unable to load PathField Config from Index. (may be OK for the first run!)");
        } catch (SolrServerException e) {
            log.error("Unable to load PathField Config from Index. (may be OK for the first run!)");
        }
        if(config == null){
            log.info("No PathFieldMapping Configuration present. Start with an empty mapping");
        } else {
            for(String fieldName : config.getFieldNames()){
                String[] configFieldElements = fieldName.split(Character.toString(SolrConst.PATH_SEPERATOR));
                if(SPECIAL_CONFIG_FIELD.equals(configFieldElements[0])){
                    if(SPECIAL_CONFIG_FIELD.length() > 1){
                        String prefix = configFieldElements[1];
                        Object value = config.getFieldValue(fieldName);
                        if(value != null){
                            if(__namespaceMap.containsKey(value.toString())){
                                log.error("found two prefixes ("+__namespaceMap.get(value.toString())+" and "+prefix+") for Namespace "+ value.toString()+" keep the first one");
                            } else {
                                log.debug(" > prefix: "+prefix+" value: "+value);
                                __prefixMap.put(prefix, value.toString());
                                __namespaceMap.put(value.toString(), prefix);
                                //check for default NS
                                if(prefix.startsWith(DEFAULT_NS_PREFIX_STRING)){
                                    String prefixNumber = prefix.substring(DEFAULT_NS_PREFIX_STRING.length());
                                    try{
                                        int num = Integer.parseInt(prefixNumber);
                                        if(num>defaultNsPrefixNumber){
                                            defaultNsPrefixNumber = num;
                                        }
                                    } catch (NumberFormatException e) {
                                        log.warn("Unable to parse Integer for Number part of default prefix "+prefix+" (this is OK if by accident an other Namespace prefix starts with '"+DEFAULT_NS_PREFIX_STRING+"')");
                                    }
                                }
                            }
                        } else {
                            log.warn("No value for prefix "+prefix+" found in the Configuration (Field Name: "+fieldName+")");
                        }
                    } else {
                        log.warn("encountered wrong Formatted Config field "+fieldName);
                    }
                }
            }
        }
    }
    private String getConfigFieldName(String configName){
        return SPECIAL_CONFIG_FIELD+PATH_SEPERATOR+configName;
    }
    /**
     * Saves the current configuration to the index!
     */
    private void saveNamespaceConfig(){
        Map<String,String> prefixMap = getPrefixMap();
        SolrInputDocument inputDoc = new SolrInputDocument();
        inputDoc.addField(getDocumentIdField(), FieldMapper.URI);
        for(Entry<String, String> entry : prefixMap.entrySet()){
            inputDoc.addField(getConfigFieldName(entry.getKey()), entry.getValue());
        }
        try {
            server.add(inputDoc);
        } catch (IOException e) {
            log.error("Unable save Configuration to SolrProvider",e);
        } catch (SolrServerException e) {
            log.error("Unable save Configuration to SolrProvider",e);
        } catch (SolrException e){
            log.error("Unable save Configuration to SolrProvider",e);
        }
    }
    /**
     * Getter for a SolrDocument based on the ID. Used to load the config from
     * the index.
     * @param inputDoc the document to store
     */
    protected SolrDocument getSolrDocument(String uri) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.addField("*"); //select all fields
        solrQuery.setRows(1); //we query for the id, there is only one result
        String queryString = String.format("%s:%s",
                this.getDocumentIdField(),SolrUtil.escapeSolrSpecialChars(uri));
        solrQuery.setQuery(queryString);
        QueryResponse queryResponse = server.query(solrQuery);
        if(queryResponse.getResults().isEmpty()){
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
