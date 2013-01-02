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

# geonames.org with local index for the Apache Stanbol Entityhub

This build a bundle that can be installed to add the [geonames.org](http://geonames.org/) 
data set as a ReferencedSite to the Apache Entityhub.

The binary data for the local cache are not included but need to be
downloaded (TODO: add download location as soon as available) or built locally
by using the geonames.org indexing utility.

## Installation

First build the bundle by calling

    mvn install

It the command succeeds the bundle is available in the target folder
    
    target/org.apache.stanbol.data.sites.geonames-.*.jar

This bundle can now be installed to a running Stanbol instance e.g. by using
the Apache Felix Webconsole.

NOTE: This steps requires the Sling Installer Framework as well as the 
Stanbol BundleInstaller extension to be active. Both are typically included
within the Stanbol Launcher.

After installing and starting this Bundle the Stanbol Data File Provider (a
tab within the Apache Felix Webconsole) will show a request for the binary
file for the local index.

To finalise the installation you need to copy the requested file to the
directory used by the Stanbol Data File Provider

    sling/datafiles/
    
and that restart the SolrYard instance with the name
    
    geonamesIndex
    
 
## Building the geonames.org index

To build a local Index for geonames.org the Apache Entityhub provides an own 
utility. The module is located at

    {stanbol}/entityhub/indexing/geonames

A detailed documentation on how to use this utility is provided by the
README file.
