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

Namespace Prefix Service
==================

The Namespace Prefix Service allows to provide and manage namespace prefix mappings. Prefixes are unique however multiple prefixes might be used for the same namespace.


## Architecture

This module defines two services:

1. __NamespacePrefixService__ - user level service used to lookup namespace prefixes. It also allows to set prefix mappings 
2. __NamespacePrefixProvider__ - internally used to provide prefix to namespace mappings 

### NamespacePrefixService

User level service used to lookup namespace prefixes. It also allows to set prefix mappings

    :::java
    NamespacePrefixService

        /** bidi mapping prefix - namespace */
        getNamespace(String prefix) : String
        getPrefix(String namespace) : String
        /** A namespace may be mapped to multiple prefixes */
        getPrefixes(String namespace) : List<String>
        /** adds an new Prefix and returns the old mapping */
        setPrefix(String prefix, String namespace) : String
    
        /** converts prefix:localName to full URIs */
        getFullName(String shortName) : String

        /** converts URIs to prefix:localName */
        getShortName(String uri) : String

### NamespacePrefixProvider

Service that provides namespace prefix mappings. Multiple of such services can be registered and will be used by the NamespacePrefixService. If a provider can not provide a mapping for a prefix/namespace, than it is expected to return null. The NamespacePrefixService will call provider in the order of their service.ranking when looking for mappings.

    :::java
    NamespacePrefixProvider

        getNamespace(String prefix) : String
        getPrefix(String namespace) : String
        /** A namespace may be mapped to multiple prefixes */
        getPrefixes(String namespace) : List<String> 