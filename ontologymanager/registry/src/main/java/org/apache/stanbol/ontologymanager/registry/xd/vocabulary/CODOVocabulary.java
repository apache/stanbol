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
package org.apache.stanbol.ontologymanager.registry.xd.vocabulary;

/**
 * @author Enrico Daga
 * 
 */
public final class CODOVocabulary {

    /**
     * Restrict instantiation
     */
    private CODOVocabulary() {}

    public static final String CODD_hasLogicalLanguage = Vocabulary.CODD.uri + "#hasLogicalLanguage";
    public static final String CODD_hasVersion = Vocabulary.CODD.uri + "#hasVersion";
    public static final String CODD_imports = Vocabulary.CODD.uri + "#imports";
    public static final String CODD_isImportedBy = Vocabulary.CODD.uri + "#isImportedBy";
    public static final String CODD_isVersionOf = Vocabulary.CODD.uri + "#isVersionOf";
    public static final String CODD_OntologyLibrary = Vocabulary.CODD.uri + "#OntologyLibrary";
    public static final String CODD_relatedToOntology = Vocabulary.CODD.uri + "#relatedToOntology";
    public static final String CODK_isReusedBy = Vocabulary.CODK.uri + "#isReusedBy";
    public static final String CODK_Ontology = Vocabulary.CODK.uri + "#Ontology";
    public static final String CODP_isIntendedOutputOf = Vocabulary.CODP.uri + "#isIntendedOutputOf";
    public static final String CODT_isInputDataFor = Vocabulary.CODT.uri + "#isInputDataFor";
    public static final String CODT_isOutputDataFor = Vocabulary.CODT.uri + "#isOutputDataFor";
    public static final String CODW_isInvolvedInDesignOperationsBy = Vocabulary.CODW.uri
                                                                     + "#isInvolvedInDesignOperationsBy";
    public static final String DESCASIT_isDescribedBy = Vocabulary.DESCASIT.uri + "#isDescribedBy";
    public static final String INTEXT_expresses = Vocabulary.INTEXT.uri + "#expresses";
    public static final String INTEXT_isAbout = Vocabulary.INTEXT.uri + "#isAbout";
    public static final String ODP_ONTOLOGY_LOCATION = Vocabulary.ODP.uri;
    public static final String ODPM_HasOntology = Vocabulary.ODPM.uri + "#hasOntology";
    public static final String ODPM_IsOntologyOf = Vocabulary.ODPM.uri + "#isOntologyOf";
    public static final String ODPM_ODPRepository = Vocabulary.ODPM.uri + "#ODPRepository";
    public static final String PARTOF_HasPart = Vocabulary.PARTOF.uri + "#hasPart";
    public static final String PARTOF_IsPartOf = Vocabulary.PARTOF.uri + "#isPartOf";
    public static final String REPOSITORY_MERGED_ONTOLOGY = "http://xd-repository/temporary/merged/ontology";
    public static final String REPRESENTATION_hasRepresentationLanguage = Vocabulary.REPRESENTATION.uri
                                                                          + "#hasRepresentationLanguage";
    public static final String[] ONTOLOGY_ANNOTATION_PROPERTIES = {
                                                                   CODOVocabulary.CODD_hasLogicalLanguage,
                                                                   CODOVocabulary.CODD_hasVersion,
                                                                   CODOVocabulary.CODD_imports,
                                                                   CODOVocabulary.CODD_isImportedBy,
                                                                   CODOVocabulary.CODD_isVersionOf,
                                                                   CODOVocabulary.CODD_relatedToOntology,
                                                                   CODOVocabulary.CODK_isReusedBy,
                                                                   CODOVocabulary.CODP_isIntendedOutputOf,
                                                                   CODOVocabulary.CODT_isInputDataFor,
                                                                   CODOVocabulary.CODT_isOutputDataFor,
                                                                   CODOVocabulary.CODW_isInvolvedInDesignOperationsBy,
                                                                   CODOVocabulary.DESCASIT_isDescribedBy,
                                                                   CODOVocabulary.INTEXT_expresses,
                                                                   CODOVocabulary.INTEXT_isAbout,
                                                                   CODOVocabulary.REPRESENTATION_hasRepresentationLanguage};
}
