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
* STOPED: all installable resource will be uninstalled
* UPDATED: all installable resources will be first uninstalled and than installed


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
