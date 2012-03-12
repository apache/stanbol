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

Entityhub defaults
==================

This artifacts provides the default configuration for the Stanbol Enhancer.
It depends on the 

    org.apache.stanbol.commons.installer.bundleprovider

to be available as this module is used to actually load the provided configuration.

This [STANBOL-529](https://issues.apache.org/jira/browse/STANBOL-529) for more details.


## OSGI Event Admin configuration

The event job manager (org.apache.stanbol.enhancer.eventjobmanager) uses OSGI events to implement asynchronous execution of EnhancementChains. By default OSGI blacklists Components that consume/process event if they are not able to finish within 5sec. However in case of Enhancement Engines it is reasonable to disable this feature. This is ensured by adding 

    org.apache.felix.eventadmin.IgnoreTimeout=org.apache.stanbol.enhancer.jobmanager.event.impl.EnhancementJobHandler

to the configuration of the Apache Felix EventAdmin


## OpenNLP NER engine configuration

This includes a default configuration for the OpenNLP Named Entity Recognition engine. This configuration creates an engine that is activated for text in any language. The availability for a given language depends on available of OpenNLP NER models. Such models are not provided by this default configuration. You will need to include the bundles provided by "{stanbol}/data/opennlp/ner" or directly copying models to the /datafiles directory.

## Engine Configurations

Other than the NER engine this does not include any configurations of Enhancement Engines. For specific configurations of well known datasets please see the bundles provided by "{stanbol}/data/sites"


