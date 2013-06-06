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

Basic Chinese language support based on Paoding Analyzer
==============

This BundleList includes modules that allow to use the Paoding Analyzer to 

* use Solr indexes that use the Paoding Analyzer in Field configurations
* tokenize Chinese text with the Stanbol Enhancer
* link Entities with Chinese labels with the EntityLinkingEngine

It is highly recommended to use the Paoding bundle list in combination with the smartcn one as Paoding does not provide sentence detection.
Because of that a typical EnhancementChain for Chinese should also include the 'smartcn-sentence' engine

    :::text
    langdetect
    smartcn-sentence
    paoding-token
    {your-entitylinking}

where '{your-entitylinking}' will typically be an [EntityhubLinkingEngine](http://stanbol.apache.org/docs/trunk/components/enhancer/engines/entityhublinking) engine configured for your vocabulary containing the Entities with Chinese labels.

Please also note the comments in the [lists.xml](src/main/bundles/list.xml)

Solr Configuration
---

When you plan to use the Paoding Analyzer to process Chinese texts it is important to also properly configure the Solr schema.xml used by the Entityhub SolrYard. The DZone article [Indexing Chinese in Solr](http://java.dzone.com/articles/indexing-chinese-solr) by [Jason Hull](http://java.dzone.com/users/hullj) provides really great background information on that.

When following those instructions keep in mind that the {working-dir} of the Stanbol Entityhub IndexingTool is that directory where you call '<code>java -jar …</code>' therefore if you configure the 'PAODING_DIC_HOME' the value will be relative to the {working-dir}.

For the use of Paoding within Apache Stanbol the directory will be automatically initialized and be located in the persistent storage location of the <code>org.apache.stanbol:org.apache.stanbol.commons.solr.extras.paoding:0.10.0-SNAPSHOT</code> bundle.

### Solr Field Configuration

To use the Paoding Analyzer for Chinese literals a FieldType and a DynamicField configuration need to be added to the Solr schema.xml.


1. the fieldType specification for Chinese

    :::xml
    <fieldType name="text_zh" class="solr.TextField">
        <analyzer class="net.paoding.analysis.analyzer.PaodingAnalyzer"/>
    </fieldType>

2. A dynamic field using this field type that matches against Chinese language literals

    :::xml
    <!--
     Dynamic field for Chinese languages.
     -->
    <dynamicField name="@zh*" type="text_zh" indexed="true" stored="true" multiValued="true" omitNorms="false"/>



The [smartcn.solrindex.zip](https://svn.apache.org/repos/asf/stanbol/trunk/entityhub/yard/solr/src/main/resources/solr/core/smartcn.solrindex.zip) is identical with the default configuration but uses the above fieldType and dynamicField specification.

### Usage with the EntityhubIndexing Tool

1. Extract the [paoding.solrindex.zip](https://svn.apache.org/repos/asf/stanbol/trunk/entityhub/yard/solr/src/main/resources/solr/core/paoding.solrindex.zip) to the "indexing/config" directory.

2. Copy the Paoding Bundle (<code>org.apache.stanbol:org.apache.stanbol.commons.solr.extras.paoding</code>) in the lib directory of the Solr Core configuration "indexing/config/paoding/lib". Solr includes all jar files within this directory in the Classpath. Because of that it will find the padding analyzer implementation during indexing.

3. Rename the "indexing/config/paoding" directory to the {site-name} (the value of the "name" property of the "indexing/config/indexing.properties" file).

    As an alternative to (2) you can also explicitly configure the name of the solr config as value to the "solrConf:smartcn" of SolrYardIndexingDestination.

        :::text
        indexingDestination=org.apache.stanbol.entityhub.indexing.destination.solryard.SolrYardIndexingDestination,solrConf:smartcn,boosts:fieldboosts

4. Copy the padding dictionary to '{paoding-dic-dir}'. You can obtain the dic from the original paoding projects [SVN repository](http://paoding.googlecode.com/svn/trunk/paoding-analysis/dic/). An [Zip archive](svn.apache.org/repos/asf/stanbol/trunk/launchers/bundlelists/language-extras/paoding/src/main/resources/paoding-dict.zip) with the dictionary is also included in the Paoding OSGI bundle part of Stanbol.

5. Correctly parse the -DPAODING_DIC_HOME={paoding-dic-dir} when calling the Entityhub indexing tool. As alternative you can also set the 'PAODING_DIC_HOME' as system environment variable.


### Usage with the Entityhub SolrYard

If you want to create an empty SolrYard instance using the [paoding.solrindex.zip](https://svn.apache.org/repos/asf/stanbol/trunk/entityhub/yard/solr/src/main/resources/solr/core/paoding.solrindex.zip) configuration you will need to

1. copy the paoding.solrindex.zip to the datafile directory of your Stanbol instance ({working-dir}/stanbol/datafiles)
2. rename it to the {name} of the SolrYard you want to create. The file name needs to be {name}.solrindex.zip
3. create the SolrYard instance and configure the "Solr Index/Core" (org.apache.stanbol.entityhub.yard.solr.solrUri) to {name}. Make sure the "Use default SolrCore configuration" (org.apache.stanbol.entityhub.yard.solr.useDefaultConfig) is disabled.

If you want to use the paoding.solrindex.zip as default you can rename the file in the datafilee folder to "default.solrindex.zip" and the enable the "Use default SolrCore configuration" (org.apache.stanbol.entityhub.yard.solr.useDefaultConfig) when you configure a SolrYard instance.

See also the documentation on how to [configure a managed site](http://stanbol.apache.org/docs/trunk/components/entityhub/managedsite#configuration-of-managedsites)).
