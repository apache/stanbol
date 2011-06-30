# Data files for the default Stanbol distributions

This source repository only holds the pom.xml file and folder structure to build
the org.apache.stanbol.defaultdata artifact to be included in the standard
distributions of stanbol.

To avoid loading subversion repository with large binary files this artifact has
to be build and deployed manually to retrieve precomputed models from other
sites.


## Downloading the OpenNLP statistical model files and pre-built Solr Index

Under Unix, use the `download_models.sh` script and then run `mvn install`.

Under windows, read the script content and do the same operations manually :)


## Building Entity Hub indices

See the online documentation: (TODO: put the URL here when no longer staging)
