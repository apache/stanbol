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

DataFileProvider for OSGI Bundles
---------------------------------

Implemenation of a DataFileProvider that allows to load DataFiles from
OSGI Bundles.

### Apache Sling OSGI Installer

The [OSGi installer](http://sling.apache.org/site/osgi-installer.html) 
is a central service for handling installs, updates and  uninstall of "artifacts". 
By default, the installer supports bundles and configurations for the OSGi 
configuration admin. Apache Stanbol extends this by the possibility to install
Solr indexes (see "org.apache.stanbol.commons.solr.install" for details).

Note: While the Sling OSGI Installer by default supports the installation of 
Bundles this extension allows to install resources provided by bundles.

### Usage

This implementation tracks all Bundles of the OSGI Environment that define the
"Data-Files" key. The value is interpreted as a comma separated list of
paths to the folders that contain the data files.

For each Bundle that provides Data-Files an own DataFileProvider instance is
registered if the Bundle is STARTED and ungegisterd as soon as the Bundle is
STOPPED.
The MainDataFileProvider keeps track of all active DataFileProviders.

In addition to the "Data-Files" key an optional "Data-Files-Priority" can be
used to spefify the service-ranking for the DataFileProvider created for the
configured folders within a Bundle.
If a data file is provided by more than one DataFileProvider the one provided
by the DataFileProvider with the higest Ranking will be returned.
The default ranking is "0".

Data file names parsed to this DataFileProvider are interpreted as relative to
all configured data file paths. 

### Defining Manifest keys with Maven

When using the maven-bundle-plugin the "Install-Path" header can be defined
like this:

    <plugin>
        <groupId>org.apache.felix</groupId>
        <artifactId>maven-bundle-plugin</artifactId>
        ...
        <configuration>
            <instructions>
                <Data-Files>data,extras/data</Data-Files>
                <Data-Files-Priority>-100</Data-Files-Priority>
            </instructions>
        </configuration>
     </plugin>

This would install all data files located within

    data/
    extras/data
    
and will register them with a priority of "-100".
