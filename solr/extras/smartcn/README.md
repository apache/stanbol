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


Bundle adding support for the Smartcn Analyzer 
=============================================

If installed to ApacheStanbol it will allow Solr Cores managed by Apache Stanbol ('org.apache.stanbol.commons.solr.core' module) to support fieldType definitions referring to SmartChinese analyzers

e.g.

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

Installing this bundle is required because Solr when running within OSGI can not load classes from Jar files located in the '{instanceDir}/lib' Directory.
