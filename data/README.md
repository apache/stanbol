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

# Data files and configurations for Stanbol

## Introduction

This source repository holds artifacts that are used to provide 

* data files (NER models, Entity data ...)
* configurations of stanbol components or well known datasets, spefic domains

This also contains the default configuration used with the default Stanbol
Launchers. Have a look at the defaultdata and opennlp bundlelists in the
launchers/bundlelists folder. Generally this is a good place for users that
want to build their own launcher to look for examples.

## Management of Data Files 

To avoid loading subversion repository with large binary files this artifacts
are typically not included but need to be build/precomputed or downloaded form
other sites. The the documentations of the according module for details.

Modules of this repository tree are typically NOT part of the Stanbol reactor.
Because they are considered optional and typically it is necessary to
download/ precompute some resources users might not want to do for each build.

Bundles used as default configuration by the Stanbol Launchers are also
available by included Maven repositories and will be downloaded during the
normal Stanbol build (if not yet available in the local cache).

## OpenNLP

This sub-folder contains bundles that contain several OpenNLP models. Such
bundles will contribute such files to the Stanbol DataFileProvider.

## Sites

This sub-folder contains bundles that install ReferencedSites to the
Stanbol Entityhub. Typically such bundles only contain the configuration but
do not include the actual data. However for small data sets the index might
also be included in the bundle.
See the README.md files for details.

## Notes

Bundles created by the various modules depend on the following two components:

### DataFileProvider Service

The DataFileProvoder Service is typically used by components that need to load
big binary files to Apache Stanbol.
See {stanbol-root}/commons/stanboltools/datafileprovider for details

### Bundleprovider

The Bundleprovider is an extension to the Apache Sling installer framework
and supports to load multiple configuration files form a single bundle.

It is intended to be used in cases where a single Stanbol module needs to
package several configuration files (e.g. the configuration of several OSGI
Services).

See {stanbol-root}/commons/installer/bundleprovider for details.
