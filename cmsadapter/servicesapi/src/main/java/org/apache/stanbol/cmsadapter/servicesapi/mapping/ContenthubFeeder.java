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
package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import java.util.Dictionary;

/**
 * This interface provides methods to submit and delete content items to/from Contenthub.
 * 
 * Stanbol provides default implementations of this interface for JCR and CMIS content repositories. However,
 * it is also possible to provide custom implementations based on the needs of content repository. It is still
 * possible to provide new implementations for JCR or CMIS repositories. <code>ContenthubFeederManager</code>
 * gives higher priority to custom implementations when selecting the appropriate {@link ContenthubFeeder}
 * instance.
 * 
 * While submitting content items to Contenthub properties of content repository objects are provided as
 * metadata of the content items. Supplied metadata is used to provide faceted search feature in the
 * Contenthub.
 * 
 * @author suat
 * 
 */
public interface ContenthubFeeder {
    /**
     * Session property for default JCR and CMIS Contenthub Feeder implementations
     */
    public static final String PROP_SESSION = "org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeeder.session";

    /**
     * Content properties property. It indicates the fields that holds the actual content in the content
     * repository item.
     */
    public static final String PROP_CONTENT_PROPERTIES = "org.apache.stanbol.cmsadapter.servicesapi.mapping.ContenthubFeeder.contentFields";

    /**
     * Creates a content item in Contenthub by leveraging the content repository object itself e.g <b>Node</b>
     * in JCR, <b>Document</b> in CMIS. If there is an already existing content item in the Contenthub with
     * the same id, the existing content item should be deleted first.
     * 
     * @param o
     *            Content repository object to be transformed into a content item in Contenthub
     * @param id
     *            Optional ID for the content item in Contenthub. If this parameter is specified, it will be
     *            used as the ID of the content item in Contenthub. Otherwise, the object's own ID in the
     *            content repository will be used.
     */
    void submitContentItemByCMSObject(Object o, String id);

    /**
     * Creates a content item in Contenthub by leveraging the content repository object itself e.g <b>Node</b>
     * in JCR, <b>Document</b> in CMIS. If there is an already existing content item in the Contenthub with
     * the same id, the existing content item should be deleted first.
     * 
     * @param o
     *            Content repository object to be transformed into a content item in Contenthub
     * @param id
     *            Optional ID for the content item in Contenthub. If this parameter is specified, it will be
     *            used as the ID of the content item in Contenthub. Otherwise, the object's own ID in the
     *            content repository will be used.
     * @param indexName
     *            Name of the Solr index managed by Contenthub. Specified index will be used to store the
     *            submitted content item
     */
    void submitContentItemByCMSObject(Object o, String id, String indexName);

    /**
     * Submits content item by its ID to the Contenthub. If there is an already existing content item in the
     * Contenthub with the same id, the existing content item should be deleted first.
     * 
     * @param contentItemID
     *            ID of the content item in the repository
     */
    void submitContentItemByID(String contentItemID);

    /**
     * Submits content item by its ID to the Contenthub. If there is an already existing content item in the
     * Contenthub with the same id, the existing content item should be deleted first.
     * 
     * @param contentItemID
     *            ID of the content item in the repository
     * @param indexName
     *            Name of the Solr index managed by Contenthub. Specified index will be used to store the
     *            submitted content item
     */
    void submitContentItemByID(String contentItemID, String indexName);

    /**
     * Submits content item by its path to the Contenthub. If there is an already existing content item in the
     * Contenthub with the same id, the existing content item should be deleted first.
     * 
     * @param contentItemPath
     *            path of the content item in the repository
     */
    void submitContentItemByPath(String contentItemPath);

    /**
     * Submits content item by its path to the Contenthub. If there is an already existing content item in the
     * Contenthub with the same id, the existing content item should be deleted first.
     * 
     * @param contentItemPath
     *            path of the content item in the repository
     * @param indexName
     *            Name of the Solr index managed by Contenthub. Specified index will be used to store the
     *            submitted content item
     */
    void submitContentItemByPath(String contentItemPath, String indexName);

    /**
     * Submits all of the content items under the specified path to the Contenthub. If there are already
     * existing content items in the Contenthub with same ids of submitted content items, the existing content
     * items should be deleted first.
     * 
     * @param rootPath
     *            root path in the content repository
     */
    void submitContentItemsUnderPath(String rootPath);

    /**
     * Submits all of the content items under the specified path to the Contenthub. If there are already
     * existing content items in the Contenthub with same ids of submitted content items, the existing content
     * items should be deleted first.
     * 
     * @param rootPath
     *            root path in the content repository
     * @param indexName
     *            Name of the Solr index managed by Contenthub. Specified index will be used to store the
     *            submitted content items
     */
    void submitContentItemsUnderPath(String rootPath, String indexName);

