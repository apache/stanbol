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
package org.apache.stanbol.entityhub.yard.solr.model;

import java.util.Collection;
import java.util.List;

/**
 * Implementations of this interface implement the mapping of logical field (as represented by
 * {@link IndexField}) to the actual field name(s) used to index such values.
 * <p>
 * Such mappings are needed for indexing as well as querying for information within the full text index.
 * <p>
 * Implementations of this class are NOT responsible for converting actual values to there index
 * representation. This is provided by the {@link IndexValueFactory} that converts java object's to
 * {@link IndexValue} instances and vis versa. However this interface contains a convenience method that
 * allows to get the field names for a <code>List&lt;String&gt; path</code> and a
 * <code>IndexValue indexValue</code> because this is the most common task while indexing data.
 * <p>
 * The three encode** methods are used to encode query constraints.
 * <p>
 * Implementations are encouraged to cache mappings, because typically there will be many request with similar
 * parameters.
 * 
 * @author Rupert Westenthaler.
 */
public interface FieldMapper {

    /**
     * This is the unique ID of this component. This ID must be be used to store configurations that should be
     * unique for this component.
     */
    String URI = "urn:eu.iksproject:rick.yard.solr:config.namespacePrefixConfig";
    // TODO: Need to change that to following
    // String URI = "urn:org.apache.stanbol:entityhub.yard.solr:config.namespacePrefixConfig";
    // ... will do that later, because it would invalidate all existing indexes
    /**
     * The field used to store all namespace prefixes
     */
    String PREFIX_FIELD = "urn:eu.iksproject:rick.yard.solr:config.namespacePrefix";

    // TODO: Need to change that to following
    // String PREFIX_FIELD = "urn:org.apache.stanbol:entityhub.yard.solr:config.namespacePrefix";
    // ... will do that later, because it would invalidate all existing indexes
    /**
     * Getter for the Field used to store the domain(s) of the Document. This field is used if Documents of
     * different domains are stored within the same Index.
     * <p>
     * 
     * @return the field name to store the document domain within the index
     */
    String getDocumentDomainField();

    /**
     * Getter for the actual field names for parsed path and index value.
     * <p>
     * Note that the dataType and the language are encoded as prefixes and/or suffixes to the field name. A
     * single value may be indexed in several fields in the document (especially to support searches for
     * multiple languages)
     * 
     * @param path
     *            the path. MUST NOT be <code>null</code> nor empty and MUST NOT contain an element that is
     *            <code>null</code> or empty.
     * @param indexValue
     *            the index value. MUST NOT be <code>null</code>.
     * @return the field names. Guaranteed to contain at least one element. All element are guaranteed to be
     *         not <code>null</code> nor empty.
     * @throws IllegalArgumentException
     *             if the parsed path and/or index value do not confirm with the requirements
     */
    Collection<String> getFieldNames(List<String> path, IndexValue indexValue) throws IllegalArgumentException;

    /**
     * Getter for the actual field names used to index the parsed logical index field. A single index field
     * may be indexed in several fields in the document (especially to support searches for multiple
     * languages)
     * 
     * @param indexField
     *            the index field. MUST NOT be <code>null</code>
     * @return the field names. Guaranteed to contain at least one element. All element are guaranteed to be
     *         not <code>null</code> nor empty.
     * @throws IllegalArgumentException
     *             if the parsed index field is <code>null</code>.
     */
    Collection<String> getFieldNames(IndexField indexField) throws IllegalArgumentException;
    /**
     * Getter for the actual field names representing the parsed logical {@link IndexField}
     * in the context of a Query.
     * @param indexField the index field
     * @return the actual field names in the index that need to be searched for the
     * finding values of the parsed {@link IndexField}
     * @throws IllegalArgumentException
     */
    Collection<String> getQueryFieldNames(IndexField indexField) throws IllegalArgumentException;

    /**
     * Getter for the logical {@link IndexField} of an given fieldName, typically as found in a document
     * retrieved from the index.
     * 
     * @param fieldName
     *            the name of the field in the index document
     * @return the logical index field or <code>null</code> if the parsed field name does not represent a
     *         logical field.
     *         <p>
     *         Note that this may only happen, if the index is configured to store values for fields that do
     *         not represent logical field. This is typically not the case, but may be activated for debugging
     *         reasons.
     */
    IndexField getField(String fieldName);

    /**
     * Getter for the field name used to store the unique identification-
     * 
     * @return
     */
    String getDocumentIdField();

    /**
     * Getter for the field name used to store id's of documents this document depends on.
     * <p>
     * This is only needed when path with a length > 1 are indexed. This information is needed to for
     * maintenance of the index if such entities are changed. However this information might also be used to
     * calculate the semantic context.
     * <p>
     * Note that the values stored in this field are a subset of the values stored by the
     * {@link #getReferredDocumentField()} field.
     * 
     * @return the field name used for storing the id's of documents this document depends on.
     */
    String getDependentDocumentField();

    /**
     * Getter for the field name used to store the id's of documents this document refers to.
     * <p>
     * This information is needed for maintenance of the index if documents are deleted. However this
     * information might also be used to calculate the semantic context.
     * <p>
     * Note that the values stored in this field are a superset of the values stored by the
     * {@link #getDependentDocumentField()} field.
     * 
     * @return the field name used for storing the id's of documents this document refers to.
     */
    String getReferredDocumentField();
    /**
     * Getter for the field name used to index all textual values of an Entity.
     * @return the field name that indexes all textual values of an Entity.
     */
    String getFullTextSearchField();
    /**
     * Getter for the Merger Field for the parse language
     * 
     * @param lang
     *            the language or <code>null</code> for the merger field for all languages
     * @return the merger field for the parsed language
     */
    String getLanguageMergerField(String lang);

    /**
     * Getter for the field prefixes for the parsed languages
     * 
     * @param lang
     *            the languages. <code>null</code> or an empty array to indicate that no language is present.
     * @return the field prefixes used to encode the parsed languages. Guaranteed to return NOT
     *         <code>null</code>. If no prefix is needed to encode, an empty collection is returned
     */
    Collection<String> encodeLanguages(IndexField indexField);

    /**
     * Getter for the prefix/suffix used to encode the parsed index data type.
     * 
     * @param dataType
     *            the data type
     * @return An array with two entries, where the first represents the prefix and the second the suffix used
     *         to encode the index data type. In case no prefix/suffix is used to represent the parsed index
     *         data type, the according entry in the array is <code>null</code>
     * @throws IllegalArgumentException
     *             if no prefix/suffix mapping is defined for the parsed index data type
     */
    String[] encodeDataType(IndexField indexField) throws IllegalArgumentException;

    /**
     * Getter for the encoded path
     * 
     * @param path
     *            the path. MUST NOT be <code>null</code> nor empty and MUST NOT contain an element that is
     *            <code>null</code> or empty.
     * @return the encoded path
     * @throws IllegalArgumentException
     *             if the parsed path and/or index value do not confirm with the requirements
     */
    String encodePath(IndexField indexField) throws IllegalArgumentException;
}
