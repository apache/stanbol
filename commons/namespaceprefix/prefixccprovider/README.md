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

Namespace Prefix Provider for prefix.cc
---------------------------------

This provides an implementation of the NamespacePrefixProvider interface that
provides all namespace prefixes defined/managed by [prefix.cc](http://prefix.cc).

This implementation can be used within and outside of an OSGI environment.
When running within OSGI it will deactivate itself if the Stanbol OfflineMode
is active. 

Mappings are periodically updated but hold locally in-memory. The default update
cycle is one hour, but can be configured by manually constructing an instance
or via the OSGI component configuration. 


