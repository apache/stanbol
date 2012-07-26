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

LDPath implementation for the Entityhub
=======================================

This artifact adds LDPath support to the Entityhub. It contains RDFBackend
implementations for all Repository APIs available for the Entityhub.

* YardBackend: The Yard is the lower level Storage API of the Entityhub that
  does not require OSGI to run.
* SiteBackend: This is the RDFBackend implementation for ReferencedSite. This
  implementation is intended to be used for using LdPath with a single entity
  source registered with the entityhub.
* SiteManagerBackend: This RDFBackend implementation can be used to execute
  LDPath expressions on all ReferencedSites registered with the Entityhub
* EntityhubBackend: This can be used to use LDPath on locally managed Entities.
* QueryResultBackend: This Backend wraps an other RDFBackend and a query result
  and allows to perform LDPath on query results without requiring to lookup 
  Representations already selected by the query.


