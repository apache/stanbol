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

Data File Provider
==================

The DataFileProvider provides data files (InputStreams actually) to 
OSGi components.

Usage
-----

Datafiles are requested by name. Optionally the symbolic name of the bundle
requesting the data file can be parsed. This allows to serve different versions
of the same data file to different bundles. In addition requesters can provide
a Map<String,Stirng> with additional information about the requested file.
Currently this information are only used for visualisation purposes, but future
versions might also bind specific actions to known keys.

Main DataFileProvider
---------------------

The MainDataFileProvider is the default implementation of the DataFileProvider
interface. It registers itself with a service.ranking of Integer.MAX_VALUE to 
make it the default provider. I also keeps a list of all the other 
DataFileProviders.

The main DataFileProvider ignores other providers if it finds the requested data 
file in the filesystem, in a specific folder ("datafiles folder"). 
The name of that folder is configurable in the main DataFileProvider service.
The default value is "sling/datafiles".

If it can not find the requested file it forwards the request to all other
active DataFileProvider instances sorted by service.ranking.

When a bundle with symbolic name foo asks for data file bar.bin, the main 
DataFileProvider first looks for a file named foo-bar.bin in the datafiles 
folder, then for a file named bar.bin. Only if both files could not be found the 
request is forwarded to the other registered DataFileProviders.

Providing DataFiles
-------------------

Bundles may provide there own DataFileProvider service. This might be useful
if they need to provide a default version for a data file, but intend to allow 
users to override this by copying an other version to the "datafiles folder"
of the Main DataFileProvider.

It they provide such a DataFileProvider service it must register it in its 
Activator, so that it's up before any component of the bundle asks for it.

If the  Bundle does not want to provide a file also to other bundles than it
should check if the parsed bundleSymbolicName is equals to its own.

[Bundle DataFileProvider](../bundledatafileprovider/README.md)
-----------------------

This extension allows to provide data files within a Bundle by using the
"Data-Files" key in the Bundles manifest to point to a comma separated list
of paths within the bundle that contains the data files.

OSGI Console Pligin
-------------------

An OSGi console plugin lists all (successful or failed) requests to the main 
DataFileProvider service, along with their downloadExplanations. This list of 
requests can also be queried so that failed requests can be shown on the stanbol 
server home page, for example. This provides a single location where stanbol 
users see what data files are needed and which ones were actually loaded from 
where.


Data File Tracker
=================

While the DataFileProvider only supports requests for resources the Tracker 
allows register DataFileListener for a specific DataFile.

If the requested DataFile becomes available or unavailable the listener is
notified about the state.

Because the DataFileProvider does not natively support such events the
tracker uses periodical requests for all tracked DataFiles.

Note that registered Listeners are not kept if the DataFileTracker service is 
restarted.
