<!--
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
[FST Linking Engine](https://issues.apache.org/jira/browse/STANBOL-1128)
==================

This engine implements entity linking functionality based on Lucene (Finite State Transducer) technology. This allows this engine to perform the label based Entity lookup fully in-memory. Only Entity specific information (URI, labels, types and ranking) for tagged Entities need to be loaded from disc (or retrieved from an in-memory cache. By doing so this engine can outperform query based entity linking engines by a factor of ten or more.

## Lucene FST for Entity Linking

This [persentation](https://docs.google.com/presentation/d/1Z7OYvKc5dHAXiVdMpk69uulpIT6A7FGfohjHx8fmHBU/edit#slide=id.p) by ???? ????? provides a good overview of FST and how they are implemented and used in Solr/Lucene.

This Engine does not use the Lucene FST directly, but is based on OpenSextant [SolrTextTagger](https://github.com/OpenSextant/SolrTextTagger/) that provides already a naive text tagger functionality. This [video](http://www.youtube.com/watch?v=3kQyYbTyXfc) of a presentation by David Smileys provides a lot of details on how this all works. 

To give users some Idea on how efficient FST can be used to hold information this are the statistics for FST models required for Entity Linking against [Freebase](http://freebase.com):

* Number of Entities: ~40 million
* FST for English labels: < 200MByte
* FST for other major languages are all < 20MByte
* FST for all ~200 used language codes are about 500MByte

This means that the memory requirements for in-memory Entity Linking against Freebase are less as for English NLP processing using [Stanford NLP](https://github.com/westei/stanbol-stanfordnlp) or [Freeling](https://github.com/insideout10/stanbol-freeling).

## Building the Engine

This engine currently depends to an unreleased version 1.2 of the [SolrTextTagger](https://github.com/OpenSextant/SolrTextTagger/) module. Because of that users will need to get the source from Github and mvm install it to your local repository.

Currently it is highly recommended to use the [SolrTextTagger fork](https://github.com/westei/SolrTextTagger/) of Rupert Westenthaler as it includes already a [Pull](https://github.com/OpenSextant/SolrTextTagger/pull/9) requests that adds support for multi valued fields.

After completing this the engine can be normally build and used with Apache Stanbol.

## Configuration of the Engine

### Configuration of the Solr Index

The Solr index is configured by using the `enhancer.engines.linking.solrfst.solrcore` configuration property of the Engine. This property needs to point to a Solr index that runs embedded in the same JVM as Apache Stanbol. The Stanbol Commons Solr modules provide two Components that allow to configure embedded Solr Indexes:

1. __[ReferencedSolrServer](http://stanbol.apache.org/docs/trunk/utils/commons-solr#referencedsolrserver)__: This components allows uses to configure a directory containing a SolrServer configuration (the directory with the solr.xml file). All Solr indexes defined by the Solr.xml will be initialized and published as OSGI services to Apache Stanbol. Such indexes can be configured to the engine by using {server-name}:{index-name}. {server-name} is the name of the ReferencedSolrServer as provided in the configuration. {index-name} is the name of the Solr index as defined in the solr.xml.
1. __[ManagedSolrServer](http://stanbol.apache.org/docs/trunk/utils/commons-solr#managedsolrserver)__: This component allows to have a Solr server that is fully managed by Apache Stanbol. Indexes can be installed by copying '{name-name}.solrindex.zip' files to the 'stanbol/datafiles'. Solr indexes initialized like that will be available under '{index-name}' and 'default:{index-name}'.

Used Solr indexes need also confirm to the requirements of the [SolrTextTagger](https://github.com/OpenSextant/SolrTextTagger/) module. That means that fields used for FST linking MUST use field analyzers that produce consecutive positions (i.e. the position increment of each term must always be 1). This means that typical field analyzers as sued for searches will not work.

The SolrTextTagger README provides an example for a Field Analyzer configuration that does work. To make things easier this engine includes this [XML file](fst_field_types.xml) that includes a schema.xml fragment with FST tagging compatible configurations for most languages supported by Solr.


### Solr Index Layout Configuration

This part of the configuration is used to specify the layout if the used Solr index. It specifies how Entity information are stored in the Solr index.

#### Field Name Encoding 

The Field Name Encoding configuration `enhancer.engines.linking.solrfst.fieldEncoding` specifies how Solr fields for multiple languages are encoded. As an example a Vocabulary with labels in multiple languages might use "en_label" for the English language labels and "de_label" for the German language labels. In this case users should set this property to `UnderscorePrefix` and simple use "label" when configuring the FST field name. 

The Field Name Encodings work well with Solr dynamic field configurations that allow to map language specific FieldType specifications to prefixes and suffixes such as

   <dynamicField name="en_*" type="text_en_fst" indexed="true" stored="true" multiValued="true" omitNorms="false"/>
   <dynamicField name="de_*" type="text_en_fst" indexed="true" stored="true" multiValued="true" omitNorms="false"/>

This is the full list of supported Field encodings:

* SolrYard: This supports the encoding use by the Stanbol Entityhub SolrYard implementation to encode RDF data types and language literals. If you configure the FST Linking Engine for a Solr index build for the SolrYard you need to use this encoding
* MinusPrefix: {lang}-{field} (e.g. "en-name")
* UnderscorePrefix: {lang}_{field} (e.g. "en_name")
* AtPrefix: {lang}@{field} (e.g. "en@name")
* MinusSuffix: {field}-{lang} (e.g. "name-en")
* UnderscoreSuffix: {field}-{lang} (e.g. "name_en")
* AtSuffix: {field}-{lang} (e.g. "name@en")
* None: In this case no prefix/suffix rewriting of configured `field` and `store` values is done. This means that the FST Configuration MUST define the exact field names in the Solr index for every configured language.

#### FST Tagging Configuration

The FST Tagging Configuration `enhancer.engines.linking.solrfst.fstconfig` defines several things:

1. for what languages FST models should be build. This configuration is basically a list of language codes but also supports wildcards '*' and exclusions '!{en}'
2. what fields in the Solr Index are used to build FST models. Two fields per language are required: a) an 'Indexed Field' (_field_ parameter) and b) a 'Stored Field' (_stored_ parameter). Both the indexed and stored field might refer to the same field in the Solr index. In that case this field needs to use `indexed="true" stored="true"`.
3. if FST models can be build by the Engine at runtime as well as the name of the serialized models.

This configuration is line based (multi valued) and uses the following generic syntax:

    {language};{param}={value};{param1}={value1};
    !{language}

`{language}` is either the name of the language (e.g. 'en'), '*' for all languages or '' (empty string) for defining default parameter values without including all languages. Lines that do start with '!' do explicitly exclude a language. Those lines do not allow parameters.

The following parameters are supported by the Engine:

* __field__: The indexed field in the configured Solr index. In multilingual scenarios this might be the 'base name' of the field that is extended by a prefix or suffix to get the actual field name in the Solr index (see also the field encoding configuration)
* __stored__ (default: _field_ value) : The field in the Solr index with the stored label information. This parameter is optional. If not present `stored` is assumed to be equals to `field`.
* __fst__ (default based on _field_ value): Optionally allows to manually specify the base file name of the FST models. Those files are assumed within the data directory of the configured Solr index under `fst/{fst}.{lang}.fst`. By default the configured `field` name is used (with non alpha-numeric chars replaced by '_').If runtime creation is enabled those files will be created if not present.
* __generate__ (default: false): If enabled the Engine will generate missing FST models. If this is enabled the engine will also be able to update FST models after changes to the Solr Index. __NOTE__ that the creation of FST models is an expensive operation (both CPU and memory wise). The FST engine uses a pool of low priority threads to create FST models. The size of the pool can be configured by using the `enhancer.engines.linking.solrfst.fstThreadPoolSize` parameter. Because of this the default is `false`.

A more advanced Configuration might look like:

    ;field=fise:fstTagging;stored=rdfs:label;generate=true
    en
    de
    es
    fr
    it

This would set the index field to "fise:fstTagging", the stored field to "rdfs:label" and allow runtime generation. It would also enable to process English, German, Spanish, French and Italian texts. A similar configuration that would build FST models for all languages would look as follows 

    *;field=fise:fstTagging;stored=rdfs:label;generate=true

#### Additional Entity Information

* __Entity Type Field__ _(enhancer.engines.linking.solrfst.typeField)_: This field specifies the Solr field name holding entity type information of Entities. In case 'SolrYard' is used as _Field Name Encoding_ one can use the the QNAME of the property (typically 'rdf:type'). Otherwise the value must be the exact field name holding the type information. Values are expected to be URIs.
* __Entity Ranking Field__ _(enhancer.engines.linking.solrfst.rankingField)_: This is an __ADDITIONAL__ property used to configure the name of the Field storing the floating point value of the ranking for the Entity. Entities with higher ranking will get a slightly better `fise:confidence` value if labels of several Entities do match the text.

### Runtime FST generation Thread Pool

The `enhancer.engines.linking.solrfst.fstThreadPoolSize` parameter can be used to configure the size of the thread pool used for the runtime generation of FST models. The default size of the thread pool is `1`. Threads do use the lowest possible priority to reduce the performance impact on enhancements as much as possible.

When configuring the size of the thread pool users need to be aware that the generation of FST models does need a lot more memory as the resulting model. So having to manny parallel threads might require to increase the memory settings of the JVM. On typical machines FST creation threads will consume 100% CPU. That means that the number of threads should be configured to the number of CPU cores that can be spared for FST generation.

_NOTE_ that the `generate` parameter of the FST Tagging Configuration needs to be set to `true` to enable runtime generation.


### Entity Cache Configuration

While FST tagging is fully done in-memory the FST linking engine needs to read information of matching Entities from the Solr index. This requires disc IO and is typically the part of the process that consumes the most time. The Entity Cache tries to prevent such disc level IO by caching SolrDocuments containing only fields required for the linking process (labels, types and (if available) entity rankings).  To further reduce memory requirements only labels in languages requested by processed ContentItems are stored in the cache. The Cache uses the LRU semantic and is based on the Solr cache implementation.

The size of the cache can be configured by using the `enhancer.engines.linking.solrfst.entityCacheSize` parameter. The default size is ~65k entities. Increasing the maximum size of the cache will improve performance. For small and medium sized vocabularies the cache can be configured in a way that all entities are cached in memory. 

### Text Processing Configuration

During the development of this Engine the SolrTextTagger was extended by a feature that allows to only lookup some tokens in the text (see this [Pull Request](https://github.com/OpenSextant/SolrTextTagger/pull/7) for details). This feature is used to integrate the [Stanbol NLP Processing API](http://stanbol.apache.org/docs/trunk/components/enhancer/nlp/) with the SolrTextTagger. Meaning that NLP processing results (such as POS tags, Chunks and Named Entities) can be used to tell the SOlrTextTagger what tokens to lookup in the Vocabulary.

For now this engine uses the exact same [Text Processing configuration](http://stanbol.apache.org/docs/trunk/components/enhancer/engines/entitylinking#text-processing-configuration) as the Entity Linking Engine. Please see the linked section of the EntityLinkingEngine documentation for details.


### Entity Linking Configuration

The Entity Linking Configuration of this Engine is very similar as the one for the [EntityLinking engine](http://stanbol.apache.org/docs/trunk/components/enhancer/engines/entitylinking#entity-linker-configuration). The configuration does use the exact same keys, but it does not support all properties and some do have a slightly different meaning. In the following only the differences are described. For the all other things please refer to the linked section of the documentation of the EntityLinking engine.

* <s>__Label Field__ _(enhancer.engines.linking.labelField)_</s>: The label field is __IGNORED__ as the field holding the labels is anyway provided by the [FST Tagging Configuration]. That means that the field defined by the _stored_ parameter is used. If the _stored_ parameter is not present it fallbacks to the _field_ parameter.
* <s>__Type Field__ _(enhancer.engines.linking.typeField)_</s>: This configuration gets __IGNORED__ in favor of the `enhancer.engines.linking.solrfst.typeField`. See the [Additional Entity Information] section for details. 
* __Redirect Field__ _(enhancer.engines.linking.redirectField)_</s>: Note implemented. __NOTE__ This might not be possible to efficiently implement. When those redirects need already be considered when building the FST models.
* <s>__Use EntityRankings (enhancer.engines.linking.useEntityRankings)_</s>: This configuration gets __IGNORED__. EntityRanking based sorting is enabled as soon as the _Entity Ranking Field_ is configured.
* <s>__Lemma based Matching__ _(enhancer.engines.linking.lemmaMatching)_</s>: Not Yet implemented
* <s>__Min Match Score__ _(enhancer.engines.linking.minMatchScore)_</s>: Not Yet Implemented. Currently all linked Entities are added regardless of their score. However the way the Tagging is done makes it very unlikely to have suggestions with `fise:confidence` values less as 0.5.

In addition the following properties are __IGNORED__ as they are not relevant for the FST Linking Engine:

* <s>__Max Search Token Distance__ _(enhancer.engines.linking.maxSearchTokenDistance)_</s>
* <s>__Max Search Tokens__ _(enhancer.engines.linking.maxSearchTokens)_</s>
* <s>__Min Matched Tokens__ _(enhancer.engines.linking.minFoundTokens)_</s>
* <s>__Min Text Score__ _(enhancer.engines.linking.minTextScore)_</s>


## Further Information

### Runtime generation of FST models

The `generate`

### FST model updates

The FST Model

## TODOs:

__Making existing Entityhub SolrYard indexes Compatible with FST linking:__

* Add (or adapt existing) Solr index configuration for the Entityhub SolrYard to support FST linking: The current one do not confirm with the restrictions of the SolrTextTagger
* Provide updated versions of downloadable Indexes that are compatible with FST linking. Those indexes will have additional indexed only fields that need to be configured as _field_ parameter of the FST configuration. The _stored_ parameter will be kept to the current label field (typically rdfs:label).
    * update the DBpedia default data index (included in the Stanbol Launcher) to be FST linking compatible
    * include built FST models in the package of the DBpedia default data index to avoid the need to build them at runtime.

__Build process and Testing related:__

* Include the FST linking engine in the default build process. Depends on SolrTextTagger 1.2 to be released and available on Maven Central
* Add unit tests for the FST linking engine
* Add an integration tests with the FST linking engine to check concurrency and performance (depends on the DBpedia default data index to be FST compatible)
* Add tests for non SolrYard build Solr indexes.

__Feature related__

* Implement support for rebuilding FST models after a Solr index change.
* Tests with Chinese and Japanese: As this Engine does not operate on Words it should outperform the EntityLinking engine for Chinese and Japanese. However this is not yet tested.
* Implement Lemma based matching support
* Check implementation or Redirect Modes as supported by the EntityLinking engine

__Other__

* Not tested with enabled SecurityManager
* Implementation of an own Entity Dereferencing Engine: This is required as the FST Linking Engine can not dereference Entity data (as the EntityLinking and the EntityTagging engine).


## Known Issues:

As the first version of the FST Linking Engine is still in active development their are some know issues:

* The Japanese FieldType as specified in the [fst_field_types.xml](fst_field_types.xml) file does produce position increments != 1. This is caused by Kuromoji's [JapaneseTokenizer](http://lucene.apache.org/core/3_6_0/api/contrib-kuromoji/org/apache/lucene/analysis/ja/JapaneseTokenizer.html) outputting several tokens for the same position (posInc=0). The implementation of [Issue10](https://github.com/OpenSextant/SolrTextTagger/issues/10) will solve this by adding support for such TokenStream configurations.
* the RefCounted EntityCache is not destroyed prior to finalise(). This means that at some point the reference count is not correctly dereferenced. 


