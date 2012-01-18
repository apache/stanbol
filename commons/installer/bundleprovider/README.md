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

OSGI Bundle Provider
--------------------

Provider for the Apache Sling OSGI Installer Framework that installs Resources
provided by OSGI Bundles.

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
"Install-Path" key. The value is interpreted as path to the folder within the
bundle where all installable resources are located. Also resources in
sub-directories will be installed.

If a Bundle defining this key is 

* STARTED: all installable resources will be installed
* UNINSTALL: all installable resource will be uninstalled
* UPDATED: all installable resources will be first uninstalled and than installed

_NOTE:_ Precious versions uninstalled resources if a Bundle was STOPPED. See [STANBOL-464](https://issues.apache.org/jira/browse/STANBOL-464) for more information regarding the change to use UNINSTALL instead.

### Defining Manifest keys with Maven

When using the maven-bundle-plugin the "Install-Path" header can be defined
like this:

    <plugin>
        <groupId>org.apache.felix</groupId>
        <artifactId>maven-bundle-plugin</artifactId>
        ...
        <configuration>
            <instructions>
                <Install-Path>data/config</Install-Path>
            </instructions>
        </configuration>
     </plugin>

This would install all resrouces located within

    data/config/*

Note: that also Resource within sub-directories would be installed.
