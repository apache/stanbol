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

# DBpedia.org with local cache

This build a bundle will install (configure) all required OSGI components to 
use the SPARQL Endpoint of DBPedia.org for query and retrieval. In addition a
local cache is configured that stores retrieved entities. 

This module is not part of the default build process.

## NOTE

One needs to uninstall/delete any other DBPedia.org configurations before using
this bundle. This is especially true for the default configuration for dbpedia
included in the Stanbol launchers.

Simple search for installed bundles starting with

    org.apache.stanbol.data.sites.dbpedia.*
    
and stop/remove them before activating this one.
