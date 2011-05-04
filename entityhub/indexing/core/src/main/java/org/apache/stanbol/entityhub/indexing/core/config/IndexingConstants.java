package org.apache.stanbol.entityhub.indexing.core.config;

import java.io.File;

/**
 * Constants defines/used for Indexing.
 * @author Rupert Westenthaler
 *
 */
public interface IndexingConstants {

    String KEY_NAME                  = "name";
    String KEY_DESCRIPTION           = "description";
    String KEY_ENTITY_DATA_ITERABLE  = "entityDataIterable";
    String KEY_ENTITY_DATA_PROVIDER  = "entityDataProvider";
    String KEY_ENTITY_ID_ITERATOR    = "entityIdIterator";
    String KEY_ENTITY_SCORE_PROVIDER = "entityScoreProvider";
    String KEY_INDEXING_DESTINATION = "indexingDestination";
    String KEY_INDEX_FIELD_CONFIG = "fieldConfiguration";
    /**
     * usage:<br>
     * <pre>
     * {class1},name:{name1};{class2},name:{name2};...
     * </pre>
     * The class implementing the normaliser and the name of the configuration
     * file stored within /config/normaliser/{name}.properties
     */
    String KEY_SCORE_NORMALIZER      = "scoreNormalizer";
    String KEY_ENTITY_PROCESSOR      = "entityProcessor";
}
