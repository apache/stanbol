<!--
Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the
NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF
licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file
except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing
permissions and limitations under the License.
-->

Apache Stanbol Commons Solr defaults
====================================

This artifacts provides the default configuration for Stanbol Commons Solr.
It depends on the 

    org.apache.stanbol.commons.installer.bundleprovider

to be available as this module is used to actually load the provided configuration.

This [STANBOL-529](https://issues.apache.org/jira/browse/STANBOL-529) for more details.

## Default ManagedSolrServer configuration

This includes a configuration for a ManagedSolrServer with the name "default"
and a "service.ranking" of Integer.MAX_VALUE. This ensures that this instance is 
used as default ManagedSolrServer.

The included configuration also enables the publishing of the RESTful interface 
by the SolrPublishingComponnt. This means that the RESTful API of all Solr cores
running on the default ManagedSolrServer will be published at

    http:{host}:{port}/solr/{core-name}


## Sling logger configuration for Apache Solr

As Apache Solr provides a lot og INFO level logging this includes a configuration
for the Apache Sling LogManager that sets the default log level for
Apache Solr to WARN.

Users that do not run Apache Stanbol with an Sling Launcher will need to provide
there own logger configuration. This configuration will be ignored in such
cases.