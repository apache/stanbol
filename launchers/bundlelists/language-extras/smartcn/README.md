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

Basic Chinese language support based on Lucene Smartcn Analyzer
==============

This BundleList includes three modules that bring basic language support for Chinese to Apache Stanbol.

See comments in the [lists.xml](src/main/bundles/list.xml) for more details.

Solr Field Configuration
---

When you plan to use the Smartcn Analyzer to process Chinese texts it is important to also properly configure the Solr schema.xml used by the Entityhub SolrYard.

For that you will need to add two things:

1. A fieldType specification for Chinese

    :::xml
    <fieldType name="text_zh" class="solr.TextField" positionIncrementGap="100">
      <analyzer type="index">
        <tokenizer class="solr.SmartChineseSentenceTokenizerFactory"/>
        <filter class="solr.SmartChineseWordTokenFilterFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
        <filter class="solr.RemoveDuplicatesTokenFilterFactory"/>
      </analyzer>
      <analyzer type="query">
        <tokenizer class="solr.SmartChineseSentenceTokenizerFactory"/>
        <filter class="solr.SmartChineseWordTokenFilterFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
        <filter class="solr.RemoveDuplicatesTokenFilterFactory"/>
        <filter class="solr.PositionFilterFactory" />
      </analyzer>
    </fieldType> 

2. A dynamic field using this field type that matches against Chinese language literals

    :::xml
    <!--
     Dynamic field for Chinese languages.
     -->
    <dynamicField name="@zh*" type="text_zh" indexed="true" stored="true" multiValued="true" omitNorms="false"/>

The [smartcn.solrindex.zip](https://svn.apache.org/repos/asf/stanbol/trunk/entityhub/yard/solr/src/main/resources/solr/core/smartcn.solrindex.zip) is identical with the default configuration but uses the above fieldType and dynamicField specification.

### Usage with the EntityhubIndexing Tool

1. Extract the [smartcn.solrindex.zip](https://svn.apache.org/repos/asf/stanbol/trunk/entityhub/yard/solr/src/main/resources/solr/core/smartcn.solrindex.zip) to the "indexing/config" directory 
2. Rename the "indexing/config/smartcn" directory to the {site-name} (the value of the "name" property of the "indexing/config/indexing.properties" file).

As an alternative to (2) you can also explicitly configure the name of the solr config as value to the "solrConf:smartcn" of SolrYardIndexingDestination.

    :::text
    indexingDestination=org.apache.stanbol.entityhub.indexing.destination.solryard.SolrYardIndexingDestination,solrConf:smartcn,boosts:fieldboosts

### Usage with the Entityhub SolrYard

If you want to create an empty SolrYard instance using the [smartcn.solrindex.zip](https://svn.apache.org/repos/asf/stanbol/trunk/entityhub/yard/solr/src/main/resources/solr/core/smartcn.solrindex.zip) configuration you will need to

1. copy the smartcn.solrindex.zip to the datafile directory of your Stanbol instance ({working-dir}/stanbol/datafiles)
2. rename it to the {name} of the SolrYard you want to create. The file name needs to be {name}.solrindex.zip
3. create the SolrYard instance and configure the "Solr Index/Core" (org.apache.stanbol.entityhub.yard.solr.solrUri) to {name}. Make sure the "Use default SolrCore configuration" (org.apache.stanbol.entityhub.yard.solr.useDefaultConfig) is disabled.

If you want to use the smartcn.solrindex.zip as default you can rename the file in the datafilee folder to "default.solrindex.zip" and the enable the "Use default SolrCore configuration" (org.apache.stanbol.entityhub.yard.solr.useDefaultConfig) when you configure a SolrYard instance.

See also the documentation on how to [configure a managed site](http://stanbol.apache.org/docs/trunk/components/entityhub/managedsite#configuration-of-managedsites)).
