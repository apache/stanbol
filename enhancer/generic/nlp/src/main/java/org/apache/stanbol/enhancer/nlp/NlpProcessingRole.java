package org.apache.stanbol.enhancer.nlp;

/**
 * Defines NLP processing Roles engines can take. The idea is to use those roles
 * to ease the configuration or NLP enhancement chains. Basically users would
 * just configure what NLP features the want to use and the NLP chain would
 * choose the fitting Engines based on their "service.ranking" values.
 *
 */
public enum NlpProcessingRole {

    LanguageDetection,
    SentenceDetection,
    Tokenizing,
    PartOfSpeachTagging,
    Chunking, 
    SentimentTagging, 
    Lemmatize
}
