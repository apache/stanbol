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

package org.apache.stanbol.contenthub.servicesapi.store.vocabulary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Vocabulary class which provides constant properties to be used in the communication with Solr. Most of
 * these properties point to the fields defined in the <b>schema.xml</b> of Solr.
 * 
 * @author anil.sinaci
 * 
 */
public class SolrVocabulary {

    public static final String STANBOLRESERVED_PREFIX = "stanbolreserved_";

    public enum SolrFieldName {

        /**
         * Name of the field which holds entities which carry the dbpedia Place property.
         */
        PLACES("places" + SOLR_DYNAMIC_FIELD_TEXT),

        /**
         * Name of the field which holds entities which carry the dbpedia Person property.
         */
        PEOPLE("people" + SOLR_DYNAMIC_FIELD_TEXT),

        /**
         * Name of the field which holds entities which carry the dbpedia Organization property.
         */
        ORGANIZATIONS("organizations" + SOLR_DYNAMIC_FIELD_TEXT),

        /**
         * Name of the unique ID field.
         */
        ID(STANBOLRESERVED_PREFIX + "id"),

        /**
         * Name of the field which holds the title of the content.
         */
        TITLE(STANBOLRESERVED_PREFIX + "title"),

        /**
         * Name of the field which holds the actual content.
         */
        CONTENT(STANBOLRESERVED_PREFIX + "content"),

        /**
         * Name of the field which holds the binary content
         */
        BINARYCONTENT(STANBOLRESERVED_PREFIX + "binarycontent"),

        /**
         * Name of the field which holds the mime type (content type) of the content.
         */
        MIMETYPE(STANBOLRESERVED_PREFIX + "mimetype"),

        /**
         * Name of the field which holds the creation date of the content.
         */
        CREATIONDATE(STANBOLRESERVED_PREFIX + "creationdate"),

        /**
         * Name of the field which holds the number of enhancements for the content.
         */
        ENHANCEMENTCOUNT(STANBOLRESERVED_PREFIX + "enhancementcount"),

        /**
         * Name of the field which holds the countries of the cities mentioned in the content. This field is
         * populated by the semantic operations through the enhancements of the content.
         */
        COUNTRIES(STANBOLRESERVED_PREFIX + "countries"),

        /**
         * Name of the field which holds the image captions of the entities mentioned in the content. This
         * field is populated by the semantic operations through the enhancements of the content.
         */
        IMAGECAPTIONS(STANBOLRESERVED_PREFIX + "imagecaptions"),

        /**
         * Name of the field which holds the geographic regions of the cities mentioned in the content. This
         * field is populated by the semantic operations through the enhancements of the content.
         */
        REGIONS(STANBOLRESERVED_PREFIX + "regions"),

        /**
         * Name of the field which holds the governors of the provinces mentioned in the content. This field
         * is populated by the semantic operations through the enhancements of the content.
         */
        GOVERNORS(STANBOLRESERVED_PREFIX + "governors"),

        /**
         * Name of the field which holds the capital cities of the countries mentioned in the content. This
         * field is populated by the semantic operations through the enhancements of the content.
         */
        CAPITALS(STANBOLRESERVED_PREFIX + "capitals"),

        /**
         * Name of the field which holds the largest cities of the entities mentioned in the content. This
         * field is populated by the semantic operations through the enhancements of the content.
         */
        LARGESTCITIES(STANBOLRESERVED_PREFIX + "largestcities"),

        /**
         * Name of the field which holds the names of the leaders of the countries mentioned in the content.
         * This field is populated by the semantic operations through the enhancements of the content.
         */
        LEADERNAMES(STANBOLRESERVED_PREFIX + "leadernames"),

        /**
         * Name of the field which holds the given names of the persons mentioned in the content. This field
         * is populated by the semantic operations through the enhancements of the content.
         */
        GIVENNAMES(STANBOLRESERVED_PREFIX + "givennames"),

        /**
         * Name of the field which holds the important events which are known/invented through the persons
         * mentioned in the content. This field is populated by the semantic operations through the
         * enhancements of the content.
         */
        KNOWNFORS(STANBOLRESERVED_PREFIX + "knownfors"),

        /**
         * Name of the field which holds the birthplaces/placeofbirths of the persons mentioned in the
         * content. This field is populated by the semantic operations through the enhancements of the
         * content.
         */
        BIRTHPLACES(STANBOLRESERVED_PREFIX + "birthplaces"),
        PLACEOFBIRTHS(STANBOLRESERVED_PREFIX + "placeofbirths"),

        /**
         * Name of the field which holds the names of the institutions at which the persons mentioned in the
         * content worked. This field is populated by the semantic operations through the enhancements of the
         * content.
         */
        WORKINSTITUTIONS(STANBOLRESERVED_PREFIX + "workinstitutions"),

        /**
         * Name of the field which holds the captions of the images belonging to the persons mentioned in the
         * content. This field is populated by the semantic operations through the enhancements of the
         * content.
         */
        CAPTIONS(STANBOLRESERVED_PREFIX + "captions"),