    /**
     * Filters content items from content repository via the specific {@link ContentItemFilter} implementation
     * passed as a parameter and submits the filtered content items to the Contenthub. If there are already
     * existing content items in the Contenthub with same ids of submitted content items, the existing content
     * items should be deleted first.
     * 
     * @param customContentItemFilter
     *            custom {@link ContentItemFilter} implementation
     */
    void submitContentItemsByCustomFilter(ContentItemFilter customContentItemFilter);

    /**
     * Filters content items from content repository via the specific {@link ContentItemFilter} implementation
     * passed as a parameter and submits the filtered content items to the Contenthub. If there are already
     * existing content items in the Contenthub with same ids of submitted content items, the existing content
     * items should be deleted first.
     * 
     * @param customContentItemFilter
     *            custom {@link ContentItemFilter} implementation
     * @param indexName
     *            Name of the Solr index managed by Contenthub. Specified index will be used to store the
     *            submitted content items
     */
    void submitContentItemsByCustomFilter(ContentItemFilter customContentItemFilter, String indexName);

    /**
     * Deletes content item by its ID from the Contenthub. Please note that specified identifier should be the
     * one that identifying the content item in Contenthub.
     * 
     * @param contentItemID
     *            ID of the content item in the <b>Contenthub</b>
     */
    void deleteContentItemByID(String contentItemID);

    /**
     * Deletes content item by its ID from the Contenthub. Please note that specified identifier should be the
     * one that identifying the content item in Contenthub.
     * 
     * @param contentItemID
     *            ID of the content item in the <b>Contenthub</b>
     * @param indexName
     *            Name of the Solr index managed by Contenthub. Specified index will be used to delete the
     *            submitted content item from.
     */
    void deleteContentItemByID(String contentItemID, String indexName);

    /**
     * Deletes content item by its path from the Contenthub
     * 
     * @param contentItemPath
     *            path of the content item in the repository
     */
    void deleteContentItemByPath(String contentItemPath);

    /**
     * Deletes content item by its path from the Contenthub
     * 
     * @param contentItemPath
     *            path of the content item in the repository
     * @param indexName
     *            Name of the Solr index managed by Contenthub. Specified index will be used to delete the
     *            submitted content item from.
     */
    void deleteContentItemByPath(String contentItemPath, String indexName);

    /**
     * Deletes all of the content items under the specified path to Contenthub
     * 
     * @param rootPath
     *            root path in the content repository
     */
    void deleteContentItemsUnderPath(String rootPath);

    /**
     * Deletes all of the content items under the specified path to Contenthub
     * 
     * @param rootPath
     *            root path in the content repository
     * @param indexName
     *            Name of the Solr index managed by Contenthub. Specified index will be used to delete the
     *            submitted content items from.
     */
    void deleteContentItemsUnderPath(String rootPath, String indexName);

    /**
     * Filters content items from content repository via the specific {@link ContentItemFilter} implementation
     * passed as a parameter and deletes the filtered content items from the Contenthub
     * 
     * @param customContentItemFilter
     *            custom {@link ContentItemFilter} implementation
     */
    void deleteContentItemsByCustomFilter(ContentItemFilter customContentItemFilter);

    /**
     * Filters content items from content repository via the specific {@link ContentItemFilter} implementation
     * passed as a parameter and deletes the filtered content items from the Contenthub
     * 
     * @param customContentItemFilter
     *            custom {@link ContentItemFilter} implementation
     * @param indexName
     *            Name of the Solr index managed by Contenthub. Specified index will be used to delete the
     *            submitted content items from.
     */
    void deleteContentItemsByCustomFilter(ContentItemFilter customContentItemFilter, String indexName);

    /**
     * This method is used for identification of {@link ContenthubFeeder}s based on the specified
     * <code>session</code> object. If the specified instance can be used in certain implementation, it
     * returns <code>true</code>, otherwise <code>false</code>.
     * 
     * @param session
     *            Session object to be checked
     * @return whether certain implementation can handle specified connection type
     */
    boolean canFeedWith(Object session);

    /**
     * Provides injecting of implementation dependent configurations at runtime for different
     * {@link ContenthubFeeder}s.
     * 
     * @param configs
     *            Configurations passed in a {@link Dictionary} instance
     * @throws ContenthubFeederException
     */
    void setConfigs(Dictionary<String,Object> configs) throws ContenthubFeederException;
}