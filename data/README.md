# Data files for optional extensions of the Stanbol distributions

This source repository holds the pom.xml file and folder structure to build
optional packages for Apache Stanbol.

To avoid loading subversion repository with large binary files this artifacts
are typically not included but need to be build/precomputed or downloaded
form other sites.
The the documentations of the according module for details.

## DataFileProvider Service

The DataFileProvoder Service is typically used by components that need to load
big binary files to Apache Stanbol.
See {stanbol-root}/commons/stanboltools/datafileprovider for details

## Bundleprovider

The Bundleprovider is an extension to the Apache Sling installer framework
and supports to load multiple configuration files form a single bundle.

It is intended to be used in cases where a single Stanbol module needs to
package several configuration files (e.g. the configuration of several OSGI
Services).

See {stanbol-root}/commons/installer/bundleprovider for details.