        /**
         * Name of the field which holds the short descriptions of the persons mentioned in the content. This
         * field is populated by the semantic operations through the enhancements of the content.
         */
        SHORTDESCRIPTIONS(STANBOLRESERVED_PREFIX + "shortdescriptions"),

        /**
         * Name of the field which holds the fields on which the persons mentioned in the content studied.
         * This field is populated by the semantic operations through the enhancements of the content.
         */
        FIELDS(STANBOLRESERVED_PREFIX + "fields");

        private final String name;

        private SolrFieldName(String n) {
            this.name = n;
        }

        @Override
        public final String toString() {
            return this.name;
        }

        public static SolrFieldName[] getSemanticFieldNames() {
            SolrFieldName[] semanticFieldNames = {COUNTRIES, IMAGECAPTIONS, REGIONS, GOVERNORS, CAPITALS,
                                                  LARGESTCITIES, LEADERNAMES, GIVENNAMES, KNOWNFORS,
                                                  BIRTHPLACES, PLACEOFBIRTHS, WORKINSTITUTIONS, CAPTIONS,
                                                  SHORTDESCRIPTIONS, FIELDS};
            return semanticFieldNames;
        }

        public static SolrFieldName[] getAnnotatedEntityFieldNames() {
            SolrFieldName[] annotatedEntityFieldNames = {PLACES, PEOPLE, ORGANIZATIONS};
            return annotatedEntityFieldNames;
        }

        public static boolean isNameReserved(String name) {
            return name.startsWith(STANBOLRESERVED_PREFIX);
        }

        public static boolean isAnnotatedEntityFacet(String facetName) {
            for (SolrFieldName sfn : getAnnotatedEntityFieldNames()) {
                if (sfn.toString().equals(facetName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Ending characters for dynamic fields of {@link String} type.
     * The type of this field is "string" in the Solr schema.
     */
    public static final String SOLR_DYNAMIC_FIELD_TEXT = "_t";

    /**
     * Ending characters for dynamic fields of {@link String} type. 
     * This field is indexed with "text_ws" type to tokenize the values
     * and enable case insensitive search on the field. 
     * Although "*_t" are copied to stanbolreserved_text_all, this field
     * enables the search only on this field through Solr interface.  
     */
    public static final String SOLR_DYNAMIC_FIELD_INDEXEDTEXT = "_i";

    /**
     * Ending characters for dynamic fields of {@link Long} type.
     */
    public static final String SOLR_DYNAMIC_FIELD_LONG = "_l";

    /**
     * Ending characters for dynamic fields of {@link Double} type.
     */
    public static final String SOLR_DYNAMIC_FIELD_DOUBLE = "_d";

    /**
     * Ending characters for dynamic fields of {@link Date} type.
     */
    public static final String SOLR_DYNAMIC_FIELD_DATE = "_dt";

    public static final List<String> DYNAMIC_FIELD_EXTENSIONS;
    static {
        DYNAMIC_FIELD_EXTENSIONS = new ArrayList<String>();
        DYNAMIC_FIELD_EXTENSIONS.add(SOLR_DYNAMIC_FIELD_DATE);
        DYNAMIC_FIELD_EXTENSIONS.add(SOLR_DYNAMIC_FIELD_INDEXEDTEXT);
        DYNAMIC_FIELD_EXTENSIONS.add(SOLR_DYNAMIC_FIELD_DOUBLE);
        DYNAMIC_FIELD_EXTENSIONS.add(SOLR_DYNAMIC_FIELD_LONG);
        DYNAMIC_FIELD_EXTENSIONS.add(SOLR_DYNAMIC_FIELD_TEXT);
    }

    /**
     * "OR" keyword for Solr queries.
     */
    public static final String SOLR_OR = " OR ";

    /**
     * Checks whether the specified field is excluded or not i.e whether the field will be presented as a
     * facet or not
     * 
     * @param name
     *            field name
     */
    public static boolean isNameExcluded(String name) {
        // trim the data type extension if there is one
        int underscoreIndex = name.lastIndexOf("_");
        if (underscoreIndex != -1) {
            String extension = name.substring(underscoreIndex);
            if (DYNAMIC_FIELD_EXTENSIONS.contains(extension)) {
                name = name.substring(0, underscoreIndex);
            }
        }

        return excludedFields.contains(name);
    }

    /**
     * Fields that will not be presented as facets
     */
    private static List<String> excludedFields = new ArrayList<String>();
    static {
        excludedFields.add("skos:definition");
        excludedFields.add("rdfs:label");
    }

    /**
     * Checks whether the specified type is a range type or not 
     * i.e whether the field will be used in range
     * queries or not
     * 
     * @param name
     *            field name
     */
    public static boolean isRangeType(String type) {
        return rangeFieldTypes.contains(type);
    }

    /**
     * Types that will be used in range queries and will not be escaped in {@link SolrSearchEngineHelper}
     * class
     */
    private static List<String> rangeFieldTypes = new ArrayList<String>();
    static {
    	rangeFieldTypes.add("int");
    	rangeFieldTypes.add("float");
    	rangeFieldTypes.add("long");
    	rangeFieldTypes.add("double");
    	rangeFieldTypes.add("date");
    	rangeFieldTypes.add("tdate");
    }
}
