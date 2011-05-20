# DBpedia.org with local index for the Apache Stanbol Entityhub

This build a bundle that can be installed to add DBpedia.org as a
ReferencedSite to the Apache Entityhub.

It will override the "dbpedia" referenced site included in the default
configuration of the "full" launcher of Apache Stanbol.

The binary data for the local cache are not included but need to be
downloaded (TODO: add download location as soon as available) or built locally
by using the DBpedia.org indexing utility.


## Installation

First build the bundle by calling

    mvn install

It the command succeeds the bundle is available in the target folder

    target/org.apache.stanbol.data.sites.dbpedia-.*.jar

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

    DBpediaIndex


## Building the DBpedia.org index

To build a local Index for DBPedia the Apache Entityhub provides an own utility
The module is located at

    {stanbol}/entityhub/indexing/dbpedia

A detailed documentation on how to use this utility is provided by the
README file.


